package org.namewta.system.service.impl;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.TaskAssigneeService;
import org.namewta.system.api.domain.TaskAssigneeDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.api.model.TaskAssigneeBody;
import org.namewta.system.domain.bo.SysDeptBo;
import org.namewta.system.domain.bo.SysPostBo;
import org.namewta.system.domain.bo.SysRoleBo;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysDeptVo;
import org.namewta.system.domain.vo.SysPostVo;
import org.namewta.system.domain.vo.SysRoleVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.service.ISysDeptService;
import org.namewta.system.service.ISysPostService;
import org.namewta.system.service.ISysRoleService;
import org.namewta.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工作流设计器获取任务执行人
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysTaskAssigneeServiceImpl implements TaskAssigneeService {

    // 上级Service注入下级Service 其他Service永远不可能注入当前类 避免循环注入
    private final ISysPostService postService;
    private final ISysDeptService deptService;
    private final ISysUserService userService;
    private final ISysRoleService roleService;

    /**
     * 查询角色并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    @Override
    public TaskAssigneeDTO selectRolesByTaskAssigneeList(TaskAssigneeBody taskQuery) {
        PageQuery pageQuery = new PageQuery(taskQuery.getPageSize(), taskQuery.getPageNum());
        SysRoleBo bo = new SysRoleBo();
        bo.setRoleKey(taskQuery.getHandlerCode());
        bo.setRoleName(taskQuery.getHandlerName());
        bo.setStatus(SystemConstants.NORMAL);
        LoginUser loginUser = LoginHelper.getLoginUser();
        bo.setClientId(loginUser == null ? null : loginUser.getClientPk());
        Map<String, Object> params = bo.getParams();
        params.put("beginTime", taskQuery.getBeginTime());
        params.put("endTime", taskQuery.getEndTime());
        PageResult<SysRoleVo> page = roleService.selectPageRoleList(bo, pageQuery);
        // 使用封装的字段映射方法进行转换
        List<TaskAssigneeDTO.TaskHandler> handlers = TaskAssigneeDTO.convertToHandlerList(page.getRows(),
            item -> Convert.toStr(item.getRoleId()), SysRoleVo::getRoleKey, SysRoleVo::getRoleName, item -> "", SysRoleVo::getCreateTime);
        return new TaskAssigneeDTO(page.getTotal(), handlers);
    }

    /**
     * 查询岗位并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    @Override
    public TaskAssigneeDTO selectPostsByTaskAssigneeList(TaskAssigneeBody taskQuery) {
        PageQuery pageQuery = new PageQuery(taskQuery.getPageSize(), taskQuery.getPageNum());
        SysPostBo bo = new SysPostBo();
        bo.setPostCategory(taskQuery.getHandlerCode());
        bo.setPostName(taskQuery.getHandlerName());
        bo.setStatus(SystemConstants.NORMAL);
        Map<String, Object> params = bo.getParams();
        params.put("beginTime", taskQuery.getBeginTime());
        params.put("endTime", taskQuery.getEndTime());
        bo.setBelongDeptId(Convert.toLong(taskQuery.getGroupId()));
        PageResult<SysPostVo> page = postService.selectPagePostList(bo, pageQuery);
        // 使用封装的字段映射方法进行转换
        List<TaskAssigneeDTO.TaskHandler> handlers = TaskAssigneeDTO.convertToHandlerList(page.getRows(),
            item -> Convert.toStr(item.getPostId()), SysPostVo::getPostCategory, SysPostVo::getPostName, item -> Convert.toStr(item.getDeptId()), SysPostVo::getCreateTime);
        return new TaskAssigneeDTO(page.getTotal(), handlers);
    }

    /**
     * 查询部门并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    @Override
    public TaskAssigneeDTO selectDeptsByTaskAssigneeList(TaskAssigneeBody taskQuery) {
        PageQuery pageQuery = new PageQuery(taskQuery.getPageSize(), taskQuery.getPageNum());
        SysDeptBo bo = new SysDeptBo();
        bo.setDeptCategory(taskQuery.getHandlerCode());
        bo.setDeptName(taskQuery.getHandlerName());
        bo.setStatus(SystemConstants.NORMAL);
        Map<String, Object> params = bo.getParams();
        params.put("beginTime", taskQuery.getBeginTime());
        params.put("endTime", taskQuery.getEndTime());
        bo.setBelongDeptId(Convert.toLong(taskQuery.getGroupId()));
        PageResult<SysDeptVo> page = deptService.selectPageDeptList(bo, pageQuery);
        // 使用封装的字段映射方法进行转换
        List<TaskAssigneeDTO.TaskHandler> handlers = TaskAssigneeDTO.convertToHandlerList(page.getRows(),
            item -> Convert.toStr(item.getDeptId()), SysDeptVo::getDeptCategory, SysDeptVo::getDeptName, item -> Convert.toStr(item.getParentId()), SysDeptVo::getCreateTime);
        return new TaskAssigneeDTO(page.getTotal(), handlers);
    }

    /**
     * 查询用户并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    @Override
    public TaskAssigneeDTO selectUsersByTaskAssigneeList(TaskAssigneeBody taskQuery) {
        PageQuery pageQuery = new PageQuery(taskQuery.getPageSize(), taskQuery.getPageNum());
        SysUserBo bo = new SysUserBo();
        bo.setUserName(taskQuery.getHandlerCode());
        bo.setNickName(taskQuery.getHandlerName());
        bo.setStatus(SystemConstants.NORMAL);
        Map<String, Object> params = bo.getParams();
        params.put("beginTime", taskQuery.getBeginTime());
        params.put("endTime", taskQuery.getEndTime());
        bo.setDeptId(Convert.toLong(taskQuery.getGroupId()));
        PageResult<SysUserVo> page = userService.selectPageUserList(bo, pageQuery);
        // 使用封装的字段映射方法进行转换
        List<TaskAssigneeDTO.TaskHandler> handlers = TaskAssigneeDTO.convertToHandlerList(page.getRows(),
            item -> Convert.toStr(item.getUserId()), SysUserVo::getUserName, SysUserVo::getNickName, item -> Convert.toStr(item.getDeptId()), SysUserVo::getCreateTime);
        return new TaskAssigneeDTO(page.getTotal(), handlers);
    }

}
