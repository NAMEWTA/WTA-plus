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
@TableName("sso_business_session")
public class SsoBusinessSession extends BaseEntity {
    @TableId private Long businessSessionId;
    private String sessionHash;
    private String clientId;
    private String tokenHash;
    private String encryptedToken;
    private String status;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
