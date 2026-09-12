# Home Web

应用用户门户和用户中心。公开首页不要求登录，登录后通过服务端返回的菜单进入档案中心，认证表单直接调用 `wta-profile` 的 self API。

本 App 使用独立 `Home-Token` 会话键和 `home` Client，不导入 `admin-web` 内部实现。
