package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.notify.domain.vo.NotifyRecipientUserVo;
import org.namewta.notify.port.NotifyRecipientDirectoryPort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/** 通知目标目录用例，隔离 system 用户合同与 HTTP 入口。 */
@Service
@RequiredArgsConstructor
public class NotifyRecipientUseCase {
    private final NotifyRecipientDirectoryPort directory;

    public PageResult<NotifyRecipientUserVo> search(String keyword, int pageSize) {
        if (keyword == null || keyword.isBlank()) return PageResult.build(List.of(), 0);
        int boundedSize = Math.clamp(pageSize, 1, 50);
        List<NotifyRecipientUserVo> rows = directory.search(keyword.strip(), boundedSize);
        return PageResult.build(rows, rows.size());
    }

    public List<NotifyRecipientUserVo> byIds(String userIds) {
        if (userIds == null || userIds.isBlank()) return List.of();
        try {
            List<Long> ids = Arrays.stream(userIds.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Long::valueOf).toList();
            return directory.byIds(ids);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("用户编号格式错误", exception);
        }
    }
}
