package local.dev.llm_bedrock_test.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Configuration
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void logAwsEnv() {
        System.out.println("AWS_PROFILE=" + System.getenv("AWS_PROFILE"));
        System.out.println("AWS_REGION=" + System.getenv("AWS_REGION"));
    }
}
