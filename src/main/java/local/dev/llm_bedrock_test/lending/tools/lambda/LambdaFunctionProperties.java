package local.dev.llm_bedrock_test.lending.tools.lambda;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "lambda")
public class LambdaFunctionProperties {

    private Map<String, String> functions = new HashMap<>();

    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }
}
