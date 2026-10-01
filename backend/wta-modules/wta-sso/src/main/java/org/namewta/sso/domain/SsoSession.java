package org.namewta.sso.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

/** 中央会话及关联业务会话的持久事实，Redis不拥有撤销权威。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sso_session")
public class SsoSession extends BaseEntity {
    @TableId private Long sessionId;
    private String sessionHash;
    private Long userId;
    private String username;
    private java.time.Instant authenticatedAt;
    private java.time.Instant expiresAt;
    private String status;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
