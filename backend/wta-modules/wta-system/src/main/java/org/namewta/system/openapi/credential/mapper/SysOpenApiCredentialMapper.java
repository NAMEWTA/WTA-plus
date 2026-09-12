package org.namewta.system.openapi.credential.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.openapi.credential.domain.SysOpenApiCredential;
import org.namewta.system.openapi.credential.model.OpenApiCredentialUserRow;

import java.util.List;

/**
 * Credential persistence queries with explicit logical-delete and owner validity checks.
 */
public interface SysOpenApiCredentialMapper
    extends BaseMapperPlus<SysOpenApiCredential, SysOpenApiCredential> {

    @Select("""
        select *
        from sys_open_api_credential
        where owner_user_id = #{ownerUserId} and del_flag = '0'
        limit 1
        """)
    SysOpenApiCredential selectByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Select("""
        select c.*
        from sys_open_api_credential c
        inner join sys_user u on u.user_id = c.owner_user_id
            and u.status = '0' and u.del_flag = '0'
        where c.app_key = #{appKey} and c.del_flag = '0'
        limit 1
        """)
    SysOpenApiCredential selectUsableByAppKey(@Param("appKey") String appKey);

    @Select("""
        select count(1)
        from sys_user
        where user_id = #{ownerUserId} and status = '0' and del_flag = '0'
        """)
    boolean existsActiveOwner(@Param("ownerUserId") Long ownerUserId);

    @Select("""
        select u.user_id, u.user_name, u.nick_name,
               c.open_api_credential_id as credential_id, c.app_key, c.app_name,
               c.status, c.expires_at, c.remark, c.create_time, c.update_time
        from sys_user u
        left join sys_open_api_credential c on c.owner_user_id = u.user_id and c.del_flag = '0'
        where u.del_flag = '0'
          and (#{keyword} is null or #{keyword} = ''
               or u.user_name like concat('%', #{keyword}, '%')
               or u.nick_name like concat('%', #{keyword}, '%'))
        order by u.user_id
        limit #{limit}
        """)
    List<OpenApiCredentialUserRow> selectUserSummaries(@Param("keyword") String keyword,
                                                        @Param("limit") int limit);
}
