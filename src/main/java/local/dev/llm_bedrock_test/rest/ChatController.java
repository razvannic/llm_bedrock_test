package local.dev.llm_bedrock_test.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String reply) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {
        return new ChatResponse("Echo: " + req.message() + " (session=" + req.sessionId() + ")");
    }
}
