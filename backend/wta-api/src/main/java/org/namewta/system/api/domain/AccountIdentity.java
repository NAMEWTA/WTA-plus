package org.namewta.system.api.domain;

/**
 * 身份协议使用的账户最小内部投影；必须按应用字段授权转换，不能整体序列化给第三方。
 * @param userId 账户主键
 * @param username 用户名
 * @param nickname 昵称
 * @param email 账户邮箱，未提供核验承诺
 * @param phoneNumber 账户手机号，未提供核验承诺
 * @param avatarId 头像 OSS 主键，不是可公开的永久 URL
 * @param status 正常账户状态
 */
public record AccountIdentity(Long userId, String username, String nickname, String email,
                              String phoneNumber, Long avatarId, String status) {
}

