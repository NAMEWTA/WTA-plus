package org.namewta.test.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class SnailAiRetirementTest {
    @Test
    void legacyBridgeIsUnmappedEvenWhenAnOldConfigurationEnablesIt() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "snail-ai.enabled=true", "snail-ai.open-api.enabled=true");
            // 在旧版本仅替换外部 vendor client，真实 Controller 扫描/路由仍参与退出断言。
            try {
                Class<?> vendor = Class.forName("com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient");
                var definition = new RootBeanDefinition(vendor);
                definition.setInstanceSupplier(() -> mock(vendor));
                context.addBeanFactoryPostProcessor(factory ->
                    ((org.springframework.beans.factory.support.BeanDefinitionRegistry) factory)
                        .registerBeanDefinition("ownedLegacyVendor", definition));
            } catch (ClassNotFoundException retired) {
                // 退出后无需任何 vendor client。
            }
            context.register(MvcFixture.class, LiveControl.class);
            context.refresh();
            var mappings = context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods();
            assertThat(mappings.keySet()).noneMatch(mapping -> mapping.getPatternValues().stream()
                .anyMatch(path -> path.startsWith("/snail-ai")));
            var http = MockMvcBuilders.webAppContextSetup(context).build();
            http.perform(get("/owned-retirement-control")).andExpect(status().isOk());
            http.perform(post("/snail-ai/user/register")).andExpect(status().isNotFound());
        }
    }

    @Test
    void businessClasspathHasNoRetiredVendorOrAutoConfiguration() {
        var loader = getClass().getClassLoader();
        for (String resource : new String[] {
            "org/namewta/ai/controller/SnailAiController.class",
            "org/namewta/common/ai/config/SnailAiConfig.class",
            "com/aizuda/snail/ai/openapi/client/core/api/OpenApiUserClient.class"
        }) {
            assertThat(loader.getResource(resource)).as(resource).isNull();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @ComponentScan(basePackages = "org.namewta.ai", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestController.class))
    static class MvcFixture { }

    @RestController
    static class LiveControl {
        @GetMapping("/owned-retirement-control")
        String reachable() { return "ok"; }
    }
}
