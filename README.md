<p align="center">
  <img src="/doc/logo.svg" width="120" alt="Easy Extension Logo">
</p>

<h1 align="center">Easy Extension</h1>

<p align="center">
  <b>Java 扩展点框架，让复杂系统的业务扩展变简单</b>
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.xiaoshicae/easy-extension-core"><img src="https://img.shields.io/maven-central/v/io.github.xiaoshicae/easy-extension-core?color=blue" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green" alt="License"></a>
  <img src="https://img.shields.io/badge/JDK-17+-orange" alt="JDK 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5%20%7C%204.0-brightgreen" alt="Spring Boot">
</p>

<p align="center">
  <a href="#怎么解决">怎么解决</a> · <a href="#核心概念">核心概念</a> · <a href="#快速开始">快速开始</a> · <a href="#40-新用法预览">4.0 预览</a> · <a href="#管理后台">管理后台</a> · <a href="https://github.com/xiaoshicae/easy-extension/wiki">文档</a>
</p>

> **版本说明** · 当前发布版本是 3.x,下面的「快速开始」「调用方式」等章节对应当前代码。
> **4.0 正在设计中,尚未实现**:新用法见 [4.0 新用法预览](#40-新用法预览),决策见
> [ADR-0001](doc/adr/0001-v4-architecture.md) 与 [ADR-0002](doc/adr/0002-simplify-user-facing-api.md),API 草图见 [doc/design/v4-api-sketch.md](doc/design/v4-api-sketch.md)。

---

## 什么问题？

当一个系统需要接入多个业务方，每个业务方有不同的定制化需求时，代码通常会变成这样：

```java
// 到处都是 if-else，越来越难维护
if ("retail".equals(bizCode)) {
    freight = BigDecimal.ZERO;       // 零售包邮
} else if ("fresh".equals(bizCode)) {
    freight = calcColdChainFreight(); // 生鲜冷链运费
} else if ("digital".equals(bizCode)) {
    freight = DEFAULT_FREIGHT;       // 数码默认运费
}
// 促销、风控、支付方式... 每个流程都要这样写一遍
```

**Easy Extension 解决的就是这个问题**——用扩展点插件化的方式，替代满天飞的 if-else，让系统通用流程和业务定制逻辑彻底解耦。

## 怎么解决？

```java
@RestController
public class OrderController {

    // 注入 运费计算扩展点
    // 扩展点，不同业务有不同实现，框架自动选择当前业务对应的那个
    @ExtensionInject
    private FreightCalcExtension freightCalc;

    @PostMapping("/checkout")
    public String checkout() {
        BigDecimal freight = freightCalc.calcFreight(ctx); // 不需要 if-else，框架自动路由
        return "运费: ¥" + freight;
    }
}
```

> 没有 if-else，没有策略工厂。注入扩展点，直接调用，框架自动按业务身份和优先级选择正确的实现。

## 核心概念

<img src="/doc/concept.svg" alt="核心概念">

- **扩展点 (Extension Point)** — 底层接口契约，规定"做什么"（如运费计算、订单校验）
- **能力 (Ability)** — 通用的扩展点实现（如包邮、VIP券），可被多个业务复用
- **业务 (Business)** — 接入方（如零售、生鲜），挂载需要的能力，也可直接实现扩展点
- **默认实现 (Default Impl)** — 系统兜底实现，业务和能力均未覆盖时调用，保证扩展点永远可调用。可以按领域拆成多个，各自负责自己实现的扩展点；没有合理默认值的扩展点标 `@ExtensionPoint(mandatory = true)`，由业务或能力必须提供

> 运行时解析按优先级：**业务自身实现 → 业务挂载的能力 → 默认实现**

## 工作原理

<img src="/doc/how-it-works.svg" alt="运行流程">

一个请求进来后，框架自动完成：**匹配业务 → 激活能力 → 按优先级排序 → 调用正确的实现**。业务方只需实现自己关心的扩展点，其余自动降级到通用能力或默认实现。

## 快速开始

完整样例: [easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.xiaoshicae</groupId>
    <artifactId>easy-extension-spring-boot-starter</artifactId>
    <version>3.3.6</version>
</dependency>
```

> **兼容性**: JDK 17+，Spring Boot 3.5 与 4.0。CI 在 JDK 17 / 21 × Spring Boot 3.5 / 4.0 的组合上跑全量测试。
> 扩展点实现类可以放心使用 `@Cacheable` / `@Transactional` / `@Async` 等 Spring AOP 增强，框架调用的就是被增强后的 Bean。

### 2. 定义扩展点

```java
@ExtensionPoint
public interface FreightCalcExtension {
    BigDecimal calcFreight(OrderContext ctx);
}
```

### 3. 定义能力（可复用的通用实现）

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements Matcher<OrderMatchParam>, FreightCalcExtension {
    @Override
    public boolean match(OrderMatchParam param) {
        return param.getAbilityCodes() != null && param.getAbilityCodes().contains("free-shipping");
    }

    @Override
    public BigDecimal calcFreight(OrderContext ctx) {
        return BigDecimal.ZERO; // 包邮
    }
}
```

### 4. 定义业务（挂载能力 + 自定义实现）

```java
@Business(code = "biz.retail", priority = 100,
    abilities = {"ability.free-shipping::10"})
public class RetailBusiness implements Matcher<OrderMatchParam>, FreightCalcExtension {
    @Override
    public boolean match(OrderMatchParam param) {
        return "retail".equals(param.getBizCode());
    }

    @Override
    public BigDecimal calcFreight(OrderContext ctx) {
        return new BigDecimal("8.00"); // 零售默认运费
    }
}
```

> **优先级说明**: `ability.free-shipping::10` 表示包邮能力优先级为 10，RetailBusiness 自身优先级为 100。数字越小越优先，所以包邮能力会覆盖 Business 的运费计算。

### 5. 注入即用

和 [怎么解决？](#怎么解决) 中的代码一样，`@ExtensionInject` 注入扩展点，直接调用即可。

## 调用方式

除了 `@ExtensionInject` 注入代理对象，还可以通过 `IExtensionContext` 编程式调用：

```java
@Autowired
private IExtensionContext<OrderMatchParam> context;

// 调用最高优先级的实现
String risk = context.invoke(RiskControlExtension.class, e -> e.checkRisk(ctx));

// 调用所有匹配实现，返回列表
List<String> channels = context.invokeAll(NotifyExtension.class, e -> e.getNotifyChannels(ctx));

// 聚合所有匹配实现的结果（如累加优惠金额）
BigDecimal totalDiscount = context.invokeReduce(
    PromotionCalcExtension.class,
    e -> e.calcPromotion(ctx),
    BigDecimal.ZERO,
    BigDecimal::add
);
```

实现类抛出的异常会**原样**抛给调用方：运行时异常、`Error`、扩展点方法声明的受检异常都不会被代理层包裹；
找不到可用实现（例如没有初始化会话）时抛 `InvokeException`。

## 高级特性

<table>
<tr><td width="50%">

**扩展点版本化**
```java
@ExtensionPoint(version = 2)
public interface PaymentExtension {
    String pay(OrderContext ctx);
    // v2 新增，default 方法保证向后兼容
    default PaymentResult payV2(
        OrderContext ctx, PaymentOptions opts) {
        return new PaymentResult(pay(ctx));
    }
}
```

</td><td>

**能力互斥与依赖**
```java
// 分期需要风控先执行
@Ability(code = "ability.installment",
    requires = {"ability.risk-control"})

// 包邮和急速达互斥
@Ability(code = "ability.free-shipping",
    excludes = {"ability.rapid-delivery"})
```

</td></tr>
<tr><td>

**作用域会话**
```java
// 同一请求中多个独立匹配上下文
context.initSession(orderParam);
context.initSession("after-sale",
    afterSaleParam);
```

</td><td>

**解析追踪**
```java
context.initSession(param);
ResolveTrace trace = context.getLastResolveTrace();
// 命中业务、各能力匹配状态、解析链、耗时
```

</td></tr>
</table>

### 默认实现可以拆分，扩展点可以"必选"

```java
@ExtensionPointDefaultImplementation          // 只负责运费相关的扩展点
public class FreightDefaults implements FreightCalcExtension { ... }

@ExtensionPointDefaultImplementation          // 另一个领域，另一个类
public class PromotionDefaults implements PromotionCalcExtension { ... }

@ExtensionPoint(mandatory = true)             // 没有合理默认值：业务/能力必须自己实现
public interface InvoiceExtension { ... }
```

同一个扩展点只能有一个默认实现；不同 code 的默认实现不能使用同一个优先级（注册时就会报错，而不是每个请求都失败）。
启动时校验：每个非 `mandatory` 的扩展点都必须有默认实现。
如果有多个默认实现 Bean 且其中一个标了 `@Primary`，就只用它（与 3.3 一致）；没有 `@Primary` 时全部登记。

### 异步 / 线程池里沿用请求的业务身份

```java
ResolvedChain chain = context.currentChain();            // 请求线程上捕获已解析的身份（不可变）
executor.submit(() -> ExtensionSessionScope.runWith(     // 工作线程上绑定，不会再次执行 match
    context, chain, () -> service.handleAsync()));       // 结束后自动清理
```

任务不一定在新线程里跑（直接执行器、线程池饱和时的 `CallerRunsPolicy`、并行流都会在提交任务的线程上执行）。这时 `runWith` / `restore` 结束后会把该线程原来的会话重新绑回去，而不是清掉它。

### 拦截器：链路追踪、指标、熔断的挂载点

```java
@Bean
ExtensionInterceptor metrics(MeterRegistry registry) {
    return invocation -> {
        Timer.Sample sample = Timer.start(registry);
        try {
            return invocation.proceed();
        } finally {
            sample.stop(registry.timer("extension", "point", invocation.extensionPoint().getSimpleName(),
                    "impl", invocation.implementationCode()));
        }
    };
}
```

每次对扩展点实现的调用（`@ExtensionInject`、`invoke*`、`getFirstMatchedExtension` 等）都会经过拦截器，按 `@Order` 排序。
容器里的 `IScopedSessionManager`（会话存放位置，默认 ThreadLocal）和 `BusinessMatchSelector`（多业务匹配时怎么选）Bean 同样会被自动采用——前提是只有一个（或其中一个标了 `@Primary`）；有多个又没有 `@Primary` 时都不采用，并打印一条 WARN。

## 配置参考

```yaml
easy-extension:
  enable-log: true                    # 打印匹配过程日志
  unknown-business-policy: reject     # 无业务匹配: reject 报错 / default 走默认实现兜底
  multi-match-policy: reject          # 多业务匹配: reject 报错 / select 按下面的顺序（或 BusinessMatchSelector）选一个；两者都没配、只能按注册顺序取第一个时，同一组合打印一次 WARN
  allow-unknown-business: false       # 旧开关，两项策略都没配时生效: false = 都 reject，true = default + select
  business-match-order:               # multi-match-policy=select 时的优先级
    - biz.retail
    - biz.fresh
  admin:
    enable: true                      # 启用管理后台
    path: /easy-extension-admin       # 访问路径
    extension-point-order:            # 扩展点展示顺序
      - OrderValidateExtension
      - FreightCalcExtension
```

## 从 3.3 升级

这一批变更（4.0 的基础，见 [ADR-0001](doc/adr/0001-v4-architecture.md)）对公共 API 只做增量（由 CI 里的 japicmp 门禁保证），但有几处**行为**变化，升级前请对照检查：

- **异常原样抛出。** 实现类抛出的异常不再被 `UndeclaredThrowableException` 层层包裹。需要旧行为可加
  JVM 参数 `-Deasy-extension.legacy-exception-wrapping=true`（仅此一个版本，4.0 移除）。它**只认 JVM 系统属性**，
  写进 `application.yml` 不生效；启用后日志里会有一条 WARN。
- **被 Spring AOP 代理的实现类开始参与匹配。** `@Cacheable` / `@Transactional` / `@Async` 或被切面命中的
  `@Business` / `@Ability` / `@ExtensionPointDefaultImplementation`，此前被静默跳过（运行时才报 `no business matched`），
  现在会被正常注册和匹配。请先检查这类业务：`allow-unknown-business=true` 时，原来走默认实现的请求可能改走它们；
  与别的业务匹配条件重叠时，严格模式会报 `multiple business found`。扫描到却无法注册的 Bean 现在启动即报错。
- **框架使用的就是容器里的那个实例**（此前会另外创建一份）。测试里用 `@MockitoBean` / `@MockBean` 替换这类 Bean 时，
  被替换的对象就是框架调用的对象，记得 stub `match()`，否则会 `no business matched`。
- **继承来的扩展点也算。** 实现类的父类或父接口实现的扩展点接口现在会被识别（此前只看类自己声明的接口）。
  后果：父类实现了某个扩展点的业务，现在会真的响应它，此前由默认实现响应，**路由会变**；升级后可用
  `context.explain(扩展点.class)` 看每个扩展点由谁响应。继承来的扩展点若没有被注册（所在模块不在扫描范围内，或不是 public），
  与 3.3 一样被忽略；类自己声明的扩展点仍必须已注册。
- **关闭作用域会话只清自己的作用域。** `ExtensionSessionScope.openScoped(...)` 关闭时只移除那个作用域，
  此前会清掉本线程的所有会话。如果在块内另外 `initSession(...)` 了默认作用域，请自己 `removeSession()`。
- **几处错误信息变了**：没有任何默认实现时，`doRegister()` 报
  `extension point default implementation not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist`
  （原为 `...should not be null`）；默认实现未覆盖某扩展点的报错末尾多了关于 `mandatory = true` 的提示，并且在能力和业务都登记完之后才抛出；
  没有任何默认实现又没有业务匹配时，`initSession` 抛 `SessionException`（原为 `NullPointerException`）。
- **扩展点接口里不要声明 `getInstance()` / `getTargetClass()`**：这两个名字由框架的代理（`IProxy`）占用，
  返回类型不同会在创建代理时报错，返回类型相同则调用到不了你的实现。

完整列表见 [CHANGELOG](CHANGELOG.md)。

## 4.0 新用法预览

> ⚠️ **设计稿,尚未实现。** 这一节把 4.0 的用法摊开,为的是在写代码之前评审体验。API 以 [API 草图](doc/design/v4-api-sketch.md) 为准,取舍和理由见 [ADR-0001](doc/adr/0001-v4-architecture.md) 与 [ADR-0002](doc/adr/0002-simplify-user-facing-api.md)(用户面简化,提议中)。要用的话,请按上面的「快速开始」使用当前版本。

**目标:一个典型用户只写两个类、一行配置(外加依赖),其余都是普通注入。** 校验、路由、线程绑定、缓存都留在框架里。

### 60 秒上手

**0. 引入依赖** — `easy-extension-spring-boot-starter`(坐标不变,版本以 4.0 发布为准)。

**1. 扩展点** — 一个 `public` 接口,`default` 方法就是系统兜底:

```java
@ExtensionPoint
public interface FreightCalcExtension {
    default BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("10.00"); }
}
```

**2. 业务** — 一个带 `@Business` 的类,实现它关心的扩展点:

```java
@Business(code = "biz.retail")
public class RetailBusiness implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("8.00"); }
}
```

**3. 一行配置** — 业务码从哪个请求头来:

```yaml
easy-extension.web.business-header: X-Biz-Code
```

**4. 普通注入** — 没有 `@ExtensionInject`,也没有 if-else:

```java
@RestController
class OrderController {
    private final FreightCalcExtension freight;
    OrderController(FreightCalcExtension freight) { this.freight = freight; }

    @PostMapping("/checkout")
    String checkout(@RequestBody OrderContext ctx) { return "运费: ¥" + freight.calcFreight(ctx); }
}
```

注入进来的不是 `RetailBusiness`,而是框架生成的代理:每次调用,按**当前请求的业务**选实现。

| 请求 | 回答者 | 结果 |
|---|---|---|
| `X-Biz-Code: biz.retail` | 业务自己 | 8.00 |
| `X-Biz-Code: biz.other`(另一个没有实现它的业务) | 接口的 `default` | 10.00 |
| `X-Biz-Code: biz.unknown`(没有这个业务) | —— | HTTP 400:`business [biz.unknown] not found` |
| 没有请求头,却调用了扩展点 | —— | 报错(没人处理就是 HTTP 500),消息告诉你怎么办 |

- 业务类放在 `@SpringBootApplication` 所在的包树里就会被自动扫描,不需要 `@Component`,也不用写扫描配置。放在包树之外时,症状是"业务码未知"(上表第三行),用 `@ExtensionScan(scanPackages = "...")` 追加包。
- **请求头是信任边界**:`X-Biz-Code` 应由网关或认证层设置并覆盖客户端传来的值,应用直接暴露在外时任何客户端都能选业务。业务要从认证信息里取,见下面「进阶」里的 `IdentityResolver`。
- 业务码重复、能力不存在这类问题,启动时一次性全部报出。

### 复用逻辑:能力

**能力**是可被多个业务复用的实现,业务用 `abilities` 挂载它:

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}

// 业务自己没实现的方法,由挂载的能力回答
@Business(code = "biz.fresh", abilities = {"ability.free-shipping"})
public class FreshBusiness implements ColdChainExtension { ... }

// 要让能力压过业务自己的逻辑:放进 first
@Business(code = "biz.retail-plus", first = {"ability.free-shipping"})
public class RetailPlusBusiness implements FreightCalcExtension { ... }
```

回答顺序固定,一句话:**先于业务的能力(`first`)→ 业务自己 → 其余能力(`abilities`)→ 接口的 `default`。** 按**方法**逐个往下找:一个对象没实现某个方法,就轮到下一个。(兜底要用 Spring Bean 时,有一个排在接口 `default` 之前的 `@DefaultProvider`,见「进阶」。)

一个请求默认启用业务挂载的**全部**能力;只启用一部分,见下一节。

### 非 Web 入口、异步与收窄能力

```java
private final Extensions extensions;      // 和别的 Bean 一样注入

// MQ 消费者、定时任务:显式带上业务身份
extensions.run("biz.retail", () -> handle(message));

// 只启用业务挂载的一部分能力;去掉一个用 .without("ability.coupon")
extensions.run(Identity.of("biz.retail-plus").only("ability.free-shipping"), () -> handle(message));

// 异步:Boot 的 applicationTaskExecutor(@Async 的默认执行器)自动沿用提交时的身份。
// 自己声明的线程池要设置 ExtensionTaskDecorator;new Thread、CompletableFuture 的默认池、并行流:用 Extensions.wrap(...)
CompletableFuture.runAsync(() -> audit.record(order), Extensions.wrap(executor));   // wrap 是静态方法

// 取全部实现并聚合(替代 invokeAll / invokeReduce;不含接口的 default 体)
BigDecimal discount = extensions.all(PromotionCalcExtension.class).stream()
        .map(e -> e.calcPromotion(ctx)).reduce(BigDecimal.ZERO, BigDecimal::add);
```

- `Extensions.wrap` 捕获**调用它的那个线程**当时的身份;任务在本线程内联执行时(直接执行器、线程池饱和),本线程原来的身份会在任务结束后还原,不会被清掉。
- Web 里的能力头 `easy-extension.web.abilities-header`(逗号分隔)对应 `only`;要 `without`,写一个 `IdentityResolver`。
- 启用的能力,它 `requires` 的能力也必须启用,否则 `ResolutionException`。

### 必选的扩展点

方法不带 `default`,就是"必选":没有业务或能力实现它,调用时就失败,不需要写占位类。

```java
@ExtensionPoint
public interface InvoiceExtension {
    Invoice issue(OrderContext ctx);
}
```

零售业务没有实现它时,调用 `issue` 会得到:

```
ExtensionNotFoundException: Extension<InvoiceExtension#issue(OrderContext)> not found:
  abstract method, none of [biz.retail] implements it
```

### 测试

```java
// 纯 Java:不起 Spring
Extensions extensions = Extensions.of(new RetailBusiness());
FreightCalcExtension freight = extensions.extension(FreightCalcExtension.class);

@Test void retailPaysEight() {
    assertEquals(new BigDecimal("8.00"), extensions.call("biz.retail", () -> freight.calcFreight(ctx)));
}

// Spring 测试:直接指定身份,不用构造请求
@SpringBootTest
@WithIdentity("biz.retail")
class CheckoutTest { ... }
```

`@WithIdentity` 在 `easy-extension-test` 里(test 依赖);`Extensions.of(...)` 与启动时走同一套校验,测试里同样会得到聚合的启动期错误。

<details>
<summary><b>进阶</b>(按需再看):兜底要用 Bean、拦截器、身份不在请求头里、解释路由</summary>

**兜底逻辑要用 Spring Bean。** 接口的 `default` 方法拿不到注入的 Bean,这时用 `@DefaultProvider`(每个扩展点至多一个;没有业务或能力实现时由它回答,排在接口的 `default` 之前):

```java
@DefaultProvider
class DefaultTax implements TaxExtension {
    private final TaxRateService rates;
    DefaultTax(TaxRateService rates) { this.rates = rates; }
    @Override public BigDecimal tax(OrderContext ctx) { return rates.standardRate().apply(ctx.amount()); }
}
```

**指标、追踪、熔断。** 拦截器,用法和现在一样:

```java
@Bean ExtensionInterceptor metrics(MeterRegistry registry) { ... }
```

**身份不在请求头里**(JWT、路径变量、租户表……):提供一个 `IdentityResolver` Bean,它代替那行配置:

```java
@Bean IdentityResolver<HttpServletRequest> resolver() {
    return req -> {
        String tenant = tenantOf(req);                       // 例如从 JWT 里取
        return tenant == null ? null : Identity.of(tenant);  // null:弃权,不绑定(健康检查这类不带凭据的请求)
    };
}
```

返回 `Identity.none()` 表示"没有业务,只用默认层";返回 `null` 表示不绑定,调用扩展点时照样报错;抛 `new ResolutionException("...")`,请求得到 HTTP 400。

**继续用 `match()` 的写法。** 业务和能力继续实现 `Matcher<Req>`(在可选的 `core.matching` 子包里),由 `MatcherIdentityResolver` 判定身份。`Req` 是 `HttpServletRequest` 时 Web filter 自动使用它;`Req` 是你自己的参数类时,注入 `MatcherIdentityResolver<Req>`,在入口 `extensions.run(resolver.resolve(param), ...)`。无命中、多命中怎么处理是它的选项,也能用 `easy-extension.matching.*` 配置,默认都是 `reject`,不静默退回默认层:

```yaml
easy-extension:
  matching:
    no-match: reject        # reject(默认)| none:没有业务匹配时只由默认层回答
    multi-match: reject     # reject(默认)| first | ordered
    order: [biz.retail, biz.fresh]
```

**解释"这个身份下,每个方法由谁回答"。**

```java
Explanation e = extensions.open(Identity.of("biz.retail")).explain(FreightCalcExtension.class);
```

```
GET /actuator/extensions                  → 全部扩展点、业务、能力、提供者、提示
GET /actuator/extensions/explain?business=biz.retail&type=com.acme.FreightCalcExtension
                                          → 每个方法的候选者和被选中者
```

</details>

### 从 3.x 迁移

| | 现在(3.x) | 4.0 |
|---|---|---|
| 注入 | `@ExtensionInject` 字段 | 普通注入,构造器注入即可 |
| 判定请求是谁 | 每个业务实现 `Matcher<P>`,配 `@MatcherParam` | 一行配置读请求头;别的来源写一个 `IdentityResolver`;仍想用 `match()` 也可以 |
| 谁先回答 | 数字优先级:`priority`、`ability::10` | 业务先答;要让能力先答,写 `first = {...}` |
| 默认实现 | 一个类实现全部扩展点;没有合理默认值的标 `mandatory` | 接口的 `default` 方法;没有 `default` 的方法就是必选 |
| 会话 | 手动 `initSession` / `removeSession`,命名 scope | 不用管;非 Web 入口用 `extensions.run(...)` |
| 异步 | 自己把会话带进线程 | Boot 自动配置的线程池(`@Async` 的默认执行器)自动沿用;其余包一层 |
| 异常 | 13 个异常类,多数是受检异常 | 3 个运行时异常 |
| 管理后台 | 内嵌 UI | Actuator 端点(JSON),UI 另起仓库 |

- `@ExtensionInject X x` → 普通注入 `X x`;`context.invoke(X.class, ...)` → 直接调用注入的 `X`;`invokeAll` / `invokeReduce` → `extensions.all(X.class)` 加 stream。
- `initSession(param)` / `removeSession()` → 请求头一行配置,或一个 `IdentityResolver`;其他入口用 `extensions.run(...)`。
- `@Business(priority, abilities = {"a::10"})` → 按 3.x **实际解析出的数字**比较:小于业务自身 `priority` 的能力进 `first`,其余进 `abilities`,各自按数字升序。未写数字的能力,3.x 自动编为 1、2……(业务默认是 0)。适用于注解方式注册的类,计划提供 OpenRewrite recipe。
- `@ExtensionPointDefaultImplementation` 大类 → 接口的 `default` 方法;需要注入的用 `@DefaultProvider`。`@ExtensionPoint` 的 `mandatory` 直接删掉(没有 `default` 就是必选),`scenarios` / `version` 也去掉(它们只用于管理后台展示,接口演进照旧靠新增 `default` 方法)。
- 手写 `IBusiness` / `IAbility`(含数据驱动的业务) → 带注解的类,或 `Extensions.builder().business(...)` / `.ability(...)`。
- **要特别检查四处语义**:带 `default` 方法的扩展点(路由按方法而不是按接口)、能力的先后(3.x 按数字比较,业务默认 0、未编号的能力自动编为 1、2……;4.0 默认业务在前,数字更小的能力要进 `first`)、能力的启用(4.0 默认挂载的全部启用,原来靠 `match()` 按请求启用的,用 `only` / `without` 或 Matcher 风格)、聚合(`all` 不含接口的 `default` 体,原来默认实现也会被 `invokeAll` 遍历到)。

完整对照表见 [API 草图 §8](doc/design/v4-api-sketch.md#8-从-3x-迁移)。

## 管理后台

引入依赖即可使用，提供扩展点、能力、业务的可视化管理和冲突检测。

```xml
<dependency>
    <groupId>io.github.xiaoshicae</groupId>
    <artifactId>easy-extension-admin-spring-boot-starter</artifactId>
    <version>3.3.6</version>
</dependency>
```

默认访问: `/easy-extension-admin` 

![管理后台](/doc/admin-extension.png)

## 适用场景

框架适用于**多接入方 + 复杂定制**的中台系统：

| 场景 | 扩展点举例 | 不同业务的差异 |
|------|----------|-------------|
| **电商交易** | 订单校验、运费计算、促销计算、支付方式 | 零售包邮 vs 生鲜冷链运费 vs 数码分期支付 |
| **履约系统** | 仓库选择、配送方式、签收规则 | 普通快递 vs 冷链配送 vs 同城急送 |
| **营销中台** | 优惠计算、券核销、活动规则 | 新人券 vs 会员折扣 vs 满减活动 |
| **支付系统** | 风控检查、渠道路由、对账规则 | 小额免密 vs 大额人脸识别 vs 企业审批 |

## 文档

[Wiki](https://github.com/xiaoshicae/easy-extension/wiki) · [Go 版本](https://github.com/xiaoshicae/go-easy-extension) · [完整样例](https://github.com/xiaoshicae/easy-extension-sample)

规划中的 4.0:[架构决策 ADR-0001](doc/adr/0001-v4-architecture.md) · [用户面简化 ADR-0002](doc/adr/0002-simplify-user-facing-api.md) · [API 草图](doc/design/v4-api-sketch.md)

## License

[Apache 2.0](LICENSE)
