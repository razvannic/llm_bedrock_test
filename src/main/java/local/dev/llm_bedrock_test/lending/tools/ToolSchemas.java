package local.dev.llm_bedrock_test.lending.tools;

import software.amazon.awssdk.core.document.Document;

import java.util.List;
import java.util.Map;

public final class ToolSchemas {
    private ToolSchemas() {
    }

    public static Document getStatus() {
        return objectSchema(Map.of("sessionId", string()), List.of("sessionId"));
    }

    public static Document updatePersonal() {
        return objectSchema(Map.of(
                "sessionId", string(),
                "firstName", string(),
                "lastName", string(),
                "email", string()
        ), List.of("sessionId"));
    }

    public static Document updateBusiness() {
        return objectSchema(Map.of(
                "sessionId", string(),
                "companyName", string(),
                "registrationId", string()
        ), List.of("sessionId"));
    }

    public static Document updateFinancials() {
        return objectSchema(Map.of(
                "sessionId", string(),
                "requestedAmount", number(),
                "termMonths", number(),
                "monthlyRevenue", number()
        ), List.of("sessionId"));
    }

    public static Document submit() {
        return objectSchema(Map.of("sessionId", string()), List.of("sessionId"));
    }

    private static Document objectSchema(Map<String, Document> properties, List<String> required) {
        Document.MapBuilder b = Document.mapBuilder()
                .putString("type", "object")
                .putMap("properties", properties)
                .putBoolean("additionalProperties", false);

        if (required != null && !required.isEmpty()) {
            b.putList("required", required.stream().map(Document::fromString).toList());
        }
        return b.build();
    }

    private static Document string() {
        return Document.mapBuilder().putString("type", "string").build();
    }

    private static Document number() {
        return Document.mapBuilder().putString("type", "number").build();
    }
}