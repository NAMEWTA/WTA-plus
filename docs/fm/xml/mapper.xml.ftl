<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${packageName}.mapper.${ClassName}Mapper">

<#if architectureMode == "classic">
<#if table.tree>
    <sql id="Tree_Structure_Columns">
        ${pkColumn.columnName}, ${treeParentColumn.columnName}<#if "" != treeAncestorsField>, ${treeAncestorsColumn}</#if>
    </sql>
    <select id="selectStructure" resultType="${packageName}.domain.${ClassName}">
        select <include refid="Tree_Structure_Columns"/> from ${tableName}
        where ${pkColumn.columnName} = #{id} and del_flag = 0
        <if test="locking">for update</if>
    </select>
    <select id="lockTreeRoots" resultType="${packageName}.domain.${ClassName}">
        select <include refid="Tree_Structure_Columns"/> from ${tableName} force index (PRIMARY)
        where del_flag = 0 and ${pkColumn.columnName} in
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
        order by ${pkColumn.columnName} for update
    </select>
    <select id="selectVisibleForUpdate" resultType="${packageName}.domain.${ClassName}">
        select <include refid="Tree_Structure_Columns"/> from ${tableName}
        where ${pkColumn.columnName} = #{id} and del_flag = 0 for update
    </select>
    <select id="selectTreeChildrenForUpdate" resultType="${packageName}.domain.${ClassName}">
        select <include refid="Tree_Structure_Columns"/> from ${tableName} force index (${treeParentIndex})
        where del_flag = 0 and ${treeParentColumn.columnName} in
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
        order by ${pkColumn.columnName} for update
    </select>
</#if>
<#else>

    <sql id="Base_Column_List">
<#list columns as column>
<#if !table.isSuperColumn(column.javaField)>
        ${column.columnName}<#if column_has_next>,</#if>
</#if>
</#list>
    </sql>

    <select id="selectRowById" resultType="${packageName}.domain.model.read.${ClassName}Row">
        select <include refid="Base_Column_List"/>
        from ${tableName}
        where ${pkColumn.columnName} = #{${pkColumn.javaField}}
    </select>

    <select id="selectPageByCondition" resultType="${packageName}.domain.model.read.${ClassName}Row">
        select <include refid="Base_Column_List"/>
        from ${tableName}
        <where>
<#list columns as column>
<#if column.query && !table.isSuperColumn(column.javaField)>
            <if test="command.${column.javaField} != null<#if column.javaType == 'String'> and command.${column.javaField} != ''</#if>">
                and ${column.columnName} = #{command.${column.javaField}}
            </if>
</#if>
</#list>
        </where>
        order by ${pkColumn.columnName} desc
    </select>

    <select id="selectListByCondition" resultType="${packageName}.domain.model.read.${ClassName}Row">
        select <include refid="Base_Column_List"/>
        from ${tableName}
        <where>
<#list columns as column>
<#if column.query && !table.isSuperColumn(column.javaField)>
            <if test="command.${column.javaField} != null<#if column.javaType == 'String'> and command.${column.javaField} != ''</#if>">
                and ${column.columnName} = #{command.${column.javaField}}
            </if>
</#if>
</#list>
        </where>
        order by ${pkColumn.columnName} desc
    </select>

    <insert id="insertRecord">
        insert into ${tableName}
        <trim prefix="(" suffix=")" suffixOverrides=",">
<#list columns as column>
<#if !table.isSuperColumn(column.javaField)>
            <if test="entity.${column.javaField} != null">${column.columnName},</if>
</#if>
</#list>
        </trim>
        <trim prefix="values (" suffix=")" suffixOverrides=",">
<#list columns as column>
<#if !table.isSuperColumn(column.javaField)>
            <if test="entity.${column.javaField} != null">#{entity.${column.javaField}},</if>
</#if>
</#list>
        </trim>
    </insert>

    <update id="updateRecord">
        update ${tableName}
        <set>
<#list columns as column>
<#if !table.isSuperColumn(column.javaField) && !column.pk>
            <if test="entity.${column.javaField} != null">${column.columnName} = #{entity.${column.javaField}},</if>
</#if>
</#list>
        </set>
        where ${pkColumn.columnName} = #{entity.${pkColumn.javaField}}
    </update>

    <update id="deleteRecords">
        update ${tableName}
        set del_flag = 1
        where ${pkColumn.columnName} in
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </update>
</#if>
</mapper>
