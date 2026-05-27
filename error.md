"C:\Program Files\Java\jdk-17\bin\java.exe" -XX:TieredStopAtLevel=1 -Dspring.output.ansi.enabled=always -Dcom.sun.management.jmxremote -Dspring.jmx.enabled=true -Dspring.liveBeansView.mbeanDomain -Dspring.application.admin.enabled=true "-Dmanagement.endpoints.jmx.exposure.include=*" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.7\lib\idea_rt.jar=51730" -Dfile.encoding=UTF-8 -classpath E:\T\u-trade\admin-service\target\classes;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.3.4\spring-boot-starter-web-3.3.4.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter\3.3.4\spring-boot-starter-3.3.4.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot\3.3.4\spring-boot-3.3.4.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-logging\3.3.4\spring-boot-starter-logging-3.3.4.jar;C:\Users\zhao\.m2\repository\ch\qos\logback\logback-classic\1.5.8\logback-classic-1.5.8.jar;C:\Users\zhao\.m2\repository\ch\qos\logback\logback-core\1.5.8\logback-core-1.5.8.jar;C:\Users\zhao\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.23.1\log4j-to-slf4j-2.23.1.jar;C:\Users\zhao\.m2\repository\org\apache\logging\log4j\log4j-api\2.23.1\log4j-api-2.23.1.jar;C:\Users\zhao\.m2\repository\org\slf4j\jul-to-slf4j\2.0.16\jul-to-slf4j-2.0.16.jar;C:\Users\zhao\.m2\repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-json\3.3.4\spring-boot-starter-json-3.3.4.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.17.2\jackson-datatype-jdk8-2.17.2.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.17.2\jackson-datatype-jsr310-2.17.2.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\module\jackson-module-parameter-names\2.17.2\jackson-module-parameter-names-2.17.2.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.3.4\spring-boot-starter-tomcat-3.3.4.jar;C:\Users\zhao\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.30\tomcat-embed-core-10.1.30.jar;C:\Users\zhao\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.30\tomcat-embed-el-10.1.30.jar;C:\Users\zhao\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.30\tomcat-embed-websocket-10.1.30.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-web\6.1.13\spring-web-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-beans\6.1.13\spring-beans-6.1.13.jar;C:\Users\zhao\.m2\repository\io\micrometer\micrometer-observation\1.13.4\micrometer-observation-1.13.4.jar;C:\Users\zhao\.m2\repository\io\micrometer\micrometer-commons\1.13.4\micrometer-commons-1.13.4.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-webmvc\6.1.13\spring-webmvc-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-aop\6.1.13\spring-aop-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-context\6.1.13\spring-context-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-expression\6.1.13\spring-expression-6.1.13.jar;C:\Users\zhao\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus-spring-boot3-starter\3.5.7\mybatis-plus-spring-boot3-starter-3.5.7.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus\3.5.7\mybatis-plus-3.5.7.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus-extension\3.5.7\mybatis-plus-extension-3.5.7.jar;C:\Users\zhao\.m2\repository\org\mybatis\mybatis\3.5.16\mybatis-3.5.16.jar;C:\Users\zhao\.m2\repository\com\github\jsqlparser\jsqlparser\4.9\jsqlparser-4.9.jar;C:\Users\zhao\.m2\repository\org\mybatis\mybatis-spring\3.0.3\mybatis-spring-3.0.3.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.7\mybatis-plus-spring-boot-autoconfigure-3.5.7.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.3.4\spring-boot-autoconfigure-3.3.4.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.3.4\spring-boot-starter-jdbc-3.3.4.jar;C:\Users\zhao\.m2\repository\com\zaxxer\HikariCP\5.1.0\HikariCP-5.1.0.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-jdbc\6.1.13\spring-jdbc-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-tx\6.1.13\spring-tx-6.1.13.jar;C:\Users\zhao\.m2\repository\org\projectlombok\lombok\1.18.34\lombok-1.18.34.jar;E:\T\u-trade\u-common\target\classes;C:\Users\zhao\.m2\repository\io\jsonwebtoken\jjwt\0.9.1\jjwt-0.9.1.jar;C:\Users\zhao\.m2\repository\com\aliyun\oss\aliyun-sdk-oss\3.18.4\aliyun-sdk-oss-3.18.4.jar;C:\Users\zhao\.m2\repository\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar;C:\Users\zhao\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;C:\Users\zhao\.m2\repository\org\jdom\jdom2\2.0.6.1\jdom2-2.0.6.1.jar;C:\Users\zhao\.m2\repository\org\codehaus\jettison\jettison\1.5.4\jettison-1.5.4.jar;C:\Users\zhao\.m2\repository\com\aliyun\aliyun-java-sdk-core\4.7.8\aliyun-java-sdk-core-4.7.8.jar;C:\Users\zhao\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;C:\Users\zhao\.m2\repository\org\apache\commons\commons-lang3\3.14.0\commons-lang3-3.14.0.jar;C:\Users\zhao\.m2\repository\javax\xml\bind\jaxb-api\2.3.1\jaxb-api-2.3.1.jar;C:\Users\zhao\.m2\repository\javax\activation\javax.activation-api\1.2.0\javax.activation-api-1.2.0.jar;C:\Users\zhao\.m2\repository\org\glassfish\jaxb\jaxb-runtime\4.0.5\jaxb-runtime-4.0.5.jar;C:\Users\zhao\.m2\repository\org\glassfish\jaxb\jaxb-core\4.0.5\jaxb-core-4.0.5.jar;C:\Users\zhao\.m2\repository\org\eclipse\angus\angus-activation\2.0.2\angus-activation-2.0.2.jar;C:\Users\zhao\.m2\repository\org\glassfish\jaxb\txw2\4.0.5\txw2-4.0.5.jar;C:\Users\zhao\.m2\repository\com\sun\istack\istack-commons-runtime\4.1.2\istack-commons-runtime-4.1.2.jar;C:\Users\zhao\.m2\repository\org\bouncycastle\bcprov-jdk18on\1.79\bcprov-jdk18on-1.79.jar;C:\Users\zhao\.m2\repository\io\opentracing\opentracing-api\0.33.0\opentracing-api-0.33.0.jar;C:\Users\zhao\.m2\repository\io\opentracing\opentracing-util\0.33.0\opentracing-util-0.33.0.jar;C:\Users\zhao\.m2\repository\io\opentracing\opentracing-noop\0.33.0\opentracing-noop-0.33.0.jar;C:\Users\zhao\.m2\repository\com\aliyun\aliyun-java-sdk-ram\3.1.0\aliyun-java-sdk-ram-3.1.0.jar;C:\Users\zhao\.m2\repository\com\aliyun\aliyun-java-sdk-kms\2.11.0\aliyun-java-sdk-kms-2.11.0.jar;C:\Users\zhao\.m2\repository\com\aliyun\java-trace-api\0.2.11-beta\java-trace-api-0.2.11-beta.jar;C:\Users\zhao\.m2\repository\io\opentelemetry\opentelemetry-api\1.37.0\opentelemetry-api-1.37.0.jar;C:\Users\zhao\.m2\repository\io\opentelemetry\opentelemetry-context\1.37.0\opentelemetry-context-1.37.0.jar;C:\Users\zhao\.m2\repository\com\aliyun\aliyun-java-core\0.2.11-beta\aliyun-java-core-0.2.11-beta.jar;C:\Users\zhao\.m2\repository\org\dom4j\dom4j\2.1.4\dom4j-2.1.4.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.7\mybatis-plus-annotation-3.5.7.jar;C:\Users\zhao\.m2\repository\com\baomidou\mybatis-plus-core\3.5.7\mybatis-plus-core-3.5.7.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.3.4\spring-boot-starter-validation-3.3.4.jar;C:\Users\zhao\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.1.Final\hibernate-validator-8.0.1.Final.jar;C:\Users\zhao\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C:\Users\zhao\.m2\repository\org\jboss\logging\jboss-logging\3.5.3.Final\jboss-logging-3.5.3.Final.jar;C:\Users\zhao\.m2\repository\com\fasterxml\classmate\1.7.0\classmate-1.7.0.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.17.2\jackson-databind-2.17.2.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.17.2\jackson-annotations-2.17.2.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.17.2\jackson-core-2.17.2.jar;C:\Users\zhao\.m2\repository\io\github\openfeign\feign-core\13.2.1\feign-core-13.2.1.jar;E:\T\u-trade\u-api\target\classes;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-openfeign-core\4.1.1\spring-cloud-openfeign-core-4.1.1.jar;C:\Users\zhao\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.3.4\spring-boot-starter-aop-3.3.4.jar;C:\Users\zhao\.m2\repository\org\aspectj\aspectjweaver\1.9.22.1\aspectjweaver-1.9.22.1.jar;C:\Users\zhao\.m2\repository\io\github\openfeign\form\feign-form-spring\3.8.0\feign-form-spring-3.8.0.jar;C:\Users\zhao\.m2\repository\io\github\openfeign\form\feign-form\3.8.0\feign-form-3.8.0.jar;C:\Users\zhao\.m2\repository\commons-fileupload\commons-fileupload\1.5\commons-fileupload-1.5.jar;C:\Users\zhao\.m2\repository\commons-io\commons-io\2.11.0\commons-io-2.11.0.jar;C:\Users\zhao\.m2\repository\com\alibaba\cloud\spring-cloud-starter-alibaba-nacos-discovery\2023.0.1.0\spring-cloud-starter-alibaba-nacos-discovery-2023.0.1.0.jar;C:\Users\zhao\.m2\repository\com\alibaba\cloud\spring-cloud-alibaba-commons\2023.0.1.0\spring-cloud-alibaba-commons-2023.0.1.0.jar;C:\Users\zhao\.m2\repository\com\alibaba\nacos\nacos-client\2.3.2\nacos-client-2.3.2.jar;C:\Users\zhao\.m2\repository\com\alibaba\nacos\nacos-auth-plugin\2.3.2\nacos-auth-plugin-2.3.2.jar;C:\Users\zhao\.m2\repository\com\alibaba\nacos\nacos-encryption-plugin\2.3.2\nacos-encryption-plugin-2.3.2.jar;C:\Users\zhao\.m2\repository\commons-codec\commons-codec\1.16.1\commons-codec-1.16.1.jar;C:\Users\zhao\.m2\repository\org\apache\httpcomponents\httpasyncclient\4.1.5\httpasyncclient-4.1.5.jar;C:\Users\zhao\.m2\repository\org\apache\httpcomponents\httpcore-nio\4.4.16\httpcore-nio-4.4.16.jar;C:\Users\zhao\.m2\repository\org\apache\httpcomponents\httpcore\4.4.16\httpcore-4.4.16.jar;C:\Users\zhao\.m2\repository\io\prometheus\simpleclient\0.16.0\simpleclient-0.16.0.jar;C:\Users\zhao\.m2\repository\io\prometheus\simpleclient_tracer_otel\0.16.0\simpleclient_tracer_otel-0.16.0.jar;C:\Users\zhao\.m2\repository\io\prometheus\simpleclient_tracer_common\0.16.0\simpleclient_tracer_common-0.16.0.jar;C:\Users\zhao\.m2\repository\io\prometheus\simpleclient_tracer_otel_agent\0.16.0\simpleclient_tracer_otel_agent-0.16.0.jar;C:\Users\zhao\.m2\repository\io\micrometer\micrometer-core\1.13.4\micrometer-core-1.13.4.jar;C:\Users\zhao\.m2\repository\org\hdrhistogram\HdrHistogram\2.2.2\HdrHistogram-2.2.2.jar;C:\Users\zhao\.m2\repository\org\latencyutils\LatencyUtils\2.0.3\LatencyUtils-2.0.3.jar;C:\Users\zhao\.m2\repository\com\alibaba\spring\spring-context-support\1.0.11\spring-context-support-1.0.11.jar;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-commons\4.1.2\spring-cloud-commons-4.1.2.jar;C:\Users\zhao\.m2\repository\org\springframework\security\spring-security-crypto\6.3.3\spring-security-crypto-6.3.3.jar;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-context\4.1.2\spring-cloud-context-4.1.2.jar;C:\Users\zhao\.m2\repository\com\alibaba\cloud\spring-cloud-starter-alibaba-nacos-config\2023.0.1.0\spring-cloud-starter-alibaba-nacos-config-2023.0.1.0.jar;C:\Users\zhao\.m2\repository\org\slf4j\slf4j-api\2.0.16\slf4j-api-2.0.16.jar;C:\Users\zhao\.m2\repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-starter-openfeign\4.1.1\spring-cloud-starter-openfeign-4.1.1.jar;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-starter\4.1.2\spring-cloud-starter-4.1.2.jar;C:\Users\zhao\.m2\repository\org\springframework\security\spring-security-rsa\1.1.2\spring-security-rsa-1.1.2.jar;C:\Users\zhao\.m2\repository\io\github\openfeign\feign-slf4j\13.2.1\feign-slf4j-13.2.1.jar;C:\Users\zhao\.m2\repository\org\springframework\cloud\spring-cloud-loadbalancer\4.1.2\spring-cloud-loadbalancer-4.1.2.jar;C:\Users\zhao\.m2\repository\io\projectreactor\reactor-core\3.6.10\reactor-core-3.6.10.jar;C:\Users\zhao\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\zhao\.m2\repository\io\projectreactor\addons\reactor-extra\3.5.2\reactor-extra-3.5.2.jar;C:\Users\zhao\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.2\jakarta.xml.bind-api-4.0.2.jar;C:\Users\zhao\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.3\jakarta.activation-api-2.1.3.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-core\6.1.13\spring-core-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springframework\spring-jcl\6.1.13\spring-jcl-6.1.13.jar;C:\Users\zhao\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.6.0\springdoc-openapi-starter-webmvc-ui-2.6.0.jar;C:\Users\zhao\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-api\2.6.0\springdoc-openapi-starter-webmvc-api-2.6.0.jar;C:\Users\zhao\.m2\repository\org\springdoc\springdoc-openapi-starter-common\2.6.0\springdoc-openapi-starter-common-2.6.0.jar;C:\Users\zhao\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.22\swagger-core-jakarta-2.2.22.jar;C:\Users\zhao\.m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.22\swagger-annotations-jakarta-2.2.22.jar;C:\Users\zhao\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.22\swagger-models-jakarta-2.2.22.jar;C:\Users\zhao\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.17.2\jackson-dataformat-yaml-2.17.2.jar;C:\Users\zhao\.m2\repository\org\webjars\swagger-ui\5.17.14\swagger-ui-5.17.14.jar com.u.admin.AdminServiceApplication
Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2026-05-20T16:39:56.070+08:00  INFO 21424 --- [u-common] [           main] com.u.admin.AdminServiceApplication      : Starting AdminServiceApplication using Java 17.0.17 with PID 21424 (E:\T\u-trade\admin-service\target\classes started by zhao in E:\T\u-trade)
2026-05-20T16:39:56.072+08:00  INFO 21424 --- [u-common] [           main] com.u.admin.AdminServiceApplication      : No active profile set, falling back to 1 default profile: "default"
2026-05-20T16:39:56.109+08:00  WARN 21424 --- [u-common] [           main] c.a.c.n.c.NacosConfigDataLoader          : [Nacos Config] config[dataId=u-common.yml, group=DEFAULT_GROUP] is empty
2026-05-20T16:39:56.109+08:00  WARN 21424 --- [u-common] [           main] c.a.c.n.c.NacosConfigDataLoader          : [Nacos Config] config[dataId=shared-application.yml, group=DEFAULT_GROUP] is empty
2026-05-20T16:39:56.965+08:00  INFO 21424 --- [u-common] [           main] o.s.cloud.context.scope.GenericScope     : BeanFactory id=db037356-bfde-3add-99c8-9a091ab75e53
2026-05-20T16:39:57.029+08:00 ERROR 21424 --- [u-common] [remote.worker.1] c.a.n.c.remote.client.grpc.GrpcClient    : Server check fail, please check server 192.168.150.101 ,port 9848 is available , error ={}

java.util.concurrent.ExecutionException: com.alibaba.nacos.shaded.io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
	at com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture.getDoneValue(AbstractFuture.java:592) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture.get(AbstractFuture.java:467) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.common.remote.client.grpc.GrpcClient.serverCheck(GrpcClient.java:243) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.common.remote.client.grpc.GrpcClient.connectToServer(GrpcClient.java:367) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.common.remote.client.RpcClient.reconnect(RpcClient.java:502) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.common.remote.client.RpcClient.lambda$start$1(RpcClient.java:329) ~[nacos-client-2.3.2.jar:na]
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) ~[na:na]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:842) ~[na:na]
Caused by: com.alibaba.nacos.shaded.io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
	at com.alibaba.nacos.shaded.io.grpc.Status.asRuntimeException(Status.java:537) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.stub.ClientCalls$UnaryStreamToFuture.onClose(ClientCalls.java:548) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.DelayedClientCall$DelayedListener$3.run(DelayedClientCall.java:489) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.DelayedClientCall$DelayedListener.delayOrExecute(DelayedClientCall.java:453) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.DelayedClientCall$DelayedListener.onClose(DelayedClientCall.java:486) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:567) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:71) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:735) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:716) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) ~[nacos-client-2.3.2.jar:na]
	... 3 common frames omitted
Caused by: com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: no further information: /192.168.150.101:9848
Caused by: java.net.ConnectException: Connection refused: no further information
	at java.base/sun.nio.ch.Net.pollConnect(Native Method) ~[na:na]
	at java.base/sun.nio.ch.Net.pollConnectNow(Net.java:684) ~[na:na]
	at java.base/sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:946) ~[na:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel.doFinishConnect(NioSocketChannel.java:337) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.finishConnect(AbstractNioChannel.java:334) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:776) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[nacos-client-2.3.2.jar:na]
	at com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[nacos-client-2.3.2.jar:na]
	at java.base/java.lang.Thread.run(Thread.java:842) ~[na:na]

2026-05-20T16:39:57.138+08:00  INFO 21424 --- [u-common] [remote.worker.1] c.a.n.c.remote.client.grpc.GrpcClient    : grpc client connection server:192.168.150.101 ip,serverPort:9848,grpcTslConfig:{"sslProvider":"","enableTls":false,"mutualAuthEnable":false,"trustAll":false}
2026-05-20T16:39:57.158+08:00  WARN 21424 --- [u-common] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration$DeferringLoadBalancerInterceptorConfig' of type [org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration$DeferringLoadBalancerInterceptorConfig] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [lbRestClientPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2026-05-20T16:39:57.160+08:00  WARN 21424 --- [u-common] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'deferringLoadBalancerInterceptor' of type [org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [lbRestClientPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2026-05-20T16:39:57.398+08:00  INFO 21424 --- [u-common] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8087 (http)
2026-05-20T16:39:57.412+08:00  INFO 21424 --- [u-common] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-05-20T16:39:57.412+08:00  INFO 21424 --- [u-common] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.30]
2026-05-20T16:39:57.477+08:00  INFO 21424 --- [u-common] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2026-05-20T16:39:57.477+08:00  INFO 21424 --- [u-common] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1367 ms
Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts
2026-05-20T16:39:57.719+08:00  INFO 21424 --- [u-common] [           main] o.s.c.openfeign.FeignClientFactoryBean   : For 'user-service' URL not provided. Will try picking an instance via load-balancing.
2026-05-20T16:39:57.739+08:00 ERROR 21424 --- [u-common] [           main] o.s.c.o.support.SpringMvcContract        : Cannot process class: com.u.api.user.api.InternalAdminUserApi. @RequestMapping annotation is not allowed on @FeignClient interfaces.
2026-05-20T16:39:57.740+08:00  WARN 21424 --- [u-common] [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'adminController': Unsatisfied dependency expressed through field 'adminService': Error creating bean with name 'adminServiceImpl' defined in file [E:\T\u-trade\admin-service\target\classes\com\u\admin\service\impl\AdminServiceImpl.class]: Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'com.u.api.client.user.UserClient': FactoryBean threw exception on object creation
2026-05-20T16:39:57.741+08:00  WARN 21424 --- [u-common] [           main] s.c.a.AnnotationConfigApplicationContext : Exception thrown from ApplicationListener handling ContextClosedEvent

org.springframework.beans.factory.BeanCreationNotAllowedException: Error creating bean with name 'applicationTaskExecutor': Singleton bean creation not allowed while singletons of this factory are in destruction (Do not request a bean from a BeanFactory in a destroy method implementation!)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:220) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:205) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.context.event.AbstractApplicationEventMulticaster.retrieveApplicationListeners(AbstractApplicationEventMulticaster.java:265) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.event.AbstractApplicationEventMulticaster.getApplicationListeners(AbstractApplicationEventMulticaster.java:222) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.event.SimpleApplicationEventMulticaster.multicastEvent(SimpleApplicationEventMulticaster.java:145) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:452) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:458) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:385) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.doClose(AbstractApplicationContext.java:1139) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.close(AbstractApplicationContext.java:1102) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.cloud.context.named.NamedContextFactory.destroy(NamedContextFactory.java:113) ~[spring-cloud-context-4.1.2.jar:4.1.2]
	at org.springframework.beans.factory.support.DisposableBeanAdapter.destroy(DisposableBeanAdapter.java:211) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroyBean(DefaultSingletonBeanRegistry.java:587) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroySingleton(DefaultSingletonBeanRegistry.java:559) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.destroySingleton(DefaultListableBeanFactory.java:1202) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroySingletons(DefaultSingletonBeanRegistry.java:520) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.destroySingletons(DefaultListableBeanFactory.java:1195) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.destroyBeans(AbstractApplicationContext.java:1195) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:638) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:335) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1363) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1352) ~[spring-boot-3.3.4.jar:3.3.4]
	at com.u.admin.AdminServiceApplication.main(AdminServiceApplication.java:16) ~[classes/:na]

2026-05-20T16:39:57.743+08:00  INFO 21424 --- [u-common] [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2026-05-20T16:39:57.761+08:00  INFO 21424 --- [u-common] [           main] .s.b.a.l.ConditionEvaluationReportLogger : 

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-05-20T16:39:57.779+08:00 ERROR 21424 --- [u-common] [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'adminController': Unsatisfied dependency expressed through field 'adminService': Error creating bean with name 'adminServiceImpl' defined in file [E:\T\u-trade\admin-service\target\classes\com\u\admin\service\impl\AdminServiceImpl.class]: Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'com.u.api.client.user.UserClient': FactoryBean threw exception on object creation
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.resolveFieldValue(AutowiredAnnotationBeanPostProcessor.java:788) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.inject(AutowiredAnnotationBeanPostProcessor.java:768) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.annotation.InjectionMetadata.inject(InjectionMetadata.java:145) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessProperties(AutowiredAnnotationBeanPostProcessor.java:509) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(AbstractAutowireCapableBeanFactory.java:1439) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:599) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:337) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:971) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.13.jar:6.1.13]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:335) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1363) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1352) ~[spring-boot-3.3.4.jar:3.3.4]
	at com.u.admin.AdminServiceApplication.main(AdminServiceApplication.java:16) ~[classes/:na]
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'adminServiceImpl' defined in file [E:\T\u-trade\admin-service\target\classes\com\u\admin\service\impl\AdminServiceImpl.class]: Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'com.u.api.client.user.UserClient': FactoryBean threw exception on object creation
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:795) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:237) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1375) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1212) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:337) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.resolveFieldValue(AutowiredAnnotationBeanPostProcessor.java:785) ~[spring-beans-6.1.13.jar:6.1.13]
	... 20 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'com.u.api.client.user.UserClient': FactoryBean threw exception on object creation
	at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.doGetObjectFromFactoryBean(FactoryBeanRegistrySupport.java:188) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.getObjectFromFactoryBean(FactoryBeanRegistrySupport.java:124) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getObjectForBeanInstance(AbstractBeanFactory.java:1867) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.getObjectForBeanInstance(AbstractAutowireCapableBeanFactory.java:1296) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:347) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904) ~[spring-beans-6.1.13.jar:6.1.13]
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:782) ~[spring-beans-6.1.13.jar:6.1.13]
	... 33 common frames omitted
Caused by: java.lang.IllegalArgumentException: @RequestMapping annotation not allowed on @FeignClient interfaces
	at org.springframework.cloud.openfeign.support.SpringMvcContract.processAnnotationOnClass(SpringMvcContract.java:186) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at feign.Contract$BaseContract.parseAndValidateMetadata(Contract.java:104) ~[feign-core-13.2.1.jar:na]
	at org.springframework.cloud.openfeign.support.SpringMvcContract.parseAndValidateMetadata(SpringMvcContract.java:197) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at feign.Contract$BaseContract.parseAndValidateMetadata(Contract.java:65) ~[feign-core-13.2.1.jar:na]
	at feign.ReflectiveFeign$ParseHandlersByName.apply(ReflectiveFeign.java:137) ~[feign-core-13.2.1.jar:na]
	at feign.ReflectiveFeign.newInstance(ReflectiveFeign.java:56) ~[feign-core-13.2.1.jar:na]
	at feign.ReflectiveFeign.newInstance(ReflectiveFeign.java:48) ~[feign-core-13.2.1.jar:na]
	at feign.Feign$Builder.target(Feign.java:201) ~[feign-core-13.2.1.jar:na]
	at org.springframework.cloud.openfeign.DefaultTargeter.target(DefaultTargeter.java:30) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at org.springframework.cloud.openfeign.FeignClientFactoryBean.loadBalance(FeignClientFactoryBean.java:429) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at org.springframework.cloud.openfeign.FeignClientFactoryBean.getTarget(FeignClientFactoryBean.java:477) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at org.springframework.cloud.openfeign.FeignClientFactoryBean.getObject(FeignClientFactoryBean.java:452) ~[spring-cloud-openfeign-core-4.1.1.jar:4.1.1]
	at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.doGetObjectFromFactoryBean(FactoryBeanRegistrySupport.java:182) ~[spring-beans-6.1.13.jar:6.1.13]
	... 43 common frames omitted

2026-05-20T16:39:57.780+08:00  WARN 21424 --- [u-common] [       Thread-1] c.a.n.common.http.HttpClientBeanHolder   : [HttpClientBeanHolder] Start destroying common HttpClient
2026-05-20T16:39:57.780+08:00  WARN 21424 --- [u-common] [       Thread-3] c.a.nacos.common.notify.NotifyCenter     : [NotifyCenter] Start destroying Publisher
2026-05-20T16:39:57.781+08:00  WARN 21424 --- [u-common] [       Thread-3] c.a.nacos.common.notify.NotifyCenter     : [NotifyCenter] Destruction of the end

Process finished with exit code 1
上述为admin-service启动失败日志，给gemini后得出的分析：
你的日志中暴露出两个明显的问题。虽然第一个问题（Nacos 连接失败）目前只是警告，但稍后会导致微服务功能异常；而导致你当前系统直接崩溃（Crash）的致命原因，是 Feign 客户端的配置错误。

以下是具体的错误分析和解决办法：

1. 致命崩溃原因：Feign 客户端禁用 @RequestMapping
看准你日志里的这一行核心报错：

Plaintext
Cannot process class: com.u.api.user.api.InternalAdminUserApi. @RequestMapping annotation is not allowed on @FeignClient interfaces.
为什么会报错？
你当前使用的是 Spring Boot 3.3.4 匹配的较新版本 Spring Cloud OpenFeign。在较新的版本中，Spring Cloud 做了严格限制：不允许在标注了 @FeignClient 的接口类上直接使用类级别的 @RequestMapping 注解。

如何修复？
打开你的 InternalAdminUserApi.java 接口文件，你现在的代码大概长这样：

Java
// ❌ 错误写法
@FeignClient(name = "user-service")
@RequestMapping("/internal/admin/users") // 就是这行导致了崩溃！
public interface InternalAdminUserApi {
    
    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
请将其修改为：去掉类级别的 @RequestMapping，把基础路径（Base Path）移入 @FeignClient 的 path 属性中：

Java
//  正确写法
@FeignClient(name = "user-service", path = "/internal/admin/users") // 基础路径写在这里
public interface InternalAdminUserApi {
    
    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
2. 潜在隐患：Nacos 服务连接失败
当你把上面的 Feign 问题修复后，程序可以正常初始化了，但随后你可能会遇到无法注册服务或读取配置的问题，因为你的项目无法连接到 Nacos：

Plaintext
Server check fail, please check server 192.168.150.101 ,port 9848 is available , error ={}
Caused by: java.net.ConnectException: Connection refused: no further information
为什么会报错？
Nacos 2.x 版本之后，除了传统处理 HTTP 请求的的 8848 端口外，还引入了 gRPC 协议进行客户端与服务端通信。这就要求必须额外开放两个端口：

9848（客户端 gRPC 请求服务端端口）-> 你的项目正卡在这个端口连不上

9849（服务端内部同步端口）

如何修复？
如果 Nacos 运行在远程服务器/虚拟机 (192.168.150.101)：检查该机器的防火墙（如 iptables 或 firewalld），确保放行了 9848 端口（TCP 协议）。

如果 Nacos 是用 Docker 部署的：检查 Docker 容器启动命令或 docker-compose.yml 文件，确认是否把 9848 端口映射出来了。例如：

YAML
ports:
  - "8848:8848"
  - "9848:9848"
  - "9849:9849"
检查 Nacos 是否正常存活：确保那台服务器上的 Nacos 服务没有挂掉。

建议排查顺序
先去修改 InternalAdminUserApi 接口，把 @RequestMapping 改掉，让 Spring 容器能够顺利启动。如果启动后依然报错无法连接 Nacos，在你的本地电脑打开命令行（PowerShell），用指令 tcping 192.168.150.101 9848 或 telnet 192.168.150.101 9848 测试一下网络端口是否畅通。