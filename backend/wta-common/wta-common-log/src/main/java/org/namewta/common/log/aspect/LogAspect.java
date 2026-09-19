package org.namewta.common.log.aspect;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.json.utils.LogSanitizer;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessStatus;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 操作日志记录处理
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@AutoConfiguration
public class LogAspect {

    /**
     * URL 最大记录长度
     */
    private static final int MAX_URL_LENGTH = 255;

    /**
     * 客户端标识最大记录长度
     */
    private static final int MAX_CLIENT_KEY_LENGTH = 32;

    /**
     * 日志内容最大记录长度
     */
    private static final int MAX_CONTENT_LENGTH = 3800;

    /**
     * 执行目标方法并记录操作日志。
     *
     * @param joinPoint     切点
     * @param controllerLog 日志注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around(value = "@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object jsonResult = joinPoint.proceed();
            handleLog(joinPoint, controllerLog, null, jsonResult, stopWatch);
            return jsonResult;
        } catch (Exception e) {
            handleLog(joinPoint, controllerLog, e, null, stopWatch);
            throw e;
        }
    }

    /**
     * 组装并发布操作日志事件。
     *
     * @param joinPoint     切点
     * @param controllerLog 日志注解
     * @param e             异常信息
     * @param jsonResult    返回结果
     * @param stopWatch     耗时统计
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult, StopWatch stopWatch) {
        try {

            // *========数据库日志=========*//
            OperLogEvent operLog = new OperLogEvent();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            HttpServletRequest request = ServletUtils.getRequest();
            // 请求的地址
            String ip = ServletUtils.getClientIP();
            operLog.setOperIp(ip);
            // 路径变量可能是上传/会话令牌；审计保存服务端路由模板，不复制原始URI。
            Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            operLog.setOperUrl(limit(route == null ? "[unmapped]" : route.toString(), MAX_URL_LENGTH));
            operLog.setClientKey(limit(request.getHeader(LoginHelper.CLIENT_KEY), MAX_CLIENT_KEY_LENGTH));
            // Sa-Token 对空 token 的会话读取会抛异常；匿名签发同样必须留下审计事件。
            LoginUser loginUser = LoginHelper.isLogin() ? LoginHelper.getLoginUser() : null;
            if (ObjectUtil.isNotNull(loginUser)) {
                operLog.setOperName(loginUser.getUsername());
                operLog.setUserId(loginUser.getUserId());
                operLog.setDeptId(loginUser.getDeptId());
                operLog.setDeptName(loginUser.getDeptName());
                operLog.setDeviceType(loginUser.getDeviceType());
                operLog.setBrowser(loginUser.getBrowser());
                operLog.setOs(loginUser.getOs());
                if (StringUtils.isBlank(operLog.getClientKey())) {
                    operLog.setClientKey(loginUser.getClientKey());
                }
            }

            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(LogSanitizer.failure(e));
            }
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            operLog.setRequestMethod(request.getMethod());
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 设置消耗时间
            stopWatch.stop();
            operLog.setCostTime(stopWatch.getDuration().toMillis());
            // 发布事件保存数据库
            SpringUtils.context().publishEvent(operLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("记录操作日志异常，异常类型={}", LogSanitizer.failure(exp));
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param joinPoint  切点
     * @param log        日志
     * @param operLog    操作日志
     * @param jsonResult 返回结果
     * @throws Exception 异常
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, OperLogEvent operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }
        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && ObjectUtil.isNotNull(jsonResult) && !LogSanitizer.omitResponseBody(requestPath())) {
            operLog.setJsonResult(limit(LogSanitizer.object(jsonResult, requestPath(), log.excludeParamNames()), MAX_CONTENT_LENGTH));
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param joinPoint         切点
     * @param operLog           操作日志
     * @param excludeParamNames 排除参数名
     * @throws Exception 异常
     */
    private void setRequestValue(JoinPoint joinPoint, OperLogEvent operLog, String[] excludeParamNames) throws Exception {
        Map<String, String[]> paramsMap = ServletUtils.getRequest().getParameterMap();
        String requestMethod = operLog.getRequestMethod();
        if (MapUtil.isEmpty(paramsMap) && StringUtils.equalsAny(requestMethod, HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name())) {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(limit(params, MAX_CONTENT_LENGTH));
        } else {
            operLog.setOperParam(limit(LogSanitizer.object(paramsMap, requestPath(), excludeParamNames), MAX_CONTENT_LENGTH));
        }
    }

    /**
     * 将方法参数序列化为日志字符串。
     *
     * @param paramsArray       参数数组
     * @param excludeParamNames 排除字段名
     * @return 参数字符串
     */
    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames) {
        StringJoiner params = new StringJoiner(",", "[", "]");
        if (ArrayUtil.isEmpty(paramsArray)) {
            return params.toString();
        }
        for (Object o : paramsArray) {
            if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                params.add(serializeArg(o, excludeParamNames));
            }
        }
        return params.toString();
    }

    /**
     * 序列化单个方法参数，并移除排除字段。
     *
     * @param arg     参数对象
     * @param exclude 排除字段名
     * @return 参数日志字符串
     */
    private String serializeArg(Object arg, String[] exclude) {
        return LogSanitizer.object(arg, requestPath(), exclude);
    }

    /** 获取应用内路径，避免 context-path 影响 OAuth 凭据策略。 */
    private String requestPath() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return null;
        }
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isEmpty()
            ? request.getRequestURI().substring(request.getContextPath().length()) : servletPath;
    }

    /**
     * 限制日志字段长度。
     *
     * @param value     原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String limit(String value, int maxLength) {
        return StringUtils.substring(value, 0, maxLength);
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            if (MultipartFile.class.isAssignableFrom(clazz.getComponentType())) {
                return true;
            }
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                if (isFilterValue(Array.get(o, i))) {
                    return true;
                }
            }
            return false;
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) o;
            for (Object value : collection) {
                if (isFilterValue(value)) {
                    return true;
                }
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Object value : map.values()) {
                if (isFilterValue(value)) {
                    return true;
                }
            }
        }
        return isFilterValue(o);
    }

    /**
     * 判断是否为日志参数过滤类型。
     *
     * @param value 参数值
     * @return true 需要过滤 false 不需要过滤
     */
    private boolean isFilterValue(Object value) {
        return value instanceof MultipartFile || value instanceof HttpServletRequest || value instanceof HttpServletResponse
            || value instanceof BindingResult;
    }
}
