# T-41 恢复批 F 第二次检查点

Revision180: recovery F cb8063b6 attempt2 failed Chrome at B shared-title locator112 after Apage26/oldest/foreign-negative and Blogin/unread2/two rows. Seed title=summary renders twice; authorize two unique table-row/title-cell assertions, retaining counts and all negative/readAll checks. Ffull/core/static/OpenAPI pass, source/JAR stable cleanup[]. Oldbatch3 and recoveryE/F retained; nextG attempt3.12done/2cancelled/T41in_progress/35ready.

run62047a08743c9ffa的数字失败位置112:54，前面的顶部关闭、A第26页最旧一条、A不可见B专属及B实际登录/未读2/表格2行均通过；全量read-all和最终SQL核对尚未运行。F clean源码及JAR前后一致，临时资源全部清理。

三个reviewer静态核seed_plan把title/message/content设相同合成文本，InboxPage分别渲染标题和摘要；B shared/bOnly两个页面级正向文本选择器会匹配两列。原始错误正文未保留，具体strict-mode异常仍为源码推断。唯一产品writer cors_audit只在这两个正向断言使用表格行归属与精确标题单元格，先断言唯一行；不对页面级重复文本使用first，不删除/放宽原有2行总数、他人缺席、读取或时间戳断言。Lead取得非空测试修复commit后执行G完整浏览器，恢复批第3次，不重置计数。

F core/package、bundle、Skill/fullstack/layered/FM/handbook及OpenAPI check都实际exit0；后续只按源码输入等价复用。详见 recovery-f/manifest.json。若G失败则按三次规则先停止重派并复盘。
