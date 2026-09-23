package org.namewta.common.encrypt.core;

import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.encrypt.annotation.EncryptField;
import org.namewta.common.encrypt.enums.AlgorithmType;
import org.namewta.common.encrypt.enums.EncodeType;
import org.namewta.common.encrypt.properties.EncryptorProperties;

import java.lang.reflect.Field;

/**
 * 加密上下文工厂。
 */
public class EncryptContextFactory {

    private final EncryptorProperties defaultProperties;

    /**
     * 构造加密上下文工厂。
     *
     * @param defaultProperties 默认加密配置
     */
    public EncryptContextFactory(EncryptorProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    /**
     * 根据字段注解和默认配置创建加密上下文。
     *
     * @param field 加密字段
     * @return 加密上下文
     */
    public EncryptContext create(Field field) {
        EncryptField encryptField = field.getAnnotation(EncryptField.class);
        EncryptContext encryptContext = new EncryptContext();
        encryptContext.setAlgorithm(encryptField.algorithm() == AlgorithmType.DEFAULT ? defaultProperties.getAlgorithm() : encryptField.algorithm());
        encryptContext.setEncode(encryptField.encode() == EncodeType.DEFAULT ? defaultProperties.getEncode() : encryptField.encode());
        encryptContext.setPassword(StringUtils.isBlank(encryptField.password()) ? defaultProperties.getPassword() : encryptField.password());
        encryptContext.setPrivateKey(StringUtils.isBlank(encryptField.privateKey()) ? defaultProperties.getPrivateKey() : encryptField.privateKey());
        encryptContext.setPublicKey(StringUtils.isBlank(encryptField.publicKey()) ? defaultProperties.getPublicKey() : encryptField.publicKey());
        return encryptContext;
    }
}
