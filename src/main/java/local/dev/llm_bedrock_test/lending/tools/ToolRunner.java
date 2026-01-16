package local.dev.llm_bedrock_test.lending.tools;

import software.amazon.awssdk.core.document.Document;

public interface ToolRunner {
    Document run(String toolName, Document input);
}