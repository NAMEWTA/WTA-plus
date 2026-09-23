package ${packageName}.dao;

import ${packageName}.domain.${ClassName};
import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.model.read.${ClassName}Row;
import ${packageName}.mapper.${ClassName}Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.core.domain.PageResult;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

/**
 * ${functionName} 数据访问对象。
 *
 * <p>DAO 统一封装查询条件、分页、锁语义和 Mapper 调用，只返回实体或读模型。</p>
 *
 * @date ${datetime}
 */
@RequiredArgsConstructor
@Repository
public class ${ClassName}Dao {

    private final ${ClassName}Mapper ${className}Mapper;

    /**
     * 按主键查询 ${functionName}。
     *
     * @param ${pkColumn.javaField} 主键
     * @return ${functionName}，不存在时返回 {@code null}
     */
    public ${ClassName}Row queryById(${pkColumn.javaType} ${pkColumn.javaField}) {
        return ${className}Mapper.selectRowById(${pkColumn.javaField});
    }

<#if table.crud>
    /**
     * 按查询条件分页查询 ${functionName}。
     *
     * @param command 查询条件
     * @param pageNum 当前页码，可为空
     * @param pageSize 每页条数，可为空
     * @param orderByColumn 排序字段，可为空
     * @param isAsc 排序方向，可为空
     * @return 分页读模型
     */
    public PageResult<${ClassName}Row> queryPageList(${ClassName}Bo command,
                                                     Integer pageNum,
                                                     Integer pageSize,
                                                     String orderByColumn,
                                                     String isAsc) {
        PageQuery pageQuery = new PageQuery(pageSize, pageNum);
        pageQuery.setOrderByColumn(orderByColumn);
        pageQuery.setIsAsc(isAsc);
        Page<${ClassName}Row> page = ${className}Mapper.selectPageByCondition(pageQuery.build(), command);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
</#if>

    /**
     * 按查询条件查询 ${functionName} 列表。
     *
     * @param command 查询条件
     * @return 查询结果
     */
    public List<${ClassName}Row> queryList(${ClassName}Bo command) {
        return ${className}Mapper.selectListByCondition(command);
    }

    /**
     * 持久化新增命令。
     *
     * @param entity 待持久化实体
     * @return 是否新增成功
     */
    public Boolean insert(${ClassName} entity) {
        return ${className}Mapper.insertRecord(entity) > 0;
    }

    /**
     * 持久化修改命令。
     *
     * @param entity 待持久化实体
     * @return 是否修改成功
     */
    public Boolean update(${ClassName} entity) {
        return ${className}Mapper.updateRecord(entity) > 0;
    }

    /**
     * 按主键批量删除数据。
     *
     * @param ${pkColumn.javaField}s 待删除主键
     * @return 是否删除成功
     */
    public Boolean remove(${pkColumn.javaType}[] ${pkColumn.javaField}s) {
        return ${className}Mapper.deleteRecords(Arrays.asList(${pkColumn.javaField}s)) > 0;
    }
}
