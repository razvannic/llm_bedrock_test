package local.dev.llm_bedrock_test.rest;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import local.dev.llm_bedrock_test.service.LlmService;

@RestController
@RequestMapping("/api")
@Validated
public class ChatController {

    private final LlmService llmService;

    public ChatController(LlmService llmService) {
        this.llmService = llmService;
    }

    public record ChatRequest(
            @NotBlank String sessionId,
            @NotBlank String message
    ) {}

    public record ChatResponse(String reply) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody @Validated ChatRequest req) {
        String reply = llmService.chat(req.sessionId(), req.message());
        return new ChatResponse(reply);
    }
}