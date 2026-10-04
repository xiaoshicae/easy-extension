# 4.0 API 草图

> **状态:草稿,P0 评审通过后冻结。** 这是接口和语义的草图,不是最终 API:签名可能微调,语义规则(§5)是评审的重点。
> 决策与理由见 [ADR-0001](../adr/0001-v4-architecture.md) 和 [ADR-0002](../adr/0002-simplify-user-facing-api.md)(用户面简化,**提议中**,拟修订前者的 D1、D3)。本文按 ADR-0002 写;它评审不通过时,用户面部分回到 ADR-0001。本文不出现具体版本号,发版时不需要同步。

## 1. 设计目标:用户面最小,复杂留给框架

一个典型用户要写的:**两个类(扩展点接口、业务;业务带一个 `match`)、入口上一个注解、普通注入。**入口绑定与传输无关:HTTP 请求体、RPC、MQ 的入口都是 Bean 的方法,用同一个注解,请求对象交给业务和能力的 `Matcher` 判断;没有请求对象的入口(定时任务、网关设在请求头里的业务码)直接给业务码。

用户面按"谁需要懂它"分三层,上手只需要第 1 层:

| 层 | 谁用 | 内容 |
|---|---|---|
| 1 | 每个用户 | `@ExtensionPoint`、`@Business`、`@Ability`、`Matcher`、`@WithIdentity`、`Extensions` |
| 2 | 按需 | `Identity`(显式身份:点名启用或去掉能力、测试)、`IdentityResolver`(HTTP 过滤器里自定义身份)、`@MatcherParam`(匹配参数不是第一个参数时)、`ExtensionInterceptor`(指标、追踪)、`@DefaultProvider`(默认实现要注入 Bean) |
| 3 | 运维、测试、高级 | `Session`、`Explanation`、`Description`、`Extensions.Builder`、各异常类 |

框架替用户做掉的事,用户不需要知道:

- 启动时一次性校验,聚合报出**全部**问题:编码重复、能力不存在、`requires` / `excludes` 冲突、扩展点接口不是 `public` ……
- 从容器收集实现(感知 AOP 代理),按**方法**路由,预计算路由表,缓存"身份 → 链"的结果。
- 入口匹配:请求对象交给 `Matcher`,业务恰好一个命中、能力各自判断,规则固定,报错带上类型和业务码(§5.8)。
- 线程绑定与嵌套还原。换线程要带身份,规则只有一条:`Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`(§5.3)。没有"自动覆盖某个线程池"的例外。
- 异常原样透传;路由 Bean 与实现 Bean 的注入歧义;接口 `default` 方法的调用。

## 2. 一张图

```
启动时:  从容器收集 @Business / @Ability / @DefaultProvider ──► 一次性校验 ──► 不可变的 Extensions
入口时:  请求对象(@WithIdentity ──► Matcher)/ 业务码(字面量、请求头、extensions.run) ──► Identity ──► 链(按 Identity 缓存) ──► 绑定到当前线程
调用时:  freight.calcFreight(ctx) ─ 路由代理 ─► 当前身份的链 ─► 回答的实现(经拦截器)
```

三个阶段:**启动校验 → 入口绑定 → 调用路由**。启动期报全部错误;之后注册表不可变;入口把请求(或业务码)变成身份,再按身份取链;调用期只做"链 → 方法"。

## 3. 公开类型

| 层 | 类型 | 角色 | 取代 3.x 的 |
|---|---|---|---|
| 1 | `@ExtensionPoint` | 扩展点接口,没有属性 | 同名(去掉 `mandatory`、`scenarios`、`version`) |
| 1 | `@Business` | 业务:`code`、`abilities`、`overridingAbilities`;可选实现 `Matcher` | `@Business(priority, abilities)` |
| 1 | `@Ability` | 能力:`code`、`requires`、`excludes`;可选实现 `Matcher` | 同名 |
| 1 | `Matcher<P>` | 业务:这个请求归不归我(恰好一个业务命中);能力:这个请求要不要我。P 由类的泛型解析,可以是接口 | 同名(没有 3.x 的全局匹配参数类 `T`,即原来标了 `@MatcherParam` 的那个类;没有无命中 / 多命中策略和选择器) |
| 1 | `@WithIdentity` | 入口(或测试)上的身份绑定:无值 = 方法的匹配参数交给 Matcher;有值 = 业务码字面量 | 入口处手写的 `initSession` / `ExtensionSessionScope.run` |
| 1 | `Extensions` | 注入、`identityOf`、`run` / `call`、`all`、`wrap` | `IExtensionContext` 及四对 Manager |
| 2 | `Identity` | 显式身份:业务码,以及点名启用或去掉哪些能力(`only` / `with` / `without`);Matcher 的结果也是一个 `Identity` | (3.x 的身份是会话里的隐含状态) |
| 2 | `IdentityResolver<Req>` | HTTP 过滤器里的自定义身份:请求 → `Identity`(只被 HTTP 过滤器使用;里面可以调用 `identityOf` 用上 Matcher) | 新增 |
| 2 | `@MatcherParam` | 标在入口方法的参数上:它是匹配参数(默认是第一个参数) | 3.x 的同名注解(标在全局匹配参数类上)改标参数 |
| 2 | `ExtensionInterceptor`、`Invocation` | 环绕拦截 | 同名(语义不变) |
| 2 | `@DefaultProvider` | 默认实现要注入 Bean 时用 | `@ExtensionPointDefaultImplementation` |
| 3 | `Session` | 不可变的已解析身份:显式持有、解释 | `ResolvedChain`、`IExtensionSession`、`ExtensionSessionScope` |
| 3 | `Explanation`、`Description` | 每个方法由谁回答;注册表的 JSON 描述 | `ExtensionExplanation`、`ResolveTrace`、admin 的数据模型 |
| 3 | `ExtensionException` 与 `RegistryException`、`ResolutionException`、`ExtensionNotFoundException` | 全部 unchecked | 13 个异常类 |

其余全部放 `core.internal`。`Matcher` 在 4.0 里保留(ADR-0002 S12),但它只是得到 `Identity` 的一种方式:核心仍是显式的 `Identity`,非泛型,链按 `Identity` 缓存。starter 与 test-kit 的公开类型见 §4.6。

计数口径:表里是 core 的**顶层**公开类型(19 个),不含嵌套类型(`Extensions.Builder`、`Session.Binding` 等)、starter 和 test-kit。"第 1 层 6 个类型"是新手要认识的类型:业务码在网关设的请求头里的用户不需要 `@WithIdentity`;`Extensions` 有十几个成员,第 1 层只用 `all` / `wrap` / `of`(`identityOf`、`run` / `call` 在需要显式身份或手动匹配时用)。

## 4. 签名草图

包:公开 API 在 `io.github.xiaoshicae.extension.core`,内部在 `...core.internal`(ADR-0001 §9 的开放问题 3)。

下面是签名草图:方法体省略,`static` 方法和类的方法也只写签名,不是可直接编译的代码。

### 4.1 注解

```java
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface ExtensionPoint { }          // 没有属性:scenarios、version 随 admin 一起去掉

/** 业务。可以同时实现 {@link Matcher},认领自己的请求;不实现就只能用业务码显式选中(定时任务、测试、请求头)。 */
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Business {
    String code();
    /** 挂载的能力。业务自己没实现的方法,按声明顺序由它们回答。 */
    String[] abilities() default {};
    /** 覆盖业务自己的能力(也算挂载),按声明顺序:它们实现的方法,先于业务自己回答。例如包邮、风控这类要压过业务自己逻辑的能力。 */
    String[] overridingAbilities() default {};
}

/** 能力。可以同时实现 {@link Matcher}:有它,入口有请求对象时由 `match` 决定启用与否;没有,业务被选中后始终启用。 */
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Ability {
    String code();
    String[] requires() default {};   // 必须同时挂载在该业务上
    String[] excludes() default {};   // 不得同时挂载
}

/** 默认实现里需要注入 Bean 时用。每个扩展点至多一个。链上没人回答时,它排在接口自己的 default 之前回答。 */
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface DefaultProvider { }

/** 业务和能力可选实现。`match` 在每次入口匹配时调用:要快、没有副作用、不抛异常,对请求字段判空(抛了会变成 ResolutionException,带上它的码)。规则见 §5.8。 */
@FunctionalInterface
public interface Matcher<P> {
    boolean match(P param);        // 业务:这个请求归不归我;能力:这个请求要不要我
}

/** 标在入口方法的参数上:它是匹配参数。不标则是第一个参数。 */
@Documented @Retention(RUNTIME) @Target(PARAMETER)
public @interface MatcherParam { }

/**
 * 入口(或测试)上的身份绑定:被标注的方法执行期间绑定身份,执行完恢复进入前的绑定。加在类上(只能是字面量),是该类全部 public 方法;方法上的覆盖类上的。
 * 注解本身在 core(没有依赖);starter 用 Spring AOP 处理,test-kit 用 TestExecutionListener 处理(加在测试类上)。语义见 §5.7。
 */
@Documented @Retention(RUNTIME) @Target({TYPE, METHOD})
public @interface WithIdentity {
    /**
     * 非空:业务码字面量,例如 "biz.retail"(定时任务、固定业务的消费者、测试),身份是 Identity.of(code),不运行 Matcher。
     * 空:匹配模式:方法的匹配参数交给业务和能力的 Matcher,得到身份(§5.8)。只能标在方法上。
     * 没有表达式语言,也没有 only / without 属性:按请求决定能力,让能力实现 Matcher。
     */
    String value() default "";
}
```

### 4.2 身份

```java
/** 不可变的值对象:equals / hashCode 按内容,是"身份 → 链"缓存的键。能力集合顺序和重复项无关。显式身份(of / only / with / without)不运行 Matcher;Matcher 的结果见 Extensions.identityOf。 */
public final class Identity {
    public static Identity of(String business);         // 业务,以及它挂载的、没有 Matcher 的能力(带 Matcher 的能力要有请求才能判断,这里不启用)
    public static Identity none();                       // 没有业务:只有默认提供者和接口 default 回答
    public Identity only(String... abilities);           // 只启用挂载的能力里的这几个(可以点名带 Matcher 的);空 = 只用业务自己。再次调用是替换,不是叠加
    public Identity with(String... abilities);           // 再启用这几个(带 Matcher 的能力在显式身份下用它点名);多次调用叠加
    public Identity without(String... abilities);        // 再去掉其中几个;多次调用叠加。启用的 = ((only 所列的,没调用 only 则是没有 Matcher 的挂载能力) ∪ with) − without,与调用先后无关
    public String business();                            // none() 时为 null
    public boolean isNone();
}

@FunctionalInterface
public interface IdentityResolver<Req> {
    /**
     * 返回 Identity.none():明确"没有业务",只用默认层。
     * 要用上业务和能力的 Matcher:组装一个匹配参数,返回 extensions.identityOf(param)。
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
    Identity identityOf(Object param);                      // 把请求对象交给业务和能力的 Matcher,得到身份(§5.8);无命中 / 多命中 → ResolutionException。入口用 @WithIdentity 时不必自己调用
    <E> E extension(Class<E> type);                         // 扩展点 E 的路由代理:每次调用按**调用线程当前绑定的会话**回答,与它出自哪个 Extensions 无关(§5.3)。Spring 里被注入的就是它
    void run(String business, Runnable body);               // 在某个业务身份下执行:代码里要绑身份、入口不是 Spring Bean 的方法、一个入口里切换到别的业务时用(入口一般用 @WithIdentity)
    <R> R call(String business, Supplier<R> body);
    void run(Identity identity, Runnable body);
    <R> R call(Identity identity, Supplier<R> body);
    <E> List<E> all(Class<E> type);                         // 当前身份下链上全部是 E 的对象,按链顺序(替代 invokeAll / invokeReduce)

    // ---- 把"当前身份"带到别的线程(换线程要带身份,没有自动覆盖的线程池,见 §5.3) ----
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
        Builder business(String code, List<String> overridingAbilities, List<String> abilities, Object implementation);
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
    public record BusinessInfo(String code, Class<?> implementation, List<String> overridingAbilities, List<String> abilities, List<Class<?>> extensionPoints, Class<?> matcherParam) { }   // matcherParam:没有 Matcher 时为 null
    public record AbilityInfo(String code, Class<?> implementation, List<String> requires, List<String> excludes, List<Class<?>> extensionPoints, Class<?> matcherParam) { }
    public record ProviderInfo(Class<?> implementation, List<Class<?>> extensionPoints) { }
}

public abstract class ExtensionException extends RuntimeException { }
public final class RegistryException extends ExtensionException { public List<String> problems(); }   // 启动:全部问题
public final class ResolutionException extends ExtensionException {             // 业务码未知 / 能力未挂载或违反 requires / 没有绑定身份 / 匹配无命中、多命中、Matcher 抛异常
    public ResolutionException(String message);                                 // IdentityResolver 里无法解析时由用户抛出
}
public final class ExtensionNotFoundException extends ExtensionException { }    // 调用:没有实现者的抽象方法
```

### 4.6 starter 与 test-kit 的公开类型

```java
// easy-extension-spring-boot-starter
@Retention(RUNTIME) @Target(TYPE)
public @interface ExtensionScan {              // 追加扫描包;默认扫 Boot 的自动配置包(@SpringBootApplication 所在包树)
    String[] scanPackages() default {};
}

public final class ExtensionTaskDecorator implements TaskDecorator { }   // 沿用提交时的身份。应用自己设置到线程池上,starter 不自动贡献(§5.3)

// easy-extension-test:没有新的公开类型。@WithIdentity(core 里的那个)加在 Spring 测试类上时,由自动注册的 TestExecutionListener 处理(§5.7)
```

属性(`easy-extension.*`):

| 属性 | 默认 | 含义 |
|---|---|---|
| `web.business-header` | 无 | 业务码所在的请求头;配了才注册 Web filter(只在 servlet 应用里) |
| `web.abilities-header` | 无 | 能力码所在的请求头,逗号分隔,对应 `only(...)`(可以点名带 `Matcher` 的能力);缺失或空白 = 不收窄(没有 `Matcher` 的挂载能力启用) |

"身份 → 链"缓存的容量等内部参数不对外配置,用内部默认值(§5.1)。

## 5. 语义规则(评审重点)

### 5.1 链的构造

本节从一个 `Identity` 出发。请求对象怎么变成 `Identity`(`Matcher`),见 §5.8。

输入 `Identity`,业务记为 `B`;`B` 挂载的能力 = `overridingAbilities` ∪ `abilities`:

1. `Identity.none()`:链为空,只有默认提供者和接口 `default` 回答。
2. `B` 不存在 → `ResolutionException`:`business [B] not found`。
3. 启用哪些能力:启用集合 = ((`only(...)` 所列的;没调用 `only` 则是挂载的、**没有 `Matcher`** 的能力)加上 `with(...)` 所列的)减去 `without(...)` 所列的。带 `Matcher` 的能力要有请求才能判断,显式身份下默认不启用,用 `with(...)`(或 `only(...)`)点名。`only` / `with` / `without` 里列了 `B` 没有挂载的能力 → `ResolutionException`:`business [B] does not mount ability [a]`。
4. 启用集合里每个能力 `requires` 的能力,也必须在启用集合里,否则 `ResolutionException`:`ability [a] requires ability [r], which is not enabled for business [B]`。`requires` 在建库时只按"挂载"校验过,这一步补上请求期收窄(`only` / `without`)之后的情形。
5. 链 = 启用的 `overridingAbilities`(按声明顺序)→ **业务自己** → 启用的 `abilities`(按声明顺序)。
6. 链的后面依次是各 `@DefaultProvider`,最后是接口自己的 `default`。

一句话:**覆盖业务的能力 → 业务自己 → 其余能力 → 默认。**

`Identity` 是不可变的值对象:`only` / `with` / `without` 里的能力是集合,顺序和重复项无关(`only(a, b)` 等于 `only(b, a)`);`only` 再次调用是替换,`with` / `without` 多次调用是叠加,与调用先后无关。同一个 `Identity` 得到同一条链,`Extensions` 按 `Identity` 缓存:容量是内部默认值 10000 条(LRU,不对外配置),只缓存合法的身份(未知的业务、能力在进缓存前就被拒绝)。身份可能来自请求内容(Matcher 的结果、能力头),所以缓存必须有上限;超出上限只影响性能,不影响正确性,因为链是身份的纯函数。

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
- **换线程要带身份,规则只有一条,框架不自动覆盖任何线程池。** 两种带法:`Extensions.wrap(...)` 包住任务;或者给线程池设置 `ExtensionTaskDecorator`,它在提交时捕获身份(`executor.setTaskDecorator(new ExtensionTaskDecorator())`;要让 Boot 的 `applicationTaskExecutor`,也就是 `@Async` 的默认执行器采用它,就把它声明成 `TaskDecorator` Bean)。已经有自己的 `TaskDecorator` 时,自己组合,例如 Spring 的 `CompositeTaskDecorator`。
- **为什么不自动贡献 `TaskDecorator` Bean。** Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用(多于一个则**都不生效**),Boot 4 会把多个组合起来(两个版本的自动配置已核对);Boot 在应用自己声明了任何 `Executor` Bean 时不再创建 `applicationTaskExecutor`;`ThreadPoolTaskExecutor` 又没有读取已有 decorator 的 getter,框架没法替应用自己的线程池组合式加装。自动贡献只能覆盖一部分线程池,还会在 3.5 上让应用已有的 decorator 失效;显式的规则则处处一样。
- **不会带身份的地方**:`@Async` 的默认执行器(除非按上面设置了 `ExtensionTaskDecorator`)、`new Thread`、自己 `new` 的线程池、`CompletableFuture` 的默认池、并行流(`parallelStream`)的工作线程。用 `Extensions.wrap(...)`,或在任务里自己 `call(...)`。
- `Extensions.wrap(Runnable / Supplier)` 捕获**调用 `wrap` 的那个线程**当时绑定的身份;`Extensions.wrap(Executor)` 返回的执行器在每次 `execute` 时捕获提交线程的身份。捕获时没有身份,就原样放行。不使用 `InheritableThreadLocal`。
- 调用扩展点时线程上没有身份 → `ResolutionException`,消息说明怎么办:`no identity is bound to this thread: bind one at the entry (@WithIdentity or the business header), call extensions.run(...), or wrap a task that moved to another thread with Extensions.wrap(...)`。不会悄悄退回到"只有默认实现"。
- 虚拟线程下行为与平台线程一致。

### 5.4 注册表不可变

`Extensions` 一旦建好就不可变,会话自己持有链、路由表和拦截器列表,路由代理与注册表无关。4.0 **不提供**运行时增删实现或热替换;因为上述性质,以后加这个能力不需要改 API。

### 5.5 构建期校验

`build()` / 启动时一次性报告全部问题(`RegistryException#problems()`,消息逐行列出):

- 同一个类上标了不止一个 `@Business` / `@Ability` / `@DefaultProvider`。
- 业务码重复;能力码重复(两个命名空间各自检查)。
- `overridingAbilities` / `abilities` 里的能力码不存在、重复,或同一个能力同时出现在两个列表里。
- `requires` 的能力没有同时挂载(包括该码根本不存在);`excludes` 的能力同时挂载了。
- 能力或提供者没有实现任何 `@ExtensionPoint`。
- 同一个扩展点有两个 `@DefaultProvider`。
- 实现类**直接声明**的 `@ExtensionPoint` 接口不是 `public`(继承来的非 public 接口被忽略)。
- `add(declaredAs, instance)` 里,实例没有实现 `declaredAs` 所实现的全部扩展点接口。容器的 JDK 代理不是 `declaredAs`(带注解的用户类)的实例,但实现了它的接口,所以不拒绝;检查的是接口,不是 `instanceof declaredAs`。
- `Matcher` 的检查(§5.8):参数类型 P 解析不出来(原始类型 `Matcher`,或泛型信息被擦除);同一个方法上有多个 `@MatcherParam`。
- starter 对 `@WithIdentity` 另加的检查(§5.7):字面量的业务码不存在;无值的 `@WithIdentity` 标在类上、方法没有参数、匹配参数的声明类型与所有业务的 P 互不相容(两个类型中任一个能赋值给另一个,就算相容)。

用 `business(...)` / `ability(...)` / `provider(...)` 登记的,与注解方式走同一套校验。不阻止构建的提示(`describe().warnings()`):某个扩展点的抽象方法没有任何实现者;`excludes` 里的能力码不存在(可能是别的部署才有的能力,也可能拼错了);配了 `web.business-header`,而挂载的能力里有带 `Matcher` 的:请求头绑定不运行 Matcher,这些能力在这条路径上不启用(除非用能力头点名),需要按请求判断时用 `@WithIdentity`(无值),启动时同时打一条 WARN 日志(§5.6);带 `Matcher` 的能力,它的 P 与挂载它的所有业务的 P 互不相容:匹配模式下它永远不会启用;没有 `Matcher` 的能力 `requires` 带 `Matcher` 的能力:前者始终启用、后者按请求启用,某些请求上一定违反 `requires`(§5.8 第 4 条)。resolver 返回什么身份没法静态检查,按 §5.8 的规则由写 resolver 的人负责。

### 5.6 HTTP 请求头绑定(starter,可选)

身份由网关设置在请求头里时,配置 `easy-extension.web.business-header`(可选 `easy-extension.web.abilities-header`,逗号分隔,对应 `only(...)`;`without(...)` 要写 `IdentityResolver`)后,starter 注册一个 filter,只在 servlet 应用里。身份在请求体里、或者根本没有 HTTP(RPC、MQ、定时任务)时,用 `@WithIdentity`(§5.7)。过滤器绑定的是显式身份:**不运行 Matcher**,启用的是没有 `Matcher` 的挂载能力,带 `Matcher` 的能力不启用(除非用能力头点名);请求里有按内容判断的能力时,用 `@WithIdentity`(无值),或写 `IdentityResolver` 并在里面调用 `extensions.identityOf(param)`。过滤器绑定在本请求的线程上,请求结束还原。规则:

| 请求头 | 结果 |
|---|---|
| 业务头缺失或空白 | 不绑定。不调用扩展点的接口(健康检查、静态资源)不受影响;调用了得到 §5.3 的 `ResolutionException`,没人处理就是 HTTP 500 |
| 业务头重复(多个值) | HTTP 400,消息说明头必须只有一个值 |
| 业务头有值,业务码已知 | 绑定 `Identity.of(code)` |
| 业务头有值,业务码未知 | HTTP 400:`business [x] not found` |
| 能力头缺失或空白 | 业务挂载的、没有 `Matcher` 的能力启用 |
| 能力头有值 | `.only(...)`;列了未挂载的能力 → HTTP 400 |
| 只有能力头、没有业务头 | 忽略能力头,不绑定 |

- 400 由 filter 自己写,消息在响应体里,不依赖 `server.error.include-message`(Boot 默认不返回消息)。Controller 里抛出的 `ResolutionException`(例如请求头缺失)按应用自己的异常处理走,没有处理就是 500,与其他未处理的异常一样;要映射成 400,写一个 `@ExceptionHandler`。
- **这个头是信任边界。** 任何能直接访问应用的客户端都能用它选业务。它应当由网关或认证层设置,并覆盖客户端传来的值;不要把应用直接暴露在外。业务应当从认证信息里取(JWT 的某个声明、租户表……)时,写 `IdentityResolver`。
- 有 `IdentityResolver<HttpServletRequest>` Bean 时用它代替属性:返回 `Identity.none()` 表示"没有业务,只用默认层";返回 `null` 表示弃权,不绑定(与头缺失相同,适合不带凭据的请求,如健康检查);抛 `ResolutionException` → HTTP 400。
- 非 HTTP 入口(RPC、MQ 监听、定时任务)用 `@WithIdentity`(§5.7);代码里要绑身份用 `extensions.run(...)`。

### 5.7 入口绑定:`@WithIdentity`

入口绑定与传输无关。很多 Spring Boot 应用是 RPC(Dubbo、gRPC 等)、MQ 消费者或定时任务,没有 HTTP;即使是 HTTP,业务也常在请求体里,过滤器读不到。它们的入口都是 Spring Bean 的方法,所以在方法上声明身份:

```java
@DubboService
class OrderFacadeImpl implements OrderFacade {
    @WithIdentity                                        // 无值:第一个参数交给业务和能力的 Matcher(§5.8)
    public Result create(CreateOrderRequest request) { ... }
}

@RabbitListener(queues = "order")
@WithIdentity                                            // MQ 监听:同一个写法
public void onOrder(OrderMessage msg) { ... }

@Component @WithIdentity("biz.retail")                  // 字面量:定时任务、固定业务的消费者
class SettleJob { @Scheduled(cron = "0 0 2 * * *") public void settle() { ... } }
```

规则:

1. **哪些方法。** Spring Bean 上经过代理的 public 方法:标在方法上只管该方法;标在类上管全部 public 方法(不含 `Object` 的方法);方法上的覆盖类上的。starter 用 Spring AOP 的 Advisor 实现,不需要 AspectJ。Dubbo、gRPC、MQ 监听、`@Scheduled`、Controller 的入口都是 Bean 的方法。
2. **两种写法。** `value` 非空:业务码字面量,身份是 `Identity.of(code)`,显式身份,不运行 Matcher。`value` 为空:匹配模式,身份由 §5.8 得到;只能标在方法上(标在类上要写字面量)。没有表达式语言:业务和能力由 `Matcher` 或字面量决定,编译器和 IDE 查得到。
3. **匹配参数。** 默认是方法的第一个参数;不是第一个,就在那个参数上标 `@MatcherParam`。参数值为 `null` → `ResolutionException`。
4. **没有"弃权"。** 匹配模式下,无命中、多命中、`Matcher` 抛异常、参数为 `null` 都是 `ResolutionException`,在进入方法体**之前**抛出,方法体没有执行。不需要身份的方法不要标这个注解。这是确定性的错误,重试没有意义:MQ 监听器要配置不重试或进死信队列,否则默认的重投可能一直循环。
5. **绑定与还原。** 与 `extensions.call(...)` 相同:绑定在执行这个方法的线程上,方法结束恢复进入前的绑定;嵌套时内层覆盖外层。**例外:** HTTP 请求头先绑定了业务,而匹配模式得到的业务与它不同 → `ResolutionException`(请求头是网关断言的,请求体是调用方给的,后者不能静默覆盖前者);相同则以匹配得到的能力集合为准。字面量形式和 `extensions.call` 是代码里的显式选择,内层覆盖外层。
6. **线程。** 绑定的是执行入口方法的线程,方法返回时还原。入口方法里换线程,照 §5.3 带上身份;返回 `CompletableFuture` / `Mono` 的异步入口(Dubbo 的异步提供者、gRPC、响应式),绑定只覆盖方法同步返回之前,续体要用 `Extensions.wrap`(§5.3)。与 `@Async` 标在同一个方法上时,绑定在调用线程,方法体在别的线程执行,不带身份。
7. **限制。** Spring AOP 的常规限制:同一个类内部的调用不经代理,`final` 类和方法、`private` 方法不生效;只对 Spring Bean 生效,不是 Bean 的入口用 `extensions.run(...)` 或 `extensions.call(extensions.identityOf(param), ...)`。`@WithIdentity` 的通知排在事务、缓存等其他切面之外(最先执行)。
8. **容器。** 由所在容器的 `Extensions` 处理:父子容器各有自己的注册表,子容器里的入口只在子容器登记的业务里匹配,没有就是"无命中"。
9. **启动期检查**(§5.5):字面量的业务码必须存在;匹配模式要有匹配参数,且它的声明类型与至少一个业务的 P 相容(两个类型中任一个能赋值给另一个,就算相容)。
10. **测试。** 字面量形式加在 Spring 测试类上(`@SpringBootTest @WithIdentity("biz.retail")`),由 test-kit 自动注册的 `TestExecutionListener` 在每个测试方法前后绑定和还原,不需要构造请求;显式身份只启用没有 `Matcher` 的能力,要测带 `Matcher` 的能力,用 `Identity.with(...)` 点名,或对有代表性的请求对象调用 `extensions.identityOf(...)`,断言得到的 `Identity`(`Identity` 按内容相等:`assertEquals(Identity.of("biz.retail").only("ability.coupon"), extensions.identityOf(sample))`)。纯 Java 测试没有容器,用 `extensions.call(...)`。

身份在传输层元数据里(RPC 的 attachment、gRPC 的 metadata、MQ 的消息头),或入口不是 Bean 时,写在 RPC 框架自己的服务端拦截点里:

```java
// 伪代码:以所用 RPC 框架的拦截器 API 为准
Result invoke(Call call, Chain chain) {
    String biz = call.metadata("biz-code");
    return biz == null ? chain.proceed() : extensions.call(biz, chain::proceed);     // 显式身份,不运行 Matcher
}
```

注意:gRPC 的监听器回调可能不在 `interceptCall` 的线程上,要在回调里绑定;Dubbo 的异步提供者在别的线程完成响应,要照 §5.3 带上身份。核心里不维护各 RPC 框架的适配器,配方放在文档里;需求多了再出独立模块。

### 5.8 由 Matcher 得到身份

入口有请求对象时,请求对象交给业务和能力的 `Matcher`,得到一个 `Identity`;之后的链、缓存、路由都只认 `Identity`(§5.1–§5.4),与身份怎么来的无关。`extensions.identityOf(param)` 就是这一步,`@WithIdentity`(无值)在进入方法体之前调用它。

**`Matcher<P>`** 业务和能力都**可选**实现:`class RetailBusiness implements Matcher<CheckoutRequest>`。P 由类的泛型参数解析。一个类只能有一个 P(Java 不允许同一个接口带两个不同的类型参数)。同一个业务要认领几种请求类型时(例如 Dubbo 的请求对象和 MQ 的消息),让这些类型实现同一个小接口(放在 api 模块里,不依赖 easy-extension),`Matcher` 取这个接口。没有 `Matcher` 的业务只能用业务码显式选中(定时任务、测试、请求头);没有 `Matcher` 的能力,业务被选中后始终启用(必须执行的能力,例如风控,就不要给它 `Matcher`:条件写在它自己的方法里)。

规则(严格,没有开关、策略和选择器):

1. **候选业务。** `Matcher` 的 P 能接受该请求对象的类的业务。按请求类建索引,冻结后懒建、只建一次;AOP 代理的 Bean 读目标类的泛型。
2. **业务恰好一个命中。** 候选业务全部评估一遍。都不命中 → `ResolutionException`:`no business matches a CheckoutRequest (3 business matcher(s) evaluated)`;多个命中 → `ResolutionException`:`businesses [biz.retail, biz.retail-vip] all match a CheckoutRequest; business matchers must be mutually exclusive`。消息只写类型、数量和业务码,不写请求内容。
3. **启用哪些能力。** 被选中业务挂载的能力(`overridingAbilities` ∪ `abilities`)里:没有 `Matcher` 的启用;有 `Matcher` 且 P 能接受该请求对象的,`match` 为真才启用;有 `Matcher` 但 P 不能接受的,不启用。
4. **`requires`。** 启用集合里每个能力 `requires` 的能力也必须启用(§5.1 第 4 步),否则 `ResolutionException`。这是比 3.x 更严的地方:3.x 只在挂载时校验 `requires`,请求期按 `match()` 启用的组合不检查。所以 X `requires` Y 时,要保证"X 启用则 Y 启用":让被依赖的 Y 不带 `Matcher`(始终启用),或让 X 的 `match(r)` 为真时 Y 的 `match(r)` 也为真。
5. **结果**是 `Identity.of(业务).only(启用集合)`,按 §5.1 取链并缓存。

要点:

- **显式身份不运行 Matcher。** `@WithIdentity("biz.x")`、请求头、`extensions.run("biz.x", ...)`、`Identity.of(...)`(不经过 Matcher 得到的身份)都不问 `match`:启用的是挂载的、**没有 `Matcher`** 的能力;带 `Matcher` 的能力要有请求才能判断,默认不启用,用 `Identity.with(...)` 或 `only(...)` 点名(ADR-0002 S3)。`identityOf` 的结果是 Matcher 得到的身份,传给 `run` 时按它自己的启用集合。这样没有请求的入口(定时任务、测试、请求头)不会意外启用按请求才该启用的能力(包邮、折扣);必须一直执行的能力不要给它 `Matcher`。
- **匹配用的是请求里的字段,而请求是调用方给的。** HTTP 请求体、RPC 调用方传来的业务码和能力判断用的字段,调用方都能改,与请求头的信任边界同理(§5.6)。选业务的字段应由网关或认证层确定;影响价格的能力(包邮、折扣)不要让调用方直接"点名"(例如 `abilityCodes.contains("free-shipping")`),判断依据用服务端能核实的订单事实(金额、会员等级、优惠券是否有效)。
- **`match` 要快、没有副作用、不抛异常,对请求字段判空。** 每次入口、每个候选业务调用一次(所有候选都会评估,为了发现重叠)。任何一个候选的 `match` 抛异常,都让这类请求整体失败,哪怕别的业务本来会认领;抛出的异常变成 `ResolutionException`,带上它的业务码或能力码。
- **`match` 经容器里的 Bean 调用**(与 3.x 一致:Spring 测试里替换业务 Bean 的测试替身要 stub `match()`)。类上的事务、缓存等切面会作用于每次 `match`,带 `Matcher` 的类上不要加类级别的 `@Transactional` / `@Cacheable`。
- **重叠是运行期才会发现的错误。** 两个业务的 `match` 对同一个请求都为真,只在这类请求上抛 `ResolutionException`,典型数据的测试测不出来。对有代表性的请求对象调用 `identityOf` 写单测,断言命中的业务码。
- **成本**随候选业务数线性增长,但很小。设计原型的粗测(一次性程序,单线程,非 JMH,预热后,`match` 只是字符串相等,不能由仓库复现):每个业务的 `match` 约 5–6 ns,另有约 0.1 µs 的固定开销,所以 10 个业务约 0.15 µs,100 个约 0.6 µs,1000 个约 6 µs;显式身份的链查找约 30 ns。远小于一次 RPC;真实的 `match` 更重时按比例放大。3.x 的 `initSession` 在稳定化分支上的实测(ADR-0001 D1,普通循环,1000 个业务约 3.0 µs)与它同量级。
- **排查"为什么没命中"。** 消息给出类型和候选数;DEBUG 日志(名字暂定 `io.github.xiaoshicae.extension.matching`)逐个打印每个 `match` 的结果。是否提供匹配追踪 API 见 §9。
- **3.x 的对应。** 3.x 默认(`allow-unknown-business=false`)就是这套严格规则;`allow-unknown-business=true`(无命中走默认实现、多命中按 `business-match-order` 选一个)在 4.0 没有对应,见 §8、§9。另一处比 3.x 更严:`requires` 在请求期校验(第 4 条)。

## 6. 七个典型场景

### 场景 1:60 秒上手(Spring Boot,请求体里有业务码)

```java
@ExtensionPoint
public interface FreightCalcExtension {
    default BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("10.00"); }   // default = 系统兜底
}

@Business(code = "biz.retail")
public class RetailBusiness implements Matcher<CheckoutRequest>, FreightCalcExtension {
    @Override public boolean match(CheckoutRequest r) { return "biz.retail".equals(r.bizCode()); }   // 认领哪些请求
    @Override public BigDecimal calcFreight(OrderContext ctx) { return new BigDecimal("8.00"); }
}

@RestController
class OrderController {
    private final FreightCalcExtension freight;                // 普通注入:路由 Bean 按当前请求的身份回答
    OrderController(FreightCalcExtension freight) { this.freight = freight; }

    @PostMapping("/checkout") @WithIdentity                    // 无值:第一个参数交给各个业务的 match
    public String checkout(@RequestBody CheckoutRequest request) { return "运费: ¥" + freight.calcFreight(request.toContext()); }
}
```

| 请求 | 回答者 | 结果 |
|---|---|---|
| `bizCode=biz.retail` | 业务自己 | 8.00 |
| `bizCode=biz.other`(另一个没有实现该扩展点的业务,它的 `match` 认领了这个请求) | 接口 `default` | 10.00 |
| `bizCode=biz.unknown`(没有业务认领) | —— | `ResolutionException`:`no business matches a CheckoutRequest (2 business matcher(s) evaluated)`;没人处理就是 HTTP 500,要 400 写一个 `@ExceptionHandler` |
| 两个业务都认领了 | —— | `ResolutionException`:`businesses [biz.retail, biz.retail-vip] all match a CheckoutRequest; …` |

业务码在网关设的请求头里、不在请求体里时,不需要 `match`,一行配置(显式身份,不运行 Matcher,§5.6):

```yaml
easy-extension.web.business-header: X-Biz-Code
```

### 场景 2:没有 HTTP:RPC、MQ、定时任务

```java
@DubboService
class OrderFacadeImpl implements OrderFacade {
    private final FreightCalcExtension freight;             // 普通注入,和场景 1 一样
    OrderFacadeImpl(FreightCalcExtension freight) { this.freight = freight; }

    @WithIdentity                                           // 无值:request 交给各个业务的 match
    public Result checkout(CheckoutRequest request) { return Result.ok(freight.calcFreight(request.toContext())); }
}

@Component
class OrderConsumer {
    @RabbitListener(queues = "order")
    @WithIdentity                                           // MQ 同一个写法
    public void onOrder(OrderMessage msg) { ... }
}

@Component @WithIdentity("biz.retail")                     // 固定业务:定时任务
class SettleJob { @Scheduled(cron = "0 0 2 * * *") public void settle() { ... } }
```

没有 HTTP,也就没有请求头配置和那个 filter(只在 servlet 应用里注册)。请求对象交给 `Matcher`;同一个业务要认领 `CheckoutRequest` 和 `OrderMessage` 两种请求,就让它们实现同一个小接口(例如 `OrderFacts`,放在 api 模块里),业务写 `Matcher<OrderFacts>`(一个类只能有一个 P,§5.8)。身份在 RPC 传输层元数据里的写法见 §5.7 末尾。

### 场景 3:复用逻辑:能力

```java
@Ability(code = "ability.free-shipping")
public class FreeShippingAbility implements Matcher<CheckoutRequest>, FreightCalcExtension {
    @Override public boolean match(CheckoutRequest r) { return r.amount() != null && r.amount().compareTo(new BigDecimal("99")) >= 0; }   // 这个请求要不要我:满 99 包邮
    @Override public BigDecimal calcFreight(OrderContext ctx) { return BigDecimal.ZERO; }
}

@Business(code = "biz.fresh", abilities = {"ability.free-shipping"})    // 业务自己没实现的方法,由挂载的能力回答
public class FreshBusiness implements Matcher<CheckoutRequest>, ColdChainExtension { ... }   // 没有实现 calcFreight,于是(满 99 时)包邮能力回答

@Business(code = "biz.retail-plus", overridingAbilities = {"ability.free-shipping"})   // 包邮能力覆盖业务自己的运费逻辑
public class RetailPlusBusiness implements Matcher<CheckoutRequest>, FreightCalcExtension { ... }
```

挂载的能力里:没有 `Matcher` 的,业务被选中后始终启用;有 `Matcher` 的,入口有请求对象时由它的 `match` 决定(§5.8)。**显式身份不运行 Matcher**,启用的是没有 `Matcher` 的能力;点名启用带 `Matcher` 的:`extensions.run(Identity.of("biz.retail-plus").with("ability.free-shipping"), () -> ...)`;只启用指定的几个:`Identity.of("biz.retail-plus").only("ability.free-shipping")`;去掉一个:`Identity.of("biz.retail-plus").without("ability.coupon")`。启用的能力,它 `requires` 的能力也必须启用(§5.1 第 4 步)。

### 场景 4:代码里绑身份、异步与取全部实现

```java
// 代码里绑身份:入口不是 Spring Bean 的方法,或在一个入口里切换到别的业务
extensions.run("biz.retail", () -> handle(message));

// 换线程要带身份,框架不自动覆盖任何线程池(§5.3)
CompletableFuture.runAsync(() -> audit.record(order), Extensions.wrap(executor));   // 方式一:包住任务或执行器
taskExecutor.setTaskDecorator(new ExtensionTaskDecorator());                          // 方式二:给 Spring 的线程池设置装饰器

// 取全部实现并聚合(替代 invokeAll / invokeReduce;不含接口 default 体,见 §5.2)
BigDecimal discount = extensions.all(PromotionCalcExtension.class).stream()
        .map(e -> e.calcPromotion(ctx)).reduce(BigDecimal.ZERO, BigDecimal::add);
```

### 场景 5:必选的扩展点,以及要注入 Bean 的默认

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

### 场景 6:测试

```java
// 纯 Java:不起 Spring
Extensions extensions = Extensions.of(new RetailBusiness());
FreightCalcExtension freight = extensions.extension(FreightCalcExtension.class);

@Test void retailPaysEight() {
    assertEquals(new BigDecimal("8.00"), extensions.call("biz.retail", () -> freight.calcFreight(ctx)));   // 显式身份:没有 Matcher 的能力启用
}

// 测 Matcher 本身:有代表性的请求对象 → 命中的业务和启用的能力一起断言(Identity 按内容相等)
@Test void retailRequestWithCoupon() {
    assertEquals(Identity.of("biz.retail").only("ability.coupon"), extensions.identityOf(new CheckoutRequest("biz.retail", ...)));
}

// Spring 测试:直接指定身份,不需要构造请求(自动注册的 TestExecutionListener 处理,§5.7)
@SpringBootTest
@WithIdentity("biz.retail")
class CheckoutTest { ... }
```

`Extensions.of(...)` 与启动时走同一套校验,所以测试里同样会得到聚合的启动期错误。

### 场景 7:组合订单:一个订单跨两个业务

一次执行只有一个业务身份,所以跨业务的订单有两种做法:**拆开执行再汇总**,或者**把组合本身建成一个业务**。

```java
@WithIdentity("biz.bundle")                         // 订单入口的身份是"组合业务",订单级规则在它名下
public Result placeOrder(BundleOrder order) {
    // 拆开执行:每个业务一份子订单,各自按自己的规则算,应用汇总
    BigDecimal freight = order.splitByBiz().entrySet().stream()
            .map(e -> extensions.call(e.getKey(), () -> freightExt.calcFreight(e.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    // 订单级规则(合并包邮、合并支付):此刻线程上绑的是 biz.bundle
    return Result.ok(orderPricing.priceOrder(order, freight));
}

// 组合本身是个业务:挂载两边可复用的能力
@Business(code = "biz.bundle", abilities = {"ability.retail-pricing", "ability.fresh-shipping"})
class BundleBusiness implements OrderPricingExtension { ... }
```

- 并列持有两个会话也可以,与线程无关:`Session retail = extensions.open(Identity.of("biz.retail"))`,`retail.extension(FreightCalcExtension.class)`。
- 并行拆分时,每个任务里自己 `extensions.call(biz, ...)`,**不要**用 `Extensions.wrap`:它带过去的是提交方的身份,所有部分都会按提交方的业务回答。
- 入口没有单一业务(组合订单)时,用 `@WithIdentity("biz.bundle")`,或 `extensions.run(Identity.none(), ...)`。
- 拆开的子订单也可以交给 `Matcher`:`extensions.call(extensions.identityOf(sub), () -> ...)`。
- 没有"复合身份"(一条链里同时有两个业务):两个业务实现同一方法时谁回答没有合理的默认,合并策略是业务策略,不是框架该替用户定的。要聚合用 `extensions.all(E)` 或应用里的 reduce。理由见 ADR-0002。
- 跨业务的一致性(一部分成功、另一部分失败怎么补偿)不归框架管;异常照常原样抛出。

### 进阶(第 2、3 层)

```java
// 指标、追踪:拦截器,用法和 3.x 一样
@Bean ExtensionInterceptor metrics(MeterRegistry registry) { ... }

// HTTP 里要自定义身份(例如从 JWT 取):一个 resolver Bean,代替请求头配置;返回 null = 弃权
@Bean IdentityResolver<HttpServletRequest> resolver() {
    return req -> { String t = tenantOf(req); return t == null ? null : Identity.of(t); };     // 显式身份,不运行 Matcher
}
// 要用上业务和能力的 Matcher(例如业务码在请求头、其余在请求体):自己组装匹配参数,再 identityOf(与上一个二选一)
@Bean IdentityResolver<HttpServletRequest> matchingResolver(Extensions extensions) {
    return req -> extensions.identityOf(new OrderFacts(req.getHeader("X-Biz-Code"), ...));
}

// 解释"这个身份下,每个方法由谁回答"(解释生产请求用 identityOf 得到的身份)
Explanation e = extensions.open(extensions.identityOf(sample)).explain(FreightCalcExtension.class);
Explanation base = extensions.open(Identity.of("biz.retail")).explain(FreightCalcExtension.class);    // 显式身份:没有 Matcher 的能力
```

```
GET /actuator/extensions                  → Description:全部扩展点、业务、能力、提供者、提示
GET /actuator/extensions/explain?business=biz.retail&type=com.acme.FreightCalcExtension
                                          → Explanation:每个方法的候选者和被选中者
```

## 7. 周边草图

**Spring starter。**
- **一个**注册器(由自动配置导入):`ClassPathBeanDefinitionScanner` 加 include filter,把带 `@Business` / `@Ability` / `@DefaultProvider` 的类注册成普通 Bean(类上不必再加 `@Component`,加了也无妨)。
- 扫描范围默认是 Boot 的自动配置包,即 `@SpringBootApplication` 所在的包树(与 Spring Data 的约定一致);包树之外的实现才需要 `@ExtensionScan(scanPackages = ...)` 追加。包树之外的业务,表现为"没有业务认领"(匹配模式,没人处理就是 HTTP 500)或"业务码未知"(请求头模式,HTTP 400);包树之外的能力,启动时就报错(业务找不到它)。
- **路由 Bean**(普通注入拿到的)。为"已收集的实现所实现的每个 `@ExtensionPoint` 接口"注册一个 `@Primary` 的 Bean(`Extensions.extension(type)`),不论接口在哪个包、哪个 jar;扫描包树里的 `@ExtensionPoint` 接口同样注册。启动期检查:某个注入点要一个 `@ExtensionPoint` 类型的 Bean,却解析到别的 Bean 或没有 → 失败并提示 `@ExtensionScan`(3.x 的注入点后处理器有同样的检查)。**不要注入 `List<E>`**:`@Primary` 只管单值,集合会连路由 Bean 一起注入;要全部实现用 `extensions.all(E)`。
- `Extensions` Bean 是一个**门面**,没有依赖,所以可以注入到任何地方,包括业务 Bean。真正的注册表在全部单例实例化之后才构建并冻结,再接到门面上:收集(`getBeansWithAnnotation`,AOP 代理感知;`add(目标类, Bean)` 读注解、调用 Bean)、校验、冻结。就绪点是门面自己的 `afterSingletonsInstantiated`:此前调用(构造器、`@PostConstruct`)得到"尚未就绪"的错误;应用自己的 `SmartInitializingSingleton` 与它的先后取决于 Bean 的注册顺序,不要在那里调用扩展点,改用 `ApplicationRunner` 或 `ContextRefreshedEvent`。校验失败(`RegistryException`)发生在 Web 服务器开始接收请求之前。
- **`@WithIdentity`**:starter 注册一个 Advisor,切点同时匹配类上和方法上的注解;通知按字面量得到身份,或调用 `Extensions.identityOf(匹配参数)`(匹配参数的位置按方法缓存),再调用 `Extensions.call`。基于 Spring AOP,不依赖 AspectJ;与场景里的 Dubbo、gRPC、MQ 监听、`@Scheduled` 无关,只要入口是 Bean 的方法。规则见 §5.7。
- **Matcher 索引。** 冻结时从业务和能力的类解析 `Matcher` 的 P(AOP 代理的 Bean 读目标类),运行时按请求类建索引,只评估 P 能接受该类的业务(§5.8)。
- HTTP 请求头 filter(只在 servlet 应用里)、`ExtensionTaskDecorator`(不自动贡献,§5.3)、属性见 §4.6、§5.3、§5.6。

**test-kit(`easy-extension-test`)。** 没有新的公开类型:一个通过 `META-INF/spring.factories` 自动注册的 `TestExecutionListener`,处理 Spring 测试类上的 `@WithIdentity`(§5.7)。`Explanation` 的断言放在进阶里,按需再加。

**Actuator。** 只读的 `extensions` endpoint,序列化 `Description` 与 `Explanation`。UI 另起仓库消费它。

**注解处理器(编译期)。** 报错:`@ExtensionPoint` 不是 `public` 接口;同一模块内业务码或能力码重复;同一个类上标了多个角色注解;`overridingAbilities` 与 `abilities` 里有同一个能力。提示:本模块内引用了找不到的能力码(跨模块的能力只能在启动时校验)。

## 8. 从 3.x 迁移

| 3.x | 4.0 | 自动化 |
|---|---|---|
| `@ExtensionInject X x` | 普通注入 `X x`(路由 Bean 为 `@Primary`) | recipe |
| `IExtensionContext<P>`、`initSession(param)` / `removeSession()` | 入口方法上 `@WithIdentity`(无值:请求对象交给 `Matcher`,用于 RPC、MQ、HTTP 请求体);固定业务(定时任务)`@WithIdentity("biz.x")`;业务码在网关设的请求头里:一行配置;其他:`extensions.run(...)` 或 `extensions.call(extensions.identityOf(param), ...)` | 人工 + 部分 recipe |
| `context.invoke(E.class, e -> ...)` | 注入的 `E`,直接调用 | recipe |
| `invokeAll` / `invokeReduce` | `extensions.all(E.class)` 加 stream。**不含接口 `default` 体**(3.x 的默认实现是链上的一个对象,会被遍历到),见 §5.2 | 人工 |
| `Matcher<P>`、`@MatcherParam`、`allow-unknown-business`、`business-match-order`、`BusinessMatchSelector`(以及 3.4 候选里未发布的 `unknown-business-policy` / `multi-match-policy`) | **`Matcher<P>` 保留**:业务、能力继续实现它,`match` 的写法不变。变化:① P 不再是全局唯一的一个类(`registerMatcherParamClass`、`IExtensionContext<T>`),而是每个类自己的泛型参数;同一个业务要认领几种请求时,让它们实现同一个小接口、`Matcher` 取这个接口;② `@MatcherParam` 不再标在参数类上,改标在入口方法的参数上(默认第一个参数);③ `initSession(param)` → `@WithIdentity`(无值),或 `extensions.identityOf(param)`;④ **只有严格匹配**:无命中、多命中都是 `ResolutionException`。这就是 3.x 的默认(`allow-unknown-business=false`),没有选择器、`business-match-order`、策略枚举;⑤ **显式身份**(不经过 Matcher 得到的身份:字面量、请求头、`extensions.run`、`Identity.of(...)`)不运行 Matcher,启用的是没有 `Matcher` 的能力;⑥ **`requires` 在请求期校验**(3.x 只在挂载时校验):`match` 启用的组合违反 `requires` 的,现在会 `ResolutionException`。设置了 `allow-unknown-business=true` 的应用(无命中走默认实现、多命中按顺序选一个)要人工处理:让各业务的 `match` 互斥;无命中走默认实现,见 §9 第 10 项 | 人工(`match` 本身不用改,但要检查 ④⑥) |
| `@Business(priority, abilities = {"a", "b::10"})` | 按 3.x **实际解析出的数字**比较:数字小于业务自身 `priority` 的能力 → `overridingAbilities`,其余 → `abilities`,各自按数字升序。未写 `::N` 的能力,3.x 自动编为 1、2……(业务默认是 0;业务写了 `priority = 100` 而能力不写数字,能力就在业务前),recipe 要复现这个编号 | recipe(仅注解方式) |
| 手写 `IBusiness` / `IAbility`(含数据驱动的业务) | 不再有这两个接口:改成带注解的类,或 `Extensions.builder().business(...)` / `ability(...)` | 人工 |
| `@ExtensionPointDefaultImplementation class D implements E1, E2` | 把默认体写进接口的 `default` 方法;需要注入的用 `@DefaultProvider` | 人工 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 全部删掉;接口方法不带 `default` 即为必选。`scenarios` / `version` 只用于 admin 展示;接口演进照旧靠新增 `default` 方法 | recipe |
| `IScopedSessionManager`、命名 scope | 一个请求里的多个身份 = 嵌套的 `extensions.call(biz, ...)`,或并列的多个 `Session`(场景 7) | 人工 |
| `ExtensionSessionScope.run / runWith` | `@WithIdentity` / `extensions.run` / `Extensions.wrap` | recipe |
| `SessionException`、`InvokeException` 等 13 个异常 | `ResolutionException`、`ExtensionNotFoundException`、`RegistryException` | recipe(`ChangeType`) |
| 内嵌 admin | Actuator endpoint + 独立的 UI 仓库 | 人工 |
| `-Deasy-extension.legacy-exception-wrapping` | 删除(代理层不再存在) | —— |

迁移时要特别检查五处语义:**带 `default` 方法的扩展点**(路由按方法,见 §5.2)、**能力与业务自身的先后**(3.x 按数字比较:业务默认 0,未编号的能力自动编为 1、2……;4.0 默认业务在前,3.x 里数字更小的能力要进 `overridingAbilities`)、**能力的启用**(入口有请求对象时仍由能力的 `Matcher` 决定,但 `requires` 也在请求期校验,3.x 只在挂载时校验;**显式身份**(请求头、字面量、`extensions.run`)不运行 Matcher,启用的是没有 `Matcher` 的能力,要点名用 `Identity.with` / `only`;见 §5.1、§5.8)、**聚合**(`all(E)` 不含接口 `default` 体,见 §5.2)、**匹配**(4.0 只有严格匹配:3.x 默认就是,设置过 `allow-unknown-business=true` 或 `business-match-order` 的要处理,见上表)。

## 9. 这份草图还没回答的问题

ADR-0001 §9 的六项开放问题里,草图暂定了三项:Q1 取全部实现 = `Extensions.all`(§5.2);Q3 沿用 `io.github.xiaoshicae.extension.core` 包名;Q4 缓存默认 10000 条、LRU(§5.1)。Q2(`Matcher` 放哪):`Matcher<P>` 接口在 core,匹配在 `Extensions.identityOf`,不另建模块(ADR-0002 S12)。Q5(响应式)、Q6(只读页面)仍开放。这份草图自己还有:

1. `overridingAbilities` 这个名字偏长,但方向不会读反:`first` 会被读成"`abilities` 里的第一个",`overrides` 会被读成"业务覆盖能力"(那是默认行为)。备选 `overriddenBy`。
2. 提供者在 `Explanation` 和 `Invocation` 里的标识:现在用类名,是否需要一个显式的 `code`。
3. `Identity.of(null)` 的校验用 `ResolutionException`(与"身份非法"一致)还是 `IllegalArgumentException`。
4. 请求期收窄后违反 `requires`:现在抛 `ResolutionException`(§5.1 第 4 步);备选是自动补上被依赖的能力。抛异常更显式。
5. `all(E)` 不含接口 `default` 体(§5.2)。需要"含默认值的聚合"的用户,现在只能自己在 stream 里再加一项。
6. `@WithIdentity` 的匹配参数现在是"第一个参数,或标了 `@MatcherParam` 的参数"。入口要从多个参数组装匹配参数时(例如业务码在请求头、其余在请求体),现在要自己调 `extensions.identityOf(param)`。是否给一个声明式的组装入口,例如 `@WithIdentity(param = ...)`(那又会引入表达式语言)。
7. 是否支持零注解的入口绑定:用配置给一个切点表达式和一个身份表达式,例如 `easy-extension.entry.pointcut` 加 `easy-extension.entry.business`。省掉每个入口方法上的注解,代价是又多一套配置语法,现在不做。
8. Dubbo、gRPC 等传输层元数据的适配(§5.7 末尾)是否出独立模块,以及要不要提供客户端侧把当前身份透传给下游的拦截器。现在只给配方。
9. 首个 GA 是否瘦身(编译期处理器、BOM、迁移 recipe 放到 4.x),见 ADR-0002 §4,留给维护者决定。
10. **兜底业务。** 3.x 的 `allow-unknown-business=true`(无命中走默认实现)在严格匹配下没有等价物:一个恒真的 `match` 会与其他业务重叠(它得排除所有别的认领者)。不加全局开关(2026-10-03 已确认,针对请求头缺失和业务码未知;Matcher 的无命中按同一精神处理,待确认);如要支持,建议 `@Business(fallback = true)`:其他业务都不命中时才命中,每个 P 至多一个,启动期校验。待定。其他入口要默认层:HTTP 里 resolver 返回 `Identity.none()`,其余入口用 `extensions.run(Identity.none(), ...)`。
11. **匹配追踪。** 是否提供 `Extensions.explainMatch(param)` 这类 API(3.x 的 `ResolveTrace` 的位置),现在只有异常消息和 DEBUG 日志。
12. **能力的两套来源。** 能力既可以由 `Matcher` 判断,也可以由请求头 `only(...)` 指定(`web.abilities-header`,网关断言)。两套并存是否值得,或只留 `Matcher`;请求头的信任边界见 §5.6。
13. **`@Business` 上按码匹配的简写。** 多数业务的 `match` 只是 `"biz.x".equals(r.bizCode())`,重复了 `code`。是否在 `@Business` 上给一个按码匹配的简写(需要读请求对象里的码,又会引入约定或表达式),现在不做。

维护者的意见:2026-10-03,请求头缺失或业务码未知时不加"回落到默认层"的开关(已确认)。2026-10-04,维护者倾向恢复 `Matcher`:"这个太不优雅了，还是之前的matcher优雅，business要不要也恢复成matcher的形式呢?"、"感觉还是matcher实现更优雅"。这是倾向,不是逐条的决定:"业务和能力都恢复 Matcher"以及下面这些都是我据此写的提议,待确认:匹配规则(严格单命中、无开关)、`@WithIdentity` 的两种写法与取消表达式语言和 `only` / `without` 属性、`@MatcherParam` 标参数、显式身份下带 `Matcher` 的能力默认不启用(`Identity.with` 点名)、请求头已绑定的业务与匹配得到的业务冲突时报错。`@WithIdentity` 的名字不改(入口和测试用同一个注解,"with identity"读起来是"在这个身份下执行",不暗示线程绑定这个实现细节)。
