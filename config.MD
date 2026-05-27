# 说明

Springboot3+  无需bootstrap.yml

# 服务拆分

admin-service  8087

chat-service 8086

gateway 8080

item-service 8085

order-service 8084

search-service 8083

u-api

u-common

user-service 8081

wallet-service 8082



# #nacos服务共享yml文件

~~~yml
# Nacos DataId: shared-application.yml
logging:
  level:
    com.hmall: debug
  pattern:
    dateformat: HH:mm:ss:SSS
  file:
    path: "logs/${spring.application.name}"

knife4j:
  enable: true
  openapi:
    title: ${u.swagger.title:u-trade接口文档}
    description: ${u.swagger.description:u-trade接口文档}
    email: ${u.swagger.email:zhanghuyi@itcast.cn}
    version: v1.0.0
    group:
      default:
        group-name: default
        api-rule: package
        api-rule-resources:
          - ${u.swagger.package}

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
  mapper-locations: classpath*:mapper/**/*.xml

# 统一的基础配置前缀 u
u:
  jwt:
    admin-secret-key: u-trade-admin-jwt-secret-demo
    admin-ttl: 7200000
    admin-token-name: token
    user-secret-key: u-trade-user-jwt-secret-demo
    user-ttl: 7200000
    user-token-name: token
  swagger:
    title: u-trade接口文档
    description: u-trade接口文档
    email: zhanghuyi@itcast.cn
    concat: zhao
    package: com.u.* # 建议在此处定义默认包路径，避免每个服务都配
~~~



# 私有yml文件 在nacos中的

在私有yml文件的

~~~yml
server:
  port: 8087  # ⚠️ 必须根据实际服务分配端口，不要依赖本地 fallback

spring:
  application:
    name: admin-service
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${u.datasource.host:192.168.150.101}:${u.datasource.port:3306}/admin_service?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: ${u.datasource.username:root}
    password: ${u.datasource.password:123}

data:
  redis:
    host: ${REDIS_HOST:192.168.150.101}
    port: ${REDIS_PORT:6379}

rabbitmq:
  host: ${RABBITMQ_HOST:192.168.150.101}
  port: ${RABBITMQ_PORT:5672}
  username: ${RABBITMQ_USERNAME:guest}
  password: ${RABBITMQ_PASSWORD:guest}
  virtual-host: ${RABBITMQ_VHOST:/}
  listener:
    simple:
      acknowledge-mode: manual
      prefetch: 10

# 服务特有的内部配置
u:
  gateway:
    auth-secret: u-trade-gateway-auth-secret-demo
  internal:
    auth-secret: u-trade-internal-auth-secret-demo
~~~

# 项目服务私有配置yml文件

~~~yml
spring:
  application:
    name: admin-service

  cloud:
    nacos:
      discovery:
        server-addr: 192.168.150.101:8848
      config:
        server-addr: 192.168.150.101:8848
        file-extension: yml
        group: DEFAULT_GROUP
        # 开启刷新监听
        refresh-enabled: true

  config:
    import:
      - optional:nacos:shared-application.yml?group=DEFAULT_GROUP
      - optional:nacos:${spring.application.name}.yml?group=DEFAULT_GROUP

server:
  port: 8087 # 仅作为本地兜底，确保与 Nacos 一致
~~~

