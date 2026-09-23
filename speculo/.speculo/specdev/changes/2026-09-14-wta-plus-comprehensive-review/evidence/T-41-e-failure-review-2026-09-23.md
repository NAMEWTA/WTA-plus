# T-41 恢复批 E 第一次验收

Revision179: recovery E a0dcbac8 attempt1 failed real Chrome at spec90 close locator after SQL501/2, real login, top10/detail body passed; source/JAR clean and cleanup[]. Five API-controlled Chrome cases passed5/0/0/0 on exactE using independently verified C production328 artifacts. Retain E failed result; two exact-name close locator fixes authorized within existing e2e scope; no runtime or assertion relaxation.12done/2cancelled/T41in_progress/35ready; oldbatch3 retained.

实际 result969f43a52216aebb 记录源码前后clean E、完整JAR不变、1个Chrome用例failed且0skip；数字位置90:57为顶部详情关闭点击，上一条正文断言已通过。没有保留原始浏览器错误，因此不声称动态取得strict-mode异常正文。后续第26页、B登录隔离、全量read-all尚未运行。

Lead及两个独立reviewer核当前安装Element Plus：dialog header aria-label取zh-cn el.dialog.close“关闭此对话框”；两个业务SFC默认showClose且footer按钮“关闭”。Playwright非exact名称定位同时匹配两者；既有inbox-without-realtime.spec.ts131已使用exact:true且本轮五例实跑通过。因此最小恢复只在inbox-paged-real.e2e.ts90/124增加exact:true，保持真实点击、全部断言、0retry以及原超时。后续实跑负责检验此原因推断。

完整批历史B/C/D三次已在d58保存，Dispatch02后E是第1次；接下来F为第2次，不重置。cors_audit仅改这两处测试定位；Lead提交/构建/真实服务。恢复阶段所有原始记录见recovery-e/manifest.json，已通过结果仅按相同输入明确复用。
