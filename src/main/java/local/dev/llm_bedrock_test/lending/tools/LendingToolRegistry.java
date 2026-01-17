package local.dev.llm_bedrock_test.lending.tools;


import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.util.List;

@Component
public class LendingToolRegistry {

//    public static final String GET_STATUS = "getApplicationStatus";
//    public static final String UPDATE_PERSONAL = "updatePersonalDetails";
//    public static final String UPDATE_BUSINESS = "updateBusinessDetails";
//    public static final String UPDATE_FINANCIALS = "updateFinancials";
//    public static final String SUBMIT = "submitApplication";

    public List<Tool> tools() {
        return List.of(
                tool(ToolNames.GET_STATUS,
                        "Return current application step and which fields are missing.", ToolSchemas.getStatus()),
                tool(ToolNames.UPDATE_PERSONAL,
                        "Update personal details in the loan application draft.", ToolSchemas.updatePersonal()),
                tool(ToolNames.UPDATE_BUSINESS,
                        "Update business details in the loan application draft.", ToolSchemas.updateBusiness()),
                tool(ToolNames.UPDATE_FINANCIALS,
                        "Update financial details in the loan application draft.", ToolSchemas.updateFinancials()),
                tool(ToolNames.SUBMIT,
                        "Validate completeness and mark application as submitted if complete.", ToolSchemas.submit())
        );
    }

    private Tool tool(String name, String description, Document jsonSchemaDoc) {
        ToolSpecification spec = ToolSpecification.builder()
                .name(name)
                .description(description)
                .inputSchema(ToolInputSchema.fromJson(jsonSchemaDoc)) // JSON schema as Document
                .build();
        return Tool.fromToolSpec(spec);
    }
}