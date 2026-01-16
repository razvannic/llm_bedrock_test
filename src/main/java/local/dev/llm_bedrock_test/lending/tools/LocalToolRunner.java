package local.dev.llm_bedrock_test.lending.tools;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

@Component
@ConditionalOnProperty(name = "tools.mode", havingValue = "local", matchIfMissing = true)
public class LocalToolRunner implements ToolRunner {

    private final LendingToolExecutor executor;

    public LocalToolRunner(LendingToolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Document run(String toolName, Document input) {
        return executor.execute(toolName, input);
    }
}