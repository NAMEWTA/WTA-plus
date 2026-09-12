# T-03 Routing Provider

状态：done

已实现 `ALL` 编排、统一 SMS/Mail/In-App Provider Adapter、直接目标与逻辑用户解析、供应商状态归并及回调验签入口。Provider 选择仍封装在 notify，不向业务暴露 `providerKey`。
