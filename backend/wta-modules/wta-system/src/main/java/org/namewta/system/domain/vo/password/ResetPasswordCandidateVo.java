package org.namewta.system.domain.vo.password;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.namewta.system.domain.vo.SysUserInfoVo;

/**
 * 用户初始化或永久重置使用的可编辑密码候选。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResetPasswordCandidateVo extends SysUserInfoVo {

    private String password;
}
