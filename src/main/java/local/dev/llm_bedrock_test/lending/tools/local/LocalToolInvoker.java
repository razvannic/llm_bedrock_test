package local.dev.llm_bedrock_test.lending.tools.local;

import local.dev.llm_bedrock_test.lending.tools.ToolInvoker;
import local.dev.llm_bedrock_test.lending.tools.LendingToolExecutor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "tools.mode", havingValue = "local", matchIfMissing = true)
public class LocalToolInvoker implements ToolInvoker {

    private final LendingToolExecutor executor;

    public LocalToolInvoker(LendingToolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Document invoke(String toolName, Document input) {
        return executor.execute(toolName, input);
    }
}
