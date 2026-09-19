# 客户端来源地址

公网 LB（HTTP/TLS）用 socket `$remote_addr` 覆盖外来 XFF，并清除 `Forwarded`；内层 App Nginx 继续追加它的 socket peer。新增 App 的生成器遵循同一合同。不要把内层 App 代理直接当作公网信任入口。

后端 `TRUSTED_PROXY_CIDRS` 是逗号分隔的数字 IPv4/IPv6 CIDR，默认空集合，未配置时只使用 socket peer。配置应只包含受控 LB/内层代理的实际地址范围；不把所有私网、客户端网段或任意来源作为默认可信代理。应用从右到左跳过可信 hop，取最右侧不可信来源。畸形可信链返回 400；不可信 socket peer 的所有转发头都被忽略。

`server.forward-headers-strategy` 必须保持 `none`，容器不能先按外来头改写 socket peer。配置代理 CIDR 错误时启动失败；调整信任配置需要重启，并重新跑直连、单/双代理、IPv4/IPv6及伪造头矩阵。新增外部网关时，应由该受控入口清洗头并重新确定边界。

真实部署的代理地址、客户端地址、正常白名单与限流请求仍需发布前实测。仓内隔离测试网段不能复制成生产信任值。
