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
  <img src="https://img.shields.io/badge/JDK-21+-orange" alt="JDK 21+">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen" alt="Spring Boot">
</p>

<p align="center">
  <a href="#怎么解决">怎么解决</a> · <a href="#核心概念">核心概念</a> · <a href="#快速开始">快速开始</a> · <a href="#管理后台">管理后台</a> · <a href="https://github.com/xiaoshicae/easy-extension/wiki">文档</a>
</p>

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

> 没有 if-else，没有策略工厂。注入扩展点，直接调用，框架自动按业务身份选择正确的实现。

## 核心概念

<img src="/doc/concept.svg" alt="核心概念">

- **扩展点 (Extension Point)** — 底层接口契约，规定"做什么"（如运费计算、订单校验）
- **能力 (Ability)** — 通用的扩展点实现（如包邮、VIP券），可被多个业务复用
- **业务 (Business)** — 接入方（如零售、生鲜），挂载需要的能力，也可直接实现扩展点
- **默认实现 (Default Impl)** — 系统兜底实现，业务和能力均未覆盖时调用，保证扩展点永远可调用

> 运行时解析顺序由业务的 `abilities` 声明决定(数组顺序即优先级,业务自身用 `Self.class` 标记,缺省排最前),最后是各扩展点的默认实现。

## 工作原理

<img src="/doc/how-it-works.svg" alt="运行流程">

一个请求进来后，框架自动完成：**匹配业务 → 激活能力 → 按 `abilities` 顺序排列 → 调用正确的实现**。业务方只需实现自己关心的扩展点，其余自动降级到通用能力或默认实现。

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

### 2. 定义扩展点

```java
@ExtensionPoint
public interface FreightCalcExtension {
    BigDecimal calcFreight(OrderContext ctx);
}
```

### 3. 定义默认实现（兜底）

每个扩展点都有兜底，保证永远可调用。三种写法，按需选：

- 只有 `void` 方法的扩展点（钩子类）：**什么都不用写**，框架提供空实现。
- 单方法扩展点：一个 lambda `@Bean` 即可。
- 其他：写一个 `@DefaultImplementation` 类（可放在接口内部）；一个类可以兜底多个扩展点，但每个扩展点至多一个兜底。有返回值又没有兜底，启动时会报错，不会静默返回 `null`。

```java
@Bean @DefaultImplementation
FreightCalcExtension defaultFreight() { return ctx -> new BigDecimal("10.00"); }
```

不用 Spring 时：`builder.defaultImplementationFor(FreightCalcExtension.class, ctx -> ...)`，只作为这一个扩展点的默认实现。

不用 lambda 时：

```java
@DefaultImplementation
public class DefaultFreight implements FreightCalcExtension {
    @Override
    public BigDecimal calcFreight(OrderContext ctx) {
        return new BigDecimal("10.00");
    }
}
```

### 4. 定义能力（可复用的通用实现）

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

### 5. 定义业务（挂载能力 + 自定义实现）

```java
@Business(code = "biz.retail", abilities = {FreeShippingAbility.class, Self.class})
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

> **优先级说明**: `abilities` 数组的顺序就是优先级,越靠前越优先。`Self.class` 代表业务自身,不写时默认排在最前。上例中包邮能力排在 `Self` 之前,所以它会覆盖 RetailBusiness 自己的运费计算。

### 6. 告诉框架"这个请求是谁"

提供一个 `MatcherParamResolver` Bean,starter 会在每个 Web 请求开始时绑定业务身份、结束时自动解绑(异步请求同样处理):

```java
@Bean
MatcherParamResolver<OrderMatchParam> matcherParamResolver() {
    return request -> OrderMatchParam.from(request);
}
```

### 7. 注入即用

和 [怎么解决？](#怎么解决) 中的代码一样，`@ExtensionInject` 注入扩展点（字段、构造器参数均可），直接调用即可。

扩展点、能力、业务、默认实现会从 Spring Boot 应用包自动扫描，无需配置；需要额外范围时使用 `@ExtensionScan(basePackages = ...)`。

## 调用方式

除了 `@ExtensionInject` 注入代理对象，还可以通过 `ExtensionContext` 编程式调用：

```java
@Autowired
private ExtensionContext<OrderMatchParam> context;

try (Binding b = context.bind(orderParam)) {          // 绑定到当前线程,close 时恢复
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
}
```

不依赖线程绑定时,`context.resolve(param)` 返回不可变的 `Resolution` 快照,线程安全,可以显式传递(包括跨线程、响应式场景)。

## 不使用 Spring

core 只依赖 JDK 与 slf4j:

```java
ExtensionContext<OrderMatchParam> ctx = ExtensionContext.<OrderMatchParam>builder()
        .extensionPoint(FreightCalcExtension.class)
        .defaultImplementation(new DefaultFreight())
        .ability(new FreeShippingAbility())
        .business(new RetailBusiness())
        .build();          // 一次性校验全部装配,失败整体失败;产物不可变
```

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
// 分期需要风控能力同时挂载
@Ability(code = "ability.installment",
    requires = {RiskControlAbility.class})

// 包邮和急速达互斥
@Ability(code = "ability.free-shipping",
    excludes = {RapidDeliveryAbility.class})
```

</td></tr>
<tr><td>

**同一请求多个身份**
```java
// 持有多个 Resolution,按需使用
Resolution main = context.resolve(orderParam);
Resolution afterSale = context.resolve(afterSaleParam);
// 临时切换注入代理看到的身份(可嵌套)
try (Binding b = context.bind(afterSale)) { ... }
```

</td><td>

**解析追踪**
```java
Resolution r = context.resolve(param);
ResolveTrace trace = r.trace();
// 命中业务、各能力匹配状态、解析链、耗时
var why = r.explain(FreightCalcExtension.class);
```

</td></tr>
<tr><td>

**按 code 直达业务**
```java
// 业务无需实现 Matcher, O(1) 命中
@Bean
BusinessResolver<OrderMatchParam> resolver() {
    return p -> Optional.ofNullable(p.getBizCode());
}
```

</td><td>

**运行期元数据**
```java
ExtensionCatalog catalog = context.catalog();
catalog.businesses();   // 含有序的 mounts
```

</td></tr>
</table>

## 配置参考

```yaml
easy-extension:
  allow-unknown-business: false       # 无业务匹配时是否报错(true:只走各扩展点的默认实现)
  enable-session-auto-cleanup: true   # 请求结束兜底清理线程上残留的绑定
  matcher-param-type:                 # 可选:显式指定 Matcher 参数类型
  session-exclude-path-patterns:      # 可选:不绑定业务身份的路径(如 /actuator/**),严格模式下避免这些请求报错
  business-match-order:               # 仅 allow-unknown-business=true 且多个业务同时匹配时,按此 code 顺序选一个
    - biz.retail
    - biz.fresh
  admin:
    enable: true                      # 启用管理后台(缺省启用,见下方安全提示)
    path: /easy-extension-admin       # 访问路径
    auth:
      basic:
        username: admin               # 非空才启用内置 Basic 认证
        password: ${ADMIN_PASSWORD}   # 建议从环境变量注入
    extension-point-order:            # 扩展点展示顺序
      - OrderValidateExtension
      - FreightCalcExtension
```

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

> **安全提示**:管理后台会展示扩展点、能力、业务的**源码**。它缺省启用,且**不配置认证时不做任何校验**。生产环境请配置 `easy-extension.admin.auth.basic`(或注册自己的 `AdminAuthenticationProvider` 接入 Spring Security),或设置 `easy-extension.admin.enable=false` 关闭。 

![管理后台](/doc/admin-extension.png)

## 日志与排查

不再有 `enable-log` 开关,用日志级别控制:

```yaml
logging:
  level:
    io.github.xiaoshicae.extension.core.internal.Resolver: DEBUG   # 每次请求命中的业务、解析链、被跳过的能力、耗时
```

启动时 `Assembler` 会输出一行 INFO 汇总(扩展点、默认实现、能力、业务的数量)。要查"某个请求为什么选了这个实现",用 `resolution.trace()` / `resolution.explain(Point.class)`。

## 适用场景

框架适用于**多接入方 + 复杂定制**的中台系统：

| 场景 | 扩展点举例 | 不同业务的差异 |
|------|----------|-------------|
| **电商交易** | 订单校验、运费计算、促销计算、支付方式 | 零售包邮 vs 生鲜冷链运费 vs 数码分期支付 |
| **履约系统** | 仓库选择、配送方式、签收规则 | 普通快递 vs 冷链配送 vs 同城急送 |
| **营销中台** | 优惠计算、券核销、活动规则 | 新人券 vs 会员折扣 vs 满减活动 |
| **支付系统** | 风控检查、渠道路由、对账规则 | 小额免密 vs 大额人脸识别 vs 企业审批 |

## 升级

从 3.x 升级请阅读 [4.0 迁移指南](doc/migration-4.0.md) 与 [CHANGELOG](CHANGELOG.md);设计说明见 [doc/design-4.0.md](doc/design-4.0.md)。

## 文档

[Wiki](https://github.com/xiaoshicae/easy-extension/wiki) · [Go 版本](https://github.com/xiaoshicae/go-easy-extension) · [完整样例](https://github.com/xiaoshicae/easy-extension-sample)

## License

[Apache 2.0](LICENSE)
