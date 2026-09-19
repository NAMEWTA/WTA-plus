package org.namewta.test.phone;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import cn.hutool.extra.spring.SpringUtil;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class PhoneTestDatabaseGuardTest {
    @Test
    void earlyFailureDoesNotReplaceSpringStateOwnedByAnotherTest() throws Exception {
        Object previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        Object previousContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        String previousUrl = System.getProperty("phone.mysql.integration.url");
        var factory = new DefaultListableBeanFactory();
        try (var context = new GenericApplicationContext()) {
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", factory);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", context);
            System.clearProperty("phone.mysql.integration.url");
            var fixture = new PhoneRegistrationMySqlIntegrationTest();
            assertThatThrownBy(fixture::open).isInstanceOf(IllegalArgumentException.class);
            fixture.close();
            assertThat(ReflectionTestUtils.getField(SpringUtil.class, "beanFactory")).isSameAs(factory);
            assertThat(ReflectionTestUtils.getField(SpringUtil.class, "applicationContext")).isSameAs(context);
        } finally {
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousContext);
            if (previousUrl == null) System.clearProperty("phone.mysql.integration.url");
            else System.setProperty("phone.mysql.integration.url", previousUrl);
        }
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "jdbc:mysql://127.0.0.1:3306/production?comment=/namewta_phone_test_i",
        "jdbc:mysql://127.0.0.1:3306/production#/namewta_phone_test_i",
        "jdbc:mysql://127.0.0.1:3306/namewta_phone_test_i/../production",
        "jdbc:mysql://127.0.0.1:3306,namewta.example:3306/namewta_phone_test_i",
        "jdbc:mysql://127.0.0.1:3306/namewta_phone_test_i?sessionVariables=sql_mode=''",
        "jdbc:mysql://db.example:3306/namewta_phone_test_i"
    })
    void rejectsNonOwnedTargetsBeforeAnyDatabaseConnection(String url) {
        assertThatThrownBy(() -> PhoneRegistrationMySqlIntegrationTest.requireOwnedDatabaseUrl(url))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:mysql://127.0.0.1:33306/namewta_phone_test_i",
        "jdbc:mysql://127.0.0.1:33306/namewta_phone_test_i?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    })
    void acceptsExplicitLocalTaskSchemas(String url) {
        assertThatCode(() -> PhoneRegistrationMySqlIntegrationTest.requireOwnedDatabaseUrl(url)).doesNotThrowAnyException();
    }
}
