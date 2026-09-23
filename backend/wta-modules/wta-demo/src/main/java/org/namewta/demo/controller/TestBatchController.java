package org.namewta.demo.controller;

import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.log.annotation.Log;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.web.core.BaseController;
import org.namewta.demo.domain.TestDemo;
import org.namewta.demo.mapper.TestDemoMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试批量方法
 *
 * @date 2021-05-30
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/batch")
public class TestBatchController extends BaseController {

    /**
     * 为了便于测试 直接引入mapper
     */
    private final TestDemoMapper testDemoMapper;

    /**
     * 新增批量方法 可完美替代 saveBatch 秒级插入上万数据 (对mysql负荷较大)
     * <p>
     * 3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
     */
    @PostMapping("/add")
//    @DS("slave")
    @Log(title = "批量演示", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> add() {
        List<TestDemo> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            TestDemo testDemo = new TestDemo();
            testDemo.setOrderNum(-1);
            testDemo.setTestKey("批量新增");
            testDemo.setValue("测试新增");
            list.add(testDemo);
        }
        return toAjax(testDemoMapper.insertBatch(list));
    }

    /**
     * 新增或更新 可完美替代 saveOrUpdateBatch 高性能
     * <p>
     * 3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
     */
    @PostMapping("/addOrUpdate")
//    @DS("slave")
    @Log(title = "批量演示", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> addOrUpdate() {
        List<TestDemo> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            TestDemo testDemo = new TestDemo();
            testDemo.setOrderNum(-1);
            testDemo.setTestKey("批量新增");
            testDemo.setValue("测试新增");
            list.add(testDemo);
        }
        testDemoMapper.insertBatch(list);
        for (int i = 0; i < list.size(); i++) {
            TestDemo testDemo = list.get(i);
            testDemo.setTestKey("批量新增或修改");
            testDemo.setValue("批量新增或修改");
            if (i % 2 == 0) {
                testDemo.setId(null);
            }
        }
        return toAjax(testDemoMapper.insertOrUpdateBatch(list));
    }

    /**
     * 删除批量方法
     */
    @PostMapping
//    @DS("slave")
    @Log(title = "批量演示", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> remove() {
        return toAjax(testDemoMapper.lambda()
            .eq(TestDemo::getOrderNum, -1L)
            .deleteCount());
    }

}
