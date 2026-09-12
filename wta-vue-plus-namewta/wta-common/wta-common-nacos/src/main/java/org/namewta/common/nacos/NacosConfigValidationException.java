package org.namewta.common.nacos;

final class NacosConfigValidationException extends RuntimeException {

    private final String code;

    NacosConfigValidationException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    NacosConfigValidationException(String code) {
        super(code);
        this.code = code;
    }

    String code() {
        return code;
    }
}
