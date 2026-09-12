package org.namewta.common.web.config.properties;

import org.namewta.common.nacos.NacosConfigAccessor;
import org.namewta.common.nacos.NacosConfigParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

import java.util.Set;

/**
 * 验证码 配置属性
 *
 * @author Lion Li
 */
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties implements NacosConfigParticipant<CaptchaProperties.Snapshot> {

    /**
     * 是否启用验证码校验。
     */
    private Boolean enable;

    /**
     * 验证码类型
     */
    private String type;

    /**
     * 数字验证码位数
     */
    private Integer numberLength;

    /**
     * 字符验证码长度
     */
    private Integer charLength;

    private volatile NacosConfigAccessor nacosConfigAccessor;

    @Override
    public String id() {
        return "captcha";
    }

    @Override
    public Set<String> prefixes() {
        return Set.of("captcha.");
    }

    @Override
    public Snapshot prepare(Binder binder) {
        return validate(binder.bind("captcha", Bindable.of(Snapshot.class)).orElseGet(this::localSnapshot));
    }

    public Snapshot currentSnapshot() {
        NacosConfigAccessor accessor = nacosConfigAccessor;
        return accessor == null
            ? localSnapshot()
            : accessor.configuration(id(), Snapshot.class).orElseGet(this::localSnapshot);
    }

    public Snapshot localSnapshot() {
        return new Snapshot(enable, type, numberLength, charLength);
    }

    @Autowired(required = false)
    public void setNacosConfigAccessor(NacosConfigAccessor nacosConfigAccessor) {
        this.nacosConfigAccessor = nacosConfigAccessor;
    }

    public Boolean getEnable() {
        return currentSnapshot().enable();
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getType() {
        return currentSnapshot().type();
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNumberLength() {
        return currentSnapshot().numberLength();
    }

    public void setNumberLength(Integer numberLength) {
        this.numberLength = numberLength;
    }

    public Integer getCharLength() {
        return currentSnapshot().charLength();
    }

    public void setCharLength(Integer charLength) {
        this.charLength = charLength;
    }

    private Snapshot validate(Snapshot snapshot) {
        if (snapshot.enable() == null || snapshot.type() == null || snapshot.type().isBlank()
            || snapshot.numberLength() == null || snapshot.numberLength() <= 0
            || snapshot.charLength() == null || snapshot.charLength() <= 0) {
            throw new IllegalArgumentException("Invalid captcha runtime configuration");
        }
        return snapshot;
    }

    /**
     * 单次验证码业务调用使用的不可变配置。
     */
    public record Snapshot(Boolean enable, String type, Integer numberLength, Integer charLength) {
    }
}
