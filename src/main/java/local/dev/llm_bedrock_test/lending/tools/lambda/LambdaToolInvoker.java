package local.dev.llm_bedrock_test.lending.tools.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.llm_bedrock_test.lending.tools.ToolInvoker;
import local.dev.llm_bedrock_test.lending.tools.ToolResultFactory;
import local.dev.llm_bedrock_test.lending.tools.ToolStateMirror;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tools.mode", havingValue = "lambda")
public class LambdaToolInvoker implements ToolInvoker {

    private final LambdaClient lambda;
    private final LambdaFunctionMap fnMap;
    private final ToolStateMirror mirror;
    private final ObjectMapper om = new ObjectMapper();

    public LambdaToolInvoker(LambdaClient lambda, LambdaFunctionMap fnMap, ToolStateMirror mirror) {
        this.lambda = lambda;
        this.fnMap = fnMap;
        this.mirror = mirror;
    }

    @Override
    public Document invoke(String toolName, Document input) {
        String fn = fnMap.functionNameForTool(toolName);
        if (fn == null) {
            return ToolResultFactory.error("NO_LAMBDA_MAPPING", "No Lambda mapping for tool: " + toolName);
        }

        try {
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
                return ToolResultFactory.error("LAMBDA_FUNCTION_ERROR", resp.functionError() + ": " + outJson);
            }

            JsonNode node = om.readTree(outJson);
            Document out = jsonNodeToDocument(node);

            // Mirror state locally ONLY if tool execution succeeded (status != ERROR)
            if (!isError(out)) {
                mirror.apply(toolName, input);
            }

            return out;

        } catch (Exception e) {
            return ToolResultFactory.error("LAMBDA_INVOKE_FAILED", e.getMessage());
        }
    }

    private boolean isError(Document out) {
        if (out == null || !out.isMap()) return false;
        Document status = out.asMap().get("status");
        return status != null && status.isString() && "ERROR".equalsIgnoreCase(status.asString());
    }

    private Object toPlainJava(Document d) {
        if (d == null || d.isNull()) return null;
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
        if (node.isNumber()) return Document.fromNumber(node.decimalValue());

        return Document.fromString(node.asText());
    }
}
