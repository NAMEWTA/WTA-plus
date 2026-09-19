package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/** 已提交回执的幂等凭据；仅保存摘要和投递关联，不保存原文或收件地址。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_provider_receipt")
public class NotifyProviderReceipt extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 回执主键。 */
    @TableId
    private Long providerReceiptId;
    /** 渠道、账号配置标识和事件编号的 SHA-256。 */
    private String eventKey;
    /** 业务事实摘要，不含重签时可刷新的传输时间戳。 */
    private String factsHash;
    /** 已校验关联的投递主键。 */
    private Long deliveryId;
    /** 乐观锁版本。 */
    @Version
    private Integer version;
    /** 逻辑删除标志；运行时不提供删除或过期清理入口。 */
    @TableLogic
    private String delFlag;
}
