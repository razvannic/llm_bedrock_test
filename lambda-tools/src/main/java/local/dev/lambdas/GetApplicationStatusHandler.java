package local.dev.lambdas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetApplicationStatusHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event) {
        // Expected input from Spring:
        // { "toolName": "...", "input": { "sessionId": "..." } }
        System.out.println("GetApplicationStatusHandler invoked");
        System.out.println("Event: " + event);
        System.out.println("CDK_DEPLOY_MARKER=v1");


        Map<String, Object> input =
                (Map<String, Object>) event.getOrDefault("input", Map.of());

        String sessionId = (String) input.getOrDefault("sessionId", "unknown");

        // For now: static response (prototype). Tomorrow: read DynamoDB/DB etc.
        Map<String, Object> out = new HashMap<>();
        out.put("status", "OK");
        out.put("step", "PERSONAL_DETAILS");
        out.put("missingFields", List.of("firstName", "lastName", "email"));
        out.put("sessionId", sessionId);
        out.put("toolName", event.getOrDefault("toolName", "unknown"));

        return out;
    }
}
