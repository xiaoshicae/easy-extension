# 4.0 API 草图

> **状态:草稿,P0 评审通过后冻结。** 这是接口和语义的草图,不是最终 API:签名可能微调,语义规则(§5)是评审的重点。
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
- 线程绑定与嵌套还原;Boot 自动配置的线程池(`@Async` 的默认执行器)自动沿用请求身份,其余线程用 `Extensions.wrap` 带上(边界见 §5.3)。
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

可选子包 `core.matching`(默认 resolver):`Matcher<Req>`、`MatcherIdentityResolver<Req>`。其余全部放 `core.internal`。starter 与 test-kit 的公开类型见 §4.7。

计数口径:表里是 core 的**顶层**公开类型(16 个,另有 `core.matching` 的 2 个),不含嵌套类型(`Extensions.Builder`、`Session.Binding` 等)、starter 和 test-kit。"第 1 层 5 个类型"是新手要认识的类型;其中 `Extensions` 有十几个成员,第 1 层只用 `run` / `call` / `all` / `wrap` / `of`。

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
/** 不可变的值对象:equals / hashCode 按内容,是"身份 → 链"缓存的键。能力集合顺序和重复项无关。 */
public final class Identity {
    public static Identity of(String business);         // 业务,以及它挂载的全部能力
    public static Identity none();                       // 没有业务:只有默认提供者和接口 default 回答
    public Identity only(String... abilities);           // 只启用挂载的能力里的这几个;空 = 只用业务自己。再次调用是替换,不是叠加
    public Identity without(String... abilities);        // 再去掉其中几个;多次调用叠加。启用的 = (only 或全部挂载) − without,与调用先后无关
    public String business();                            // none() 时为 null
    public boolean isNone();
}

@FunctionalInterface
public interface IdentityResolver<Req> {
    /**
     * 返回 Identity.none():明确"没有业务",只用默认层。
     * 返回 null:弃权,不绑定任何身份(与请求头缺失相同,调用扩展点时才报错),适合不带凭据的请求(健康检查)。
     * 无法解析时抛 ResolutionException(Web 下是 HTTP 400)。
     */
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
    <E> E extension(Class<E> type);                         // 扩展点 E 的路由代理:每次调用按**调用线程当前绑定的会话**回答,与它出自哪个 Extensions 无关(§5.3)。Spring 里被注入的就是它
    void run(String business, Runnable body);               // 在某个业务身份下执行(MQ、定时任务、测试)
    <R> R call(String business, Supplier<R> body);
    void run(Identity identity, Runnable body);
    <R> R call(Identity identity, Supplier<R> body);
    <E> List<E> all(Class<E> type);                         // 当前身份下链上全部是 E 的对象,按链顺序(替代 invokeAll / invokeReduce)

    // ---- 把"当前身份"带到别的线程(Boot 的 applicationTaskExecutor 已自动处理,其余线程才需要,边界见 §5.3) ----
    static Runnable wrap(Runnable task);                    // 捕获**调用 wrap 的线程**当时绑定的身份;当时没有身份就原样返回
    static <R> Supplier<R> wrap(Supplier<R> task);
    static Executor wrap(Executor executor);                // 每次 execute 时捕获提交线程的身份

    // ---- 进阶 ----
    Session open(Identity identity);                        // 显式持有会话,不绑定线程
    Description describe();

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
public final class ResolutionException extends ExtensionException {             // 业务码未知 / 能力未挂载或违反 requires / 没有绑定身份
    public ResolutionException(String message);                                 // IdentityResolver 里无法解析时由用户抛出
}
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
    public enum NoMatch { NONE, REJECT }              // 默认 REJECT:不静默退回默认层
    public enum MultiMatch { REJECT, FIRST, ORDERED } // 默认 REJECT
}
```

### 4.7 starter 与 test-kit 的公开类型

```java
// easy-extension-spring-boot-starter
@Retention(RUNTIME) @Target(TYPE)
public @interface ExtensionScan {              // 追加扫描包;默认扫 Boot 的自动配置包(@SpringBootApplication 所在包树)
    String[] scanPackages() default {};
}

public final class ExtensionTaskDecorator implements TaskDecorator { }   // 沿用提交时的身份;自己配置线程池、或要和已有的 TaskDecorator 组合时用

// easy-extension-test
@Retention(RUNTIME) @Target(TYPE)
public @interface WithIdentity {               // 加在测试类上:整个测试在这个身份下执行
    String value();                            // 业务码
    String[] only() default {};                // 对应 Identity.only(...);空 = 不收窄
    String[] without() default {};             // 对应 Identity.without(...)
}
```

属性(`easy-extension.*`):

| 属性 | 默认 | 含义 |
|---|---|---|
| `web.business-header` | 无 | 业务码所在的请求头;配了才注册 Web filter |
| `web.abilities-header` | 无 | 能力码所在的请求头,逗号分隔,对应 `only(...)`;缺失或空白 = 全部启用 |
| `matching.no-match` | `reject` | Matcher 风格:没有业务匹配。`none` = 只用默认层 |
| `matching.multi-match` | `reject` | Matcher 风格:多个业务匹配。`first` / `ordered` |
| `matching.order` | 空 | `ordered` 时的业务优先级 |
| `identity-cache-size` | `10000` | "身份 → 链"缓存的条数上限(LRU),见 §5.1 |

## 5. 语义规则(评审重点)

### 5.1 链的构造

输入 `Identity`,业务记为 `B`;`B` 挂载的能力 = `first` ∪ `abilities`:

1. `Identity.none()`:链为空,只有默认提供者和接口 `default` 回答。
2. `B` 不存在 → `ResolutionException`:`business [B] not found`。
3. 启用哪些能力:启用集合 = (`only(...)` 所列的;没调用 `only` 则是挂载的**全部**)减去 `without(...)` 所列的。`only` / `without` 里列了 `B` 没有挂载的能力 → `ResolutionException`:`business [B] does not mount ability [a]`。
4. 启用集合里每个能力 `requires` 的能力,也必须在启用集合里,否则 `ResolutionException`:`ability [a] requires ability [r], which is not enabled for business [B]`。`requires` 在建库时只按"挂载"校验过,这一步补上请求期收窄(`only` / `without`、Matcher 风格的 `match()`)之后的情形。
5. 链 = 启用的 `first`(按 `first` 的声明顺序)→ **业务自己** → 启用的 `abilities`(按 `abilities` 的声明顺序)。
6. 链的后面依次是各 `@DefaultProvider`,最后是接口自己的 `default`。

一句话:**先于业务的能力 → 业务自己 → 其余能力 → 默认。**

`Identity` 是不可变的值对象:`only` / `without` 里的能力是集合,顺序和重复项无关(`only(a, b)` 等于 `only(b, a)`);`only` 再次调用是替换,`without` 多次调用是叠加,与调用先后无关。同一个 `Identity` 得到同一条链,`Extensions` 按 `Identity` 缓存:容量默认 10000 条(LRU,属性 `easy-extension.identity-cache-size`),只缓存合法的身份(未知的业务、能力在进缓存前就被拒绝)。能力头由客户端传来,所以缓存必须有上限;超出上限只影响性能,不影响正确性,因为链是身份的纯函数。

### 5.2 路由(每次调用)

对扩展点 `E` 的方法 `m`:

1. 依次看链上每个对象 `x`。若 `x` **自己实现了** `m`,调用它,结束。"自己实现"指 `x` 的类(或其父类)声明了 `m`;若 `m` 在 `E` 里是 `default` 而 `x` 没有覆盖,**不算**,继续往下找。比 `E` 更具体的子接口覆盖了该 `default`,算 `x` 的实现。
2. 没有 → 看 `E` 的 `@DefaultProvider`,规则同上。
3. 没有 → 若 `m` 是 `default`,用 `InvocationHandler.invokeDefault` 调用接口自己的默认体。`this` 是路由代理,默认体里再调用 `E` 的其他方法会**重新路由**。
4. 否则 → `ExtensionNotFoundException`,消息列出整条链。
5. 实现类抛出的异常原样抛出,不包装。拦截器包住"选定实现之后"的那一次调用。
6. `Object` 的 `equals` / `hashCode` / `toString` 由代理自己回答,与身份无关(`toString` 形如 `ExtensionRouter(FreightCalcExtension)`):把它放进集合、写进日志,不会触发解析,也不需要绑定身份。

这就是 [ADR-0001 §4 差异 1](../adr/0001-v4-architecture.md#4-与-3x-的已知行为差异有意的):路由按方法而不是按扩展点接口。方法全是抽象方法的扩展点,与 3.x 完全一致。

**取全部实现 `all(E)`**(`Extensions.all` 与 `Session.all`)返回链上"自己实现了 `E` 的至少一个方法"的每个对象的代理,按链顺序;`@DefaultProvider` 若实现了 `E` 也在其中,排在最后。接口自己的 `default` 体不是对象,不出现。对每个元素的调用都经过拦截器,`Invocation.implementationCode()` 是该元素的码。元素代理直接调用这个对象,不再往下找:对象没有覆盖的 `default` 方法,执行的就是接口的默认体。与 3.x 的 `invokeAll` / `invokeReduce` 的差别见 §8:3.x 的默认实现是链上的一个对象,聚合时会被遍历到,4.0 的接口 `default` 体不会。

### 5.3 绑定与传递

- 一个线程同一时刻最多绑定一个当前会话。`run` / `call` / `bind()` 结束时**恢复进入前的绑定**(没有则清除),所以可以嵌套,也可以用在"任务内联执行"的线程上(直接执行器、`CallerRunsPolicy`)。
- 会话自己持有链、路由表和拦截器列表。线程上绑定的会话由打开它的 `Extensions` 提供这些;路由代理(`extension(E)`)每次调用读线程上的当前会话,不依赖它出自哪个 `Extensions`。所以两个 `Extensions`(父子容器、`Extensions.of(...)` 与 Spring 并存)嵌套时,内层 `run` 绑定内层的会话,谁打开会话谁的链回答,结束后恢复外层。
- **自动沿用的范围**:Boot 自动配置的 `applicationTaskExecutor`,也就是 `@Async` 的默认执行器。starter 提供 `ExtensionTaskDecorator`,在提交时捕获身份。应用自己声明了任何 `Executor` Bean 时,Boot 不再创建 `applicationTaskExecutor`;应用自己声明的线程池要自己设置 `ExtensionTaskDecorator`,或者用 `Extensions.wrap(...)`。`ThreadPoolTaskExecutor` 没有读取已有 decorator 的 getter,所以 starter 不能替应用声明的线程池"组合式加装"。
- **与应用自己的 `TaskDecorator` 共存**:starter 保证不会因为加入本框架而让应用已有的 `TaskDecorator` 失效。Boot 4 会把多个 `TaskDecorator` Bean 组合起来,starter 照常贡献自己的 Bean;Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用(多于一个则**都不生效**),所以应用已有自己的 `TaskDecorator` 时,starter 不贡献 Bean,并在启动期 WARN,说明把 `ExtensionTaskDecorator` 组合进自己的 decorator。
- **不会自动带身份的地方**:`new Thread`、自己 `new` 的线程池、`CompletableFuture` 的默认池、并行流(`parallelStream`)的工作线程。用 `Extensions.wrap(...)`,或在任务里自己 `call(...)`。
- `Extensions.wrap(Runnable / Supplier)` 捕获**调用 `wrap` 的那个线程**当时绑定的身份;`Extensions.wrap(Executor)` 返回的执行器在每次 `execute` 时捕获提交线程的身份。捕获时没有身份,就原样放行。不使用 `InheritableThreadLocal`。
- 调用扩展点时线程上没有身份 → `ResolutionException`,消息说明怎么办:`no identity is bound to this thread: send the business header, call extensions.run(...), or wrap a task that moved to another thread with Extensions.wrap(...)`。不会悄悄退回到"只有默认实现"。
- 虚拟线程下行为与平台线程一致。

### 5.4 注册表不可变

`Extensions` 一旦建好就不可变,会话自己持有链、路由表和拦截器列表,路由代理与注册表无关。4.0 **不提供**运行时增删实现或热替换;因为上述性质,以后加这个能力不需要改 API。

### 5.5 构建期校验

`build()` / 启动时一次性报告全部问题(`RegistryException#problems()`,消息逐行列出):

- 同一个类上标了不止一个 `@Business` / `@Ability` / `@DefaultProvider`。
- 业务码重复;能力码重复(两个命名空间各自检查)。
- `first` / `abilities` 里的能力码不存在、重复,或同一个能力同时出现在两个列表里。
- `requires` 的能力没有同时挂载(包括该码根本不存在);`excludes` 的能力同时挂载了。
- 能力或提供者没有实现任何 `@ExtensionPoint`。
- 同一个扩展点有两个 `@DefaultProvider`。
- 实现类**直接声明**的 `@ExtensionPoint` 接口不是 `public`(继承来的非 public 接口被忽略)。
- `add(declaredAs, instance)` 里,实例没有实现 `declaredAs` 所实现的全部扩展点接口。容器的 JDK 代理不是 `declaredAs`(带注解的用户类)的实例,但实现了它的接口,所以不拒绝;检查的是接口,不是 `instanceof declaredAs`。

用 `business(...)` / `ability(...)` / `provider(...)` 登记的,与注解方式走同一套校验。不阻止构建的提示(`describe().warnings()`):某个扩展点的抽象方法没有任何实现者;`excludes` 里的能力码不存在(可能是别的部署才有的能力,也可能拼错了)。

### 5.6 Web 绑定(starter)

配置 `easy-extension.web.business-header`(可选 `easy-extension.web.abilities-header`,逗号分隔,对应 `only(...)`;`without(...)` 要写 `IdentityResolver`)后,starter 注册一个 filter。绑定在本请求的线程上,请求结束还原。规则:

| 请求头 | 结果 |
|---|---|
| 业务头缺失或空白 | 不绑定。不调用扩展点的接口(健康检查、静态资源)不受影响;调用了得到 §5.3 的 `ResolutionException`,没人处理就是 HTTP 500 |
| 业务头重复(多个值) | HTTP 400,消息说明头必须只有一个值 |
| 业务头有值,业务码已知 | 绑定 `Identity.of(code)` |
| 业务头有值,业务码未知 | HTTP 400:`business [x] not found` |
| 能力头缺失或空白 | 业务挂载的全部能力都启用 |
| 能力头有值 | `.only(...)`;列了未挂载的能力 → HTTP 400 |
| 只有能力头、没有业务头 | 忽略能力头,不绑定 |

- 400 由 filter 自己写,消息在响应体里,不依赖 `server.error.include-message`(Boot 默认不返回消息)。Controller 里抛出的 `ResolutionException`(例如请求头缺失)按应用自己的异常处理走,没有处理就是 500,与其他未处理的异常一样;要映射成 400,写一个 `@ExceptionHandler`。
- **这个头是信任边界。** 任何能直接访问应用的客户端都能用它选业务。它应当由网关或认证层设置,并覆盖客户端传来的值;不要把应用直接暴露在外。业务应当从认证信息里取(JWT 的某个声明、租户表……)时,写 `IdentityResolver`。
- 有 `IdentityResolver<HttpServletRequest>` Bean 时用它代替属性:返回 `Identity.none()` 表示"没有业务,只用默认层";返回 `null` 表示弃权,不绑定(与头缺失相同,适合不带凭据的请求,如健康检查);抛 `ResolutionException` → HTTP 400。
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
| 没有请求头,且调用了扩展点 | —— | `ResolutionException`(没人处理就是 HTTP 500):`no identity is bound to this thread ...` |

### 场景 2:复用逻辑:能力

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements FreightCalcExtension {
    @Override public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}

@Business(code = "biz.fresh", abilities = {"ability.free-shipping"})    // 业务自己没实现的方法,由挂载的能力回答
public class FreshBusiness implements ColdChainExtension { ... }       // 没有实现 calcFreight,于是包邮能力回答

@Business(code = "biz.retail-plus", first = {"ability.free-shipping"}) // 包邮能力先于业务自己回答
public class RetailPlusBusiness implements FreightCalcExtension { ... }
```

请求默认启用业务挂载的全部能力。只启用一部分:`extensions.run(Identity.of("biz.retail-plus").only("ability.free-shipping"), () -> ...)`;去掉一个:`Identity.of("biz.retail-plus").without("ability.coupon")`。启用的能力,它 `requires` 的能力也必须启用(§5.1 第 4 步)。

### 场景 3:非 Web 入口与异步

```java
// MQ 消费者、定时任务:显式带上业务身份
extensions.run("biz.retail", () -> handle(message));

// Boot 的 applicationTaskExecutor(@Async 的默认执行器):什么都不用写,自动沿用提交时的身份
// 自己声明的线程池、new Thread、CompletableFuture 的默认池、并行流:包一层(边界见 §5.3)
Executor bound = Extensions.wrap(executor);
CompletableFuture.runAsync(() -> audit.record(order), bound);

// 取全部实现并聚合(替代 invokeAll / invokeReduce;不含接口 default 体,见 §5.2)
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
Extensions extensions = Extensions.of(new RetailBusiness());
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
- **一个**注册器(由自动配置导入):`ClassPathBeanDefinitionScanner` 加 include filter,把带 `@Business` / `@Ability` / `@DefaultProvider` 的类注册成普通 Bean(类上不必再加 `@Component`,加了也无妨)。
- 扫描范围默认是 Boot 的自动配置包,即 `@SpringBootApplication` 所在的包树(与 Spring Data 的约定一致);包树之外的实现才需要 `@ExtensionScan(scanPackages = ...)` 追加。包树之外的业务,表现为"业务码未知"(HTTP 400)。
- **路由 Bean**(普通注入拿到的)。为"已收集的实现所实现的每个 `@ExtensionPoint` 接口"注册一个 `@Primary` 的 Bean(`Extensions.extension(type)`),不论接口在哪个包、哪个 jar;扫描包树里的 `@ExtensionPoint` 接口同样注册。启动期检查:某个注入点要一个 `@ExtensionPoint` 类型的 Bean,却解析到别的 Bean 或没有 → 失败并提示 `@ExtensionScan`(3.x 的注入点后处理器有同样的检查)。**不要注入 `List<E>`**:`@Primary` 只管单值,集合会连路由 Bean 一起注入;要全部实现用 `extensions.all(E)`。
- `Extensions` Bean 是一个**门面**,没有依赖,所以可以注入到任何地方,包括业务 Bean。真正的注册表在全部单例实例化之后才构建并冻结,再接到门面上:收集(`getBeansWithAnnotation`,AOP 代理感知;`add(目标类, Bean)` 读注解、调用 Bean)、校验、冻结。就绪点是门面自己的 `afterSingletonsInstantiated`:此前调用(构造器、`@PostConstruct`)得到"尚未就绪"的错误;应用自己的 `SmartInitializingSingleton` 与它的先后取决于 Bean 的注册顺序,不要在那里调用扩展点,改用 `ApplicationRunner` 或 `ContextRefreshedEvent`。校验失败(`RegistryException`)发生在 Web 服务器开始接收请求之前。
- Web filter、`ExtensionTaskDecorator`、属性见 §4.7、§5.3、§5.6。**Matcher 风格**:没有 resolver Bean,且业务和能力实现的是 `Matcher<HttpServletRequest>` 时,starter 用它们构造 `MatcherIdentityResolver`,交给 Web filter,选项来自 `easy-extension.matching.*`(默认都是 `reject`,不静默退回默认层)。`Req` 是应用自己的参数类时(3.x 里最常见:`Matcher<OrderMatchParam>`),filter 拿不到这个对象,starter 只提供 `MatcherIdentityResolver<Req>` Bean,应用在入口自己 `extensions.run(resolver.resolve(param), ...)`。

```yaml
easy-extension:
  matching:
    no-match: reject        # reject(默认)| none:没有业务匹配时只用默认层
    multi-match: reject     # reject(默认)| first | ordered
    order: [biz.retail, biz.fresh]
```

**test-kit(`easy-extension-test`)。** 只有 `@WithIdentity`(§4.7)。`Explanation` 的断言放在进阶里,按需再加。

**Actuator。** 只读的 `extensions` endpoint,序列化 `Description` 与 `Explanation`。UI 另起仓库消费它。

**注解处理器(编译期)。** 报错:`@ExtensionPoint` 不是 `public` 接口;同一模块内业务码或能力码重复;同一个类上标了多个角色注解;`first` 与 `abilities` 里有同一个能力。提示:本模块内引用了找不到的能力码(跨模块的能力只能在启动时校验)。

## 8. 从 3.x 迁移

| 3.x | 4.0 | 自动化 |
|---|---|---|
| `@ExtensionInject X x` | 普通注入 `X x`(路由 Bean 为 `@Primary`) | recipe |
| `IExtensionContext<P>`、`initSession(param)` / `removeSession()` | Web:一行配置或一个 `IdentityResolver`;其他入口:`extensions.run(...)` | 人工 + 部分 recipe |
| `context.invoke(E.class, e -> ...)` | 注入的 `E`,直接调用 | recipe |
| `invokeAll` / `invokeReduce` | `extensions.all(E.class)` 加 stream。**不含接口 `default` 体**(3.x 的默认实现是链上的一个对象,会被遍历到),见 §5.2 | 人工 |
| `@MatcherParam`、`Matcher<P>` | `Matcher<P>` 保留在 `core.matching`;`@MatcherParam` 去掉(类型从 `Matcher<P>` 的泛型参数读) | recipe |
| `@Business(priority, abilities = {"a", "b::10"})` | 按 3.x **实际解析出的数字**比较:数字小于业务自身 `priority` 的能力 → `first`,其余 → `abilities`,各自按数字升序。未写 `::N` 的能力,3.x 自动编为 1、2……(业务默认是 0;业务写了 `priority = 100` 而能力不写数字,能力就在业务前),recipe 要复现这个编号 | recipe(仅注解方式) |
| 手写 `IBusiness` / `IAbility`(含数据驱动的业务) | 不再有这两个接口:改成带注解的类,或 `Extensions.builder().business(...)` / `ability(...)` | 人工 |
| `@ExtensionPointDefaultImplementation class D implements E1, E2` | 把默认体写进接口的 `default` 方法;需要注入的用 `@DefaultProvider` | 人工 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 全部删掉;接口方法不带 `default` 即为必选。`scenarios` / `version` 只用于 admin 展示;接口演进照旧靠新增 `default` 方法 | recipe |
| `allow-unknown-business` 与两个策略属性 | `easy-extension.matching.*`(Matcher 风格),或 resolver 自己决定 | 人工 |
| `IScopedSessionManager`、命名 scope | 一个请求里的多个身份 = 多个 `Session` 对象 | 人工 |
| `ExtensionSessionScope.run / runWith` | `extensions.run` / `Extensions.wrap` | recipe |
| `SessionException`、`InvokeException` 等 13 个异常 | `ResolutionException`、`ExtensionNotFoundException`、`RegistryException` | recipe(`ChangeType`) |
| 内嵌 admin | Actuator endpoint + 独立的 UI 仓库 | 人工 |
| `-Deasy-extension.legacy-exception-wrapping` | 删除(代理层不再存在) | —— |

迁移时要特别检查四处语义:**带 `default` 方法的扩展点**(路由按方法,见 §5.2)、**能力与业务自身的先后**(3.x 按数字比较:业务默认 0,未编号的能力自动编为 1、2……;4.0 默认业务在前,3.x 里数字更小的能力要进 `first`)、**能力的启用**(4.0 默认挂载的全部启用;原来靠 `match()` 或请求参数条件启用的,要用 `only` / `without` 或 `MatcherIdentityResolver`;收窄后 `requires` 也会在请求期校验,见 §5.1)、**聚合**(`all(E)` 不含接口 `default` 体,见 §5.2)。

## 9. 这份草图还没回答的问题

ADR-0001 §9 的六项开放问题里,草图暂定了四项:Q1 取全部实现 = `Extensions.all`(§5.2);Q2 `Matcher` 放 `core.matching` 子包;Q3 沿用 `io.github.xiaoshicae.extension.core` 包名;Q4 缓存默认 10000 条、LRU(§5.1)。Q5(响应式)、Q6(只读页面)仍开放。这份草图自己还有:

1. `first` 的命名。备选:`precedence`、`overriding`。现在用 `first` 是因为它最短,但可能被读成"abilities 里的第一个"。
2. 请求头缺失、或业务码未知时:现在分别是"不绑定"和 HTTP 400;是否需要一个开关改成绑定 `Identity.none()`(只用默认层)。3.x 的 `allow-unknown-business` 就是这个意思;现在要这样只能写 `IdentityResolver`,或用 Matcher 风格的 `no-match: none`。
3. 提供者在 `Explanation` 和 `Invocation` 里的标识:现在用类名,是否需要一个显式的 `code`。
4. `Identity.of(null)` 的校验用 `ResolutionException`(与"身份非法"一致)还是 `IllegalArgumentException`。
5. 请求期收窄后违反 `requires`:现在抛 `ResolutionException`(§5.1 第 4 步);备选是自动补上被依赖的能力。抛异常更显式,但对 Matcher 风格(`match()` 的结果被映射成 `only(...)`)是比 3.x 更严的行为。
6. `all(E)` 不含接口 `default` 体(§5.2)。需要"含默认值的聚合"的用户,现在只能自己在 stream 里再加一项。
7. 应用自己声明的线程池:`ThreadPoolTaskExecutor` 没有读取已有 decorator 的 getter,starter 没法替它们组合式加装,只能提供 `ExtensionTaskDecorator` 让应用自己设置(§5.3)。是否接受,或者 P2 用反射读私有字段。
