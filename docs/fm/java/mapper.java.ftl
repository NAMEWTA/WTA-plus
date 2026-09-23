package ${packageName}.mapper;

import ${packageName}.domain.${ClassName};
import ${packageName}.domain.vo.${ClassName}Vo;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
<#if table.tree>
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import java.util.List;
</#if>

/**
 * ${functionName}Mapper接口
 *
 * @date ${datetime}
 */
public interface ${ClassName}Mapper extends BaseMapperPlus<${ClassName}, ${ClassName}Vo> {

<#if table.tree>
    /** 仅用于内部结构校验，不返回给HTTP调用者。 */
    @InterceptorIgnore(dataPermission = "true")
    ${ClassName} selectStructure(@Param("id") ${pkColumn.javaType} id, @Param("locking") boolean locking);

    /** 按主键顺序锁定结构根。 */
    @InterceptorIgnore(dataPermission = "true")
    List<${ClassName}> lockTreeRoots(@Param("ids") List<${pkColumn.javaType}> ids);

    /** 通过资源数据权限校验当前节点和变化的新父节点。 */
    ${ClassName} selectVisibleForUpdate(@Param("id") ${pkColumn.javaType} id);

    /** 包含隐藏子节点，防止删除留下孤儿；必须配套父列索引。 */
    @InterceptorIgnore(dataPermission = "true")
    List<${ClassName}> selectTreeChildrenForUpdate(@Param("ids") List<${pkColumn.javaType}> ids);
</#if>
}
