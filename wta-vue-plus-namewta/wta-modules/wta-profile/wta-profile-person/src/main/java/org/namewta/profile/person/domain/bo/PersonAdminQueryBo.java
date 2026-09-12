package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;

/** PersonAdminQueryBo 请求参数模型。 */
public record PersonAdminQueryBo(String fullName, String documentNumber, String status, int pageNum, int pageSize) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
