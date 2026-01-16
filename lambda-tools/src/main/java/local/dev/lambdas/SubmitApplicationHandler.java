package local.dev.lambdas;

import java.util.HashMap;
import java.util.Map;

public class SubmitApplicationHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event) {
        Map<String, Object> input = (Map<String, Object>) event.getOrDefault("input", Map.of());
        String sessionId = (String) input.getOrDefault("sessionId", "unknown");

        // Stateless placeholder (later: write to DB + emit events)
        Map<String, Object> out = new HashMap<>();
        out.put("status", "SUBMITTED");
        out.put("tool", "submitApplication");
        out.put("sessionId", sessionId);
        out.put("applicationId", "APP-" + sessionId);

        return out;
    }
}
