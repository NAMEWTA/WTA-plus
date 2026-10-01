package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.oidc.domain.OidcApplication;
import org.namewta.oidc.mapper.OidcApplicationMapper;
import org.springframework.stereotype.Repository;

/** 应用登记持久化边界。 */
@Repository
@RequiredArgsConstructor
@DS("master")
public class OidcApplicationDao {
    private final OidcApplicationMapper mapper;

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OidcApplication find(Long id) {
        return mapper.selectById(id);
    }

    /** 退出只读入口保留已删除应用的登记，不恢复其签发能力。 */
    public OidcApplication findForLogout(Long id) {
        return mapper.findForLogout(id);
    }

    /** 读取独立 OIDC 客户端登记，不进行第一方 Client 准入。 */
    public OidcApplication client(String id) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcApplication>().eq(OidcApplication::getClientId, id));
    }

    /** 在签发事务中锁定应用当前行，与策略更新和撤销串行。 */
    public OidcApplication lockClient(String id) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcApplication>()
                        .eq(OidcApplication::getClientId, id)
                        .last("FOR UPDATE"));
    }

    /** 按确定顺序分页查询，限制单页记录数量。 */
    public PageResult<OidcApplication> page(String name, int page, int size) {
        var q =
                new LambdaQueryWrapper<OidcApplication>()
                        .like(name != null && !name.isBlank(), OidcApplication::getName, name)
                        .orderByDesc(OidcApplication::getApplicationId);
        var result =
                mapper.selectPage(
                        new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), q);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /** 写入新建领域记录并保留数据库唯一约束。 */
    public int insert(OidcApplication app) {
        return mapper.insert(app);
    }

    /** 校验输入和乐观版本后更新允许编辑的字段。 */
    public int update(OidcApplication app) {
        return mapper.updateById(app);
    }

    /** 按乐观版本删除应用并终止其既有授权。 */
    public int delete(OidcApplication app) {
        return mapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                OidcApplication>()
                        .eq(OidcApplication::getApplicationId, app.getApplicationId())
                        .eq(OidcApplication::getVersion, app.getVersion())
                        .set(OidcApplication::getEnabled, false)
                        .set(OidcApplication::getDelFlag, "1")
                        .setSql("version=version+1"));
    }
}
