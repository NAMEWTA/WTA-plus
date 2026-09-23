package ${packageName}.service;

import ${packageName}.dao.${ClassName}Dao;
import ${packageName}.domain.${ClassName};
import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.model.read.${ClassName}Row;
import ${packageName}.domain.vo.${ClassName}Vo;
import org.namewta.common.core.utils.MapstructUtils;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ${functionName} 业务能力服务。
 *
 * <p>Service 只承载业务规则和外部端口适配，持久化查询统一委托 DAO。</p>
 *
 * @date ${datetime}
 */
@RequiredArgsConstructor
@Service
public class ${ClassName}Service {

    private final ${ClassName}Dao ${className}Dao;

    /**
     * 查询 ${functionName}。
     *
     * @param ${pkColumn.javaField} 主键
     * @return ${functionName}
     */
    public ${ClassName}Vo queryById(${pkColumn.javaType} ${pkColumn.javaField}) {
        ${ClassName}Row row = ${className}Dao.queryById(${pkColumn.javaField});
        return MapstructUtils.convert(row, ${ClassName}Vo.class);
    }

<#if table.crud>
    /**
     * 分页查询 ${functionName}。
     *
     * @param command 查询条件
     * @param pageNum 当前页码，可为空
     * @param pageSize 每页条数，可为空
     * @param orderByColumn 排序字段，可为空
     * @param isAsc 排序方向，可为空
     * @return 分页结果
     */
    public PageResult<${ClassName}Vo> queryPageList(${ClassName}Bo command,
                                                    Integer pageNum,
                                                    Integer pageSize,
                                                    String orderByColumn,
                                                    String isAsc) {
        PageResult<${ClassName}Row> page = ${className}Dao.queryPageList(
            command, pageNum, pageSize, orderByColumn, isAsc);
        return new PageResult<>(MapstructUtils.convert(List.copyOf(page.getRows()), ${ClassName}Vo.class),
            page.getTotal());
    }
</#if>

    /**
     * 查询 ${functionName} 列表。
     *
     * @param command 查询条件
     * @return 查询结果
     */
    public List<${ClassName}Vo> queryList(${ClassName}Bo command) {
        return MapstructUtils.convert(${className}Dao.queryList(command), ${ClassName}Vo.class);
    }

    /**
     * 新增 ${functionName} 记录并返回操作结果。
     *
     * @param command 新增命令
     * @return 是否新增成功
     */
    public Boolean create(${ClassName}Bo command) {
        return ${className}Dao.insert(MapstructUtils.convert(command, ${ClassName}.class));
    }

    /**
     * 更新 ${functionName} 记录并返回操作结果。
     *
     * @param command 修改命令
     * @return 是否修改成功
     */
    public Boolean update(${ClassName}Bo command) {
        return ${className}Dao.update(MapstructUtils.convert(command, ${ClassName}.class));
    }

    /**
     * 删除 ${functionName} 记录并返回操作结果。
     *
     * @param ${pkColumn.javaField}s 待删除主键
     * @return 是否删除成功
     */
    public Boolean remove(${pkColumn.javaType}[] ${pkColumn.javaField}s) {
        return ${className}Dao.remove(${pkColumn.javaField}s);
    }
}
