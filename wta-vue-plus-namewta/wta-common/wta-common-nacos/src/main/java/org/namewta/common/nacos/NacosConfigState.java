package org.namewta.common.nacos;

import java.time.Instant;

/**
 * 单个应用实例的脱敏配置状态。
 */
public record NacosConfigState(boolean enabled, boolean connected, String digest, String result,
                               Instant lastSuccessAt, int immediateKeyCount, int restartKeyCount,
                               String errorCode) {

    NacosConfigState withConnection(boolean nextConnected) {
        return new NacosConfigState(enabled, nextConnected, digest, result, lastSuccessAt,
            immediateKeyCount, restartKeyCount, errorCode);
    }
}
