package org.namewta.common.satoken.profile.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求当前已登录普通用户具有有效的企业认证。
 *
 * <p>可标记 Controller 或 Service 的类型及公开实例方法。类型、方法、父类和接口上的
 * 个人／企业认证要求累加，超级管理员也不豁免，{@code @SaIgnore} 不取消此要求。</p>
 *
 * <p>仅在 Spring 代理调用边界生效：自调用、私有或静态方法不受保护；使用类代理时不能标记
 * final 类或 final 方法。异步及后台任务应显式传入已授权的用户 ID 使用档案公共服务，
 * 不依赖调用线程的登录态。本注解不替代权限、Client 数据隔离或业务事务内的不变量校验。</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireEnterpriseVerified {
}
