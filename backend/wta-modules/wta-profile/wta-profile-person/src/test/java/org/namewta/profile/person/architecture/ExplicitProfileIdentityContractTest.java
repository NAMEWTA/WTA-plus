package org.namewta.profile.person.architecture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 编译真实公共合同：省略身份的调用必须在编译期被拒绝，而正确调用仍可编译。 */
@Tag("dev")
class ExplicitProfileIdentityContractTest {
    @TempDir Path temporary;

    @ParameterizedTest(name = "{0}: {2}")
    @MethodSource("identityCalls")
    void identityIsRequiredAtCompileTime(String type, String explicit, String implicit) throws Exception {
        assertThat(compiles(type, explicit)).as("explicit identity call").isTrue();
        assertThat(compiles(type, implicit)).as("missing identity must not compile: %s", implicit).isFalse();
    }

    private boolean compiles(String type, String statement) throws Exception {
        Path source = temporary.resolve("IdentityConsumer.java");
        Files.writeString(source, "class IdentityConsumer { void call(" + type + " value) { " + statement + " } }");
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("JDK compiler").isNotNull();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            return Boolean.TRUE.equals(compiler.getTask(null, files, diagnostics,
                List.of("-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", temporary.toString()),
                null, files.getJavaFileObjects(source.toFile())).call());
        }
    }

    static Stream<Arguments> identityCalls() {
        return Stream.of(
            Arguments.of("org.namewta.profile.person.usecase.PersonApplicationUseCase", "value.current(1L);", "value.current();"),
            Arguments.of("org.namewta.profile.person.usecase.PersonApplicationUseCase", "value.save(1L, null);", "value.save(null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonApplicationUseCase", "value.submit(1L, 1);", "value.submit(1);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonRebindUseCase", "value.match(1L, null);", "value.match(null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonRebindUseCase", "value.confirm(1L, null);", "value.confirm(null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonRebindUseCase", "value.submit(1L, null);", "value.submit(null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonRebindUseCase", "value.unbind(1L);", "value.unbind();"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.decide(1L, 1L, null);", "value.decide(1L, null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.create(1L, null);", "value.create(null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.revise(1L, 1L, null);", "value.revise(1L, null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.manageBinding(1L, 1L, null);", "value.manageBinding(1L, null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.assign(1L, 1L, null);", "value.assign(1L, null);"),
            Arguments.of("org.namewta.profile.person.usecase.PersonAdminUseCase", "value.revoke(1L, 1L, null);", "value.revoke(1L, null);"));
    }
}
