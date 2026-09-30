package org.namewta.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/** 实例启动时冻结的人工节点办理客户端，不随角色成员或流程新版本改变。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flow_instance_node_client")
public class FlowInstanceNodeClient extends BaseEntity {
    @TableId("instance_node_client_id")
    private Long id;
    private Long instanceId;
    private String nodeCode;
    private Long clientPk;
    private Boolean applicantNode;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
