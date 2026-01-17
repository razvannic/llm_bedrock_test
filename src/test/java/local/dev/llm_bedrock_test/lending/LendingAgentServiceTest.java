package local.dev.llm_bedrock_test.lending;

import local.dev.llm_bedrock_test.lending.agent.LendingAgentService;
import local.dev.llm_bedrock_test.lending.agent.ToolLoop;
import local.dev.llm_bedrock_test.lending.state.ApplicationDraft;
import local.dev.llm_bedrock_test.lending.state.DraftStore;
import local.dev.llm_bedrock_test.lending.state.LendingStateReducer;
import local.dev.llm_bedrock_test.lending.state.LendingStepPolicy;
import local.dev.llm_bedrock_test.lending.tools.LendingToolExecutor;
import local.dev.llm_bedrock_test.lending.tools.LendingToolRegistry;
import local.dev.llm_bedrock_test.lending.tools.ToolInvoker;
import local.dev.llm_bedrock_test.lending.tools.local.LocalToolInvoker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.Map;

import static local.dev.llm_bedrock_test.lending.tools.ToolNames.UPDATE_PERSONAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LendingAgentServiceTest {

    @Test
    void agentExecutesToolThenReturnsFinalText() {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);

        DraftStore store = new DraftStore();
        LendingToolRegistry registry = new LendingToolRegistry();

        // Real local tool path (updates DraftStore + step transitions)
        LendingStepPolicy stepPolicy = new LendingStepPolicy();
        LendingStateReducer reducer = new LendingStateReducer(); // if your reducer is used elsewhere, keep; executor already advances steps
        LendingToolExecutor executor = new LendingToolExecutor(store, stepPolicy);
        ToolInvoker toolInvoker = new LocalToolInvoker(executor);
        ToolLoop toolLoop = new ToolLoop(toolInvoker);

        LendingAgentService agent = new LendingAgentService(
                bedrock,
                "dummy-model",
                registry,
                toolLoop,
                store
        );

        // 1) First response: TOOL_USE
        ToolUseBlock toolUse = ToolUseBlock.builder()
                .toolUseId("tu-1")
                .name(UPDATE_PERSONAL)
                .input(Document.fromMap(Map.of(
                        "sessionId", Document.fromString("s1"),
                        "firstName", Document.fromString("Razvan"),
                        "lastName", Document.fromString("Nicolae"),
                        "email", Document.fromString("razvan@test.com")
                )))
                .build();

        Message assistantToolRequest = Message.builder()
                .role(ConversationRole.ASSISTANT)
                .content(ContentBlock.fromToolUse(toolUse))
                .build();

        ConverseResponse first = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder().message(assistantToolRequest).build())
                .build();

        // 2) Second response: final text
        Message assistantFinal = Message.builder()
                .role(ConversationRole.ASSISTANT)
                .content(ContentBlock.fromText("Thanks — now tell me your company name."))
                .build();

        ConverseResponse second = ConverseResponse.builder()
                .stopReason(StopReason.END_TURN)
                .output(ConverseOutput.builder().message(assistantFinal).build())
                .build();

        when(bedrock.converse(ArgumentMatchers.any(java.util.function.Consumer.class)))
                .thenReturn(first)
                .thenReturn(second);

        String reply = agent.chat("s1", "Hi, I need a business loan.");

        assertEquals("Thanks — now tell me your company name.", reply);

        // Verify state progressed (because LocalToolInvoker->Executor updated DraftStore)
        ApplicationDraft d = store.getOrCreate("s1");
        assertEquals(ApplicationDraft.Step.BUSINESS_DETAILS, d.getStep());
        assertEquals("Razvan", d.getFirstName());
    }
}
