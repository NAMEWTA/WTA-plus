package org.namewta.test.contracts;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.log.annotation.Log;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import org.namewta.common.json.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 检查实际编译后的控制器，含条件关闭的演示与工作流，防止路由/日志再次漂移。 */
@Tag("dev")
class CrudHttpMappingContractTest {
    static class Mapping extends RequestMappingHandlerMapping {
        RequestMappingInfo describe(Method method, Class<?> type) { return getMappingForMethod(method, type); }
    }
    static List<Class<?>> controllers() throws Exception {
        var resources = new PathMatchingResourcePatternResolver();
        var reader = new SimpleMetadataReaderFactory();
        var result = new ArrayList<Class<?>>();
        for (String namespace : List.of("system", "workflow", "demo")) {
            for (var resource : resources.getResources("classpath*:org/namewta/" + namespace + "/controller/**/*Controller.class")) {
                Class<?> type = Class.forName(reader.getMetadataReader(resource).getClassMetadata().getClassName(), false,
                    CrudHttpMappingContractTest.class.getClassLoader());
                if (AnnotatedElementUtils.hasAnnotation(type, Controller.class)) result.add(type);
            }
        }
        result.add(org.namewta.web.controller.AuthController.class);
        return result;
    }

    @Test
    void firstPartyCrudHasNoLegacyMethodsAndEveryPostHasAuditMetadata() throws Exception {
        var violations = new ArrayList<String>();
        int mapped = 0;
        for (Class<?> type : controllers()) {
            for (Method method : type.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) continue;
                mapped++;
                String label = type.getSimpleName() + "." + method.getName();
                for (RequestMethod verb : mapping.method()) {
                    if (List.of(RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE).contains(verb)) {
                        violations.add(label + " uses " + verb);
                    }
                    if (verb == RequestMethod.POST) {
                        Log audit = method.getAnnotation(Log.class);
                        if (audit == null || audit.title().isBlank()) violations.add(label + " lacks audit metadata");
                    }
                }
            }
        }
        assertThat(mapped).isGreaterThan(200);
        assertThat(violations).isEmpty();
    }

    @Test
    void springRegistersTheCompleteResourceSurfaceWithoutDuplicateMappings() throws Exception {
        try (var context = new StaticWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.refresh();
            var mapping = new Mapping();
            mapping.setApplicationContext(context);
            mapping.afterPropertiesSet();
            for (Class<?> type : controllers()) {
                Object controller = mock(type);
                for (Method method : type.getDeclaredMethods()) {
                    RequestMappingInfo info = mapping.describe(method, type);
                    if (info != null) mapping.registerMapping(info, controller, method);
                }
            }
            assertThat(mapping.getHandlerMethods()).hasSizeGreaterThan(200);
            String output = System.getProperty("crud.mapping.output");
            if (output != null) {
                var rows = mapping.getHandlerMethods().entrySet().stream().map(entry -> Map.of(
                    "paths", entry.getKey().getPatternValues().stream().sorted().toList(),
                    "methods", entry.getKey().getMethodsCondition().getMethods().stream().map(Enum::name).sorted().toList(),
                    "controller", entry.getValue().getMethod().getDeclaringClass().getName(),
                    "java", entry.getValue().getMethod().getName(),
                    "signature", entry.getValue().getMethod().toGenericString()
                )).toList();
                Files.writeString(Path.of(output), JsonUtils.toJsonString(rows));
            }
        }
    }
}
