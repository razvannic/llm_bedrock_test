package local.dev.llm_bedrock_test.lending;

import local.dev.llm_bedrock_test.lending.tools.LendingToolRegistry;
import local.dev.llm_bedrock_test.lending.tools.ToolRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LendingAgentService {

    private final BedrockRuntimeClient bedrock;
    private final String modelId;
    private final LendingToolRegistry toolRegistry;
    private final ToolRunner toolRunner;
    //private final LendingToolExecutor toolExecutor;
    private final DraftStore store;

    public LendingAgentService(
            BedrockRuntimeClient bedrock,
            @Value("${bedrock.modelId}") String modelId,
            LendingToolRegistry toolRegistry,
            //            LendingToolExecutor toolExecutor,
            ToolRunner toolRunner,
            DraftStore store
    ) {
        this.bedrock = bedrock;
        this.modelId = modelId;
        this.toolRegistry = toolRegistry;
        this.toolRunner = toolRunner;
        this.store = store;
    }

    public String chat(String sessionId, String userMessage) {
        ApplicationDraft draft = store.getOrCreate(sessionId);

        List<Message> messages = new ArrayList<>();
        messages.add(userMsg(userMessage));

        ToolConfiguration toolConfig = ToolConfiguration.builder()
                .tools(toolRegistry.tools())
                .toolChoice(ToolChoice.fromAuto(AutoToolChoice.builder().build()))
                .build();

        int maxTurns = 5;

        for (int i = 0; i < maxTurns; i++) {
            String context = contextBlock(sessionId, draft);

            ConverseResponse res = bedrock.converse(r -> r
                    .modelId(modelId)
                    .system(SystemContentBlock.fromText(systemPrompt() + "\n\n" + context))
                    .messages(messages)
                    .toolConfig(toolConfig)
                    .inferenceConfig(cfg -> cfg.maxTokens(300).temperature(0.2f).topP(0.9f))
            );

            Message assistant = res.output().message();
            messages.add(assistant);

            if (res.stopReason() != StopReason.TOOL_USE) {
                return assistant.content().stream()
                        .map(ContentBlock::text)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("(no text)");
            }

            // TOOL_USE: execute each tool request
            for (ContentBlock block : assistant.content()) {
                if (block.toolUse() == null) continue;

                ToolUseBlock toolUse = block.toolUse();
                String toolName = toolUse.name();
                String toolUseId = toolUse.toolUseId();
                Document input = toolUse.input();

                Document output = toolRunner.run(toolName, input);

                // refresh draft after tool updates (local store may not change if tool is remote)
                draft = store.getOrCreate(sessionId);

                messages.add(toolResultMsg(toolUseId, output));
            }
        }

        return "Sorry — I hit a tool loop limit. Please try again.";
    }

    private String systemPrompt() {
        return """
                You are a lending assistant helping a user apply for a business loan through a step-by-step dialogue.

                Rules:
                - Never approve/deny a loan. Never invent data.
                - Use tools to read/update the application. Do NOT assume fields are saved unless a tool confirms it.
                - Ask ONE clear question at a time to gather missing fields.
                - Follow the application step shown in CONTEXT. Don't skip ahead.
                - When the user provides data, call the appropriate update tool.
                - If the user asks "where am I", call getApplicationStatus.
                - If a tool returns status=ERROR or status=INCOMPLETE, explain what is missing and ask for the next required field.
                """;
    }

    private String contextBlock(String sessionId, ApplicationDraft d) {
        return """
                CONTEXT (authoritative, from backend):
                sessionId=%s
                currentStep=%s
                knownFields:
                  firstName=%s
                  lastName=%s
                  email=%s
                  companyName=%s
                  registrationId=%s
                  requestedAmount=%s
                  termMonths=%s
                  monthlyRevenue=%s
                """.formatted(
                sessionId,
                d.getStep().name(),
                safe(d.getFirstName()),
                safe(d.getLastName()),
                safe(d.getEmail()),
                safe(d.getCompanyName()),
                safe(d.getRegistrationId()),
                d.getRequestedAmount() == null ? "null" : d.getRequestedAmount().toPlainString(),
                d.getTermMonths() == null ? "null" : d.getTermMonths().toString(),
                d.getMonthlyRevenue() == null ? "null" : d.getMonthlyRevenue().toPlainString()
        );
    }

    private String safe(String s) { return s == null ? "null" : s; }

    private Message userMsg(String text) {
        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(text))
                .build();
    }

    private Message toolResultMsg(String toolUseId, Document output) {
        String outText = output == null ? "null" : output.toString();
        ToolResultStatus status = mapToolResultStatus(output);

        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromToolResult(tr -> tr
                        .toolUseId(toolUseId)
                        .content(ToolResultContentBlock.fromText(outText))
                        .status(status)
                ))
                .build();
    }

    private ToolResultStatus mapToolResultStatus(Document output) {
        try {
            if (output == null || !output.isMap()) return ToolResultStatus.SUCCESS;
            Map<String, Document> m = output.asMap();
            Document s = m.get("status");
            if (s == null || !s.isString()) return ToolResultStatus.SUCCESS;

            String status = s.asString();
            if ("ERROR".equalsIgnoreCase(status)) return ToolResultStatus.ERROR;
            return ToolResultStatus.SUCCESS; // INCOMPLETE still counts as SUCCESS for tool execution
        } catch (Exception ignored) {
            return ToolResultStatus.SUCCESS;
        }
    }
}
