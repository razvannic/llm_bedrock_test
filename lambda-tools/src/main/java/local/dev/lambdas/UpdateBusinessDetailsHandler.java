package local.dev.lambdas;

import java.util.HashMap;
import java.util.Map;

public class UpdateBusinessDetailsHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event) {
        Map<String, Object> input = (Map<String, Object>) event.getOrDefault("input", Map.of());

        String sessionId = (String) input.getOrDefault("sessionId", "unknown");
        String companyName = (String) input.get("companyName");
        String registrationId = (String) input.get("registrationId");

        Map<String, Object> out = new HashMap<>();
        out.put("status", "OK");
        out.put("tool", "updateBusinessDetails");
        out.put("sessionId", sessionId);
        out.put("companyName", companyName);
        out.put("registrationId", registrationId);

        return out;
    }
}
