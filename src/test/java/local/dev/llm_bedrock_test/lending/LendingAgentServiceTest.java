package local.dev.llm_bedrock_test.lending;

import local.dev.llm_bedrock_test.lending.tools.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LendingAgentServiceTest {

    /*
    Here you mock BedrockRuntimeClient and simulate a model response:

    First converse() returns StopReason.TOOL_USE with a toolUse block.

    Second converse() returns normal assistant text.

    This tests:
     “When model requests tool X, we execute it and continue”
     “We return final text once tool calls are done”
     */

    @Test
    void agentExecutesToolThenReturnsFinalText() {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);

        DraftStore store = new DraftStore();
        LendingToolRegistry registry = new LendingToolRegistry();
//        LendingToolExecutor executor = new LendingToolExecutor(store);
        ToolRunner toolRunner =  mock(ToolRunner.class);
        LendingStepPolicy stepPolicy = new LendingStepPolicy();
        LendingStateReducer reducer = new LendingStateReducer();

        LendingAgentService agent = new LendingAgentService(
                bedrock,
                "dummy-model",
                registry,
                toolRunner,
                reducer,
                stepPolicy,
                store
        );

        // 1) First response: TOOL_USE (updatePersonalDetails)
        ToolUseBlock toolUse = ToolUseBlock.builder()
                .toolUseId("tu-1")
                .name(LendingToolRegistry.UPDATE_PERSONAL)
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
                .stopReason(StopReason.END_TURN) // or null depending on SDK behavior; END_TURN is fine
                .output(ConverseOutput.builder().message(assistantFinal).build())
                .build();

        when(bedrock.converse(ArgumentMatchers.any(java.util.function.Consumer.class)))
                .thenReturn(first)
                .thenReturn(second);


        String reply = agent.chat("s1", "Hi, I need a business loan. My name is Razvan Nicolae, email razvan@test.com");

        assertEquals("Thanks — now tell me your company name.", reply);

        // Verify backend state progressed due to tool execution
        ApplicationDraft d = store.getOrCreate("s1");
        assertEquals(ApplicationDraft.Step.BUSINESS_DETAILS, d.getStep());
        assertEquals("Razvan", d.getFirstName());
    }
}
