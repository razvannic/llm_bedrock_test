package local.dev.llm_bedrock_test.lending;

import local.dev.llm_bedrock_test.lending.tools.LendingToolExecutor;
import local.dev.llm_bedrock_test.lending.tools.LendingToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LendingAgentService {

    private final BedrockRuntimeClient bedrock;
    private final String modelId;
    private final LendingToolRegistry toolRegistry;
    private final LendingToolExecutor toolExecutor;
    private final DraftStore store;

    public LendingAgentService(
            BedrockRuntimeClient bedrock,
            @Value("${bedrock.modelId}") String modelId,
            LendingToolRegistry toolRegistry,
            LendingToolExecutor toolExecutor,
            DraftStore store
    ) {
        this.bedrock = bedrock;
        this.modelId = modelId;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.store = store;
    }

    public String chat(String sessionId, String userMessage) {
        ApplicationDraft draft = store.getOrCreate(sessionId);

        // 1) Build conversation messages (for now: just user + tool results).
        // Later you can persist history per session.
        List<Message> messages = new ArrayList<>();
        messages.add(userMsg(userMessage));

        // 2) System prompt and context
        SystemContentBlock system = SystemContentBlock.fromText(systemPrompt());
        // We pass state as an extra "system-like" instruction via another system block or preface.
        // Simplest: include it in system prompt string. (Works fine for a spike.)
        // If you prefer: add another SystemContentBlock with context.

        // 3) Tool config
        ToolConfiguration toolConfig = ToolConfiguration.builder()
                .tools(toolRegistry.tools())
                .toolChoice(ToolChoice.fromAuto(AutoToolChoice.builder().build()))
                .build();

        int maxTurns = 5; // prevent infinite loops

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
                // Return first text block (good enough for now)
                return assistant.content().stream()
                        .map(ContentBlock::text)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("(no text)");
            }

            // TOOL_USE: execute each tool request and send tool results back
            for (ContentBlock block : assistant.content()) {
                if (block.toolUse() == null) continue;
                ToolUseBlock toolUse = block.toolUse();

                String toolName = toolUse.name();
                String toolUseId = toolUse.toolUseId();
                Document input = toolUse.input();

                Document output = toolExecutor.execute(toolName, input);

                // refresh draft after tool updates
                draft = store.getOrCreate(sessionId);

                // send tool result back as a user message containing a toolResult block
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
                """;
    }

    private String contextBlock(String sessionId, ApplicationDraft d) {
        // Keep it simple + explicit. This is what “feeds info to the model”.
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
        // ToolResult content can be text or structured; we’ll send JSON-ish text for readability.
        // You can also return a DocumentBlock; keeping it simple for a spike.
        String outText = output.toString();

        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromToolResult(tr -> tr
                        .toolUseId(toolUseId)
                        .content(ToolResultContentBlock.fromText(outText))
                        .status(ToolResultStatus.SUCCESS)
                ))
                .build();
    }
}
