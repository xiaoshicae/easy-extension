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
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x%20%7C%204.x-brightgreen" alt="Spring Boot">
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

> 没有 if-else，没有策略工厂。注入扩展点，直接调用，框架自动按业务身份和优先级选择正确的实现。

## 核心概念

<img src="/doc/concept.svg" alt="核心概念">

- **扩展点**: 系统定义的接口（如运费计算、订单校验），规定"做什么"
- **能力**: 通用的实现（如包邮、VIP优惠），可被多个业务复用
- **业务**: 接入方（如零售、生鲜），挂载需要的能力，也可以自己实现扩展点
- **默认实现**: 所有扩展点的兜底实现，保证扩展点的默认行为

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
    <version>3.3.4</version>
</dependency>
```

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
context.initScopedSession("after-sale",
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

## 配置参考

```yaml
easy-extension:
  enable-log: true                    # 打印匹配过程日志
  allow-unknown-business: false       # 无业务匹配时是否报错
  business-match-order:               # 多业务匹配时的优先级
    - biz.retail
    - biz.fresh
  admin:
    enable: true                      # 启用管理后台
    path: /easy-extension-admin       # 访问路径
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
    <version>3.3.4</version>
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

## License

[Apache 2.0](LICENSE)
