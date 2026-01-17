package local.dev.llm_bedrock_test.lending.tools;

import software.amazon.awssdk.core.document.Document;

import java.util.Set;

public final class ToolResultFactory {
    private ToolResultFactory() {}

    public static Document ok(String step, java.util.List<String> missingFields) {
        return Document.mapBuilder()
                .putString("status", "OK")
                .putString("step", step)
                .putList("missingFields", missingFields.stream().map(Document::fromString).toList())
                .build();
    }

    public static Document submitted(String step) {
        return Document.mapBuilder()
                .putString("status", "SUBMITTED")
                .putString("step", step)
                .build();
    }

    public static Document incomplete(String step, java.util.List<String> missingFields) {
        return Document.mapBuilder()
                .putString("status", "INCOMPLETE")
                .putString("step", step)
                .putList("missingFields", missingFields.stream().map(Document::fromString).toList())
                .build();
    }

    public static Document error(String code, String message) {
        return Document.mapBuilder()
                .putString("status", "ERROR")
                .putString("code", code)
                .putString("message", message)
                .build();
    }

    public static Document stepViolation(String currentStep, String toolName, Set<String> allowedTools) {
        Document.MapBuilder b = Document.mapBuilder()
                .putString("status", "ERROR")
                .putString("code", "STEP_VIOLATION")
                .putString("message", "Tool '" + toolName + "' is not allowed in step " + currentStep)
                .putString("currentStep", currentStep);

        if (allowedTools != null) {
            b.putList("allowedTools", allowedTools.stream().map(Document::fromString).toList());
        }
        return b.build();
    }
}