# 4.0 API 草图

> **状态:草稿,P0 评审通过后冻结。** 这是接口和语义的草图,不是最终 API:签名可能微调,语义规则(§4)是评审的重点。
> 决策与理由见 [ADR-0001](../adr/0001-v4-architecture.md)。本文不出现具体版本号,发版时不需要同步。

## 1. 一张图

```
启动时:  Extensions.builder().add(业务 / 能力 / 默认提供者).build()  ── 聚合校验 ──►  不可变的 Extensions

请求时:  请求 ─ IdentityResolver<Req> ─► Identity ─ extensions.open() ─► Session(链,按 Identity 缓存)
         session.run(() -> service.handle())
           └► 扩展点调用 ─ 路由代理 ─► 当前会话的链 ─► 回答的实现(经拦截器)
```

四个阶段:**构建 → 冻结 → 解析 → 调用**。构建期报全部错误;冻结之后注册表不可变;解析只做"身份 → 链";调用只做"链 → 方法"。

## 2. 公开类型(约 15 个)

| 类型 | 角色 | 取代 3.x 的 |
|---|---|---|
| `@ExtensionPoint` | 扩展点接口(去掉 `mandatory`) | 同名 |
| `@Business` | 业务;`chain` 声明响应顺序 | `@Business(priority, abilities)` |
| `@Ability` | 能力;`requires` / `excludes` | 同名 |
| `@DefaultProvider` | 可选:需要注入依赖的默认实现 | `@ExtensionPointDefaultImplementation` |
| `Identity` | 显式身份:业务码 + 启用的能力码 | `@MatcherParam` 对象 |
| `IdentityResolver<Req>` | 请求 → `Identity` 的 SPI | `Matcher` 的扫描逻辑 |
| `Extensions`(含 `Builder`) | 冻结的注册表 + 开会话 | `IExtensionContext` 及四对 Manager |
| `Session` | 不可变的已解析身份;绑定、传递、解释 | `ResolvedChain`、`IExtensionSession`、`ExtensionSessionScope` |
| `ExtensionInterceptor`、`Invocation` | 环绕拦截 | 同名(语义不变) |
| `Explanation` | 每个方法由谁回答、为什么 | `ExtensionExplanation`、`ResolveTrace` |
| `Description` | 注册表的不可变描述(JSON 友好) | admin 的数据模型 |
| `ExtensionException` 及 `RegistryException`、`ResolutionException`、`ExtensionNotFoundException` | 全部 unchecked | 13 个异常类 |

可选子包 `core.matching`(默认 resolver):`Matcher<Req>`、`MatcherIdentityResolver<Req>`。其余全部放 `core.internal`。

## 3. 签名草图

包:公开 API 在 `io.github.xiaoshicae.extension.core`,内部在 `...core.internal`(ADR §9 的开放问题 3)。

下面是签名草图:方法体省略,`static` 方法和类的方法也只写签名,不是可直接编译的代码。

### 3.1 注解

```java
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface ExtensionPoint {
    String[] scenarios() default {};   // 提示性元数据,不校验
    int version() default 1;           // 提示性元数据,不校验
}

@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Business {
    String SELF = "$self";
    String code();
    /** 响应顺序,越靠前越先回答。SELF 代表业务自身,必须恰好出现一次。 */
    String[] chain() default {SELF};
}

@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Ability {
    String code();
    String[] requires() default {};   // 必须同在该业务的 chain 里
    String[] excludes() default {};   // 不得出现在该业务的 chain 里
}

/** 默认实现里需要注入 Bean 时用。每个扩展点至多一个。链上没人回答时,它排在接口自己的 default 之前回答。 */
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface DefaultProvider { }
```

### 3.2 身份

```java
public final class Identity {
    public static Identity of(String business, String... abilities);
    public static Identity none();              // 没有任何业务:只有默认提供者和接口 default 回答
    public String business();                   // none() 时为 null
    public Set<String> abilities();             // 不可变;顺序无关,链的顺序由业务的 chain 决定
    public boolean isNone();
}

@FunctionalInterface
public interface IdentityResolver<Req> {
    /** 返回 Identity.none() 表示"没有业务";无法解析时抛 ResolutionException。 */
    Identity resolve(Req request);
}
```

### 3.3 注册表与会话入口

```java
public interface Extensions {

    static Builder builder();

    /** 解析出会话。不绑定线程;同一个 Identity 的结果被缓存。 */
    Session open(Identity identity);             // ResolutionException

    /** 注册表的不可变描述,给 Actuator、测试、文档用。 */
    Description describe();

    interface Builder {
        /** 实现类的实例。类上的 @Business / @Ability / @DefaultProvider 决定它是什么。 */
        Builder add(Object... implementations);

        /** 实例是容器代理时:declaredAs 是带注解的用户类。调用走实例,注解和方法从 declaredAs 读。 */
        Builder add(Class<?> declaredAs, Object implementation);

        /**
         * 不靠注解、用数据登记。上面两个方法读完注解后调用的就是它们。
         * 用于数据驱动的业务(例如租户配置存在库里,一个类对应很多个业务)。
         */
        Builder business(String code, List<String> chain, Object implementation);
        Builder ability(String code, List<String> requires, List<String> excludes, Object implementation);
        Builder provider(Object implementation);

        /** 声明没有实现者的扩展点,让它出现在 describe() 里。 */
        Builder extensionPoint(Class<?>... types);

        /** 先注册的在外层。 */
        Builder interceptor(ExtensionInterceptor interceptor);

        /** 一次性校验,聚合全部问题;产物不可变。 */
        Extensions build();                      // RegistryException
    }
}
```

### 3.4 会话

```java
public interface Session {

    // ---- 当前线程 ----
    static Optional<Session> current();
    static Session require();                    // 没有绑定就抛 ResolutionException,消息说明怎么打开会话
    /** 扩展点 E 的路由代理:每次调用都按"当前线程绑定的会话"路由。与注册表的代际无关。starter 把它注册成 @Primary Bean。 */
    static <E> E router(Class<E> type);

    // ---- 这个会话 ----
    Identity identity();
    List<ChainEntry> chain();                    // 回答顺序:业务 / 能力 / 默认提供者
    <E> E extension(Class<E> type);              // 只按本会话回答的 E,与线程无关(响应式、测试)
    <E> List<E> all(Class<E> type);              // 链上全部是 E 的对象,按链顺序(替代 invokeAll / invokeReduce)
    Explanation explain(Class<?> type);

    // ---- 绑定到线程 ----
    Binding bind();                              // try-with-resources;关闭时恢复进入前的绑定
    <R> R call(Supplier<R> body);
    void run(Runnable body);
    Runnable wrap(Runnable task);                // 任务在这个会话下执行,不论跑在哪个线程
    <R> Supplier<R> wrap(Supplier<R> task);
    Executor wrap(Executor executor);

    interface Binding extends AutoCloseable { @Override void close(); }

    record ChainEntry(String code, Kind kind, Class<?> implementation) { }
    enum Kind { BUSINESS, ABILITY, PROVIDER, INTERFACE_DEFAULT }   // INTERFACE_DEFAULT 只出现在 Explanation 里
}
```

### 3.5 拦截器、解释、描述、异常

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
    public record ExtensionPointInfo(Class<?> type, int version, List<String> scenarios, List<MethodInfo> methods) { }
    public record MethodInfo(String signature, boolean required) { }      // required = 没有 default 的抽象方法
    public record BusinessInfo(String code, Class<?> implementation, List<String> chain, List<Class<?>> extensionPoints) { }
    public record AbilityInfo(String code, Class<?> implementation, List<String> requires, List<String> excludes, List<Class<?>> extensionPoints) { }
    public record ProviderInfo(Class<?> implementation, List<Class<?>> extensionPoints) { }
}

public abstract class ExtensionException extends RuntimeException { }
public final class RegistryException extends ExtensionException { public List<String> problems(); }   // build():全部问题
public final class ResolutionException extends ExtensionException { }           // open() / 没有绑定会话 / Identity 非法
public final class ExtensionNotFoundException extends ExtensionException { }    // 调用:没有实现者的抽象方法
```

### 3.6 可选:`core.matching`(保留"每个业务自带 match()"的风格)

```java
public interface Matcher<Req> { boolean match(Req request); }      // 业务和能力可选实现

public final class MatcherIdentityResolver<Req> implements IdentityResolver<Req> {
    /** 传入带 @Business / @Ability 的对象,其中实现了 Matcher 的参与判定。 */
    public static <Req> MatcherIdentityResolver<Req> of(Object... businessesAndAbilities);
    public MatcherIdentityResolver<Req> onNoMatch(NoMatch how);            // NONE(默认实现兜底)| REJECT
    public MatcherIdentityResolver<Req> onMultipleMatches(MultiMatch how); // REJECT | FIRST | ORDERED
    public MatcherIdentityResolver<Req> order(String... businessCodes);    // ORDERED 时的优先级
    public enum NoMatch { NONE, REJECT }
    public enum MultiMatch { REJECT, FIRST, ORDERED }
}
```

3.x 的"无命中 / 多命中"两个策略枚举加旧开关,收敛成这个 resolver 自己的两个选项。

## 4. 语义规则(评审重点)

### 4.1 链的构造

输入 `Identity(B, A)`:

1. `B` 为 null(`Identity.none()`):链为空,只有默认提供者和接口 `default` 回答。
2. `B` 不存在 → `ResolutionException`:`business [B] not found`。
3. `A` 里有 `B` 没有挂载的能力(没出现在 `B` 的 `chain` 里)→ `ResolutionException`:`business [B] does not mount ability [a]`。
4. 链 = `B` 的 `chain` 声明顺序,去掉未启用的能力;`SELF` 展开成业务自身。
5. 链的后面依次是各 `@DefaultProvider`,最后是接口自己的 `default`。

同一个 `Identity` 得到同一条链;`Extensions` 按 `Identity` 缓存(容量与淘汰见 ADR §9)。

### 4.2 路由(每次调用)

对扩展点 `E` 的方法 `m`:

1. 依次看链上每个对象 `x`。若 `x` **自己实现了** `m`,调用它,结束。"自己实现"指 `x` 的类(或其父类)声明了 `m`;若 `m` 在 `E` 里是 `default` 而 `x` 没有覆盖,**不算**,继续往下找。比 `E` 更具体的子接口覆盖了该 `default`,算 `x` 的实现。
2. 没有 → 看 `E` 的 `@DefaultProvider`,规则同上。
3. 没有 → 若 `m` 是 `default`,用 `InvocationHandler.invokeDefault` 调用接口自己的默认体。`this` 是路由代理,默认体里再调用 `E` 的其他方法会**重新路由**。
4. 否则 → `ExtensionNotFoundException`,消息列出整条链。
5. 实现类抛出的异常原样抛出,不包装。拦截器包住"选定实现之后"的那一次调用。

这就是 [ADR §4 差异 1](../adr/0001-v4-architecture.md#4-与-3x-的已知行为差异有意的):路由按方法而不是按扩展点接口。方法全是抽象方法的扩展点,与 3.x 完全一致。

### 4.3 绑定与传递

- 一个线程同一时刻最多绑定一个当前会话。`bind()` / `run` / `call` 结束时**恢复进入前的绑定**(没有则清除),所以可以嵌套,也可以用在"任务内联执行"的线程上(直接执行器、`CallerRunsPolicy`、并行流)。
- 不使用 `InheritableThreadLocal`;跨线程一律 `wrap`。
- `Session.router(E)` 在没有绑定会话时抛 `ResolutionException`,不会悄悄退回到"只有默认实现"。
- 虚拟线程下行为与平台线程一致。

### 4.4 热替换

`Extensions` 不可变。更新 = `build()` 一个新的整体,由应用(starter 里的 `ReloadableExtensions`)替换引用。已打开的会话是自洽的(自己持有链与路由表),继续按旧注册表回答;之后新开的会话用新注册表。路由 Bean 与注册表的代际无关,不需要重新注入。**不支持**运行时增删单个实现,也不做类加载隔离。

### 4.5 构建期校验

`build()` 一次性报告全部问题(`RegistryException#problems()`,消息逐行列出):

- 同一个类上标了不止一个 `@Business` / `@Ability` / `@DefaultProvider`。
- 业务码重复;能力码重复(两个命名空间各自检查)。
- `chain` 里 `SELF` 不是恰好一个;`chain` 里的能力码重复或不存在。
- `requires` 的能力不在该业务的 `chain` 里;`excludes` 的能力在 `chain` 里。
- 能力或提供者没有实现任何 `@ExtensionPoint`。
- 同一个扩展点有两个 `@DefaultProvider`。
- 实现类**直接声明**的 `@ExtensionPoint` 接口不是 `public`(继承来的非 public 接口被忽略)。
- `add(declaredAs, instance)` 里实例不是 `declaredAs` 的实例。

用 `business(...)` / `ability(...)` / `provider(...)` 登记的,与注解方式走同一套校验。

不阻止构建的提示(`describe().warnings()`):某个扩展点的抽象方法没有任何实现者。

## 5. 五个典型场景

### 场景 1:Spring Boot,接入一个新业务

```java
// 扩展点:运费有合理默认值,用 default 方法表达兜底
@ExtensionPoint
public interface FreightCalcExtension {
    default BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("10.00"); }
}

// 能力:可复用的通用实现
@Ability(code = "ability.free-shipping", excludes = {"ability.rapid-delivery"})
public class FreeShippingAbility implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}

// 业务:chain 的顺序就是响应顺序,包邮能力先于业务自己
@Business(code = "biz.retail", chain = {"ability.free-shipping", Business.SELF})
public class RetailBusiness implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("8.00"); }
}

// 请求 → 身份:一个 resolver Bean,starter 在 Web 层按它打开并绑定会话
@Component
class OrderIdentityResolver implements IdentityResolver<HttpServletRequest> {
    @Override public Identity resolve(HttpServletRequest req) {
        String biz = req.getHeader("X-Biz-Code");
        if (biz == null) return Identity.none();                       // 没有业务:只有默认层回答
        return Identity.of(biz, abilitiesOf(req));
    }
}

// 使用:普通注入。路由 Bean 是 @Primary,按当前请求的身份回答
@RestController
class OrderController {
    private final FreightCalcExtension freight;
    OrderController(FreightCalcExtension freight) { this.freight = freight; }

    @PostMapping("/checkout")
    String checkout(@RequestBody OrderContext ctx) { return "运费: ¥" + freight.calcFreight(ctx); }
}
```

| 身份 | 回答者 | 结果 |
|---|---|---|
| `Identity.of("biz.retail", "ability.free-shipping")` | 包邮能力 | 0 |
| `Identity.of("biz.retail")` | 业务自己 | 8.00 |
| `Identity.none()` | 接口 `default` | 10.00 |
| `Identity.of("biz.unknown")` | —— | `ResolutionException`,Web 层默认映射为 400,可替换 |

### 场景 2:默认实现、必选、需要注入的默认

```java
@ExtensionPoint
public interface InvoiceExtension {
    Invoice issue(OrderContext ctx);               // 抽象方法 = 必选:链上没人实现就失败,不需要占位类
}

@ExtensionPoint
public interface RiskControlExtension {
    default RiskResult check(OrderContext ctx) { return RiskResult.PASS; }   // 简单兜底:接口 default
}

@ExtensionPoint
public interface TaxExtension {
    BigDecimal tax(OrderContext ctx);              // 兜底要用税率服务,接口里拿不到 Bean
}

@DefaultProvider @Component
class DefaultTax implements TaxExtension {         // 链上没人实现 tax() 时由它回答
    private final TaxRateService rates;
    DefaultTax(TaxRateService rates) { this.rates = rates; }
    @Override public BigDecimal tax(OrderContext ctx) { return rates.standardRate().apply(ctx.amount()); }
}
```

零售业务没有实现 `InvoiceExtension`,调用 `issue` 时:

```
ExtensionNotFoundException: Extension<InvoiceExtension#issue(OrderContext)> not found:
  abstract method, none of [biz.retail > ability.free-shipping] implements it
```

### 场景 3:异步、线程池、响应式

```java
Session session = Session.require();                       // 请求线程上拿到当前会话(不可变)

executor.submit(session.wrap(() -> notifier.send(order))); // 任务在同一个身份下执行
// 任务若在本线程内联执行,本线程原来的会话在结束后被还原,而不是被清掉

Executor bound = session.wrap(executor);                   // 一次包住整个 Executor
CompletableFuture.runAsync(() -> audit.record(order), bound);

// 不想碰线程本地(响应式、测试):显式持有会话,按它回答
FreightCalcExtension freight = session.extension(FreightCalcExtension.class);
Mono.fromSupplier(() -> freight.calcFreight(ctx));

// 取全部实现并聚合(替代 3.x 的 invokeReduce)
BigDecimal discount = Session.require().all(PromotionCalcExtension.class).stream()
        .map(e -> e.calcPromotion(ctx)).reduce(BigDecimal.ZERO, BigDecimal::add);
```

starter 为 `ThreadPoolTaskExecutor` 提供 `TaskDecorator`,`@Async` 方法自动沿用提交时的会话。

### 场景 4:测试,不起 Spring,断言路由

```java
class FreightRoutingTest {
    Extensions extensions = ExtensionsTest.of(new RetailBusiness(), new FreeShippingAbility());

    @Test void freeShippingBeatsTheBusiness() {
        Session s = extensions.open(Identity.of("biz.retail", "ability.free-shipping"));

        assertRoute(s.explain(FreightCalcExtension.class), "calcFreight").selects("ability.free-shipping");
        assertEquals(0, s.extension(FreightCalcExtension.class).calcFreight(ctx).signum());
    }

    @Test void unknownBusinessIsAnError() {
        assertThrows(ResolutionException.class, () -> extensions.open(Identity.of("biz.nope")));
    }
}

// 业务代码的测试:直接指定身份,不需要构造请求
@SpringBootTest
@WithIdentity(business = "biz.retail", abilities = "ability.free-shipping")
class CheckoutTest { ... }
```

`ExtensionsTest.of(...)` 就是 `Extensions.builder().add(...).build()`,所以测试里同样会得到构建期的聚合校验。

### 场景 5:运维:解释、指标、热替换

```java
// 指标:与 3.x 相同的拦截器
@Bean ExtensionInterceptor metrics(MeterRegistry registry) {
    return invocation -> {
        Timer.Sample sample = Timer.start(registry);
        try { return invocation.proceed(); }
        finally {
            sample.stop(registry.timer("extension",
                    "point", invocation.extensionPoint().getSimpleName(),
                    "impl", invocation.implementationCode()));
        }
    };
}
```

```
GET /actuator/extensions                       → Description:全部扩展点、业务、能力、提供者、提示
GET /actuator/extensions/explain
      ?business=biz.retail&abilities=ability.free-shipping&type=com.acme.FreightCalcExtension
                                               → Explanation:每个方法的候选者和被选中者
```

```java
// 热替换:构建新的整体(同样一次性校验),换掉引用
Extensions next = Extensions.builder().add(newGeneration).build();
reloadableExtensions.replace(next);            // 进行中的会话不受影响;之后新开的会话用新注册表
```

## 6. 周边草图

**Spring starter。**
- `@ExtensionScan(scanPackages = ...)` 导入**一个**注册器:`ClassPathBeanDefinitionScanner` 加 include filter,把带 `@Business` / `@Ability` / `@DefaultProvider` 的类注册成普通 Bean;把 `@ExtensionPoint` 接口注册成 `@Primary` 的路由 Bean(`Session.router(type)`)。已经是 `@Component` 的类不需要被扫两次。
- `Extensions` 在全部单例实例化之后构建(`SmartInitializingSingleton`):`getBeansWithAnnotation` 收集,AOP 代理感知;`add(目标类, Bean)` 读注解、调用 Bean。
- 有 `IdentityResolver<HttpServletRequest>` Bean 时自动注册 servlet filter:`resolve` → `open` → `bind`。非 HTTP 入口(MQ、定时任务)显式 `session.run(...)`。
- 没有 resolver、但业务实现了 `Matcher<Req>` 时,自动用 `MatcherIdentityResolver`,选项来自属性:

```yaml
easy-extension:
  matching:
    no-match: none          # none | reject
    multi-match: reject     # reject | first | ordered
    order: [biz.retail, biz.fresh]
```

**test-kit(`easy-extension-test`)。** `ExtensionsTest.of(...)`、对 `Explanation` 的断言、JUnit 5 的 `@WithIdentity`。

**Actuator。** 只读的 `extensions` endpoint,序列化 `Description` 与 `Explanation`。UI 另起仓库消费它。

**注解处理器(编译期)。** 报错:`@ExtensionPoint` 不是 `public` 接口;`chain` 没有恰好一个 `SELF`;同一模块内业务码或能力码重复;同一个类上标了多个角色注解。提示:本模块内 `chain` 引用了找不到的能力码(跨模块的能力只能在启动时校验)。

## 7. 从 3.x 迁移

| 3.x | 4.0 | 自动化 |
|---|---|---|
| `@ExtensionInject X x` | 普通注入 `X x`(路由 Bean 为 `@Primary`) | recipe |
| `IExtensionContext<P>`、`initSession(param)` / `removeSession()` | `IdentityResolver<P>` + `extensions.open(identity)` + `session.run(...)`;Web 层由 starter 的 filter 完成 | 人工 + 部分 recipe |
| `context.invoke(E.class, e -> ...)` | `Session.router(E.class)` 的方法调用,或注入的路由 Bean | recipe |
| `invokeAll` / `invokeReduce` | `Session.require().all(E.class)` 加 stream | 人工 |
| `@MatcherParam`、`Matcher<P>` | `Matcher<P>` 保留在 `core.matching`;`@MatcherParam` 去掉(类型从 `Matcher<P>` 的泛型参数读) | recipe |
| `@Business(priority = 100, abilities = {"a", "b::10"})` | `@Business(chain = {...})`:按数字升序排,业务自身的优先级决定 `SELF` 的位置 | recipe(仅注解方式) |
| 手写 `IBusiness` / `IAbility`(含数据驱动的业务) | 不再有这两个接口:改成带注解的类,或用 `Builder.business(code, chain, impl)` / `ability(...)` 按数据登记 | 人工 |
| `@ExtensionPointDefaultImplementation class D implements E1, E2` | 把默认体写进接口的 `default` 方法;需要注入的用 `@DefaultProvider` | 人工 |
| `@ExtensionPoint(mandatory = true)` | 删掉,接口方法不带 `default` 即为必选 | recipe |
| `allow-unknown-business` 与两个策略属性 | `easy-extension.matching.*` | 人工 |
| `IScopedSessionManager`、命名 scope | 一个请求里的多个身份 = 多个 `Session` 对象 | 人工 |
| `ExtensionSessionScope.run / runWith` | `session.run` / `session.wrap` | recipe |
| `SessionException`、`InvokeException` 等 13 个异常 | `ResolutionException`、`ExtensionNotFoundException`、`RegistryException` | recipe(`ChangeType`) |
| 内嵌 admin | Actuator endpoint + 独立的 UI 仓库 | 人工 |
| `-Deasy-extension.legacy-exception-wrapping` | 删除(代理层不再存在) | —— |

迁移时要特别检查两处语义:**带 `default` 方法的扩展点**(路由按方法,见 §4.2)和 **`@Business` 的优先级数字**(顺序由 `chain` 决定)。

## 8. 这份草图还没回答的问题

ADR §9 的六项开放问题之外:

1. `Session.all` 是否要排除"只继承了 `default` 体、一个方法也没覆盖"的对象。
2. 提供者在 `Explanation` 和 `Invocation` 里的标识:现在用类名,是否需要一个显式的 `code`。
3. `Identity.of` 的参数校验用 `ResolutionException`(与"身份非法"一致)还是 `IllegalArgumentException`。
4. `Session.call` 是否需要接受 `Callable`,以便调用方保留受检异常。
