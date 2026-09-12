package org.namewta.test.password.write;

import cn.hutool.crypto.digest.BCrypt;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.system.api.model.RegisterBody;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.ISysDeptService;
import org.namewta.system.service.ISysPostService;
import org.namewta.system.service.ISysRoleService;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;
import org.namewta.system.controller.system.SysProfileController;
import org.namewta.system.controller.system.SysUserController;
import org.namewta.web.service.SysRegisterService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PasswordWritePathUnitTest {

    private static final String STRONG_PASSWORD = "StrongPass1!";

    @Test
    void registrationValidatesPlaintextBeforeHashingAndPersistence() {
        ISysUserService userService = mock(ISysUserService.class);
        ISysClientService clientService = mock(ISysClientService.class);
        ISysUserTypeService userTypeService = mock(ISysUserTypeService.class);
        ISysUserTypeRelService relationService = mock(ISysUserTypeRelService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        CaptchaProperties captcha = new CaptchaProperties();
        captcha.setEnable(false);
        SysClientVo client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setRegisterEnabled(true);
        client.setUserTypeId(7L);
        SysUserTypeVo userType = new SysUserTypeVo();
        userType.setStatus(SystemConstants.NORMAL);
        when(clientService.queryByClientId("web")).thenReturn(client);
        when(userTypeService.queryById(7L)).thenReturn(userType);
        when(userService.checkUserNameUnique(any())).thenReturn(true);
        when(userService.registerUser(any())).thenReturn(false);
        SysRegisterService service = new SysRegisterService(userService, captcha, clientService, userTypeService,
            relationService, policyService);
        RegisterBody body = new RegisterBody();
        body.setClientId("web");
        body.setUsername("new-user");
        body.setPassword(STRONG_PASSWORD);

        assertThrows(UserException.class, () -> service.register(body));

        ArgumentCaptor<SysUserBo> inserted = ArgumentCaptor.forClass(SysUserBo.class);
        InOrder order = inOrder(policyService, userService);
        order.verify(policyService).validateOrThrow(STRONG_PASSWORD);
        order.verify(userService).registerUser(inserted.capture());
        assertTrue(BCrypt.checkpw(STRONG_PASSWORD, inserted.getValue().getPassword()));
    }

    @Test
    void adminAddAndResetValidateBeforeWritingAndNeverKickSessions() {
        ISysUserService userService = mock(ISysUserService.class);
        ISysDeptService deptService = mock(ISysDeptService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        when(userService.checkUserNameUnique(any())).thenReturn(true);
        when(userService.insertUser(any())).thenReturn(1);
        when(userService.resetUserPwd(anyLong(), any())).thenReturn(1);
        SysUserController controller = new SysUserController(userService, mock(ISysRoleService.class),
            mock(ISysPostService.class), deptService, policyService);
        SysUserBo add = user(10L, "added-user", STRONG_PASSWORD);

        controller.add(add);

        InOrder addOrder = inOrder(policyService, userService);
        addOrder.verify(policyService).validateOrThrow(STRONG_PASSWORD);
        addOrder.verify(userService).insertUser(add);
        assertTrue(BCrypt.checkpw(STRONG_PASSWORD, add.getPassword()));

        SysUserBo reset = user(10L, "added-user", "AnotherPass2!");
        controller.resetPwd(reset);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        InOrder resetOrder = inOrder(policyService, userService);
        resetOrder.verify(policyService).validateOrThrow("AnotherPass2!");
        resetOrder.verify(userService).resetUserPwd(org.mockito.ArgumentMatchers.eq(10L), hash.capture());
        assertTrue(BCrypt.checkpw("AnotherPass2!", hash.getValue()));
        verify(userService, never()).deleteUserByIds(any());
    }

    @Test
    void profileChangeRejectsWeakValueBeforeHashingOrWriting() {
        ISysUserService userService = mock(ISysUserService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        SysUserVo existing = new SysUserVo();
        existing.setUserId(15L);
        existing.setPassword(BCrypt.hashpw("old-weak"));
        when(userService.selectUserById(15L)).thenReturn(existing);
        doThrowPolicy(policyService, "weak");
        SysProfileController controller = new SysProfileController(userService, policyService);

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(15L);
            assertThrows(ServiceException.class,
                () -> controller.updatePwd(new SysProfileController.SysUserPasswordBo("old-weak", "weak")));
        }

        verify(policyService).validateOrThrow("weak");
        verify(userService, never()).resetUserPwd(anyLong(), any());
    }

    private static void doThrowPolicy(PasswordPolicyService policyService, String password) {
        org.mockito.Mockito.doThrow(new ServiceException("密码不符合安全策略"))
            .when(policyService).validateOrThrow(password);
    }

    private static SysUserBo user(Long id, String username, String password) {
        SysUserBo user = new SysUserBo();
        user.setUserId(id);
        user.setUserName(username);
        user.setNickName(username);
        user.setPassword(password);
        return user;
    }
}
