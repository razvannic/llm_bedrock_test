package local.dev.llm_bedrock_test.lending.tools;

import software.amazon.awssdk.core.document.Document;

public interface ToolInvoker {
    Document invoke(String toolName, Document input);
}
