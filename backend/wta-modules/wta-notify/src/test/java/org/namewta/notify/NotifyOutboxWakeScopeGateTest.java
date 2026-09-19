package org.namewta.notify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.model.*;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.service.NotifyConfigService;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Handle;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Outbox 调度不能耦合配置控制面或直调渠道；common Dispatcher 继续同步完成。
 * 旧 change 的一次性 diff 范围审查属于归档证据，不能依赖已不存在的历史 Git 对象。
 */
@Tag("dev")
class NotifyOutboxWakeScopeGateTest {

    @Test
    void outboxWakeComponentsStayOutsideConfigControlPlaneAndDirectChannelCalls() throws Exception {
        Path sources = sourceRoot();
        List<Path> components;
        try (var paths = Files.walk(sources.resolve("org/namewta/notify"))) {
            components = paths.filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.toString().contains("/adapter/worker/")
                    || path.toString().contains("/support/outbox/")
                    || path.getFileName().toString().startsWith("NotifyOutboxWake"))
                .toList();
        }
        assertTrue(components.stream().anyMatch(path -> path.endsWith("NotifyOutboxWorker.java")), "worker must exist");
        assertTrue(components.stream().anyMatch(path -> path.endsWith("NotifyOutboxWakePublisher.java")), "wake publisher must exist");
        for (Path source : components) {
            String className = sources.relativize(source).toString().replace('\\', '/').replaceAll("\\.java$", "").replace('/', '.');
            assertEquals(List.of(), forbiddenReferences(className), className);
        }
    }

    @Test
    void rejectsConfigInjectionAndDirectProviderCallsButAcceptsClaimPort() throws Exception {
        assertFalse(forbiddenReferences(ConfigInjection.class.getName()).isEmpty());
        assertFalse(forbiddenReferences(DirectChannelCall.class.getName()).isEmpty());
        assertFalse(forbiddenReferences(ConfigClassLookup.class.getName()).isEmpty());
        assertEquals(List.of(), forbiddenReferences(ClaimPortOnly.class.getName()));
    }

    @Test
    void commonDispatcherFinishesProviderAndEventOnCallerThreadBeforeReturning() {
        List<String> calls = new ArrayList<>();
        Thread caller = Thread.currentThread();
        NotifyChannel channel = NotifyChannel.of("scope-test");
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() {
                return channel;
            }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest request) {
                assertSame(caller, Thread.currentThread());
                calls.add("provider");
                return new NotifyAdapterResult("test-provider", request.request().targets().stream()
                    .map(target -> NotifyTargetResult.accepted(target, "test-message", 0)).toList());
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> {
                assertSame(caller, Thread.currentThread());
                calls.add("event");
            });
        NotifyResult result = dispatcher.send(NotifyRequest.builder().channel(channel)
            .targets(List.of(NotifyTarget.phone("13800000000"), NotifyTarget.phone("13900000000")))
            .content(new NotifyTextContent("test", "test")).build());
        calls.add("return");
        assertEquals(NotifyStatus.ACCEPTED, result.status());
        assertEquals(2, result.deliveries().size());
        assertEquals(List.of("provider", "event", "return"), calls);
    }

    private static Path sourceRoot() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            for (String suffix : List.of("src/main/java", "backend/wta-modules/wta-notify/src/main/java",
                "wta-modules/wta-notify/src/main/java")) {
                Path candidate = directory.resolve(suffix);
                if (Files.isDirectory(candidate.resolve("org/namewta/notify/adapter/worker"))) {
                    return candidate;
                }
            }
            directory = directory.getParent();
        }
        throw new AssertionError("Notify source root missing");
    }

    /** 读取编译后的类型及调用引用，避免 Java 注释/字符串里的示例造成误报。 */
    private static List<String> forbiddenReferences(String className) throws IOException {
        List<String> violations = new ArrayList<>();
        try (var bytes = NotifyOutboxWakeScopeGateTest.class.getClassLoader()
            .getResourceAsStream(className.replace('.', '/') + ".class")) {
            assertNotNull(bytes, className);
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                private void inspect(String reference) {
                    if (reference == null) {
                        return;
                    }
                    String normalized = reference.toLowerCase(Locale.ROOT);
                    if (normalized.contains("org/namewta/common/notify/")
                        || normalized.contains("/notifyconfig") || normalized.contains("/notifychannelaccount")) {
                        violations.add(reference);
                    }
                }

                private void inspectConstant(Object value) {
                    if (value instanceof Type type) {
                        inspect(type.getDescriptor());
                    } else if (value instanceof Handle handle) {
                        inspect(handle.getOwner());
                        inspect(handle.getDesc());
                    }
                }

                @Override
                public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
                    inspect(signature);
                    inspect(parent);
                    if (interfaces != null) {
                        for (String contract : interfaces) {
                            inspect(contract);
                        }
                    }
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    inspect(descriptor);
                    inspect(signature);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    inspect(descriptor);
                    inspect(signature);
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            inspectConstant(value);
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
                            inspect(descriptor);
                            inspectConstant(bootstrap);
                            for (Object argument : arguments) {
                                inspectConstant(argument);
                            }
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            inspect(type);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            inspect(owner);
                            inspect(descriptor);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            inspect(owner);
                            inspect(descriptor);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return violations;
    }

    private static class ConfigInjection {
        NotifyConfigService config;
    }

    private static class DirectChannelCall {
        void send(NotifyClient client, NotifyRequest request) {
            client.send(request);
        }
    }

    private static class ClaimPortOnly {
        NotifyOutboxClaimPort claim;
        String documentation = "org/namewta/common/notify/ is an example, not a dependency";
    }

    private static class ConfigClassLookup {
        Class<?> lookup() {
            return NotifyConfigService.class;
        }
    }
}
