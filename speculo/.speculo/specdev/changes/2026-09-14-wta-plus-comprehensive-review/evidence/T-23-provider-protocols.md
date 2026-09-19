# T-23 供应商协议取证（2026-09-19）

仅依据官方协议与当前3.3.5 JAR；未向真实短信/邮件账号发送消息。发送账号预设与回执接入分开验收，不把现有自定义HMAC协议冒充厂商原生回调。

| 来源 | 经核实的合同及实施影响 |
|---|---|
| [腾讯云短信状态通知](https://cloud.tencent.com/document/product/382/59178) | 数组，单次至多100条；sid对应SendSms SerialNo，另含nationcode/mobile/report_status/user_receive_time。未定义通用eventId或现有X-Notify-Signature。失败后再试2次，未给出最长总时长。|
| [腾讯云SendSms](https://cloud.tencent.com/document/api/382/38778) | 每个目标的SendStatusSet中有PhoneNumber和SerialNo；保留目标对应流水号，RequestId不能代替。模板变量按数组位置传递。|
| [阿里云SendSms](https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms) | Code=OK为受理；BizId为回执关联号，RequestId仅为请求ID。API不承诺发送幂等，成功但缺关联号不能冒称发送失败并自动重发。|
| [阿里云HTTP SmsReport](https://help.aliyun.com/zh/sms/developer-reference/smsreport-http) | 数组；BizId在同账号不同批次不同，批次内可共享，必须结合目标。HTTP非200/超时重试；消费失败应50x，业务体code数值并不决定成功。页面写1/5/10分钟及总次数说明，不能与FAQ混用为唯一最大窗口。|
| [阿里云重试FAQ](https://help.aliyun.com/zh/document_detail/2679064.html) | 另一官方页面写总计10次及较长间隔，与当前HTTP页冲突。不能据此猜receipt清理期限；实现默认不自动删除幂等receipt。|
| [腾讯云邮箱SMTP配置](https://intl.cloud.tencent.com/zh/document/product/1266/71700) | QQ=smtp.qq.com，腾讯企业邮箱=smtp.exmail.qq.com，163=smtp.163.com；三种均预置465/SSL。表内另有578/587笔误，预设不使用该分支。|
| [网易官方客户端设置](https://help.mail.126.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac25c12dcb3d46222b6) | 使用客户端授权密码；SSL SMTP465。账号开通SMTP仍由邮箱所有者完成。|
| [SMTP RFC5321](https://www.rfc-editor.org/rfc/rfc5321) | SMTP接受邮件承担后续投递责任，不等于最终用户已收到；预置SMTP账号不虚构HTTP送达事件。|

## 当前工作树与SDK证据

providerKey由NotifyConfigService/NotifySendPlanner选择渠道账号configKey；数据库按(channel,config_key)唯一。Sms4jBlendRegistry注册AlibabaConfig/TencentConfig，配置含AccessKey/Secret、签名及SdkAppId；原Sms4jNotificationProviderResolver丢弃全部成功响应ID。

SMS4J 3.3.5的AlibabaSmsImpl把完整JSON放入SmsResponse.data；TencentSmsImpl也保存完整JSON，其Response.SendStatusSet包含逐目标结果。SmsUtils.toArray(Map)按values迭代顺序形成位置数组，而NotifyTemplateContent使用Map.copyOf，不保证该顺序；腾讯模板必须在发送边界按明确1..N参数位置排序，不能依赖Map迭代。

取证命令javap -c -p及JAR SHA见T-23-sdk-inspection.json与对应文本；第三方JAR未修改。邮件官方页面部分旧链接仅渲染目录或不可访问，参数依据上表可回读的腾讯云/网易官方页，不引用经销商或博客作合同。

## 尚待内部设计与验证

原生短信推送未提供现有统一HMAC字段；不得只接受未鉴权公网状态并宣称安全验签。尚需冻结实际安全接入方法及持久事件身份、多目标/冲突/早到规则。用户已授权自主完成，不再等待其提供协议或重试窗口。当前T-23 in_progress、尚无回执实现通过声明。
