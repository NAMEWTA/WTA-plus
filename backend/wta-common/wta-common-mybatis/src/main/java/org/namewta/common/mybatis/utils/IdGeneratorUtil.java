package org.namewta.common.mybatis.utils;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.namewta.common.core.utils.SpringUtils;

/**
 * ID 生成工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdGeneratorUtil {

    /**
     * MyBatis-Plus 主键生成器。
     */
    private static volatile IdentifierGenerator GENERATOR;

    /**
     * 生成主键数值；应用上下文不可用时使用 MyBatis-Plus 默认雪花算法。
     *
     * @param entity 参与生成的实体
     * @return 主键数值
     */
    private static Number generate(Object entity) {
        IdentifierGenerator generator = GENERATOR;
        if (generator == null) {
            try {
                generator = SpringUtils.getBean(IdentifierGenerator.class);
                GENERATOR = generator;
            } catch (RuntimeException ignored) {
                return IdWorker.getId();
            }
        }
        return generator.nextId(entity);
    }

    /**
     * 生成字符串类型主键 ID
     * <p>
     * 调用 {@link IdentifierGenerator#nextId(Object)}，返回 String 格式 ID。
     * </p>
     *
     * @return 字符串格式主键 ID
     */
    public static String nextId() {
        return generate(null).toString();
    }

    /**
     * 生成 Long 类型主键 ID
     * <p>
     * 自动将生成的数字型主键转换为 Long 类型
     * </p>
     *
     * @return Long 类型主键 ID
     */
    public static Long nextLongId() {
        return generate(null).longValue();
    }

    /**
     * 生成 Number 类型主键 ID
     * <p>
     * 推荐在需要保留原始 Number 类型时使用
     * </p>
     *
     * @return Number 类型主键 ID
     */
    public static Number nextNumberId() {
        return generate(null);
    }

    /**
     * 根据实体生成数字型主键 ID
     * <p>
     * 若自定义的 {@link IdentifierGenerator} 根据实体内容生成 ID，则可以使用本方法
     * </p>
     *
     * @param entity 实体对象
     * @return Number 类型主键 ID
     */
    public static Number nextId(Object entity) {
        return generate(entity);
    }

    /**
     * 根据实体生成字符串主键 ID
     * <p>
     * 与 {@link #nextId(Object)} 类似，但返回 String 类型
     * </p>
     *
     * @param entity 实体对象
     * @return 字符串格式主键 ID
     */
    public static String nextStringId(Object entity) {
        return generate(entity).toString();
    }

    /**
     * 生成 32 位 UUID
     * <p>
     * 底层使用 {@link IdWorker#get32UUID()}
     * </p>
     *
     * @return 32 位 UUID 字符串
     */
    public static String nextUUID() {
        return IdWorker.get32UUID();
    }

    /**
     * 根据实体生成 32 位 UUID
     * <p>
     * 默认 {@link IdentifierGenerator#nextUUID(Object)} 实现忽略实体，但保留该方法便于扩展。
     * </p>
     *
     * @param entity 实体对象
     * @return 32 位 UUID 字符串
     */
    public static String nextUUID(Object entity) {
        IdentifierGenerator generator = GENERATOR;
        if (generator == null) {
            try {
                generator = SpringUtils.getBean(IdentifierGenerator.class);
                GENERATOR = generator;
            } catch (RuntimeException ignored) {
                return IdWorker.get32UUID();
            }
        }
        return generator.nextUUID(entity);
    }

    /**
     * 生成带指定前缀的字符串主键 ID
     * <p>
     * 示例：prefix = "ORD"，生成结果形如：{@code ORD20251211000123}
     * </p>
     *
     * @param prefix 自定义前缀
     * @return 带前缀的字符串主键 ID
     */
    public static String nextIdWithPrefix(String prefix) {
        return prefix + nextId();
    }

    /**
     * 生成带前缀的 UUID
     *
     * @param prefix 前缀
     * @return prefix + UUID
     */
    public static String nextUUIDWithPrefix(String prefix) {
        return prefix + nextUUID();
    }

}
