package org.namewta.demo.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.namewta.demo.domain.bo.TestDemoBo;
import org.namewta.demo.domain.vo.TestDemoVo;
import org.namewta.demo.domain.TestDemo;
import org.namewta.demo.mapper.TestDemoMapper;
import org.namewta.demo.service.ITestDemoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * 模板集成验收Service业务层处理
 *
 * @author NAMEWTA
 * @date 2026-09-19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TestDemoServiceImpl implements ITestDemoService {

    private final TestDemoMapper testDemoMapper;

    /**
     * 查询模板集成验收
     *
     * @param id 主键
     * @return 模板集成验收
     */
    @Override
    public TestDemoVo queryById(Long id) {
        return testDemoMapper.selectVoById(id);
    }

    /**
     * 分页查询模板集成验收列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 模板集成验收分页列表
     */
    @Override
    public PageResult<TestDemoVo> queryPageList(TestDemoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestDemo> lqw = buildQueryWrapper(bo);
        Page<TestDemoVo> result = testDemoMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    @Override
    public List<TestDemoVo> queryList(TestDemoBo bo) {
        LambdaQueryWrapper<TestDemo> lqw = buildQueryWrapper(bo);
        return testDemoMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<TestDemo> buildQueryWrapper(TestDemoBo bo) {
        return QueryBuilder.lambda(TestDemo.class)
            .orderByAsc(TestDemo::getId)
            .build();
    }

    /**
     * 新增模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(TestDemoBo bo) {
        TestDemo add = MapstructUtils.convert(bo, TestDemo.class);
        boolean flag = testDemoMapper.insert(add) > 0;
        if (flag) bo.setId(add.getId());
        return flag;
    }

    /**
     * 修改模板集成验收；树变更在同一动态事务内校验结构与数据范围。
     *
     * @param bo 编辑参数
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(TestDemoBo bo) {
        TestDemo update = MapstructUtils.convert(bo, TestDemo.class);
        return testDemoMapper.updateById(update) > 0;
    }




    /**
     * 批量删除模板集成验收；树节点必须全部可见且没有子节点。
     *
     * @param ids 待删除主键
     * @param isValid 保留的可选业务校验标志，不能跳过树完整性或数据权限
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return testDemoMapper.deleteByIds(ids) > 0;
    }
}
