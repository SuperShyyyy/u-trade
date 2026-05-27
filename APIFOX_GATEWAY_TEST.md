# Apifox 网关接口测试文档

本文档基于项目全部 Controller 整理，**统一通过 `gateway` 的 `8080` 端口测试**，不要直接调用各服务端口（8081/8085 等），否则缺少 `X-Gateway-Auth` 等网关注入头会鉴权失败。

---

## 1. 启动前检查

### 1.1 基础设施

| 组件 | 地址 |
| --- | --- |
| Nacos | `192.168.150.101:8848` |
| MySQL | `192.168.150.101:3306` |
| Redis | `192.168.150.101:6379` |
| RabbitMQ | `192.168.150.101:5672` |
| Elasticsearch（搜索/推荐） | `192.168.150.101:9200` |

### 1.2 建议启动顺序

先在根目录安装共享模块，避免单独启动服务时使用本地 Maven 仓库里的旧 `u-common` / `u-api`：

```powershell
mvn -pl u-common,u-api -am clean install "-Dmaven.test.skip=true"
```

然后启动各服务：

```powershell
mvn -pl user-service spring-boot:run
mvn -pl item-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl wallet-service spring-boot:run
mvn -pl search-service spring-boot:run
mvn -pl chat-service spring-boot:run
mvn -pl admin-service spring-boot:run
mvn -pl gateway spring-boot:run
```

若刚修改过 `u-common` 或各服务的 `application-secret.yml`，需重启相关服务。推荐顺序：

1. 停止旧的各服务与 `gateway` 进程。
2. 重新启动业务服务，确保加载最新的 `u.gateway.auth-secret`。
3. 最后启动 `gateway`。

在 Windows PowerShell 中可用下面命令确认端口没有跑错进程：

```powershell
Get-NetTCPConnection -LocalPort 8080,8081 -State Listen |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

期望结果：

- `8080` 必须是 `gateway`
- `8081` 才是 `user-service`

如果 8080 没有监听，Apifox 不可能请求到 gateway；如果 8080 不是 gateway，需要先停掉占用进程再启动 gateway。

启动后在 Nacos 服务列表确认至少有：

- `user-service`
- `item-service`
- `order-service`
- `wallet-service`
- `search-service`
- `chat-service`
- `admin-service`
- `gateway`

> **WebSocket 聊天**：REST 接口仍走 gateway `8080`；WebSocket MVP 需直连 `chat-service:8086`（见第 11 节）。

---

## 2. Apifox 环境变量

新建环境，例如 `u-trade-local`：

| 变量名 | 说明 | 初始值 |
| --- | --- | --- |
| `baseUrl` | 网关地址 | `http://localhost:8080` |
| `token` | C 端用户 JWT | 登录后自动写入 |
| `adminToken` | B 端管理员 JWT | 管理员登录后写入 |
| `itemId` | 测试商品 ID | 手动填入 |
| `orderId` | 测试订单 ID | 下单后填入 |
| `orderNo` | 测试订单号 | 下单后填入 |
| `addressId` | 测试地址 ID | 新增地址后填入 |
| `sellerId` | 卖家用户 ID | 手动填入 |
| `wsUrl` | WebSocket 直连地址 | `ws://localhost:8086/ws/chat` |
| `userId2` | 第二个测试用户 ID | 手动填入，如 `2` |

所有接口 URL 前缀：

```text
{{baseUrl}}
```

---

## 3. 网关鉴权规则

### 3.1 白名单（无需 token）

| 路径 | 说明 |
| --- | --- |
| `POST /user/register` | 用户注册 |
| `POST /user/login` | 用户登录 |
| `POST /admin/login` | 管理员登录 |
| `GET /home/**` | 首页推荐 |
| `GET /search/**` | ES 搜索 |

### 3.2 需要 token 的接口

其余接口均需携带以下任一 Header：

| Header | Value |
| --- | --- |
| `token` | `{{token}}` 或 `{{adminToken}}` |
| `Authorization` | `Bearer {{token}}` |

推荐在 Apifox 目录级 Header 中配置 `token: {{token}}`，管理端目录单独配置 `token: {{adminToken}}`。

### 3.3 通用登录后置脚本

C 端登录 `/user/login` 后置脚本：

```javascript
const json = pm.response.json();
if (json?.data?.token) {
  pm.environment.set("token", json.data.token);
}
if (json?.data?.id) {
  pm.environment.set("userId", json.data.id);
}
```

B 端登录 `/admin/login` 后置脚本：

```javascript
const json = pm.response.json();
if (json?.data?.token) {
  pm.environment.set("adminToken", json.data.token);
}
```

下单后置脚本（保存订单号）：

```javascript
const json = pm.response.json();
if (json?.data?.orderNo) {
  pm.environment.set("orderNo", json.data.orderNo);
}
if (json?.data?.id) {
  pm.environment.set("orderId", json.data.id);
}
```

---

## 4. 用户服务（user-service）

路由：`/user/**`（不含已被 item/order/wallet/chat 服务接管的子路径）

### 4.1 用户注册

```http
POST {{baseUrl}}/user/register
Content-Type: application/json
```

```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户",
  "phone": "13800000000"
}
```

### 4.2 用户登录

```http
POST {{baseUrl}}/user/login
Content-Type: application/json
```

```json
{
  "username": "testuser",
  "password": "123456"
}
```

成功响应示例：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "id": 1,
    "token": "JWT_TOKEN",
    "role": null,
    "sourceType": "USER"
  }
}
```

### 4.3 查询当前用户信息

```http
GET {{baseUrl}}/user/me
token: {{token}}
```

### 4.4 修改用户信息

```http
PUT {{baseUrl}}/user
Content-Type: application/json
token: {{token}}
```

```json
{
  "username": "testuser",
  "avatar": "https://example.com/avatar.png",
  "phone": "13800000000"
}
```

### 4.5 查询他人公开信息

```http
GET {{baseUrl}}/user/{id}/public
token: {{token}}
```

---

## 5. 用户地址（user-service）

路由前缀：`/user/address`

### 5.1 地址列表（分页）

```http
GET {{baseUrl}}/user/address/list?page=1&pageSize=10
token: {{token}}
```

### 5.2 查询地址详情

```http
GET {{baseUrl}}/user/address/{id}
token: {{token}}
```

### 5.3 新增地址

```http
POST {{baseUrl}}/user/address
Content-Type: application/json
token: {{token}}
```

```json
{
  "receiverName": "张三",
  "receiverPhone": "13800000000",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detailAddress": "科技园 1 号",
  "isDefault": 1
}
```

### 5.4 更新地址

```http
PUT {{baseUrl}}/user/address
Content-Type: application/json
token: {{token}}
```

```json
{
  "id": 1,
  "receiverName": "李四",
  "receiverPhone": "13900000000",
  "province": "广东省",
  "city": "深圳市",
  "district": "福田区",
  "detailAddress": "中心路 2 号",
  "isDefault": 0
}
```

### 5.5 删除地址

```http
DELETE {{baseUrl}}/user/address/{id}
token: {{token}}
```

### 5.6 设为默认地址

```http
PUT {{baseUrl}}/user/address/{id}/default
token: {{token}}
```

---

## 6. 用户收藏（user-service）

路由前缀：`/user/favorite`

### 6.1 收藏列表（分页）

```http
GET {{baseUrl}}/user/favorite/list?page=1&pageSize=10
token: {{token}}
```

### 6.2 收藏商品

```http
POST {{baseUrl}}/user/favorite/add?itemId={{itemId}}
token: {{token}}
```

### 6.3 取消收藏

```http
DELETE {{baseUrl}}/user/favorite/{itemId}
token: {{token}}
```

---

## 7. 商品与分类（item-service）

路由：`/user/item/**`、`/user/category/**`、`/home/**`

### 7.1 首页推荐（白名单，无需 token）

```http
GET {{baseUrl}}/home/recommend?page=1&pageSize=20
```

### 7.2 查询分类树

```http
GET {{baseUrl}}/user/category
token: {{token}}
```

### 7.3 按分类分页查询商品

```http
GET {{baseUrl}}/user/category/{id}/item?page=1&pageSize=10
token: {{token}}
```

### 7.4 商品搜索（item-service 内搜索，需 token）

> 注意：与第 12 节 ES 搜索 `/search/item` 是不同接口。

```http
GET {{baseUrl}}/user/item/search?keyword=手机&page=0&pageSize=10
token: {{token}}
```

`page` 默认从 `0` 开始。

### 7.5 发布商品

```http
POST {{baseUrl}}/user/item
Content-Type: application/json
token: {{token}}
```

```json
{
  "title": "二手手机",
  "description": "九成新，功能正常",
  "categoryId": 1,
  "originalPrice": 999.00,
  "images": [
    "https://example.com/item-1.png",
    "https://example.com/item-2.png"
  ],
  "isFreeShipping": 1,
  "shippingFee": 0.00
}
```

### 7.6 查询商品详情

```http
GET {{baseUrl}}/user/item/{id}
token: {{token}}
```

### 7.7 查询某卖家的已上架商品

```http
GET {{baseUrl}}/user/item/{sellerId}/list?page=1&pageSize=10
token: {{token}}
```

> 路径变量 `{sellerId}` 为卖家用户 ID，不是商品 ID。

### 7.8 修改商品上下架状态

```http
PUT {{baseUrl}}/user/item/{id}/status?status=1
token: {{token}}
```

| status | 含义 |
| --- | --- |
| `1` | 上架 |
| `0` | 下架 |

### 7.9 删除商品

```http
DELETE {{baseUrl}}/user/item/{id}
token: {{token}}
```

### 7.10 评价接口（未实现）

`/user/review/**` 路由已注册，但 `ReviewController` 当前为空，暂无可用接口。

---

## 8. 订单（order-service）

路由前缀：`/user/order`

### 8.1 用户下单

```http
POST {{baseUrl}}/user/order/submit
Content-Type: application/json
token: {{token}}
```

```json
{
  "itemId": 1,
  "sellerId": 2,
  "addressId": 1
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `itemId` | 是 | 商品 ID |
| `sellerId` | 否 | 卖家 ID |
| `addressId` | 否 | 收货地址 ID |

### 8.2 订单支付

```http
POST {{baseUrl}}/user/order/payment
Content-Type: application/json
token: {{token}}
```

```json
{
  "orderNo": "{{orderNo}}",
  "payType": 0
}
```

| payType | 含义 |
| --- | --- |
| `0` | 余额支付（BALANCE） |
| `1` | 银行卡（CARD） |
| `2` | 微信（WECHAT） |
| `3` | 支付宝（ALIPAY） |

### 8.3 历史订单查询

```http
GET {{baseUrl}}/user/order/historyOrders?page=1&pageSize=10&status=0
token: {{token}}
```

`status` 可选，不传则查全部。常用订单状态：

| status | 含义 |
| --- | --- |
| `0` | 待支付 |
| `1` | 已支付 |
| `2` | 已发货 |
| `3` | 已完成 |
| `4` | 已取消 |
| `5` | 退款中 |
| `6` | 已退款 |

### 8.4 查询订单详情

```http
GET {{baseUrl}}/user/order/detail/{id}
token: {{token}}
```

### 8.5 取消订单

仅待支付订单可取消。

```http
PUT {{baseUrl}}/user/order/cancel?id={{orderId}}
token: {{token}}
```

### 8.6 卖家发货

```http
PUT {{baseUrl}}/user/order/shipment?orderId={{orderId}}&logisticsCompany=顺丰&trackingNumber=SF1234567890
token: {{token}}
```

`logisticsCompany`、`trackingNumber` 可选。

### 8.7 买家确认收货

```http
POST {{baseUrl}}/user/order/confirm?id={{orderId}}
token: {{token}}
```

### 8.8 查询物流信息

```http
GET {{baseUrl}}/user/order?orderId={{orderId}}
token: {{token}}
```

---

## 9. 钱包（wallet-service）

路由前缀：`/user/wallet`

### 9.1 查询钱包余额

```http
GET {{baseUrl}}/user/wallet
token: {{token}}
```

### 9.2 查询流水列表

```http
GET {{baseUrl}}/user/wallet/logs?page=1&pageSize=20
token: {{token}}
```

可选 Query 参数：

| 参数 | 说明 |
| --- | --- |
| `bizOrderNo` | 按业务流水号筛选 |
| `startTime` | 开始时间，如 `2026-01-01T00:00:00` |
| `endTime` | 结束时间 |
| `page` | 页码，默认 1 |
| `pageSize` | 每页条数，默认 20 |

示例：

```http
GET {{baseUrl}}/user/wallet/logs?bizOrderNo=ORD20260308001&page=1&pageSize=10
token: {{token}}
```

### 9.3 充值

```http
POST {{baseUrl}}/user/wallet/recharge
Content-Type: application/json
token: {{token}}
```

```json
{
  "amount": 100.00
}
```

`bizOrderNo` 可选，不传则由后端生成。

### 9.4 提现

```http
POST {{baseUrl}}/user/wallet/withdraw
Content-Type: application/json
token: {{token}}
```

```json
{
  "amount": 50.00,
  "payChannel": "ALIPAY"
}
```

`bizOrderNo`、`payChannel` 可选。

---

## 10. 管理端（admin-service）

路由：`/admin/**`、`/admin-logs/**`

管理端接口需使用管理员 token（`{{adminToken}}`）。

### 10.1 管理员登录（白名单）

```http
POST {{baseUrl}}/admin/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 10.2 分页查询管理员列表

```http
GET {{baseUrl}}/admin/system/list?page=1&pageSize=10
token: {{adminToken}}
```

### 10.3 创建管理员账号

```http
POST {{baseUrl}}/admin/system/user
Content-Type: application/json
token: {{adminToken}}
```

```json
{
  "username": "manager01",
  "password": "123456",
  "nickname": "普通管理员",
  "role": 2,
  "avatar": "https://example.com/admin.png"
}
```

| role | 含义 |
| --- | --- |
| `1` | 超级管理员 |
| `2` | 普通管理员 |

### 10.4 删除管理员

```http
DELETE {{baseUrl}}/admin/system/user/{id}
token: {{adminToken}}
```

### 10.5 修改管理员状态

```http
PUT {{baseUrl}}/admin/system/user/{id}/status?status=1
token: {{adminToken}}
```

### 10.6 管理端分页查询用户

```http
GET {{baseUrl}}/admin/user?page=1&pageSize=10
token: {{adminToken}}
```

### 10.7 管理端修改用户状态

```http
PUT {{baseUrl}}/admin/user/{id}/status?status=1
token: {{adminToken}}
```

### 10.8 管理员操作日志（未实现）

`/admin-logs/**` 路由已注册，但 `AdminLogController` 当前为空，暂无可用接口。

---

## 11. 聊天（chat-service）

路由：`/user/chat/**`、`/user/chat-message/**`、`/user/chat-conversation/**`（REST，经 gateway）

> **重要**：REST 请求统一走 gateway `8080`；**WebSocket 不经过 gateway**，MVP 阶段直连 `chat-service:8086`。

### 11.1 REST 接口（待实现）

`ChatConversationController`、`ChatMessageController` 当前均为 TODO，暂无可用 REST 接口。

### 11.2 WebSocket 一对一聊天（MVP，已实现）

#### 连接端点

| 方式 | URL | 说明 |
| --- | --- | --- |
| userId（开发测试） | `ws://localhost:8086/ws/chat?userId=1` | 跳过 JWT，便于本地联调 |
| JWT token（生产） | `ws://localhost:8086/ws/chat?token={{token}}` | token 来自 `POST /user/login` |

Apifox WebSocket 请求 URL 示例：

```text
{{wsUrl}}?userId=1
{{wsUrl}}?token={{token}}
```

#### 客户端发送

连接成功后，发送 JSON 文本帧：

```json
{
  "receiverId": 2,
  "content": "你好，这个商品还在吗？",
  "messageType": "text"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `receiverId` | 是 | 接收方用户 ID |
| `content` | 是 | 消息内容 |
| `messageType` | 否 | `text` 或 `image`，默认 `text` |

#### 服务端下发

**连接成功（CONNECTED）：**

```json
{
  "type": "CONNECTED",
  "message": "WebSocket connected, userId=1"
}
```

**接收方收到消息（CHAT）：**

```json
{
  "type": "CHAT",
  "data": {
    "messageId": "550e8400-e29b-41d4-a716-446655440000",
    "senderId": 1,
    "receiverId": 2,
    "content": "你好，这个商品还在吗？",
    "messageType": "text",
    "timestamp": 1710000000000
  }
}
```

**发送方 ACK（服务端已接收并处理，不代表已读）：**

```json
{
  "type": "ACK",
  "data": {
    "messageId": "550e8400-e29b-41d4-a716-446655440000",
    "senderId": 1,
    "receiverId": 2,
    "content": "你好，这个商品还在吗？",
    "messageType": "text",
    "timestamp": 1710000000000
  }
}
```

**错误（ERROR）：**

```json
{
  "type": "ERROR",
  "message": "receiverId 不能为空"
}
```

#### 浏览器测试（A → B）

1. 启动 `chat-service`（端口 `8086`）
2. 打开两个浏览器标签页，访问 `http://localhost:8086/ws-test.html`
3. 标签页 1：`userId=1`，点击「连接」
4. 标签页 2：`userId=2`，点击「连接」
5. 标签页 1：`receiverId=2`，点击「发送」
6. 预期：标签页 2 收到 `type=CHAT`；标签页 1 收到 `type=ACK`

#### Apifox WebSocket 测试

1. 新建 WebSocket 请求，URL 填 `{{wsUrl}}?userId=1`
2. 点击连接，应收到 `CONNECTED`
3. 发送消息 JSON（见上文客户端发送格式）
4. 另开 Apifox 窗口连接 `{{wsUrl}}?userId={{userId2}}`，验证 CHAT 推送

#### 离线行为

接收方不在线时，发送方仍收到 `ACK`；服务端日志记录「接收方不在线，消息暂未投递」（MVP 不做持久化）。

---

## 12. 搜索服务（search-service）

路由：`/search/**`（白名单，无需 token）

### 12.1 ES 商品搜索

```http
GET {{baseUrl}}/search/item?keyword=手机&page=0&size=10
```

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `keyword` | 必填 | 搜索关键词 |
| `page` | `0` | 页码（从 0 开始） |
| `size` | `10` | 每页条数 |

> 与 `/user/item/search` 的区别：`/search/item` 走 Elasticsearch，无需登录；`/user/item/search` 走 item-service 内部搜索，需 token。

---

## 13. 推荐测试流程

按业务链路顺序测试，便于串联环境变量：

```
1. POST /user/register          → 注册
2. POST /user/login             → 获取 token
3. POST /user/address           → 获取 addressId
4. POST /user/item              → 获取 itemId（或用已有商品）
5. GET  /home/recommend         → 验证首页（无需 token）
6. GET  /search/item            → 验证 ES 搜索（无需 token）
7. POST /user/favorite/add      → 收藏商品
8. POST /user/order/submit      → 获取 orderNo / orderId
9. POST /user/wallet/recharge   → 充值（余额支付前）
10. POST /user/order/payment    → payType=0 余额支付
11. PUT  /user/order/shipment   → 卖家发货（换卖家 token）
12. POST /user/order/confirm    → 买家确认收货
13. GET  /user/order/historyOrders → 验证订单列表
14. WebSocket 聊天（直连 8086，见 §11.2）
    - 标签页 A：ws://localhost:8086/ws/chat?userId=1
    - 标签页 B：ws://localhost:8086/ws/chat?userId=2
    - A 发消息 → B 收 CHAT，A 收 ACK
```

管理端独立流程：

```
1. POST /admin/login            → 获取 adminToken
2. GET  /admin/user             → 查询用户
3. GET  /admin/system/list      → 查询管理员
```

---

## 14. 网关路由对照表

| 路径前缀 | 目标服务 |
| --- | --- |
| `/user/item/**`、`/user/category/**`、`/user/review/**`、`/home/**` | item-service |
| `/user/order/**` | order-service |
| `/user/wallet/**` | wallet-service |
| `/user/chat/**`、`/user/chat-message/**`、`/user/chat-conversation/**` | chat-service |
| `/search/**` | search-service |
| `/admin/**`、`/admin-logs/**` | admin-service |
| `/user/**`（其余） | user-service |

> **WebSocket 说明**：`/ws/chat` 为 WebSocket 协议端点，MVP 阶段直连 `chat-service:8086`，不经过 gateway `8080`。

---

## 15. 内部接口（勿用 Apifox 测试）

以下 `/inner/**` 为服务间 Feign 调用，不暴露给前端，也不应通过 Apifox 手动测试：

| 路径 | 服务 |
| --- | --- |
| `/inner/admin/users/**` | user-service |
| `/inner/order/addresses` | user-service |
| `/inner/order/items/**` | item-service |
| `/inner/wallet/**` | wallet-service |

---

## 16. 常见问题

### 401 Unauthorized

常见原因：

- 未登录，`token` / `adminToken` 环境变量为空。
- Header 未携带 `token: {{token}}`。
- token 过期或密钥不匹配，需重新调用 `/user/login` 或 `/admin/login`。
- 直接调用各服务端口，缺少网关注入的 `X-Gateway-Auth` 头。
- 修改 `application-secret.yml` 后未重启服务。

日志判断：

| 日志 | 含义 |
| --- | --- |
| gateway: `请求未携带token` | Apifox 未把 token 发到 8080 |
| gateway: `Token解析失败` | token 无效，需重新登录 |
| 业务服务: `请求缺少网关注入头 X-Gateway-Auth` | 请求绕过了 gateway |
| 业务服务: `请求缺少网关注入身份头 currentId` | token 未解析或未携带 currentId |

### 503 Service Unavailable

- 对应微服务未启动或未注册到 Nacos。
- 网关 Nacos 地址/group 与业务服务不一致。

### 404 Not Found

- 确认通过 `8080` 访问，路径不要加服务名前缀。
- 正确：`http://localhost:8080/user/login`
- 错误：`http://localhost:8080/user-service/user/login`

### 接口清单与代码不一致时

以各服务 `*Controller.java` 源码为准。当前未实现 Controller：

- `ReviewController`（`/user/review`）
- `ChatConversationController`（`/user/chat-conversation`）
- `ChatMessageController`（`/user/chat-message`）
- `AdminLogController`（`/admin-logs`）
