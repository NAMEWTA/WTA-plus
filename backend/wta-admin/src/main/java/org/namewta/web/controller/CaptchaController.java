package org.namewta.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.regex.RegexValidator;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.common.redis.annotation.RateLimiter;
import org.namewta.common.redis.enums.LimitType;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.common.web.core.WaveAndCircleCaptcha;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;

/**
 * 验证码操作处理
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class CaptchaController {

    private final CaptchaProperties captchaProperties;
    private final NotificationApplicationService notificationService;

    /**
     * 发送短信验证码。
     *
     * @param phoneNumber 用户手机号
     * @return 操作结果
     */
    @RateLimiter(key = "#phoneNumber", time = 60, count = 1)
    @GetMapping("/resource/sms/code")
    public R<Void> smsCode(@NotBlank(message = "{user.phonenumber.not.blank}") String phoneNumber) {
        if (!RegexValidator.isMobile(phoneNumber)) {
            return R.fail("请输入正确的手机号！");
        }
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phoneNumber;
        String code = RandomUtil.randomNumbers(4);
        Instant deadline = captchaNow().plus(Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION))
            .truncatedTo(ChronoUnit.SECONDS);
        try {
            notificationService.submit(new NotificationCommand("admin-web", "auth-captcha", "auth_captcha", phoneNumber,
                "PHONE", List.of(phoneNumber), "auth-captcha",
                Map.of("code", code, "expireMinutes", String.valueOf(Constants.CAPTCHA_EXPIRATION)),
                List.of(NotificationChannel.SMS), NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, deadline,
                captchaIdempotencyKey(NotificationChannel.SMS, phoneNumber), Map.of("audit", "REDACT_SENSITIVE"), java.util.List.of()));
            if (!cacheCaptchaCode(key, code, deadline)) return R.fail("验证码短信发送失败");
        } catch (Exception ex) {
            log.error("验证码短信发送失败，异常类型={}", ex.getClass().getSimpleName());
            return R.fail("验证码短信发送失败");
        }
        return R.ok();
    }

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱
     * @return 操作结果
     */
    @GetMapping("/resource/email/code")
    public R<Void> emailCode(@NotBlank(message = "{user.email.not.blank}") String email) {
        if (!RegexValidator.isEmail(email)) {
            return R.fail("请输入正确的邮箱地址！");
        }
        SpringUtils.getAopProxy(this).emailCodeImpl(email);
        return R.ok();
    }

    /**
     * 发送邮箱验证码的实际执行方法，拆分出来避免开关关闭时仍触发限流。
     *
     * @param email 邮箱
     */
    @RateLimiter(key = "#email", time = 60, count = 1)
    public void emailCodeImpl(String email) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + email;
        String code = RandomUtil.randomNumbers(4);
        Instant deadline = captchaNow().plus(Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION))
            .truncatedTo(ChronoUnit.SECONDS);
        try {
            notificationService.submit(new NotificationCommand("admin-web", "auth-captcha", "auth_captcha", email,
                "EMAIL", List.of(email), "auth-captcha",
                Map.of("code", code, "expireMinutes", String.valueOf(Constants.CAPTCHA_EXPIRATION)),
                List.of(NotificationChannel.MAIL), NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, deadline,
                captchaIdempotencyKey(NotificationChannel.MAIL, email), Map.of("audit", "REDACT_SENSITIVE"), java.util.List.of()));
            if (!cacheCaptchaCode(key, code, deadline)) throw new ServiceException("验证码邮件发送失败");
        } catch (Exception e) {
            log.error("验证码邮件发送失败，异常类型={}", e.getClass().getSimpleName());
            throw new ServiceException("验证码邮件发送失败");
        }
    }

    private String captchaIdempotencyKey(NotificationChannel channel, String target) {
        // 每个新验证码绑定独立意图，避免同一分钟新 code 命中旧意图和旧截止时间。
        return "captcha:" + channel.name().toLowerCase() + ":" + target + ":" + java.util.UUID.randomUUID();
    }

    /** 获取本次验证码的单一业务时刻，测试可固定该时刻以验证截止边界。 */
    protected Instant captchaNow() { return Instant.now(); }

    /**
     * 只按命令携带的绝对截止缓存验证码，不在异步提交后重新计时。
     * @param key 缓存键
     * @param code 本次验证码
     * @param deadline 与通知命令相同的绝对截止
     * @return Redis 时钟判定仍有效且原子写入时为 true
     */
    protected boolean cacheCaptchaCode(String key, String code, Instant deadline) {
        return RedisUtils.setCacheObjectUntil(key, code, deadline);
    }

    /**
     * 获取图片验证码。
     *
     * @return 验证码信息
     */
    @GetMapping("/auth/code")
    public R<CaptchaVo> getCode() {
        CaptchaProperties.Snapshot settings = captchaProperties.currentSnapshot();
        if (!Boolean.TRUE.equals(settings.enable())) {
            return R.ok(new CaptchaVo(false, null, null));
        }
        return R.ok(SpringUtils.getAopProxy(this).getCodeImpl(settings));
    }

    /**
     * 实际生成图片验证码并缓存结果。
     *
     * @return 验证码信息
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    public CaptchaVo getCodeImpl() {
        return getCodeImpl(captchaProperties.currentSnapshot());
    }

    /**
     * 使用请求开始时捕获的完整快照生成验证码。
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    public CaptchaVo getCodeImpl(CaptchaProperties.Snapshot settings) {
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + uuid;
        // 生成验证码
        String captchaType = settings.type();
        CodeGenerator codeGenerator;
        if ("math".equals(captchaType)) {
            codeGenerator = new MathGenerator(settings.numberLength(), false);
        } else {
            codeGenerator = new RandomGenerator(settings.charLength());
        }
        WaveAndCircleCaptcha captcha = new WaveAndCircleCaptcha(160, 60);
        // captcha.setBackground(Color.WHITE); // 不设置就是透明底
        captcha.setFont(new Font("Arial", Font.BOLD, 45));
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        // 如果是数学验证码，使用SpEL表达式处理验证码结果
        String code = captcha.getCode();
        if ("math".equals(captchaType)) {
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(StringUtils.remove(code, "="));
            code = exp.getValue(String.class);
        }
        RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        return new CaptchaVo(true, uuid, captcha.getImageBase64());
    }

    /**
     * 图片验证码响应对象。
     *
     * @param captchaEnabled 是否启用验证码
     * @param uuid           验证码标识
     * @param img            Base64 图片数据
     */
    public record CaptchaVo(Boolean captchaEnabled, String uuid, String img) {
    }

}
