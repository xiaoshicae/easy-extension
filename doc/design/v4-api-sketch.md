# 4.0 API 草图

> **状态:草稿,P0 评审通过后冻结。** 这是接口和语义的草图,不是最终 API:签名可能微调,语义规则(§4)是评审的重点。
> 决策与理由见 [ADR-0001](../adr/0001-v4-architecture.md) 和 [ADR-0002](../adr/0002-simplify-user-facing-api.md)(用户面简化,**提议中**,拟修订前者的 D1、D3)。本文按 ADR-0002 写;它评审不通过时,用户面部分回到 ADR-0001。本文不出现具体版本号,发版时不需要同步。

## 1. 设计目标:用户面最小,复杂留给框架

一个典型用户要写的:**两个类、一行配置、普通注入。**

用户面按"谁需要懂它"分三层,上手只需要第 1 层:

| 层 | 谁用 | 内容 |
|---|---|---|
| 1 | 每个用户 | `@ExtensionPoint`、`@Business`、`@Ability`、`Identity`、`Extensions` |
| 2 | 按需 | `IdentityResolver`(身份不在请求头里)、`ExtensionInterceptor`(指标、追踪)、`@DefaultProvider`(默认实现要注入 Bean) |
| 3 | 运维、测试、高级 | `Session`、`Explanation`、`Description`、`Extensions.Builder`、各异常类 |

框架替用户做掉的事,用户不需要知道:

- 启动时一次性校验,聚合报出**全部**问题:编码重复、能力不存在、`requires` / `excludes` 冲突、扩展点接口不是 `public` ……
- 从容器收集实现(感知 AOP 代理),按**方法**路由,预计算路由表,缓存"身份 → 链"的结果。
- 线程绑定与嵌套还原;Spring 的线程池和 `@Async` 自动沿用请求身份。
- 异常原样透传;路由 Bean 与实现 Bean 的注入歧义;接口 `default` 方法的调用。

## 2. 一张图

```
启动时:  从容器收集 @Business / @Ability / @DefaultProvider ──► 一次性校验 ──► 不可变的 Extensions
请求时:  请求头 X-Biz-Code ──► Identity ──► 链(按 Identity 缓存) ──► 绑定到当前线程
调用时:  freight.calcFreight(ctx) ─ 路由代理 ─► 当前身份的链 ─► 回答的实现(经拦截器)
```

三个阶段:**启动校验 → 请求绑定 → 调用路由**。启动期报全部错误;之后注册表不可变;请求期只做"身份 → 链";调用期只做"链 → 方法"。

## 3. 公开类型

| 层 | 类型 | 角色 | 取代 3.x 的 |
|---|---|---|---|
| 1 | `@ExtensionPoint` | 扩展点接口,没有属性 | 同名(去掉 `mandatory`、`scenarios`、`version`) |
| 1 | `@Business` | 业务:`code`、`abilities`、`first` | `@Business(priority, abilities)` |
| 1 | `@Ability` | 能力:`code`、`requires`、`excludes` | 同名 |
| 1 | `Identity` | 请求"是谁":业务码,可收窄启用的能力 | `@MatcherParam` 对象 |
| 1 | `Extensions` | 注入、`run` / `call`、`all`、`wrap` | `IExtensionContext` 及四对 Manager |
| 2 | `IdentityResolver<Req>` | 请求 → `Identity` | `Matcher` 的扫描逻辑 |
| 2 | `ExtensionInterceptor`、`Invocation` | 环绕拦截 | 同名(语义不变) |
| 2 | `@DefaultProvider` | 默认实现要注入 Bean 时用 | `@ExtensionPointDefaultImplementation` |
| 3 | `Session` | 不可变的已解析身份:显式持有、解释 | `ResolvedChain`、`IExtensionSession`、`ExtensionSessionScope` |
| 3 | `Explanation`、`Description` | 每个方法由谁回答;注册表的 JSON 描述 | `ExtensionExplanation`、`ResolveTrace`、admin 的数据模型 |
| 3 | `ExtensionException` 与 `RegistryException`、`ResolutionException`、`ExtensionNotFoundException` | 全部 unchecked | 13 个异常类 |

可选子包 `core.matching`(默认 resolver):`Matcher<Req>`、`MatcherIdentityResolver<Req>`。其余全部放 `core.internal`。

## 4. 签名草图

包:公开 API 在 `io.github.xiaoshicae.extension.core`,内部在 `...core.internal`(ADR-0001 §9 的开放问题 3)。

下面是签名草图:方法体省略,`static` 方法和类的方法也只写签名,不是可直接编译的代码。

### 4.1 注解

```java
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface ExtensionPoint { }          // 没有属性:scenarios、version 随 admin 一起去掉

@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Business {
    String code();
    /** 挂载的能力。业务自己没实现的方法,按声明顺序由它们回答。 */
    String[] abilities() default {};
    /** 要先于业务自己回答的能力(也算挂载),按声明顺序。例如包邮、促销这类要压过业务默认逻辑的能力。 */
    String[] first() default {};
}

@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Ability {
    String code();
    String[] requires() default {};   // 必须同时挂载在该业务上
    String[] excludes() default {};   // 不得同时挂载
}

/** 默认实现里需要注入 Bean 时用。每个扩展点至多一个。链上没人回答时,它排在接口自己的 default 之前回答。 */
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface DefaultProvider { }
```

### 4.2 身份

```java
public final class Identity {
    public static Identity of(String business);         // 业务,以及它挂载的全部能力
    public static Identity none();                       // 没有业务:只有默认提供者和接口 default 回答
    public Identity only(String... abilities);           // 只启用挂载的能力里的这几个;空 = 只用业务自己
    public Identity without(String... abilities);        // 去掉其中几个
    public String business();                            // none() 时为 null
    public boolean isNone();
}

@FunctionalInterface
public interface IdentityResolver<Req> {
    /** 返回 Identity.none() 表示"没有业务";无法解析时抛 ResolutionException。 */
    Identity resolve(Req request);
}
```

### 4.3 Extensions

```java
public interface Extensions {

    // ---- 建 ----
    static Extensions of(Object... implementations);       // 一行建好,同样一次性校验(纯 Java、测试)
    static Builder builder();                               // 进阶:拦截器、按数据登记

    // ---- 用 ----
    <E> E extension(Class<E> type);                         // 扩展点 E 的路由代理。Spring 里被注入的就是它
    void run(String business, Runnable body);               // 在某个业务身份下执行(MQ、定时任务、测试)
    <R> R call(String business, Supplier<R> body);
    void run(Identity identity, Runnable body);
    <R> R call(Identity identity, Supplier<R> body);
    <E> List<E> all(Class<E> type);                         // 当前身份下链上全部是 E 的对象,按链顺序(替代 invokeAll / invokeReduce)

    // ---- 把"当前身份"带到别的线程(Spring 的线程池和 @Async 已自动处理,自建的 Executor 才需要) ----
    static Runnable wrap(Runnable task);                    // 捕获调用点线程上的身份;当时没有身份就原样返回
    static <R> Supplier<R> wrap(Supplier<R> task);
    static Executor wrap(Executor executor);

    // ---- 进阶 ----
    Session open(Identity identity);                        // 显式持有会话,不绑定线程
    Description describe();
    static <E> E router(Class<E> type);                     // 容器集成用:与注册表无关的路由代理,starter 把它注册成 @Primary Bean

    interface Builder {
        Builder add(Object... implementations);             // 实例;类上的 @Business / @Ability / @DefaultProvider 决定它是什么
        Builder add(Class<?> declaredAs, Object implementation);   // 实例是容器代理时:declaredAs 是带注解的用户类

        /** 不靠注解、用数据登记(例如租户配置存在库里,一个类对应很多个业务)。上面的 add 读完注解后调用的就是它们。 */
        Builder business(String code, List<String> first, List<String> abilities, Object implementation);
        Builder ability(String code, List<String> requires, List<String> excludes, Object implementation);
        Builder provider(Object implementation);

        Builder extensionPoint(Class<?>... types);          // 声明没有实现者的扩展点,让它出现在 describe() 里
        Builder interceptor(ExtensionInterceptor interceptor);     // 先注册的在外层
        Extensions build();                                 // RegistryException:聚合全部问题;产物不可变
    }
}
```

### 4.4 会话(第 3 层)

```java
public interface Session {
    static Optional<Session> current();                     // 当前线程绑定的会话

    Identity identity();
    List<ChainEntry> chain();                               // 回答顺序:先于业务的能力 / 业务 / 其余能力 / 默认提供者
    <E> E extension(Class<E> type);                         // 只按本会话回答的 E,与线程无关(响应式、测试)
    <E> List<E> all(Class<E> type);
    Explanation explain(Class<?> type);

    Binding bind();                                         // try-with-resources;关闭时恢复进入前的绑定
    <R> R call(Supplier<R> body);
    void run(Runnable body);
    Runnable wrap(Runnable task);                           // 任务在这个会话下执行,不论跑在哪个线程
    <R> Supplier<R> wrap(Supplier<R> task);
    Executor wrap(Executor executor);

    interface Binding extends AutoCloseable { @Override void close(); }
    record ChainEntry(String code, Kind kind, Class<?> implementation) { }
    enum Kind { BUSINESS, ABILITY, PROVIDER, INTERFACE_DEFAULT }   // INTERFACE_DEFAULT 只出现在 Explanation 里
}
```

### 4.5 拦截器、解释、描述、异常

```java
@FunctionalInterface
public interface ExtensionInterceptor {
    Object intercept(Invocation invocation) throws Throwable;
}

public interface Invocation {
    Class<?> extensionPoint();
    Method method();
    Object[] arguments();                        // 可改
    String implementationCode();                 // 业务码 / 能力码;提供者用类名;接口自己的 default 用 "$default"
    Object proceed() throws Throwable;
}

public record Explanation(Class<?> extensionPoint, Identity identity, List<MethodRoute> methods) {
    public record MethodRoute(Method method, List<Candidate> candidates, Candidate selected) { }
    public record Candidate(String code, Session.Kind kind, boolean implementsMethod) { }
}

public record Description(String fingerprint,
                          List<ExtensionPointInfo> extensionPoints,
                          List<BusinessInfo> businesses,
                          List<AbilityInfo> abilities,
                          List<ProviderInfo> providers,
                          List<String> warnings) {
    public record ExtensionPointInfo(Class<?> type, List<MethodInfo> methods) { }
    public record MethodInfo(String signature, boolean required) { }      // required = 没有 default 的抽象方法
    public record BusinessInfo(String code, Class<?> implementation, List<String> first, List<String> abilities, List<Class<?>> extensionPoints) { }
    public record AbilityInfo(String code, Class<?> implementation, List<String> requires, List<String> excludes, List<Class<?>> extensionPoints) { }
    public record ProviderInfo(Class<?> implementation, List<Class<?>> extensionPoints) { }
}

public abstract class ExtensionException extends RuntimeException { }
public final class RegistryException extends ExtensionException { public List<String> problems(); }   // 启动:全部问题
public final class ResolutionException extends ExtensionException { }           // 业务码未知 / 能力未挂载 / 没有绑定身份
public final class ExtensionNotFoundException extends ExtensionException { }    // 调用:没有实现者的抽象方法
```

### 4.6 可选:`core.matching`(保留"每个业务自带 match()"的风格)

```java
public interface Matcher<Req> { boolean match(Req request); }      // 业务和能力可选实现

public final class MatcherIdentityResolver<Req> implements IdentityResolver<Req> {
    /** 传入带 @Business / @Ability 的对象。业务按 match() 选出;能力按 match() 决定是否启用(不实现 Matcher 的视为启用),
     *  结果映射成 Identity.only(...),只在该业务挂载的能力里取交集(没有挂载的能力即使 match() 为 true 也忽略)。 */
    public static <Req> MatcherIdentityResolver<Req> of(Object... businessesAndAbilities);
    public MatcherIdentityResolver<Req> onNoMatch(NoMatch how);            // NONE(默认层兜底)| REJECT
    public MatcherIdentityResolver<Req> onMultipleMatches(MultiMatch how); // REJECT | FIRST | ORDERED
    public MatcherIdentityResolver<Req> order(String... businessCodes);    // ORDERED 时的优先级
    public enum NoMatch { NONE, REJECT }
    public enum MultiMatch { REJECT, FIRST, ORDERED }
}
```

## 5. 语义规则(评审重点)

### 5.1 链的构造

输入 `Identity`,业务记为 `B`;`B` 挂载的能力 = `first` ∪ `abilities`:

1. `Identity.none()`:链为空,只有默认提供者和接口 `default` 回答。
2. `B` 不存在 → `ResolutionException`:`business [B] not found`。
3. 启用哪些能力:默认是挂载的**全部**;`only(...)` 只启用所列的;`without(...)` 去掉所列的。所列的能力 `B` 没有挂载 → `ResolutionException`:`business [B] does not mount ability [a]`。
4. 链 = 启用的 `first`(按 `first` 的声明顺序)→ **业务自己** → 启用的 `abilities`(按 `abilities` 的声明顺序)。
5. 链的后面依次是各 `@DefaultProvider`,最后是接口自己的 `default`。

一句话:**先于业务的能力 → 业务自己 → 其余能力 → 默认。** 同一个 `Identity` 得到同一条链;`Extensions` 按 `Identity` 缓存(容量与淘汰见 ADR-0001 §9)。

### 5.2 路由(每次调用)

对扩展点 `E` 的方法 `m`:

1. 依次看链上每个对象 `x`。若 `x` **自己实现了** `m`,调用它,结束。"自己实现"指 `x` 的类(或其父类)声明了 `m`;若 `m` 在 `E` 里是 `default` 而 `x` 没有覆盖,**不算**,继续往下找。比 `E` 更具体的子接口覆盖了该 `default`,算 `x` 的实现。
2. 没有 → 看 `E` 的 `@DefaultProvider`,规则同上。
3. 没有 → 若 `m` 是 `default`,用 `InvocationHandler.invokeDefault` 调用接口自己的默认体。`this` 是路由代理,默认体里再调用 `E` 的其他方法会**重新路由**。
4. 否则 → `ExtensionNotFoundException`,消息列出整条链。
5. 实现类抛出的异常原样抛出,不包装。拦截器包住"选定实现之后"的那一次调用。

这就是 [ADR-0001 §4 差异 1](../adr/0001-v4-architecture.md#4-与-3x-的已知行为差异有意的):路由按方法而不是按扩展点接口。方法全是抽象方法的扩展点,与 3.x 完全一致。

### 5.3 绑定与传递

- 一个线程同一时刻最多绑定一个当前身份。`run` / `call` / `bind()` 结束时**恢复进入前的绑定**(没有则清除),所以可以嵌套,也可以用在"任务内联执行"的线程上(直接执行器、`CallerRunsPolicy`、并行流)。
- Spring 的线程池和 `@Async`:starter 提供一个 `TaskDecorator` Bean,自动沿用提交时的身份。Boot 4 会把多个 `TaskDecorator` Bean 组合起来;Boot 3.5 只在 Bean 唯一时才采用(多于一个则**都不生效**),所以应用已有自己的 `TaskDecorator` 时,starter 在 3.5 上于启动期给出提示并说明怎么组合。自建的 `Executor`(`new Thread`、自己 `new` 的线程池、`CompletableFuture` 的默认池)用 `Extensions.wrap(...)`。不使用 `InheritableThreadLocal`。
- 调用扩展点时线程上没有身份 → `ResolutionException`,消息说明怎么办:`no identity is bound to this thread: send the business header, or call extensions.run(...)`。不会悄悄退回到"只有默认实现"。
- 虚拟线程下行为与平台线程一致。

### 5.4 注册表不可变

`Extensions` 一旦建好就不可变,会话自己持有链和路由表,路由代理与注册表无关。4.0 **不提供**运行时增删实现或热替换;因为上述性质,以后加这个能力不需要改 API。

### 5.5 构建期校验

`build()` / 启动时一次性报告全部问题(`RegistryException#problems()`,消息逐行列出):

- 同一个类上标了不止一个 `@Business` / `@Ability` / `@DefaultProvider`。
- 业务码重复;能力码重复(两个命名空间各自检查)。
- `first` / `abilities` 里的能力码不存在、重复,或同一个能力同时出现在两个列表里。
- `requires` 的能力没有同时挂载;`excludes` 的能力同时挂载了。
- 能力或提供者没有实现任何 `@ExtensionPoint`。
- 同一个扩展点有两个 `@DefaultProvider`。
- 实现类**直接声明**的 `@ExtensionPoint` 接口不是 `public`(继承来的非 public 接口被忽略)。
- `add(declaredAs, instance)` 里实例不是 `declaredAs` 的实例。

用 `business(...)` / `ability(...)` / `provider(...)` 登记的,与注解方式走同一套校验。不阻止构建的提示(`describe().warnings()`):某个扩展点的抽象方法没有任何实现者。

### 5.6 Web 绑定(starter)

- 配置 `easy-extension.web.business-header`(可选 `easy-extension.web.abilities-header`,逗号分隔,对应 `only(...)`)后,starter 注册一个 filter。
- 请求头**存在**:解析成 `Identity`,绑定到本请求的线程,请求结束还原。业务码未知、能力未挂载 → HTTP 400,消息是 `ResolutionException` 的。
- 请求头**不存在**:不绑定。不调用扩展点的接口(健康检查、静态资源)不受影响;调用了就得到 5.3 的 `ResolutionException`。
- 有 `IdentityResolver<HttpServletRequest>` Bean 时用它代替属性,规则相同:返回 `Identity.none()` 表示只用默认层;抛 `ResolutionException` → 400。
- 非 HTTP 入口(MQ 消费者、定时任务):显式 `extensions.run("biz.retail", () -> ...)`。

## 6. 五个典型场景

### 场景 1:60 秒上手(Spring Boot,零代码身份)

```java
@ExtensionPoint
public interface FreightCalcExtension {
    default BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("10.00"); }   // default = 系统兜底
}

@Business(code = "biz.retail")
public class RetailBusiness implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("8.00"); }
}

@RestController
class OrderController {
    private final FreightCalcExtension freight;                // 普通注入:路由 Bean 按当前请求的身份回答
    OrderController(FreightCalcExtension freight) { this.freight = freight; }

    @PostMapping("/checkout")
    String checkout(@RequestBody OrderContext ctx) { return "运费: ¥" + freight.calcFreight(ctx); }
}
```

```yaml
easy-extension.web.business-header: X-Biz-Code
```

| 请求 | 回答者 | 结果 |
|---|---|---|
| `X-Biz-Code: biz.retail` | 业务自己 | 8.00 |
| `X-Biz-Code: biz.other`(没有实现该扩展点的业务) | 接口 `default` | 10.00 |
| `X-Biz-Code: biz.unknown`(没有这个业务) | —— | HTTP 400,`business [biz.unknown] not found` |
| 没有请求头,且调用了扩展点 | —— | `ResolutionException`:`no identity is bound to this thread ...` |

### 场景 2:复用逻辑:能力

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}

@Business(code = "biz.fresh", abilities = {"ability.free-shipping"})    // 业务自己没实现的方法,由挂载的能力回答
public class FreshBusiness implements ColdChainExtension { ... }       // 没有实现 calcFreight,于是包邮能力回答

@Business(code = "biz.retail", first = {"ability.free-shipping"})       // 包邮能力先于业务自己回答
public class RetailBusiness implements FreightCalcExtension { ... }
```

请求只启用一部分能力:`Identity.of("biz.retail").only("ability.free-shipping")`;去掉一个:`Identity.of("biz.retail").without("ability.coupon")`。默认是挂载的全部。

### 场景 3:非 Web 入口与异步

```java
// MQ 消费者、定时任务:显式带上业务身份
extensions.run("biz.retail", () -> handle(message));

// Spring 的线程池和 @Async:什么都不用写,自动沿用提交时的身份
// 自己建的 Executor:包一层
Executor bound = Extensions.wrap(executor);
CompletableFuture.runAsync(() -> audit.record(order), bound);

// 取全部实现并聚合(替代 invokeAll / invokeReduce)
BigDecimal discount = extensions.all(PromotionCalcExtension.class).stream()
        .map(e -> e.calcPromotion(ctx)).reduce(BigDecimal.ZERO, BigDecimal::add);
```

### 场景 4:必选的扩展点,以及要注入 Bean 的默认

```java
@ExtensionPoint
public interface InvoiceExtension {
    Invoice issue(OrderContext ctx);               // 没有 default = 必选:链上没人实现就失败,不需要占位类
}

@ExtensionPoint
public interface TaxExtension {
    BigDecimal tax(OrderContext ctx);              // 兜底要用税率服务,接口里拿不到 Bean
}

@DefaultProvider
class DefaultTax implements TaxExtension {         // 链上没人实现 tax() 时由它回答;starter 注册成 Bean,构造器注入照常
    private final TaxRateService rates;
    DefaultTax(TaxRateService rates) { this.rates = rates; }
    @Override public BigDecimal tax(OrderContext ctx) { return rates.standardRate().apply(ctx.amount()); }
}
```

零售业务没有实现 `InvoiceExtension`,调用 `issue` 时:

```
ExtensionNotFoundException: Extension<InvoiceExtension#issue(OrderContext)> not found:
  abstract method, none of [biz.retail] implements it
```

### 场景 5:测试

```java
// 纯 Java:不起 Spring
Extensions extensions = Extensions.of(new RetailBusiness(), new FreeShippingAbility());
FreightCalcExtension freight = extensions.extension(FreightCalcExtension.class);

@Test void retailPaysEight() {
    assertEquals(new BigDecimal("8.00"), extensions.call("biz.retail", () -> freight.calcFreight(ctx)));
}

// Spring 测试:直接指定身份,不需要构造请求
@SpringBootTest
@WithIdentity("biz.retail")
class CheckoutTest { ... }
```

`Extensions.of(...)` 与启动时走同一套校验,所以测试里同样会得到聚合的启动期错误。

### 进阶(第 2、3 层)

```java
// 指标、追踪:拦截器,用法和 3.x 一样
@Bean ExtensionInterceptor metrics(MeterRegistry registry) { ... }

// 请求身份不在请求头里:一个 resolver Bean
@Bean IdentityResolver<HttpServletRequest> resolver() { return req -> Identity.of(tenantOf(req)); }

// 解释"这个身份下,每个方法由谁回答"
Explanation e = extensions.open(Identity.of("biz.retail")).explain(FreightCalcExtension.class);
```

```
GET /actuator/extensions                  → Description:全部扩展点、业务、能力、提供者、提示
GET /actuator/extensions/explain?business=biz.retail&type=com.acme.FreightCalcExtension
                                          → Explanation:每个方法的候选者和被选中者
```

## 7. 周边草图

**Spring starter。**
- **一个**注册器(由自动配置导入):`ClassPathBeanDefinitionScanner` 加 include filter,把带 `@Business` / `@Ability` / `@DefaultProvider` 的类注册成普通 Bean(类上不必再加 `@Component`,加了也无妨);把 `@ExtensionPoint` 接口注册成 `@Primary` 的路由 Bean(`Extensions.router(type)`)。
- 扫描范围默认是 Boot 的自动配置包,即 `@SpringBootApplication` 所在的包树(与 Spring Data 的约定一致);包树之外的实现才需要 `@ExtensionScan(scanPackages = ...)` 追加。
- `Extensions` Bean 是一个**门面**:全部单例实例化之后(`SmartInitializingSingleton`),才收集(`getBeansWithAnnotation`,AOP 代理感知;`add(目标类, Bean)` 读注解、调用 Bean)、构建并冻结真正的注册表,再接到门面上。业务 Bean 直接注入 `Extensions` 即可,没有循环依赖;刷新完成之前调用(例如 `@PostConstruct` 里)得到清晰的错误。校验失败(`RegistryException`)发生在 Web 服务器开始接收请求之前。
- Web filter、`TaskDecorator`、属性见 §5.3、§5.6。没有 resolver、但业务实现了 `Matcher<Req>` 时,自动用 `MatcherIdentityResolver`,选项来自属性:

```yaml
easy-extension:
  matching:
    no-match: none          # none | reject
    multi-match: reject     # reject | first | ordered
    order: [biz.retail, biz.fresh]
```

**test-kit(`easy-extension-test`)。** 只有 `@WithIdentity`。`Explanation` 的断言放在进阶里,按需再加。

**Actuator。** 只读的 `extensions` endpoint,序列化 `Description` 与 `Explanation`。UI 另起仓库消费它。

**注解处理器(编译期)。** 报错:`@ExtensionPoint` 不是 `public` 接口;同一模块内业务码或能力码重复;同一个类上标了多个角色注解;`first` 与 `abilities` 里有同一个能力。提示:本模块内引用了找不到的能力码(跨模块的能力只能在启动时校验)。

## 8. 从 3.x 迁移

| 3.x | 4.0 | 自动化 |
|---|---|---|
| `@ExtensionInject X x` | 普通注入 `X x`(路由 Bean 为 `@Primary`) | recipe |
| `IExtensionContext<P>`、`initSession(param)` / `removeSession()` | Web:一行配置或一个 `IdentityResolver`;其他入口:`extensions.run(...)` | 人工 + 部分 recipe |
| `context.invoke(E.class, e -> ...)` | 注入的 `E`,直接调用 | recipe |
| `invokeAll` / `invokeReduce` | `extensions.all(E.class)` 加 stream | 人工 |
| `@MatcherParam`、`Matcher<P>` | `Matcher<P>` 保留在 `core.matching`;`@MatcherParam` 去掉(类型从 `Matcher<P>` 的泛型参数读) | recipe |
| `@Business(priority, abilities = {"a", "b::10"})` | 优先级小于业务自身的能力 → `first`,其余 → `abilities`,各自按数字升序 | recipe(仅注解方式) |
| 手写 `IBusiness` / `IAbility`(含数据驱动的业务) | 不再有这两个接口:改成带注解的类,或 `Extensions.builder().business(...)` / `ability(...)` | 人工 |
| `@ExtensionPointDefaultImplementation class D implements E1, E2` | 把默认体写进接口的 `default` 方法;需要注入的用 `@DefaultProvider` | 人工 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 全部删掉;接口方法不带 `default` 即为必选 | recipe |
| `allow-unknown-business` 与两个策略属性 | `easy-extension.matching.*`(Matcher 风格),或 resolver 自己决定 | 人工 |
| `IScopedSessionManager`、命名 scope | 一个请求里的多个身份 = 多个 `Session` 对象 | 人工 |
| `ExtensionSessionScope.run / runWith` | `extensions.run` / `Extensions.wrap` | recipe |
| `SessionException`、`InvokeException` 等 13 个异常 | `ResolutionException`、`ExtensionNotFoundException`、`RegistryException` | recipe(`ChangeType`) |
| 内嵌 admin | Actuator endpoint + 独立的 UI 仓库 | 人工 |
| `-Deasy-extension.legacy-exception-wrapping` | 删除(代理层不再存在) | —— |

迁移时要特别检查三处语义:**带 `default` 方法的扩展点**(路由按方法,见 §5.2)、**能力与业务自身的先后**(3.x 默认业务在前,4.0 也是;原来用数字让能力在前的,要进 `first`)、**能力的启用**(4.0 默认挂载的全部启用;原来靠 `match()` 或请求参数条件启用的,要用 `only` / `without` 或 `MatcherIdentityResolver`)。

## 9. 这份草图还没回答的问题

ADR-0001 §9 的六项开放问题之外:

1. `first` 的命名。备选:`precedence`、`overriding`。现在用 `first` 是因为它最短,但可能被读成"abilities 里的第一个"。
2. 请求头缺失、或业务码未知时:现在分别是"不绑定"和 HTTP 400;是否需要一个开关改成绑定 `Identity.none()`(只用默认层)。3.x 的 `allow-unknown-business` 就是这个意思;现在要这样只能写 `IdentityResolver`,或用 Matcher 风格的 `no-match: none`。
3. `Extensions.all` 是否排除"只继承了 `default` 体、一个方法也没覆盖"的对象。
4. 提供者在 `Explanation` 和 `Invocation` 里的标识:现在用类名,是否需要一个显式的 `code`。
5. `Identity.of(null)` 的校验用 `ResolutionException`(与"身份非法"一致)还是 `IllegalArgumentException`。
