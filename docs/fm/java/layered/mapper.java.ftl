package ${packageName}.mapper;

import ${packageName}.domain.${ClassName};
import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.model.read.${ClassName}Row;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ${functionName} 持久化映射器。
 *
 * <p>分层 Mapper 只向 DAO 暴露实体和读模型，不直接暴露 HTTP VO。</p>
 *
 * @date ${datetime}
 */
public interface ${ClassName}Mapper extends BaseMapperPlus<${ClassName}, ${ClassName}Row> {

    /** 按主键查询读模型。 */
    ${ClassName}Row selectRowById(${pkColumn.javaType} ${pkColumn.javaField});

    /** 按条件分页查询读模型。 */
    Page<${ClassName}Row> selectPageByCondition(Page<${ClassName}Row> page,
                                                 @Param("command") ${ClassName}Bo command);

    /** 按条件查询读模型列表。 */
    List<${ClassName}Row> selectListByCondition(@Param("command") ${ClassName}Bo command);

    /** 插入领域实体。 */
    int insertRecord(@Param("entity") ${ClassName} entity);

    /** 更新领域实体。 */
    int updateRecord(@Param("entity") ${ClassName} entity);

    /** 按主键逻辑删除实体。 */
    int deleteRecords(@Param("ids") List<${pkColumn.javaType}> ids);
}
