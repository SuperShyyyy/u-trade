# Sec-Trade

基于 Spring Boot 构建的 C2C 二手交易平台，设计高并发订单系统，通过 Redis、RabbitMQ 实现缓存优化与最终一致性控制

---

## 技术栈

| 类别     | 技术                                         |
| -------- | -------------------------------------------- |
| 运行时   | JDK 17                                       |
| 框架     | Spring Boot 3.3.4                            |
| 持久层   | MyBatis-Plus 3.5.7                           |
| 数据库   | MySQL                                        |
| 缓存     | Redis（Spring Data Redis）                   |
| 消息队列 | RabbitMQ（发布确认、Return、消费者手动 ACK）  |
| 搜索     | Elasticsearch 8                              |
| 对象存储 | 阿里云 OSS                                   |
| 安全     | JWT（用户端 / 管理端分密钥）                  |
| API 文档 | Knife4j 4.x                                  |

---

## 仓库结构

| 模块           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| **sec-common** | 公共模型、工具、统一响应、JWT/OSS 等可被服务端引用的能力     |
| **sec-server** | Web 接口、领域服务、MyBatis Mapper、MQ 监听与定时任务；启动类 `com.sec.SecServerApplication` |

根目录 `pom.xml` 与 `sec-common`、`sec-server` 为相互独立的 Maven 工程：**需先安装 `sec-common`，再构建或运行 `sec-server`**。

---

## 运行前准备

1. **JDK 17**
2. **MySQL**：创建库名 `sec-trade`（连接串见 `application.yml`）
3. **Redis**
4. **RabbitMQ**：`virtual-host` 默认为 `/sec`（见 `application.yml`）
5. **Elasticsearch 8.x**：需要安装 IK 中文分词；索引可在首次同步/监听时创建

---

## 配置说明

- 主配置：`sec-server/src/main/resources/application.yml`
- 默认激活 profile：`spring.profiles.active: local`（见 `application.yml`）
- 本地覆盖：`application-local.yml`、`application-dev.yml`（可按环境调整数据源、JWT、RabbitMQ 等）
- 生产环境建议通过环境变量注入敏感项，例如：
  - `DB_USERNAME` / `DB_PASSWORD`（`application.yml` 中 `sec.datasource`）
  - `JWT_ADMIN_SECRET` / `JWT_USER_SECRET`（`sec.jwt`）
  - `spring.rabbitmq.username` / `spring.rabbitmq.password`

Redis、Elasticsearch、RabbitMQ 的地址在 `application.yml` 中配置，本地开发请改为你本机或内网地址。

---

## 本地构建与启动

```bash
# 1. 安装公共模块到本地仓库
cd sec-common
mvn clean install

# 2. 启动服务（默认未显式配置端口时为 8080）
cd ../sec-server
mvn spring-boot:run
```

启动成功后，浏览器打开 **Knife4j 文档**：<http://localhost:8080/doc.html>（白名单见 `WebMvcConfiguration`）。

---

## 已实现能力概览

### 用户与内容

- 用户：注册、登录、`/user/me`、公开资料
- 地址：列表、详情、增删改
- 商品：上架/下架/删除、卖家商品分页、详情、**关键词搜索**（ES）
- 分类：树形列表、分类下商品
- 首页：`/home/recommend` 推荐
- 收藏：分页列表、添加、删除

### 订单与资金

- 订单：提交、支付（冻结资金）、历史分页、详情、取消、发货、买家确认收货、按订单查物流
- 钱包：查询、流水、充值、提现（待实现充值与提现）

### 管理端

- 管理员登录、系统用户列表、创建系统用户、用户分页查询等（`/admin` 系列）

### 待接入 / 占位

- **站内聊天**：`ChatConversationController`、`ChatMessageController` 已预留路径，实现为 TODO
- **评价**：`ReviewController` 已建，暂无接口实现

---

## 架构要点

### 订单自动确认收货

- 使用 **Redis ZSet** 做延时调度，按到期时间处理；配合 **数据库兜底** 扫描，降低缓存异常导致漏处理的风险

### 消息可靠性与最终一致

- RabbitMQ：**生产者确认**、**mandatory + Return**、**消费者手动 ACK**
- 本地消息表 + 定时重试，保证投递与业务侧的最终一致
- 消费幂等：**Redis 防抖** + **数据库唯一约束**，避免重复结算

### 资金与并发

- 流程概览：冻结 → 确认收货后结算
- 钱包等关键更新使用 **乐观锁（version）** 控制并发
- 交易流水落库，便于对账与审计

### 搜索与缓存

- MySQL 与 ES 通过 MQ 异步同步，检索与主库解耦
- 热点数据可走 Redis 缓存（含防雪崩/击穿等策略，见业务实现）

---

## 许可

本项目仅为示例/学习用途 
