package local.dev.llm_bedrock_test.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Configuration
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(
            @Value("${bedrock.region:eu-central-1}") String region
    ) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                // Uses AWS_PROFILE / default chain (works with your "personal" profile)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void logAwsEnv() {
        System.out.println("AWS_PROFILE=" + System.getenv("AWS_PROFILE"));
        System.out.println("AWS_REGION=" + System.getenv("AWS_REGION"));
    }
}
