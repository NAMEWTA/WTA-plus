package org.namewta.system.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.http.HtmlUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.excel.core.ExcelListener;
import org.namewta.common.excel.core.ExcelResult;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysUserImportVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysUserService;

import java.util.List;
import java.util.function.Consumer;

/**
 * 系统用户自定义导入
 */
@Slf4j
public class SysUserImportListener extends AnalysisEventListener<SysUserImportVo> implements ExcelListener<SysUserImportVo> {

    private static final String NL = "\n";

    private final ISysUserService userService;

    private final PasswordPolicyService passwordPolicyService;

    private final Boolean isUpdateSupport;

    private final Long operUserId;

    private final Consumer<SysUserBo> validator;

    private int successNum = 0;
    private int failureNum = 0;
    private final StringBuilder successMsg = new StringBuilder();
    private final StringBuilder failureMsg = new StringBuilder();

    /**
     * 构造用户导入监听器。
     *
     * @param userService           用户服务
     * @param passwordPolicyService 密码策略服务
     * @param isUpdateSupport       是否允许更新已存在用户
     * @param operUserId            操作人用户 ID
     */
    public SysUserImportListener(ISysUserService userService, PasswordPolicyService passwordPolicyService,
                                 Boolean isUpdateSupport, Long operUserId) {
        this(userService, passwordPolicyService, isUpdateSupport, operUserId, ValidatorUtils::validate);
    }

    /**
     * 构造可替换行校验器的用户导入监听器。
     *
     * @param userService           用户服务
     * @param passwordPolicyService 密码策略服务
     * @param isUpdateSupport       是否允许更新已存在用户
     * @param operUserId            操作人用户 ID
     * @param validator             用户行校验器
     */
    public SysUserImportListener(ISysUserService userService, PasswordPolicyService passwordPolicyService,
                                 Boolean isUpdateSupport, Long operUserId, Consumer<SysUserBo> validator) {
        this.userService = userService;
        this.passwordPolicyService = passwordPolicyService;
        this.isUpdateSupport = isUpdateSupport;
        this.operUserId = operUserId;
        this.validator = validator;
    }

    /**
     * 逐行处理用户导入数据。
     *
     * @param userVo  导入用户数据
     * @param context Excel 解析上下文
     */
    @Override
    public void invoke(SysUserImportVo userVo, AnalysisContext context) {
        SysUserVo sysUser = this.userService.selectUserByUserName(userVo.getUserName());
        try {
            // 验证是否存在这个用户
            if (ObjectUtil.isNull(sysUser)) {
                SysUserBo user = BeanUtil.toBean(userVo, SysUserBo.class);
                validator.accept(user);
                String password = passwordPolicyService.generateDefaultPassword();
                passwordPolicyService.validateOrThrow(password);
                user.setPassword(BCrypt.hashpw(password));
                user.setCreateBy(operUserId);
                userService.insertUser(user);
                successNum++;
                successMsg.append(NL).append(successNum).append("、账号 ").append(user.getUserName()).append(" 导入成功");
            } else if (isUpdateSupport) {
                Long userId = sysUser.getUserId();
                SysUserBo user = BeanUtil.toBean(userVo, SysUserBo.class);
                user.setUserId(userId);
                validator.accept(user);
                userService.checkUserAllowed(user.getUserId());
                userService.checkUserDataScope(user.getUserId());
                user.setUpdateBy(operUserId);
                userService.updateUser(user);
                successNum++;
                successMsg.append(NL).append(successNum).append("、账号 ").append(user.getUserName()).append(" 更新成功");
            } else {
                failureNum++;
                failureMsg.append(NL).append(failureNum).append("、账号 ").append(sysUser.getUserName()).append(" 已存在");
            }
        } catch (Exception e) {
            failureNum++;
            String msg = NL + failureNum + "、账号 " + HtmlUtil.cleanHtmlTag(userVo.getUserName()) + " 导入失败：";
            String message = e.getMessage();
            if (e instanceof ConstraintViolationException cvException) {
                message = StreamUtils.join(cvException.getConstraintViolations(), ConstraintViolation::getMessage, ", ");
            }
            failureMsg.append(msg).append(message);
            log.error(msg, e);
        }
    }

    /**
     * 所有数据解析完成后的回调。
     *
     * @param context Excel 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }

    /**
     * 获取用户导入结果。
     *
     * @return Excel 导入结果
     */
    @Override
    public ExcelResult<SysUserImportVo> getExcelResult() {
        return new ExcelResult<>() {

            /**
             * 获取导入结果分析消息。
             *
             * @return 导入结果消息
             */
            @Override
            public String getAnalysis() {
                if (failureNum > 0) {
                    failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
                    throw new ServiceException(failureMsg.toString());
                } else {
                    successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
                }
                return successMsg.toString();
            }

            /**
             * 获取导入成功数据列表。
             *
             * @return 导入成功数据列表
             */
            @Override
            public List<SysUserImportVo> getList() {
                return null;
            }

            /**
             * 获取导入错误信息列表。
             *
             * @return 导入错误信息列表
             */
            @Override
            public List<String> getErrorList() {
                return null;
            }
        };
    }
}
