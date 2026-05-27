# u-trade 项目整体审查报告

> **审查方式**：只读代码审查，未修改任何项目文件  
> **审查日期**：2026-05-26  
> **审查范围**：gateway、u-common、u-api、user/item/order/wallet/search/admin/chat 共 8 个可运行模块

---

## 摘要

| 等级 | 数量 | 主要领域 |
|------|------|----------|
| **严重** | 12 | 内部 API 未鉴权、身份头可伪造、订单/钱包一致性、WebSocket 鉴权绕过 |
| **中等** | 15 | 配置错误、Feign/退款/聊天/搜索未完成、运维与防御纵深不足 |

**结论**：Feign Client 与 Internal API 契约已基本对齐，网关 JWT 主流程可用；但**服务间内部接口防护、钱包/订单分布式一致性、密钥管理**是当前最大风险，建议在继续功能开发前先处理严重项。

---

## 一、严重问题

### S1. user/item/wallet 的 `/inner/**` 接口完全未鉴权

**影响**：绕过网关直连服务端口，可未授权调用冻结资金、锁定商品、查询/修改用户等内部接口。

**证据**：
- `user-service/.../WebMvcConfiguration.java`：`/inner/**` 在 `excludePathPatterns` 中，且 inner 拦截器注册被注释掉
- `wallet-service/.../WebMvcConfiguration.java`：同上
- `item-service/.../WebMvcConfiguration.java`：`/inner/**` 排除，且未单独注册 inner 拦截器

**对比**：`order-service`、`admin-service`、`chat-service` 已对 `/inner/**` 注册 `JwtAuthInterceptor`（校验 `X-Internal-Auth`）。

**暴露示例**：

| 服务 | 端口 | 风险接口 |
|------|------|----------|
| wallet-service | 8082 | `/inner/wallet/freeze`、`/unfreeze`、`/settle` |
| user-service | 8081 | `/inner/admin/users/**`、`/inner/order/addresses` |
| item-service | 8085 | `/inner/order/items/**` |

---

### S2. 下游服务信任可伪造的网关注入头（无 JWT 二次校验）

**影响**：掌握 `u.gateway.auth-secret` 后，可直接访问各服务端口，伪造任意用户/管理员身份。

**证据**：
- `u-common/.../JwtAuthInterceptor.java`：仅校验 `X-Gateway-Auth` 与 `currentId` 是否存在，从 Header 读取 `role`/`sourceType`，不重新解析 JWT
- `gateway/.../JwtAuthFilter.java`：仅在请求**经过网关**时清除并重新注入 Header
- 各服务 `application-secret.yml` 中硬编码 demo 密钥：`u-trade-gateway-jwt-secret-demo`

**攻击示例**（直连 user-service 8081）：
```http
GET /user/info
X-Gateway-Auth: u-trade-gateway-jwt-secret-demo
currentId: 1
role: 1
```

---

### S3. WebSocket 可通过 `userId` 参数冒充任意用户

**影响**：无需有效 JWT 即可建立聊天连接，冒充他人收发消息。

**证据**：
- `chat-service/.../WebSocketAuthHandshakeInterceptor.java`：token 解析失败时回退接受 `userId` 查询参数
- `chat-service/.../WebMvcConfiguration.java`：`/ws/**` 排除 HTTP 拦截器
- 网关未配置 WebSocket 路由，chat 默认暴露在 **8086** 端口

---

### S4. 确认收货存在双路径，可能导致钱包重复结算

**影响**：同一订单可能被结算两次（买家双重扣款 / 卖家双重到账）。

**证据**：
- **手动确认** `confirm()`：同步调用 `walletClient.transferFrozenToSeller()`（`OrderServiceImpl.java` ~624-630）
- **自动确认** `processSingleOrderConfirm()`：订单改 FINISHED 后发 MQ，由 `OrderSettlementListener` 异步结算
- `processSingleOrderConfirm()` 更新状态时**无 version 校验**；`confirm()` 有 version 校验但**未在 update 中再次校验 status=SHIPPED**
- `Order.java` 中 `@Version` 乐观锁被注释掉（107-109 行）

两路径并发时，自动确认先完成仍可能让手动确认再次成功并同步转账。

---

### S5. 下单分布式锁在事务提交前被释放

**影响**：并发下单可能出现重复锁商品、超卖。

**证据**：`OrderServiceImpl.orderSubmit()` 中：
- 注册了 `TransactionSynchronization.afterCompletion` 释放锁（171-179 行）
- 但 `finally` 块（200-203 行）在方法返回时**立即**释放锁，早于 Spring 事务提交

注释写“事务成功后释放锁”，实际 `finally` 与 `afterCompletion` 逻辑冲突，以 `finally` 为准时锁过早释放。

---

### S6. 钱包冻结成功后，支付流水写入失败无补偿

**影响**：用户余额被冻结，订单仍为待支付，且无自动解冻。

**证据**：
- `PaymentServiceImpl.executeOrderPay()`：先 `strategy.pay()`（余额策略会 RPC 冻结），再 `save(payment)`（37-48 行）
- `OrderServiceImpl.payment()` 的 `compensateWalletFreeze()` 仅包裹 order 更新/markItemSold 的 catch（319-321 行）
- 若 `save(payment)` 抛异常，补偿逻辑不会执行

---

### S7. 钱包操作缺少订单级幂等

**影响**：支付/结算重试可能导致同一订单重复冻结或重复转账。

**证据**：
- `UserWalletServiceImpl.freezeAmount()` / `transferFrozenToSeller()`：每次调用直接改余额，无“该 orderNo 是否已处理”检查
- `WalletLog` 无 `(bizOrderNo, bizType, userId)` 唯一约束
- `OrderSettlementListener` 对 `DuplicateKeyException` 的处理暗示期望 DB 去重，但表结构不支持

---

### S8. 充值成功回调幂等判断写错字段

**影响**：重复充值回调可能**重复加余额**。

**证据**：`UserWalletServiceImpl.handleRechargeSuccess()`（228-231 行）：
```java
if (record.getStatus() == WalletBizTypeConstant.RECHARGE) { return; }
```
将**充值状态字段**与**业务类型常量 RECHARGE=1** 比较；新建记录 `status=0`（WAIT_PAY），该判断永远为 false。

---

### S9. 自动确认后 MQ 结算失败可导致“订单完成但资金永久冻结”

**影响**：买家订单 FINISHED，卖家未收到款，买家冻结资金无法自动退回。

**证据**：
- `processSingleOrderConfirm()` 先将订单置 FINISHED，再发结算 MQ（803-879 行）
- `OrderSettlementListener` 遇 `BusinessException` 执行 `basicNack(..., requeue=false)`，消息丢弃
- `OrderStatusConstant` 定义了 `REFUNDING`/`REFUNDED`，但 `RefundServiceImpl` 为空实现，无退款/解冻流程

---

### S10. `@Transactional` 边界内包含外部 RPC，本地回滚无法回滚远程副作用

**影响**：订单 DB 回滚后，商品锁定/钱包冻结/结算等远程操作可能已生效，造成数据不一致。

**涉及方法**（均在 `OrderServiceImpl.java`）：
| 方法 | 外部调用 |
|------|----------|
| `orderSubmit()` | `itemClient.lockItem()` |
| `payment()` | 钱包 freeze、`itemClient.markItemSold()` |
| `confirm()` | `walletClient.transferFrozenToSeller()` |

仅 `payment()` 对部分失败做了钱包补偿，其他路径无完整 Saga/Outbox。

---

### S11. 多服务 Redis/RabbitMQ 配置 YAML 层级错误

**影响**：本地 `application.yml` 中的 Redis/RabbitMQ 配置**不会**被 Spring Boot 识别（应为 `spring.data.redis` / `spring.rabbitmq`），若 Nacos `shared-application.yml` 未覆盖，服务将以错误/默认配置运行。

**受影响文件**（顶层误写为 `data:` / `rabbitmq:`）：
- `user-service/src/main/resources/application.yml`
- `wallet-service/src/main/resources/application.yml`
- `order-service/src/main/resources/application.yml`（Redis 部分）
- `item-service/src/main/resources/application.yml`
- `search-service/src/main/resources/application.yml`
- `admin-service/src/main/resources/application.yml`

**正确示例**：`chat-service/src/main/resources/application.yml` 使用 `spring.data.redis`、`spring.rabbitmq`。

---

### S12. 超时未支付订单无 DB 兜底取消任务

**影响**：延迟 MQ 发送失败时，订单永久 WAIT_PAY，商品锁定无法释放。

**证据**：
- `sendDelayMessageLazy()` 失败仅打日志“依赖调度兜底”（238-243 行）
- `OrderTask.java` 仅有自动确认任务，**无**扫描过期 WAIT_PAY 订单的定时任务
- 取消完全依赖 `OrderCancelListener` 消费延迟消息

---

## 二、中等问题

### M1. 密钥与数据库凭证硬编码在仓库中

**影响**：JWT 密钥、网关密钥、DB/RabbitMQ 密码可从仓库直接获取。

**证据**：各服务 `application-secret.yml`（如 `user-service/.../application-secret.yml`）含 demo 密钥和 `password: 123`；git status 显示这些文件已被跟踪。

---

### M2. `u.internal.auth-secret` 未在本地 secret 配置中定义

**影响**：服务间调用鉴权配置不完整；order/admin/chat 启用了 inner 拦截器但 secret 为空时全部 401；而 user/item/wallet 因 S1 问题 inner 接口反而完全开放。

**证据**：
- 配置项：`u-common/.../InternalAuthProperties.java`（`u.internal.auth-secret`）
- Feign 自动注入：`u-common/.../CommonFeignConfiguration.java`
- 各 `application-secret.yml` 仅有 `u.gateway.auth-secret`，无 `u.internal.auth-secret`

---

### M3. 网关 Discovery Locator 可能暴露内部路径

**影响**：持有普通用户 JWT 时，可通过 `/{serviceId}/inner/**` 等形式经网关访问后端 inner 接口（结合 S1 更危险）。

**证据**：`gateway/.../application.yml` 第 28-31 行 `discovery.locator.enabled: true`；显式路由未包含 `/inner/**`。

---

### M4. 网关白名单路径未清除客户端伪造的内部 Header

**影响**：在 `/user/login`、`/home/**`、`/search/**` 等白名单路径，客户端可携带伪造的 `X-Gateway-Auth`/`currentId` 透传到下游（若下游某接口未校验）。

**证据**：`JwtAuthFilter` 白名单直接 `chain.filter()`（50-51 行），`clearInternalHeaders()` 仅在鉴权路径执行（95 行）。

---

### M5. search-service 无任何服务级认证

**影响**：8083 端口完全开放；仅依赖网关白名单。

**证据**：search-service 无 `WebMvcConfiguration`、无 `JwtAuthInterceptor`；`SearchController` 提供公开 `GET /search/item`。

---

### M6. admin-service 的 `/admin-logs/**` 未纳入鉴权拦截

**影响**：管理日志接口与 `/admin/**` 鉴权规则不一致。

**证据**：
- 网关路由 `/admin-logs/**` → admin-service
- `admin-service/.../WebMvcConfiguration.java` 拦截器仅匹配 `/admin/**`
- `AdminLogController` 当前为 stub，但路径已暴露

---

### M7. 各服务 Swagger/API 文档在直连端口无鉴权

**影响**：通过 8081/8082 等端口可直接访问 `/doc.html`、`/v3/api-docs/**`，暴露完整 API 面。

**证据**：各服务 `WebMvcConfiguration` 均将 swagger 路径加入 `excludePathPatterns`。

---

### M8. Feign 缺少统一 ErrorDecoder / Fallback / 超时策略

**影响**：网络超时、5xx 抛出 `FeignException` 而非 `Result`，业务层 `ensureSuccess()` 无法识别，补偿逻辑难以触发。

**证据**：`WalletClient` 等 Feign 接口无 fallback；`CommonFeignConfiguration` 仅注入 internal auth header。

---

### M9. 钱包服务将业务异常包装为 `RuntimeException`，Feign 侧丢失语义

**影响**：order-service 无法区分“余额不足”与“系统故障”，错误处理和重试策略失效。

**证据**：
- `UserWalletServiceImpl` 抛 `RuntimeException("余额不足")` 等
- `GlobalExceptionHandler` 统一返回“服务器内部错误”

---

### M10. 未接入的支付方式仍对外暴露

**影响**：用户可选择支付宝/微信/银行卡支付，必然失败，体验差且可能被滥用探测。

**证据**：`AlipayPayStrategy`、`WechatPayStrategy`、`CardPayStrategy` 均抛“暂未接入”；`PayStrategyFactoryManager` 仍路由到这些策略。

---

### M11. 手动确认 vs 自动确认逻辑不一致

**影响**：同一状态迁移（SHIPPED → FINISHED）行为不同，运维和排障困难。

| 维度 | 手动 `confirm()` | 自动 `processSingleOrderConfirm()` |
|------|------------------|-------------------------------------|
| 结算方式 | 同步 Feign | 异步 MQ |
| 乐观锁 | 有 version | 无 version |
| 物流更新失败 | 仅 warn，不回滚 | 抛异常回滚 |

---

### M12. search-service Elasticsearch 默认指向 localhost

**影响**：未配置 Nacos 覆盖时，搜索服务连接错误 ES 集群。

**证据**：
- `item-service` 配置 `spring.elasticsearch.uris: http://192.168.150.101:9200`
- `search-service` 无 ES URI 配置；`ElasticsearchConfig` 默认 `http://127.0.0.1:9200`

---

### M13. chat-service 功能不完整，不适合生产

| 子项 | 说明 |
|------|------|
| REST API | `ChatMessageController`、`ChatConversationController` 多为 `//todo` |
| 消息持久化 | WebSocket 路由未写入 DB |
| 网关集成 | 无 WebSocket 路由；REST 路由与 WS 路径 `/ws/chat` 不一致 |
| 依赖冲突 | `chat-service/pom.xml` 重复 springdoc 版本、Knife4j 3.0.3 与 Boot 3 可能不兼容 |

---

### M14. admin-service 未扫描 `com.u.common`，Feign 内部认证拦截器可能未生效

**影响**：admin 调用 user-service 内部接口时可能不带 `X-Internal-Auth`（当前 user inner 无鉴权故暂未暴露问题）。

**证据**：`AdminServiceApplication.java` 仅 `scanBasePackages = {"com.u.admin"}`；对比 order/item/user 均扫描 `com.u.common`。

---

### M15. 其他代码质量与运维项

- **取消原因覆盖**：`cancelOrderInternal()` 固定写“超时未支付自动取消”，用户主动取消也被覆盖（`OrderServiceImpl.java` ~544 行）
- **重复支付返回**：已支付订单重试返回 `dto.getPayType()` 而非 DB 中 `paymentMethod`（288-291 行）
- **Nacos namespace 不一致**：仅 gateway 显式设置 `namespace: public`，其他服务未设置
- **u-api 孤儿 DTO**：`WalletRechargeDTO` 等无对应 Feign Client
- **Feign List 参数**：`getUserCreditScores(List<Long>)` 使用 `@RequestParam List`，集合编码可能在部分场景失败
- **Order 乐观锁未启用**：version 字段手动维护，与 MP `@Version` 注释掉并存，易出错

---

## 三、相对健康 / 近期已改进项

以下方面经审查状态较好，或在前序迭代中已修复：

| 领域 | 状态 |
|------|------|
| **u-api Feign ↔ Internal API 契约** | User/Wallet/Item/Address 四套 Client 与 Internal*Controller 路径、方法、参数一致 |
| **网关 JWT 解析** | 支持 user/admin 双密钥；鉴权路径会清除并重注 Header |
| **order-service 局部安全加固** | 支付分布式锁、钱包冻结补偿、结算 MQ 校验、取消 listener 幂等改进 |
| **网关与 MVC 依赖隔离** | gateway 仅扫 `com.u.gateway`；u-common web 依赖为 provided |
| **Nacos 服务名** | 各服务 `spring.application.name` 与 `discovery.service` 基本一致（admin/chat 互换问题已修） |
| **u-api 冗余层** | 已删除误导性的 controller/service 代理层 |

---

## 四、修复优先级建议

### P0 — 立即处理（安全 + 资金）

1. 为 user/item/wallet 注册 `/inner/**` 拦截器，移除 exclude；配置并启用 `u.internal.auth-secret`
2. 禁止生产环境直连服务端口；评估关闭 gateway `discovery.locator` 或限制 inner 路径
3. 轮换所有 demo 密钥；secret 文件移出 git，改用环境变量/Nacos
4. 修复 WebSocket `userId` 回退鉴权；生产环境强制 token
5. 统一确认收货结算路径，避免手动+自动双结算
6. 修复充值幂等判断（S8）

### P1 — 高优先级（一致性）

7. 修复 `orderSubmit` 锁释放时机（去掉 finally 过早 unlock，仅 afterCommit/afterCompletion 释放）
8. 支付流水写入失败时补偿解冻
9. 钱包 freeze/settle 增加 orderNo 幂等
10. 修正各服务 YAML 中 Redis/RabbitMQ 层级
11. 增加 WAIT_PAY 超时 DB 兜底扫描任务

### P2 — 中优先级（完善与防御纵深）

12. search-service 增加鉴权或网络隔离
13. admin `/admin-logs/**` 纳入拦截器
14. Feign ErrorDecoder + 超时 + 语义化异常
15. 实现退款状态机或至少余额支付解冻路径
16. chat-service 补全持久化、网关 WS 路由、清理依赖冲突
17. 白名单路径也清除 internal headers；拦截器层增加 admin 角色校验

---

## 五、服务端口速查（直连风险参考）

| 服务 | 端口 | 主要风险 |
|------|------|----------|
| gateway | 8080 | 入口，locator 可能暴露 inner |
| user-service | 8081 | inner 无鉴权、Header 可伪造 |
| wallet-service | 8082 | inner 无鉴权、资金操作 |
| search-service | 8083 | 完全无鉴权 |
| order-service | 8084 | 事务/结算一致性 |
| item-service | 8085 | inner 无鉴权 |
| chat-service | 8086 | WS 冒充、功能未完成 |
| admin-service | 8087 | `/admin-logs` 鉴权缺口 |

---

*本报告基于静态代码审查，未运行集成测试或渗透测试。实际问题严重程度还取决于部署网络隔离、Nacos 实际配置与运行环境。*
