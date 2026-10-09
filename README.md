<p align="center">
  <img src="/doc/logo.svg" width="120" alt="Easy Extension Logo">
</p>

<h1 align="center">Easy Extension</h1>

<p align="center">
  <b>Java 扩展点框架,让复杂系统的业务扩展变简单</b>
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.xiaoshicae/easy-extension-core"><img src="https://img.shields.io/maven-central/v/io.github.xiaoshicae/easy-extension-core?color=blue" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green" alt="License"></a>
  <img src="https://img.shields.io/badge/JDK-21+-orange" alt="JDK 21+">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen" alt="Spring Boot">
</p>

<p align="center">
  <a href="#快速开始">快速开始</a> · <a href="#核心概念">核心概念</a> · <a href="#常用用法">常用用法</a> · <a href="#配置">配置</a> · <a href="#管理后台">管理后台</a> · <a href="#从-3x-升级">升级</a>
</p>

---

## 解决什么问题

一个系统接入多个业务方,每个业务方的定制逻辑不同,代码就会变成这样:

```java
if ("retail".equals(bizCode)) {
    freight = BigDecimal.ZERO;            // 零售包邮
} else if ("fresh".equals(bizCode)) {
    freight = calcColdChainFreight();     // 生鲜冷链运费
} else {
    freight = DEFAULT;
} // 促销、风控、支付……每个流程都要重复一遍
```

Easy Extension 用**扩展点**替代 if-else:通用流程只依赖接口,不同业务提供各自的实现,框架在运行时按当前请求的业务身份选择正确的那个。

```java
@Service
public class OrderService {
    @ExtensionInject
    private FreightCalcExtension freightCalc;                // 注入即用(注入动态代理，根据context调用各个business的实现)

    public void process(OrderContext ctx) {
        BigDecimal freight = freightCalc.calcFreight(ctx);   // 框架自动路由,没有 if-else
    }
}
```

## 快速开始

完整样例:[easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.xiaoshicae</groupId>
    <artifactId>easy-extension-spring-boot-starter</artifactId>
    <version>4.1.1</version>
</dependency>
```

> 需要 JDK 21、Spring Boot 4.x。扩展点、能力、业务会从 Spring Boot 应用所在的包自动扫描;在其他包时用 `@ExtensionScan(basePackages = "...")` 补充。

### 2. 定义扩展点和它的默认实现

```java
@ExtensionPoint
public interface FreightCalcExtension {
    BigDecimal calcFreight(OrderContext ctx);
}

@DefaultImplementation                       // 兜底:没有业务或能力覆盖时调用
public class DefaultFreight implements FreightCalcExtension {
    public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("10.00"); }
}
```

### 3. 定义能力(可复用的实现)

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements Matcher<OrderMatchParam>, FreightCalcExtension {
    public boolean match(OrderMatchParam param) {          // 这个请求是否启用该能力
        return param.getAbilityCodes().contains("free-shipping");
    }
    public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}
```

### 4. 定义业务(挂载能力 + 自己的实现)

```java
@Business(code = "biz.retail", abilities = {FreeShippingAbility.class, Self.class})
public class RetailBusiness implements Matcher<OrderMatchParam>, FreightCalcExtension {
    public boolean match(OrderMatchParam param) {          // 这个请求是否属于该业务
        return "retail".equals(param.getBizCode());
    }
    public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("8.00"); }
}
```

`abilities` 的**顺序就是优先级**,越靠前越优先;`Self.class` 代表业务自己,不写时业务自己排最前。上例包邮能力排在 `Self` 之前,所以会覆盖零售自己的运费。

### 5. 告诉框架"这个请求是谁"

业务是**按请求**选出来的,所以要有一个入口告诉框架"当前请求是谁"。Spring MVC 的请求由 starter 代劳:声明一个 `MatcherParamResolver` Bean,starter 会在每个请求开始时(`preHandle`)用它构造匹配参数、选出业务,并**绑定到处理线程**;请求结束时(`afterCompletion`)解绑。之后注入的扩展点读到的就是本请求的业务:

```java
@Bean
MatcherParamResolver<OrderMatchParam> resolver() {
    return request -> OrderMatchParam.from(request);   // 例如从请求头/参数构造
}
```

resolver 返回业务和能力 `match` 用的那个类型。**身份只是一个业务码时更简单**:不用自己的参数类型,业务类也不用实现 `Matcher`(能力仍要实现 `Matcher<String>`),两个一行的 Bean 就够了:

```java
@Bean MatcherParamResolver<String> resolver()     { return request -> request.getHeader("X-Biz-Code"); }
@Bean BusinessResolver<String> businessResolver() { return Optional::ofNullable; }   // 业务码直接当 code,见"按 code 直达业务"
```

**不是每个接口都要走扩展点?** 用 `easy-extension.session-include-path-patterns`(只绑定这些)和 `session-exclude-path-patterns`(这些不绑定)圈定范围。范围外的接口不调用 resolver,也不要求带身份;范围内的接口,没匹配到业务默认会报错,所以不用扩展点的接口(健康检查、普通查询)请排除。启动日志会写明生效的范围(`HTTP binding is on: ...`)。

非 Spring MVC 的入口(RPC、消息、定时任务、线程池)不会自动绑定,自己一行搞定,见[绑定的各种入口](#绑定的各种入口)。

### 6. 注入使用

```java
@RestController
public class OrderController {
    @ExtensionInject private FreightCalcExtension freight;          // 当前业务的实现
    @ExtensionInject private List<NotifyExtension> notifiers;       // 所有生效的实现,按优先级,最后是默认实现

    @PostMapping("/checkout")
    public BigDecimal checkout() { return freight.calcFreight(ctx); }
}
```

`@ExtensionInject` 可以放在字段、构造器参数、方法参数上。

## 核心概念

<img src="/doc/concept.svg" alt="核心概念">

| 概念 | 说明 |
|---|---|
| **扩展点** `@ExtensionPoint` | 接口契约,规定"做什么"(运费计算、订单校验……) |
| **能力** `@Ability` | 可被多个业务复用的实现(包邮、VIP 券……),实现 `Matcher<T>` 决定何时生效 |
| **业务** `@Business` | 接入方(零售、生鲜……),实现 `Matcher<T>` 识别请求,`abilities` 挂载能力,也可以自己实现扩展点 |
| **默认实现** `@DefaultImplementation` | 兜底。每个扩展点都有,保证永远可调用 |

**解析顺序**:`abilities` 里的能力与 `Self`(业务自身)按声明顺序 → 该扩展点的默认实现。

<img src="/doc/how-it-works.svg" alt="运行流程">

一个请求进来:**匹配业务 → 判断各能力是否生效 → 按 `abilities` 顺序排列 → 调用第一个实现了该扩展点的**。业务只需实现自己关心的扩展点,其余自动落到通用能力或默认实现。

### 默认实现的三种写法

| 扩展点 | 写法 |
|---|---|
| 只有 `void` 方法(钩子) | 什么都不用写,框架提供空实现 |
| 单方法 | lambda:`@Bean @DefaultImplementation FreightCalcExtension f() { return ctx -> TEN; }` |
| 其他 | `@DefaultImplementation` 类。一个类可以兜底多个扩展点,每个扩展点至多一个兜底 |

有返回值又没有默认实现时,**启动就会报错**,不会静默返回 `null`。

### 严格模式

默认情况下,**没有匹配任何业务的请求会报错**(`NO_BUSINESS_MATCHED`),多个业务同时匹配也会报错。设置 `easy-extension.allow-unknown-business=true` 后,无匹配时所有扩展点走默认实现,多个匹配时按 `business-match-order` 选一个。

## 常用用法

### 绑定的各种入口

绑定属于**当前线程**,在一次调用开始的地方做一次,往下的调用都能读到:

| 入口 | 写法 |
|---|---|
| Spring MVC 请求 | 声明 `MatcherParamResolver` Bean,自动绑定 |
| RPC、消息消费、定时任务、测试 | `context.runWith(param, () -> ...)`,要返回值用 `callWith` |
| 线程池、`CompletableFuture` | `pool.submit(context.wrap(task))`,或 `context.executor(pool)` |
| Spring `@Async` | `easy-extension.async-propagation=true` |
| 响应式 | 显式传递 `Resolution`,用到时 `context.callWith(resolution, ...)` |

```java
@KafkaListener(topics = "orders")
public void onOrder(OrderMessage msg) {
    context.runWith(new OrderMatchParam(msg.getBizCode()), () -> orderService.handle(msg));
}
```

框架背后做了什么、各种入口(含 gRPC)的完整写法、怎么看日志和排查 `NO_BINDING`,见 [绑定指南](doc/binding.md)。

### 编程式调用

```java
@Autowired ExtensionContext<OrderMatchParam> context;

context.runWith(orderParam, () -> {                         // 绑定到当前线程,结束后恢复,可嵌套
    String risk = context.invoke(RiskControlExtension.class, e -> e.check(ctx));            // 第一个实现
    List<String> channels = context.invokeAll(NotifyExtension.class, e -> e.channels(ctx)); // 所有实现
    BigDecimal total = context.invokeReduce(PromotionCalcExtension.class,
            e -> e.calc(ctx), BigDecimal.ZERO, BigDecimal::add);                            // 聚合
});
```

不想依赖线程绑定时,`context.resolve(param)` 返回**不可变快照** `Resolution`,线程安全,可显式传递(跨线程、响应式场景);要在另一个线程里使用,`context.runWith(resolution, ...)`。同一请求需要多个身份时,持有多个 `Resolution`。

### 不使用 Spring

core 只依赖 JDK 和 slf4j:

```java
ExtensionContext<OrderMatchParam> context = ExtensionContext.<OrderMatchParam>builder()
        .extensionPoint(FreightCalcExtension.class)
        .defaultImplementation(new DefaultFreight())
        .ability(new FreeShippingAbility())
        .business(new RetailBusiness())
        .build();        // 一次性校验全部装配,失败整体失败;产物不可变
```

### 能力的依赖与互斥

```java
@Ability(code = "ability.installment", requires = RiskControlAbility.class)   // 必须同时挂载
@Ability(code = "ability.free-shipping", excludes = RapidDeliveryAbility.class) // 不能同时挂载
```

装配时校验,违反则启动失败。

### 按 code 直达业务

业务很多时,不必逐个 `match`:

```java
@Bean
BusinessResolver<OrderMatchParam> resolver() { return p -> Optional.ofNullable(p.getBizCode()); }
```

配置后业务可以不实现 `Matcher`。身份只是业务码时,`T` 直接用 `String`,见第 5 步。

### 排查"为什么选了这个实现"

```java
Resolution r = context.resolve(param);
r.trace();                                   // 命中的业务、解析链、被跳过的能力、耗时
r.explain(FreightCalcExtension.class);       // 该扩展点的所有候选,以及最终选了谁
context.catalog();                           // 全部扩展点/能力/业务/默认实现的只读元数据
context.isBound();                           // 当前线程有没有绑定;context.current() 可以直接打印
```

报 `NO_BINDING`(当前线程没有绑定)时,按 [绑定指南](doc/binding.md#6-出问题时怎么查) 里的清单逐条检查。

要看每次请求的匹配过程日志:

```yaml
logging.level.io.github.xiaoshicae.extension.core.internal.Resolver: DEBUG
```

## 配置

| 配置项 | 默认 | 说明 |
|---|---|---|
| `easy-extension.allow-unknown-business` | `false` | 无业务匹配时是否放行(放行则走默认实现) |
| `easy-extension.business-match-order` | 空 | 放行模式下多个业务同时匹配时,按业务 code 的顺序选择 |
| `easy-extension.session-include-path-patterns` | 空(全部) | 只对这些路径(Ant 风格)绑定业务身份,如 `/api/**` |
| `easy-extension.session-exclude-path-patterns` | 空 | 不绑定业务身份的路径(Ant 风格),如 `/actuator/**`,在包含之后排除 |
| `easy-extension.async-propagation` | `false` | 让 `@Async` / `applicationTaskExecutor` 的任务沿用提交者的业务绑定 |
| `easy-extension.enable-session-auto-cleanup` | `true` | 请求结束兜底清理线程上残留的绑定 |
| `easy-extension.matcher-param-type` | 自动推导 | 显式指定匹配参数类型 |
| `easy-extension.admin.enable` | `true` | 管理后台开关(引入 admin 依赖后) |
| `easy-extension.admin.path` | `/easy-extension-admin` | 管理后台访问路径 |
| `easy-extension.admin.auth.basic.username` / `password` | 空 | 非空时启用 Basic 认证 |
| `easy-extension.admin.extension-point-order` | 空 | 扩展点在后台的展示顺序 |

## 管理后台

可视化查看扩展点、能力、业务,以及同一扩展点上多个实现的冲突。

```xml
<dependency>
    <groupId>io.github.xiaoshicae</groupId>
    <artifactId>easy-extension-admin-spring-boot-starter</artifactId>
    <version>4.1.1</version>
</dependency>
```

默认访问 `/easy-extension-admin`。

![管理后台](/doc/admin-extension.png)

> **安全提示**:管理后台会展示扩展点、能力、业务的**源码**。它默认启用,**不配置认证时不做任何校验**。生产环境请配置 `easy-extension.admin.auth.basic`(或注册自己的 `AdminAuthenticationProvider` 接入 Spring Security),或设置 `easy-extension.admin.enable=false` 关闭。

## IDE 插件

IntelliJ IDEA 插件 **Easy Extension**(JetBrains Marketplace,需要 4.x 对应版本):在扩展点、能力、业务、`@ExtensionInject` 处显示导航图标,并提供扩展点拓扑视图。

## 从 3.x 升级

4.0 **不向后兼容**。概念不变,表达方式变了:`abilities` 改为能力类的有序数组、默认实现按扩展点兜底、`MatcherParamResolver` 取代手写 Interceptor 和 session API、`ExtensionContext` 不可变且无运行期注册。步骤和对照表见 [迁移指南](doc/migration-4.0.md),变更清单见 [CHANGELOG](CHANGELOG.md),设计说明见 [design-4.0.md](doc/design-4.0.md)。

## 文档

[Wiki](https://github.com/xiaoshicae/easy-extension/wiki) · [完整样例](https://github.com/xiaoshicae/easy-extension-sample) · [Go 版本](https://github.com/xiaoshicae/go-easy-extension)

## License

[Apache 2.0](LICENSE)
