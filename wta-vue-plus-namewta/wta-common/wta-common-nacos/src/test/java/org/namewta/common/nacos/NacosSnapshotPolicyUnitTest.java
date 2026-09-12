package org.namewta.common.nacos;

import com.alibaba.nacos.client.config.utils.SnapShotSwitch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class NacosSnapshotPolicyUnitTest {

    @Test
    void disablesSdkDiskSnapshotBeforeCreatingTheClient() {
        SnapShotSwitch.setIsSnapShot(true);
        try {
            NacosSdkConfigClient.disableSnapshotFallback();
            assertFalse(SnapShotSwitch.getIsSnapShot());
        } finally {
            SnapShotSwitch.setIsSnapShot(true);
        }
    }
}
