package local.dev.llm_bedrock_test.lending.agent;

import local.dev.llm_bedrock_test.lending.tools.ToolInvoker;
import local.dev.llm_bedrock_test.lending.tools.ToolNames;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;
import java.util.function.BiFunction;

@Component
public class ToolLoop {

    private final ToolInvoker toolInvoker;

    public ToolLoop(ToolInvoker toolInvoker) {
        this.toolInvoker = toolInvoker;
    }

    /**
     * Executes all toolUse blocks in the assistant message and appends toolResult messages to the conversation.
     *
     * @param assistantMessage the assistant message containing toolUse blocks
     * @param messages         conversation history list (will be appended with toolResult messages)
     * @param toolResultMsgFn  function that builds a toolResult message from (toolUseId, output)
     * @return number of tools executed
     */
    public int handleToolUse(Message assistantMessage,
                             List<Message> messages,
                             BiFunction<String, Document, Message> toolResultMsgFn) {

        int executed = 0;
        boolean statusUsedThisTurn = false; // prevents multiple getApplicationStatus calls in a single assistant turn

        for (ContentBlock block : assistantMessage.content()) {
            if (block.toolUse() == null) continue;

            ToolUseBlock toolUse = block.toolUse();
            String toolName = toolUse.name();

            // guard: only one getApplicationStatus per assistant turn
            if (ToolNames.GET_STATUS.equals(toolName)) {
                if (statusUsedThisTurn) {
                    continue; // ignore extra status calls
                }
                statusUsedThisTurn = true;
            }

            String toolUseId = toolUse.toolUseId();
            Document input = toolUse.input();

            Document output = toolInvoker.invoke(toolName, input);
            messages.add(toolResultMsgFn.apply(toolUseId, output));
            executed++;
        }

        return executed;
    }
}
