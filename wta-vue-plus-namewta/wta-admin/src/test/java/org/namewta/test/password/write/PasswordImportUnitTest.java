package org.namewta.test.password.write;

import cn.hutool.crypto.digest.BCrypt;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysUserImportVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.listener.SysUserImportListener;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PasswordImportUnitTest {

    @Test
    void randomModeGeneratesAndValidatesOneCandidatePerNewUserWithoutLeakingSummary() {
        ISysUserService userService = mock(ISysUserService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        when(policyService.generateDefaultPassword()).thenReturn("FirstPass1!", "SecondPass2!");
        SysUserImportListener listener = new SysUserImportListener(userService, policyService, false, 99L,
            ignored -> { });

        listener.invoke(row("first"), null);
        listener.invoke(row("second"), null);

        ArgumentCaptor<SysUserBo> inserted = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService, org.mockito.Mockito.times(2)).insertUser(inserted.capture());
        List<SysUserBo> users = inserted.getAllValues();
        assertTrue(BCrypt.checkpw("FirstPass1!", users.get(0).getPassword()));
        assertTrue(BCrypt.checkpw("SecondPass2!", users.get(1).getPassword()));
        verify(policyService).validateOrThrow("FirstPass1!");
        verify(policyService).validateOrThrow("SecondPass2!");
        String summary = listener.getExcelResult().getAnalysis();
        assertFalse(summary.contains("FirstPass1!"));
        assertFalse(summary.contains("SecondPass2!"));
    }

    @Test
    void existingUserUpdateNeverGeneratesOrChangesPassword() {
        ISysUserService userService = mock(ISysUserService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        SysUserVo existing = new SysUserVo();
        existing.setUserId(8L);
        existing.setUserName("existing");
        when(userService.selectUserByUserName("existing")).thenReturn(existing);
        SysUserImportListener listener = new SysUserImportListener(userService, policyService, true, 99L,
            ignored -> { });

        listener.invoke(row("existing"), null);

        ArgumentCaptor<SysUserBo> updated = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService).updateUser(updated.capture());
        assertTrue(updated.getValue().getPassword() == null);
        verify(policyService, never()).generateDefaultPassword();
        verify(policyService, never()).validateOrThrow(any());
    }

    private static SysUserImportVo row(String username) {
        SysUserImportVo row = new SysUserImportVo();
        row.setUserName(username);
        row.setNickName(username);
        return row;
    }
}
