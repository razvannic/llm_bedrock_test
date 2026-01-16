package local.dev.llm_bedrock_test.lending.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tools.mode", havingValue = "lambda")
public class LambdaToolRunner implements ToolRunner {

    private final LambdaClient lambda;
    private final ObjectMapper om = new ObjectMapper();

    // map tool name -> lambda function name (you can externalize to config later)
    private final Map<String, String> toolToFunction = Map.of(
            LendingToolRegistry.GET_STATUS, "loan-getApplicationStatus",
            LendingToolRegistry.UPDATE_PERSONAL, "loan-updatePersonalDetails",
            LendingToolRegistry.UPDATE_BUSINESS, "loan-updateBusinessDetails",
            LendingToolRegistry.UPDATE_FINANCIALS, "loan-updateFinancials",
            LendingToolRegistry.SUBMIT, "loan-submitApplication"
    );

    public LambdaToolRunner(LambdaClient lambda) {
        this.lambda = lambda;
    }

    @Override
    public Document run(String toolName, Document input) {
        String fn = toolToFunction.get(toolName);
        if (fn == null) {
            return Document.mapBuilder()
                    .putString("status", "ERROR")
                    .putString("code", "NO_LAMBDA_MAPPING")
                    .putString("message", "No Lambda mapping for tool: " + toolName)
                    .build();
        }

        try {
            // payload: {"toolName":"...", "input": {...}}
            String payloadJson = om.writeValueAsString(Map.of(
                    "toolName", toolName,
                    "input", toPlainJava(input)
            ));

            InvokeResponse resp = lambda.invoke(InvokeRequest.builder()
                    .functionName(fn)
                    .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                    .build());

            String outJson = resp.payload().asString(StandardCharsets.UTF_8);

            if (resp.functionError() != null) {
                return Document.mapBuilder()
                        .putString("status", "ERROR")
                        .putString("code", "LAMBDA_FUNCTION_ERROR")
                        .putString("message", resp.functionError() + ": " + outJson)
                        .build();
            }

            // Expect Lambda to return {"status":"OK", ...} as JSON
            JsonNode node = om.readTree(outJson);
            return jsonNodeToDocument(node);

        } catch (Exception e) {
            return Document.mapBuilder()
                    .putString("status", "ERROR")
                    .putString("code", "LAMBDA_INVOKE_FAILED")
                    .putString("message", e.getMessage())
                    .build();
        }
    }

    /**
     * Convert SDK Document to plain Java Map/List/String/Number so Jackson can serialize cleanly.
     */
    private Object toPlainJava(Document d) {
        if (d == null) return null;
        if (d.isNull()) return null;
        if (d.isString()) return d.asString();
        if (d.isNumber()) return d.asNumber();
        if (d.isBoolean()) return d.asBoolean();
        if (d.isList()) return d.asList().stream().map(this::toPlainJava).toList();
        if (d.isMap()) {
            return d.asMap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> toPlainJava(e.getValue())
                    ));
        }
        return d.toString();
    }

    private Document jsonNodeToDocument(JsonNode node) {
        if (node == null || node.isNull()) return Document.fromNull();

        if (node.isObject()) {
            Document.MapBuilder mb = Document.mapBuilder();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                mb.putDocument(e.getKey(), jsonNodeToDocument(e.getValue()));
            }
            return mb.build();
        }

        if (node.isArray()) {
            Document.ListBuilder lb = Document.listBuilder();
            for (JsonNode item : node) {
                lb.addDocument(jsonNodeToDocument(item));
            }
            return lb.build();
        }

        if (node.isTextual()) return Document.fromString(node.asText());
        if (node.isBoolean()) return Document.fromBoolean(node.asBoolean());

        if (node.isNumber()) {
            // Use BigDecimal to keep precision for money-like values
            BigDecimal bd = node.decimalValue();
            return Document.fromNumber(bd);
        }

        // fallback
        return Document.fromString(node.asText());
    }
}
