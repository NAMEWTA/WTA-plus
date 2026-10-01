package org.namewta.common.satoken.profile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Objects;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.namewta.common.satoken.profile.annotation.RequireEnterpriseVerified;
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.profile.api.domain.ProfileType;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 在 Spring 代理边界统一执行个人和企业认证要求。
 *
 * <p>只解析匹配方法的注解，不加载登录态或档案 Bean；实际调用时才获取访问服务，避免创建
 * Advisor 时提前实例化贡献者和业务服务。类和方法的继承要求累加后只查询一次档案。</p>
 */
public final class ProfileVerificationAdvisor extends AbstractPointcutAdvisor {

    private final Pointcut pointcut = new StaticMethodMatcherPointcut() {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return requirements(method, targetClass).length > 0;
        }
    };

    private final MethodInterceptor advice;

    /**
     * 创建延迟获取档案访问服务的认证门禁。
     *
     * @param profileAccess 容器中的访问服务提供器，只在受保护方法实际调用时解析
     */
    public ProfileVerificationAdvisor(ObjectProvider<ProfileAccess> profileAccess) {
        Objects.requireNonNull(profileAccess, "profileAccess");
        setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        advice = invocation -> {
            Object target = Objects.requireNonNull(invocation.getThis(), "profile verification target");
            ProfileType[] required = requirements(invocation.getMethod(), AopUtils.getTargetClass(target));
            if (required.length > 0) {
                profileAccess.getObject().requireVerified(required);
            }
            return invocation.proceed();
        };
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    /** 解析当前方法及其类型层次的累加要求，不访问任何请求或业务数据。 */
    private static ProfileType[] requirements(Method method, Class<?> targetClass) {
        if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
            || ReflectionUtils.isObjectMethod(method)) {
            return new ProfileType[0];
        }
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        Method specificMethod = BridgeMethodResolver.findBridgedMethod(
            AopUtils.getMostSpecificMethod(method, userClass));
        EnumSet<ProfileType> required = EnumSet.noneOf(ProfileType.class);
        if (hasRequirement(RequirePersonVerified.class, method, specificMethod, userClass)) {
            required.add(ProfileType.PERSON);
        }
        if (hasRequirement(RequireEnterpriseVerified.class, method, specificMethod, userClass)) {
            required.add(ProfileType.ENTERPRISE);
        }
        return required.toArray(ProfileType[]::new);
    }

    /** Spring 的 find 语义同时覆盖父类、接口以及泛型桥接方法。 */
    private static boolean hasRequirement(Class<? extends Annotation> annotation, Method method,
        Method specificMethod, Class<?> targetClass) {
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, annotation) != null
            || AnnotatedElementUtils.findMergedAnnotation(specificMethod, annotation) != null
            || AnnotatedElementUtils.findMergedAnnotation(method, annotation) != null;
    }
}
