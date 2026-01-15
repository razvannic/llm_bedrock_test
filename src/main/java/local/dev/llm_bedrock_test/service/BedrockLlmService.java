package local.dev.llm_bedrock_test.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

@Service
@ConditionalOnProperty(name = "llm.mode", havingValue = "bedrock")
public class BedrockLlmService implements LlmService {

    private final BedrockRuntimeClient bedrock;
    private final String modelId;
    private final int maxTokens;
    private final float temperature;
    private final float topP;

    public BedrockLlmService(
            BedrockRuntimeClient bedrock,
            @Value("${bedrock.modelId}") String modelId,
            @Value("${bedrockInference.maxTokens:256}") int maxTokens,
            @Value("${bedrockInference.temperature:0.2}") float temperature,
            @Value("${bedrockInference.topP:0.9}") float topP
    ) {
        this.bedrock = bedrock;
        this.modelId = modelId;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.topP = topP;
    }

    @Override
    public String chat(String sessionId, String userMessage) {
        // Minimal “chat” shape (no history yet). Add history later.
        Message message = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(userMessage))
                .build();

        try {
            ConverseResponse response = bedrock.converse(req -> req
                    .modelId(modelId)
                    .messages(message)
                    .inferenceConfig(cfg -> cfg
                            .maxTokens(maxTokens)
                            .temperature(temperature)
                            .topP(topP)
                    )
            );

            // Extract first text block from the assistant output (AWS example pattern)
            // output().message().content() returns a list of ContentBlock
            return response.output().message().content().get(0).text();

        } catch (SdkClientException e) {
            // Keep this blunt while spiking; later map to nicer errors.
            throw new RuntimeException("Bedrock invoke failed for modelId=" + modelId + ": " + e.getMessage(), e);
        }
    }
}