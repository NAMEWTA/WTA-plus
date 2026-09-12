package ${packageName}.usecase;

import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.vo.${ClassName}Vo;
import org.namewta.common.core.domain.PageResult;

import java.util.List;

/**
 * ${functionName} 应用用例合同。
 *
 * <p>入口层只依赖该合同；实现类负责调用 Service，不直接访问 DAO 或 Mapper。</p>
 *
 * @author ${author}
 * @date ${datetime}
 */
public interface ${ClassName}UseCase {

    /**
     * 查询 ${functionName}。
     *
     * @param ${pkColumn.javaField} 主键
     * @return ${functionName}
     */
    ${ClassName}Vo queryById(${pkColumn.javaType} ${pkColumn.javaField});

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
    PageResult<${ClassName}Vo> queryPageList(${ClassName}Bo command,
                                             Integer pageNum,
                                             Integer pageSize,
                                             String orderByColumn,
                                             String isAsc);
</#if>

    /**
     * 查询 ${functionName} 列表。
     *
     * @param command 查询条件
     * @return 查询结果
     */
    List<${ClassName}Vo> queryList(${ClassName}Bo command);

    /**
     * 新增 ${functionName}。
     *
     * @param command 新增命令
     * @return 是否新增成功
     */
    Boolean create(${ClassName}Bo command);

    /**
     * 修改 ${functionName}。
     *
     * @param command 修改命令
     * @return 是否修改成功
     */
    Boolean update(${ClassName}Bo command);

    /**
     * 删除 ${functionName}。
     *
     * @param ${pkColumn.javaField}s 待删除主键
     * @return 是否删除成功
     */
    Boolean remove(${pkColumn.javaType}[] ${pkColumn.javaField}s);
}
