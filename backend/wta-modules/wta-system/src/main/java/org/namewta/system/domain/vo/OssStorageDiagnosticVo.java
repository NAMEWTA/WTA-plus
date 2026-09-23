package org.namewta.system.domain.vo;

import java.time.Instant;

/** 管理员单配置诊断公开投影；只含固定类别，不含配置或 Provider 原文。 */
public record OssStorageDiagnosticVo(String status, String reason, Instant checkedAt) {
}
