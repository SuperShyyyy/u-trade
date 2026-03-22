# Sec-Trade

C2C 二手交易平台后端：支持商品发布与检索、订单与支付冻结、发货与自动确认收货结算、钱包与流水、退款与评价（Todo）、站内会话与消息（Todo）等完整交易流程。

------

## 技术栈

| 类别     | 技术                                                         |
| -------- | ------------------------------------------------------------ |
| 框架     | Spring Boot 3.x、Spring Scheduling                           |
| 持久层   | MyBatis-Plus、MySQL、Druid                                   |
| 缓存     | Redis                                                        |
| 消息队列 | RabbitMQ                                                     |
| 搜索     | Elasticsearch 7.x + IK 中文分词                              |
| 对象存储 | 阿里云 OSS                                                   |
| 安全     | JWT（用户端 / 管理端分密钥）                                 |
| API 文档 | Knife4j（OpenAPI 3）                                         |

------

## 模块说明

- **sec-common**：公共常量、工具类、统一响应与全局异常、MQ 消息体等。
- **sec-server**：Web 接口、领域服务、MyBatis Mapper、MQ 监听与定时任务；启动类 `com.sec.SecServerApplication`。

> 根目录 `pom.xml` 与 `sec-server` 为独立构建单元

------

## 运行准备

1. **JDK 17**
2. **MySQL**：创建数据库 `sec-trade`
3. **Redis**
4. **RabbitMQ**： `virtual-host`（默认 `/sec`），队列可由应用声明
5. **Elasticsearch 7.12.x**：安装 IK 分词插件，首次启动可自动创建索引

**配置文件**：`application.yml`


------

## 本地启动

```
# 安装公共模块
cd sec-common
mvn clean install

# 启动服务
cd ../sec-server
mvn spring-boot:run
```

> 默认 HTTP 端口 8080，启动成功后访问 API 文档：`http://localhost:8080/doc.html`

------

## 核心能力概览

### 1️⃣ 订单自动确认收货系统

- 基于 `Redis ZSet` 实现延时队列，按到期时间戳排序，分批扫描处理最早到期订单
- **双重保障**：Redis 定时扫描 + 数据库低频兜底，避免缓存异常造成订单遗漏
- 分布式锁控制多实例并发处理


------

### 2️⃣ 消息可靠性与最终一致性保障

- MQ 三重确认：生产者确认、返回确认、消费者手动 ACK
- 本地消息表 + 定时重试，保障消息发送最终一致
- 消费端幂等：Redis 防抖 + 数据库唯一索引，避免重复结算


------

### 3️⃣ 高并发资金流转控制

- 资金流程：冻结 → 确认收货 → 转账
- 乐观锁控制并发扣款和入账
- 完整交易流水落库，可追溯、可审计


------

### 4️⃣ 搜索与性能优化

- Elasticsearch + IK 分词实现商品多字段全文检索
- MQ 异步同步 MySQL → ES，实现业务库与搜索库解耦
- Redis 缓存热点数据，TTL + 随机过期 + 互斥锁防缓存雪崩/击穿


------

### 5️⃣ 高并发与扩展性设计

- 数据库分库分表，单表控制 ≤ 500 万条
- Redis ZSet 分片，微服务多实例部署
- MQ 队列拆分 + 批量消费，降低单队列压力
- 微服务架构拆分订单、支付、搜索、用户服务，主流程异步解耦


------

💡 **总结**

> 项目通过 Redis ZSet + MQ 异步调度 + 消费端幂等 + 乐观锁 + Elasticsearch + Redis 缓存优化，实现了高可靠、高性能、可扩展的 C2C 二手交易后端系统。
