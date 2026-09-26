# T42 A1 编译前置失败与审查记录

候选0595e2ccbd03474bced92809b2ac9056c9a15751，tree e45e29269255bf976ea481a253ef2aeac8336397；定向 Maven exit 1，三个新 OSS 测试缺少 close() checked exception 声明，实际执行测试0。不能将0fail/0error误称为通过。a20c977f285d449732301eb42cc8bd1ed67a75c7仅修复三个throws，尚未重跑。

两轴静态审查 request changes；确定的问题为取消后仍在途的旧发送可误释放、无邮箱USER永久占用引用、hold用例失败清理与v4代理接口不一致。fixture followup绑定未提交的两文件修复，仅静态结论。原报告和v4不覆盖；v5仅离线准备，真实服务尚未运行。

本次作为候选尝试1保留，包括编译前置失败；full-suite/E2E仍pending，无AC勾选。后续真实方法清单必须按新固定源码重建，不能沿用15或16计数。
