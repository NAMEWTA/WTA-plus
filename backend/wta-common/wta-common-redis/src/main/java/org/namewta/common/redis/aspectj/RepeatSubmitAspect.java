package org.namewta.common.redis.aspectj;

import cn.dev33.satoken.SaManager;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.constant.HttpStatus;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MessageUtils;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.redis.utils.RedisUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * 防止重复提交(参考美团GTIS防重系统)
 *
 * @author Lion Li
 */
@Aspect
public class RepeatSubmitAspect {

    /**
     * 在单次同步调用作用域持有租约；成功保留原窗口，失败只释放自己的值。
     * 未取得租约的调用不会执行释放，线程复用不依赖ThreadLocal清理。
     *
     * @param point        切点
     * @param repeatSubmit 防重复提交注解
     * @return 原业务返回值
     * @throws Throwable 原业务异常；释放失败作为suppressed保留，不覆盖原异常
     */
    @Around("@annotation(repeatSubmit)")
    public Object doAround(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        // 如果注解不为0 则使用注解数值
        long interval = repeatSubmit.timeUnit().toMillis(repeatSubmit.interval());

        if (interval < 1000) {
            throw new ServiceException("重复提交间隔时间不能小于'1'秒");
        }
        HttpServletRequest request = ServletUtils.getRequest();
        String nowParams = argsArrayToString(point.getArgs());

        // 请求地址（作为存放cache的key值）
        String url = request.getRequestURI();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StringUtils.trimToEmpty(request.getHeader(SaManager.getConfig().getTokenName()));

        submitKey = SecureUtil.md5(submitKey + StringUtils.COLON + nowParams);
        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = GlobalConstants.REPEAT_SUBMIT_KEY + url + submitKey;
        String owner = UUID.randomUUID().toString();
        if (!RedisUtils.setObjectIfAbsent(cacheRepeatKey, owner, Duration.ofMillis(interval))) {
            String message = repeatSubmit.message();
            if (StringUtils.startsWith(message, "{") && StringUtils.endsWith(message, "}")) {
                message = MessageUtils.message(StringUtils.substring(message, 1, message.length() - 1));
            }
            throw new ServiceException(message);
        }

        Object result;
        try {
            result = point.proceed();
        } catch (Throwable failure) {
            try {
                RedisUtils.deleteObjectIfEquals(cacheRepeatKey, owner);
            } catch (RuntimeException releaseFailure) {
                if (releaseFailure != failure) {
                    failure.addSuppressed(releaseFailure);
                }
            }
            throw failure;
        }
        if (result instanceof R<?> r && r.getCode() != HttpStatus.SUCCESS) {
            RedisUtils.deleteObjectIfEquals(cacheRepeatKey, owner);
        }
        // 非R结果（包括null）沿用成功留TTL语义，不刷新防重窗口。
        return result;
    }

    /**
     * 参数拼装
     *
     * @param paramsArray 方法参数数组
     * @return 拼装后的参数字符串
     */
    private String argsArrayToString(Object[] paramsArray) {
        StringJoiner params = new StringJoiner(" ");
        if (ArrayUtil.isEmpty(paramsArray)) {
            return params.toString();
        }
        for (Object o : paramsArray) {
            if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                params.add(JsonUtils.toJsonString(o));
            }
        }
        return params.toString();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            if (MultipartFile.class.isAssignableFrom(clazz.getComponentType())) {
                return true;
            }
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                Object value = Array.get(o, i);
                if (ObjectUtil.isNotNull(value) && isFilterObject(value)) {
                    return true;
                }
            }
            return false;
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                if (ObjectUtil.isNotNull(value) && isFilterObject(value)) {
                    return true;
                }
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.values()) {
                if (ObjectUtil.isNotNull(value) && isFilterObject(value)) {
                    return true;
                }
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
            || o instanceof BindingResult;
    }

}
