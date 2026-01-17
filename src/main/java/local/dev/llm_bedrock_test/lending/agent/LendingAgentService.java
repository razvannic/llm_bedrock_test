package local.dev.llm_bedrock_test.lending.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.llm_bedrock_test.lending.state.ApplicationDraft;
import local.dev.llm_bedrock_test.lending.state.DraftStore;
import local.dev.llm_bedrock_test.lending.tools.LendingToolRegistry;
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
    private final ToolLoop toolLoop;
    private final DraftStore store;
    private final ObjectMapper om = new ObjectMapper();

    public LendingAgentService(
            BedrockRuntimeClient bedrock,
            @Value("${bedrock.modelId}") String modelId,
            LendingToolRegistry toolRegistry,
            ToolLoop toolLoop,
            DraftStore store
    ) {
        this.bedrock = bedrock;
        this.modelId = modelId;
        this.toolRegistry = toolRegistry;
        this.toolLoop = toolLoop;
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

        int maxTurns = 6; // slightly higher now that status spam is limited

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

            // Execute tool calls via ToolLoop
            toolLoop.handleToolUse(
                    assistant,
                    messages,
                    this::toolResultMsg
            );

            // Refresh local state after tool execution(s)
            draft = store.getOrCreate(sessionId);
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

                After any tool call, you MUST:
                - Read the authoritative CONTEXT.currentStep and knownFields.
                - Ask for the NEXT required field for that step (exactly one question).
                - Never ask "What would you like to do next?"

                Step guidance:
                - PERSONAL_DETAILS: ask for firstName, then lastName, then email.
                - BUSINESS_DETAILS: ask for companyName, then registrationId.
                - FINANCIALS: ask for requestedAmount, then termMonths, then monthlyRevenue.
                - REVIEW_SUBMIT: summarize collected data and ask "Do you want to submit?".
                - SUBMITTED: confirm submission and next steps.

                Do not call getApplicationStatus more than once in a single turn.
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

    private String safe(String s) {
        return s == null ? "null" : s;
    }

    private Message userMsg(String text) {
        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(text))
                .build();
    }

    private Message toolResultMsg(String toolUseId, Document output) {
        ToolResultStatus resolvedStatus = mapToolResultStatus(output);
        String resolvedJson;

        try {
            resolvedJson = om.writeValueAsString(toPlainJava(output));
        } catch (Exception e) {
            resolvedJson = "{\"status\":\"ERROR\",\"code\":\"TOOL_RESULT_SERIALIZE_FAILED\",\"message\":\"" +
                    e.getMessage().replace("\"", "'") + "\"}";
            resolvedStatus = ToolResultStatus.ERROR;
        }

        final String outJson = resolvedJson;
        final ToolResultStatus status = resolvedStatus;

        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromToolResult(tr -> tr
                        .toolUseId(toolUseId)
                        .content(ToolResultContentBlock.fromText(outJson))
                        .status(status)
                ))
                .build();
    }

    private Object toPlainJava(Document d) {
        if (d == null || d.isNull()) return null;
        if (d.isString()) return d.asString();
        if (d.isNumber()) return d.asNumber();
        if (d.isBoolean()) return d.asBoolean();
        if (d.isList()) return d.asList().stream().map(this::toPlainJava).toList();
        if (d.isMap()) {
            return d.asMap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> toPlainJava(e.getValue())
                    ));
        }
        return d.toString();
    }

    private ToolResultStatus mapToolResultStatus(Document output) {
        try {
            if (output == null || !output.isMap()) return ToolResultStatus.SUCCESS;
            Map<String, Document> m = output.asMap();
            Document s = m.get("status");
            if (s == null || !s.isString()) return ToolResultStatus.SUCCESS;

            String status = s.asString();
            if ("ERROR".equalsIgnoreCase(status)) return ToolResultStatus.ERROR;
            return ToolResultStatus.SUCCESS;
        } catch (Exception ignored) {
            return ToolResultStatus.SUCCESS;
        }
    }
}
