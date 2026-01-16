package local.dev.lambdas;

import java.util.HashMap;
import java.util.Map;

public class UpdateFinancialsHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event) {
        Map<String, Object> input = (Map<String, Object>) event.getOrDefault("input", Map.of());

        String sessionId = (String) input.getOrDefault("sessionId", "unknown");
        Object requestedAmount = input.get("requestedAmount");
        Object termMonths = input.get("termMonths");
        Object monthlyRevenue = input.get("monthlyRevenue");

        Map<String, Object> out = new HashMap<>();
        out.put("status", "OK");
        out.put("tool", "updateFinancials");
        out.put("sessionId", sessionId);
        out.put("requestedAmount", requestedAmount);
        out.put("termMonths", termMonths);
        out.put("monthlyRevenue", monthlyRevenue);

        return out;
    }
}
