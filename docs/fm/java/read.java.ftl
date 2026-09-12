package ${packageName}.domain.model.read;

<#list importList as import>
import ${import};
</#list>
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * ${functionName} 查询读模型 ${tableName}。
 *
 * <p>该类型只在 Mapper、DAO、Service 查询边界使用，不作为 HTTP VO，避免持久化模型
 * 与传输合同耦合。</p>
 *
 * @author ${author}
 * @date ${datetime}
 */
@Data
public class ${ClassName}Row implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

<#list columns as column>
<#if !table.isSuperColumn(column.javaField)>
    /**
     * ${column.columnComment}
     */
    private ${column.javaType} ${column.javaField};

</#if>
</#list>
}
