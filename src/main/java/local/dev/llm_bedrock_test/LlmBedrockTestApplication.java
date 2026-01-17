package local.dev.llm_bedrock_test;

import local.dev.llm_bedrock_test.lending.tools.lambda.LambdaFunctionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LambdaFunctionProperties.class)
public class LlmBedrockTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmBedrockTestApplication.class, args);
	}

}
