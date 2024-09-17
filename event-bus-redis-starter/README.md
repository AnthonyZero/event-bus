## Redis(Stream)

### 特性
1. 分布式消息队列（及时消息）
2. 延时消息（任意时间颗粒度）- 暂时不支持分布式
3. 消息消费失败重试及轮询特性
4. 消息可靠性，Redis5.0 Stream（消费者组 消费组 ACK机制 PEL队列）

### 使用

Maven 引入
````
<dependency>
    <groupId>com.anthonyzero.eventbus</groupId>
    <artifactId>event-bus-redis-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
````

应用配置
````
eventbus:
  # 不配置默认spring.profiles.active
  env: dev
  # 不配置默认spring.application.name
  service-id: app
  # 底层实现redis stream
  type: redis
  # 异步消息接收并发数，默认为：1
  concurrency: 1
  # 延时消息接收并发数，默认为：2
  delay-concurrency: 2
  # 单次获取消息数量，默认：16条
  msg-batch-size: 16
  # 节点联通性配置
  test-connect:
    #  轮询检测时间间隔，单位：秒，默认：35秒进行检测一次
    poll-second: 30
    # 丢失连接最长时间大于等于次值设置监听容器为连接断开，单位：秒，默认：120秒
    lose-connect-max-milli-Second: 120
  # 消息投递失败时配置信息
  fail:
    #消息投递失败时，一定时间内再次进行投递的次数，默认：3次
    retry-count: 2
    # 下次触发时间，单位：秒，默认10秒 ，
    next-time: 10
  redis:
    # 默认为：否，不开启阻塞和轮询
    poll-block: false
    # 非阻塞轮询时，接收消息的线程池中线程最大数，默认为：5个
    poll-thread-pool-size: 5
    # 非阻塞轮询时，接收消息的线程池中空闲线程存活时长，单位：秒，默认为：300s
    poll-thread-keep-alive-time: 300
    # 消息超时时间，超时消息未被确认，才会被重新投递，单位：秒，默认：5分钟
    deliver-timeout: 60
    # 未确认消息，重新投递时每次最多拉取多少条待确认消息数据，默认：100条
    pending-messages-batch-size: 100
    # stream 过期时间，6.2及以上版本支持，单位：小时，默认：5 天
    stream-expired-hours: 120
    # stream 过期数据截取，搭配streamExpiredLength 是否精确修剪，默认：False
    stream-precise-pruning: false
    # stream 过期数据截取，值为当前保留的消息数，5.0~<6.2版本支持，单位：条，默认：50000条
    stream-expired-length: 50000

````

### 消息生产者

SpringBoot自动注入消息生产者实例MsgSender, 支持发送及时消息以及延时消息
> 注意点：及时消息支持分布式消息, 延时消息暂时只支持本服务下也就是同一个JVM进程生产和消费

* 使用1（及时消息）:消息内容实现MsgBody, 重写赋值Code编码。然后进行发送
````
void send(MsgBody body) 
````

* 使用2（及时消息）：消息编码作为参数进行发送
````
void send(String code, Object body)
````

* 使用3（延期消息）：直接指定监听器类型
````
void sendDelayMessage(Class<? extends MsgDelayListener> listener, MsgBody body, long delayTime)
````

* 使用4（延期消息）：指定消息编码作为参数发送（推荐）
````
void sendDelayMessage(String code, Object body, long delayTime)
````

注意点：由于项目启动的时候会注册相关实现Bean,可能会出现循环依赖。可以在使用的时候@Lazy MsgSender

> 更多使用详情见MsgSender的API

### 消息消费者

#### 及时消息

1. 实现类继承MsgListener<T>
* supper() 指定要监听的消息编码以及其他参数
* 业务实现void onMessage(Message<T> message) 方法

2. 注解方式-处理方法实现添加@Listener 注解
* @Listener指定需要订阅的消息编码，其他参数根据情况而定


#### 延期消息
1. 业务实现类实现MsgDelayListener<T>接口
* 实现两个接口，一个是订阅消息编码，一个是业务处理方法

2. 注解方式@DelayListener
* DelayListener指定需要订阅的消息编码，其他参数根据情况而定

### 消费失败重试机制(推荐业务使用)
* @Fail注解 支持消费失败回调的时候重试
三个参数：
1. 失败回调的方法：回调方法必须和订阅器处理器在同一个类中,最后一次重试消费投递仍然抛出异常时会调用此方法
此方法会携带两个参数, 消息体和原始异常
````
public void exceptionHandler(Message<T> message, Throwable throwable) {
    log.error("消息投递消费最终失败！: {}，{}", message.getRequestId(), throwable.getMessage());
}
````

2. 消息投递失败时，一定时间内再次进行投递的次数(retryCount), 不指定的时候只用配置中的重试次数，见上面
应用配置fail.retry-count

3. 同理投递的重试次数，可设置投递失败时，下次投递触发的间隔时间（nextTime）,单位：秒。全局配置也同理
应用配置中的fail.next-time

   
### 消费轮询机制（非常规场景）
消息消费方法上添加@Polling 注解，消费成功后会根据参数重复进行消费，详情见代码


### 消息业务拦截器
支持业务方在消息的生命周期中自定义业务逻辑进行拦截处理
只需要实现相关接口并注入Spring容器中
1. 发送前 com.anthonyzero.eventbus.core.api.interceptor.SendBeforeInterceptor
2. 发送后 com.anthonyzero.eventbus.core.api.interceptor.SendAfterInterceptor
3. 消费成功 com.anthonyzero.eventbus.core.api.interceptor.DeliverSuccessInterceptor
4. 消费最终失败 com.anthonyzero.eventbus.core.api.interceptor.DeliverThrowableInterceptor




