package org.namewta.system.api;

import org.namewta.system.api.domain.UserDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用 用户服务
 *
 * @author Lion Li
 */
public interface UserService {

    /**
     * Search active users for closed business candidate selection.
     *
     * @param keyword username, nickname, or phone keyword
     * @param limit maximum number of candidates
     * @return active user candidates
     */
    default List<UserDTO> searchActiveUsers(String keyword, int limit) {
        return List.of();
    }

    /**
     * 在事务内锁定并返回一个当前有效的用户账户。
     *
     * <p>调用方必须在覆盖后续业务状态变更的动态数据源事务中调用本方法。旧实现默认拒绝该能力，
     * 避免在没有真实数据库行锁时静默退化为普通查询。</p>
     *
     * @param userId 用户 ID，必须为正数
     * @return 未删除且状态正常的用户；不存在时返回 {@code null}
     * @throws UnsupportedOperationException 当前实现不支持事务内用户锁定时抛出
     */
    default UserDTO lockActiveById(Long userId) {
        throw new UnsupportedOperationException("Active user locking is not supported by this implementation");
    }

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    String selectUserNameById(Long userId);

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    String selectNicknameById(Long userId);

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userIds 用户ID 多个用逗号隔开
     * @return 用户昵称
     */
    String selectNicknameByIds(String userIds);

    /**
     * 通过用户ID查询用户手机号
     *
     * @param userId 用户id
     * @return 用户手机号
     */
    String selectPhonenumberById(Long userId);

    /**
     * 通过用户ID查询用户邮箱
     *
     * @param userId 用户id
     * @return 用户邮箱
     */
    String selectEmailById(Long userId);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户id
     * @return 用户列表
     */
    UserDTO selectById(Long userId);

    /**
     * 通过用户ID查询用户列表
     *
     * @param userIds 用户ids
     * @return 用户列表
     */
    List<UserDTO> selectListByIds(Collection<Long> userIds);

    /**
     * 查询可接收通知的全部正常用户。
     *
     * @param limit 最大返回数量
     * @return 正常用户列表
     */
    List<UserDTO> selectAllActiveUsers(int limit);

    /** 分页查询可接收通知的正常用户，供大规模异步通知分批生成收件人快照。 */
    List<UserDTO> selectAllActiveUsers(int offset, int limit);

    /**
     * 通过角色ID查询用户ID
     *
     * @param roleIds 角色ids
     * @return 用户ids
     */
    List<Long> selectUserIdsByRoleIds(Collection<Long> roleIds);

    /**
     * 通过角色ID查询用户
     *
     * @param roleIds 角色ids
     * @return 用户
     */
    List<UserDTO> selectUsersByRoleIds(Collection<Long> roleIds);

    /**
     * 通过部门ID查询用户
     *
     * @param deptIds 部门ids
     * @return 用户
     */
    List<UserDTO> selectUsersByDeptIds(Collection<Long> deptIds);

    /**
     * 通过岗位ID查询用户
     *
     * @param postIds 岗位ids
     * @return 用户
     */
    List<UserDTO> selectUsersByPostIds(Collection<Long> postIds);

    /**
     * 通过用户类型查询正常用户。
     *
     * @param userTypeIds 用户类型 ID
     * @return 去重后的正常用户
     */
    List<UserDTO> selectUsersByUserTypeIds(Collection<Long> userTypeIds);

    /** 查询当前操作者数据权限内的正常通知接收者；不存在或停用的用户被过滤，越权用户拒绝。 */
    List<UserDTO> selectNotificationUsers(Collection<Long> userIds);

    /**
     * 根据用户 ID 列表查询用户昵称映射关系
     *
     * @param userIds 用户 ID 列表
     * @return Map，其中 key 为用户 ID，value 为对应的用户昵称
     */
    Map<Long, String> selectUserNicksByIds(Collection<Long> userIds);

}
