package local.dev.lambdas;

import java.util.HashMap;
import java.util.Map;

public class UpdatePersonalDetailsHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event) {
        Map<String, Object> input = (Map<String, Object>) event.getOrDefault("input", Map.of());

        String sessionId = (String) input.getOrDefault("sessionId", "unknown");
        String firstName = (String) input.get("firstName");
        String lastName  = (String) input.get("lastName");
        String email     = (String) input.get("email");

        Map<String, Object> out = new HashMap<>();
        out.put("status", "OK");
        out.put("tool", "updatePersonalDetails");
        out.put("sessionId", sessionId);

        // Echo back what would be stored later
        out.put("firstName", firstName);
        out.put("lastName", lastName);
        out.put("email", email);

        return out;
    }
}
