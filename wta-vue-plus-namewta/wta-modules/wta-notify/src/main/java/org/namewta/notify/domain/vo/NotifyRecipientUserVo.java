package org.namewta.notify.domain.vo;

import lombok.Data;
import org.namewta.system.api.domain.UserDTO;

/** 通知目标选择器使用的最小用户视图。 */
@Data
public class NotifyRecipientUserVo {
    private Long userId;
    private String userName;
    private String nickName;
    private String phoneNumber;
    private String status;

    public static NotifyRecipientUserVo from(UserDTO user) {
        NotifyRecipientUserVo view = new NotifyRecipientUserVo();
        view.userId = user.getUserId();
        view.userName = user.getUserName();
        view.nickName = user.getNickName();
        view.phoneNumber = user.getPhoneNumber();
        view.status = user.getStatus();
        return view;
    }
}
