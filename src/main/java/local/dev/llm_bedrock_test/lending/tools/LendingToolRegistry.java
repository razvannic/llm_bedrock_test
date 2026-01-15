package local.dev.llm_bedrock_test.lending.tools;


import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.util.List;
import java.util.Map;

@Component
public class LendingToolRegistry {

    public static final String GET_STATUS = "getApplicationStatus";
    public static final String UPDATE_PERSONAL = "updatePersonalDetails";
    public static final String UPDATE_BUSINESS = "updateBusinessDetails";
    public static final String UPDATE_FINANCIALS = "updateFinancials";
    public static final String SUBMIT = "submitApplication";

    public List<Tool> tools() {
        return List.of(
                tool(GET_STATUS, "Return current application step and which fields are missing.", schemaGetStatus()),
                tool(UPDATE_PERSONAL, "Update personal details in the loan application draft.", schemaUpdatePersonal()),
                tool(UPDATE_BUSINESS, "Update business details in the loan application draft.", schemaUpdateBusiness()),
                tool(UPDATE_FINANCIALS, "Update financial details in the loan application draft.", schemaUpdateFinancials()),
                tool(SUBMIT, "Validate completeness and mark application as submitted if complete.", schemaSubmit())
        );
    }

    private Tool tool(String name, String description, Document jsonSchemaDoc) {
        ToolSpecification spec = ToolSpecification.builder()
                .name(name)
                .description(description)
                .inputSchema(ToolInputSchema.fromJson(jsonSchemaDoc)) // JSON schema as Document
                .build();
        return Tool.fromToolSpec(spec);
    }

    private Document schemaGetStatus() {
        return jsonObjectSchema(Map.of(
                "sessionId", jsonString()
        ), List.of("sessionId"));
    }

    private Document schemaUpdatePersonal() {
        return jsonObjectSchema(Map.of(
                "sessionId", jsonString(),
                "firstName", jsonString(),
                "lastName", jsonString(),
                "email", jsonString()
        ), List.of("sessionId"));
    }

    private Document schemaUpdateBusiness() {
        return jsonObjectSchema(Map.of(
                "sessionId", jsonString(),
                "companyName", jsonString(),
                "registrationId", jsonString()
        ), List.of("sessionId"));
    }

    private Document schemaUpdateFinancials() {
        return jsonObjectSchema(Map.of(
                "sessionId", jsonString(),
                "requestedAmount", jsonNumber(),
                "termMonths", jsonNumber(),
                "monthlyRevenue", jsonNumber()
        ), List.of("sessionId"));
    }

    private Document schemaSubmit() {
        return jsonObjectSchema(Map.of(
                "sessionId", jsonString()
        ), List.of("sessionId"));
    }

    // -------- helpers for JSON Schema (as Document) --------

    private Document jsonObjectSchema(Map<String, Document> properties, List<String> required) {
        Document.MapBuilder b = Document.mapBuilder()
                .putString("type", "object")
                .putMap("properties", properties)
                .putBoolean("additionalProperties", false);

        if (required != null && !required.isEmpty()) {
            b.putList("required", required.stream().map(Document::fromString).toList());
        }
        return b.build();
    }

    private Document jsonString() {
        return Document.mapBuilder().putString("type", "string").build();
    }

    private Document jsonNumber() {
        return Document.mapBuilder().putString("type", "number").build();
    }
}