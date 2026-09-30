package org.namewta.test.profile.contract;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.profile.person.controller.self.PersonApplicationController;
import org.namewta.profile.person.controller.self.PersonTaskReviewController;
import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.controller.self.EnterpriseTaskReviewController;
import org.namewta.workflow.domain.vo.FlowHisTaskVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/** 从实际 Controller 和 Java 模型导出增量合同，不手写或修改生成的 TypeScript。 */
@Tag("dev")
class ProfileReviewOpenApiContractTest {
    @Test
    void exportsActualSummaryAndTaskReviewContracts() throws Exception {
        OpenAPI api = new OpenAPI().openapi("3.0.1").paths(new Paths())
            .components(new Components().schemas(new TreeMap<>()));
        for (Class<?> controller : List.of(PersonApplicationController.class, EnterpriseApplicationController.class,
            PersonTaskReviewController.class, EnterpriseTaskReviewController.class)) {
            for (Method method : controller.getDeclaredMethods()) {
                var documented = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
                if (documented == null) continue;
                String base = controller.getAnnotation(RequestMapping.class).value()[0];
                GetMapping get = method.getAnnotation(GetMapping.class);
                PostMapping post = method.getAnnotation(PostMapping.class);
                String suffix = get != null ? get.value()[0] : post.value()[0];
                Operation operation = new Operation().operationId(documented.operationId());
                operation.responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")
                    .content(content(schema(api, method.getGenericReturnType())))));
                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    PathVariable variable = parameter.getAnnotation(PathVariable.class);
                    if (variable != null) {
                        String name = variable.value().isBlank() ? parameter.getName() : variable.value();
                        operation.addParametersItem(new Parameter().name(name).in("path").required(true)
                            .schema(schema(api, parameter.getParameterizedType())));
                    }
                    if (parameter.isAnnotationPresent(org.springframework.web.bind.annotation.RequestBody.class)) {
                        operation.requestBody(new RequestBody().required(true)
                            .content(content(schema(api, parameter.getParameterizedType()))));
                    }
                }
                PathItem item = new PathItem();
                if (get != null) item.get(operation); else item.post(operation);
                api.path(base + suffix, item);
            }
        }
        schema(api, FlowHisTaskVo.class);
        assertThat(api.getPaths()).hasSize(8);
        assertThat(api.getComponents().getSchemas()).containsKeys("PersonSelfSummaryVo", "EnterpriseSelfSummaryVo");
        assertThat(api.getComponents().getSchemas().get("FlowHisTaskVo").getProperties()).containsKey("taskId");
        assertThat(api.getComponents().getSchemas().get("PersonTaskDecisionBo").getProperties())
            .containsOnlyKeys("decision", "reason", "snapshotVersion");
        String output = System.getProperty("profile.openapi.output");
        if (output != null) Files.writeString(Path.of(output), Json.pretty(api));
    }

    private Schema<?> schema(OpenAPI api, Type type) {
        var resolved = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
        if (resolved.referencedSchemas != null) api.getComponents().getSchemas().putAll(resolved.referencedSchemas);
        return resolved.schema;
    }

    private Content content(Schema<?> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }
}
