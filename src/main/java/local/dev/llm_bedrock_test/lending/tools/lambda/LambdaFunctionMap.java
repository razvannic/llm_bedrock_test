package local.dev.llm_bedrock_test.lending.tools.lambda;


import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LambdaFunctionMap {

    private final Map<String, String> functions;

    public LambdaFunctionMap(LambdaFunctionProperties props) {
        this.functions = props.getFunctions();
    }

    public String functionNameForTool(String toolName) {
        return functions.get(toolName);
    }
}