package org.namewta.oidc.usecase;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.service.OidcInteractionService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 浏览器交互入口。 */
@Component
@RequiredArgsConstructor
public class OidcInteractionUseCase {
    private final OidcInteractionService service;

    /** 保存授权或退出交互的浏览器绑定快照，返回短期一次性标识。 */
    public String create(Map<String, String> p, String b, String sid, boolean force) {
        return service.create(p, b, sid, force);
    }

    /** 读取且校验交互所属浏览器，过期或绑定不符时返回协议错误。 */
    public OidcInteractionService.Interaction require(String id, String b) {
        return service.require(id, b);
    }

    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public void consume(String id, String b) {
        service.consume(id, b);
    }
}
