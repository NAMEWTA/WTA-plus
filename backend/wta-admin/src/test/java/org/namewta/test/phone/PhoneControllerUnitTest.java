package org.namewta.test.phone;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.controller.system.SysProfileController;
import org.namewta.system.controller.system.SysUserController;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.bo.SysUserProfileBo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 重复号码必须按最终落库值检查，不能由前后空白绕过。 */
@Tag("dev")
class PhoneControllerUnitTest {
    @ParameterizedTest
    @ValueSource(strings = {"add", "edit", "profile"})
    void duplicateNormalizedNumberRejectsBeforePersistence(String entry) {
        ISysUserService users = mock(ISysUserService.class);
        when(users.checkUserNameUnique(any())).thenReturn(true);
        when(users.checkPhoneUnique(any())).thenAnswer(call -> {
            SysUserBo user = call.getArgument(0);
            return !"13800138000".equals(user.getPhoneNumber());
        });
        var passwords = mock(PasswordPolicyService.class);
        var controller = new SysUserController(users, mock(ISysRoleService.class), mock(ISysPostService.class),
            mock(ISysDeptService.class), passwords);
        var user = new SysUserBo();
        user.setUserId(42L); user.setUserName("phone-user"); user.setPassword("OwnedPass!9");
        user.setPhoneNumber(" 13800138000 ");

        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            login.when(LoginHelper::getUsername).thenReturn("phone-user");
            var profile = new SysUserProfileBo(); profile.setPhoneNumber(user.getPhoneNumber());
            var result = switch (entry) {
                case "add" -> controller.add(user);
                case "edit" -> controller.edit(user);
                default -> new SysProfileController(users, passwords).updateProfile(profile);
            };
            assertThat(result.getMsg()).contains("手机号码已存在");
        }
        verify(users, never()).insertUser(any());
        verify(users, never()).updateUser(any());
        verify(users, never()).updateUserProfile(any());
        verifyNoInteractions(passwords);
    }
}
