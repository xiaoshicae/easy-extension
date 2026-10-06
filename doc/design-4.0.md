# easy-extension 4.0 设计

> 4.0 是一次**不保证兼容**的大升级:保留领域概念和解析语义,重写它们的表达方式。
> 本文是实现规格;评审过程与取舍见 PR 描述。

## 1. 为什么改

3.x 的概念模型(扩展点 / 能力 / 业务 / 默认实现 + 优先级链)是对的,问题在表达:

| # | 3.x 的问题 | 4.0 的解法 |
|---|---|---|
| 1 | 一个对象身兼三职(匹配条件 + 元数据 + 实现),`implementExtensionPoints()` 要手写;扩展点只看**直接接口**,实现来自父类/子接口时**静默回落到默认实现** | 用户只写领域接口;扩展点从**完整类型层次**自动推导;注册期校验 |
| 2 | 优先级是全局整数 + 字符串 DSL(`"ability.x::10"`),业务与能力共用数轴 | **位置即优先级**:`@Business(abilities = {A.class, Self.class})` |
| 3 | 默认实现必须是实现**所有**扩展点的唯一单例 | **按扩展点兜底**,可有多个 `@DefaultImplementation` |
| 4 | 会话是隐式 ThreadLocal;scope 让 API 变 3 套;能力里用 `@ExtensionInject` 会**依赖环启动失败** | `Resolution` 不可变值对象 + 可嵌套的 `bind`;删除 named scope;context 在所有单例就绪后才构建 |
| 5 | 62 个 public 顶层类型,含 5 对单实现 manager、3 个代理工厂 | API 与 `internal` 分离,ArchUnit 强制 |
| 6 | starter 自研第二扫描根 + 3 种 Holder + 多个 BPP/installer | 零配置自动扫描,一个扫描器 |

## 2. 已定决策

- 不支持运行期动态注册:没有 `IExtensionRegister`,只有 `Builder` 构建出的**不可变** context。
- 删除 named scope:同请求多身份 = 持有多个 `Resolution`;注入代理要临时换身份用**嵌套 `bind`**。
- 业务自身排位用 `Self.class` 位置标记。
- 不考虑向后兼容:直接删除,不保留 deprecated 别名,不做桥接版。
- core **零 Spring 依赖**(JDK + slf4j)。因此注解**不能**元注解 `@Component`;starter 负责扫描。

## 3. 用户视角

```java
@ExtensionPoint
public interface FreightCalc { BigDecimal calc(OrderContext ctx); }

@DefaultImplementation                       // 每个扩展点至多一个;一个类可兜底多个扩展点
public class DefaultFreight implements FreightCalc { ... }

@Ability                                     // code 缺省 = 类全名
public class FreeShipping implements Matcher<OrderParam>, FreightCalc { ... }

@Business(code = "biz.retail", abilities = { FreeShipping.class })   // 数组顺序 = 优先级;业务自身缺省最前
public class RetailBusiness implements Matcher<OrderParam>, FreightCalc { ... }
//  让能力盖过业务自身:  abilities = { FreeShipping.class, Self.class }

// Spring:提供一个 Bean 即可自动 bind / close
@Bean MatcherParamResolver<OrderParam> resolver() { return OrderParam::from; }
```

```java
// 不用 Spring
ExtensionContext<OrderParam> ctx = ExtensionContext.<OrderParam>builder()
        .extensionPoint(FreightCalc.class)
        .defaultImplementation(new DefaultFreight())
        .ability(new FreeShipping())
        .business(new RetailBusiness())
        .build();                                  // 一次性校验,失败整体失败;产物不可变

Resolution r = ctx.resolve(param);                 // 纯函数:不可变快照、线程安全
r.first(FreightCalc.class).calc(c);
try (Binding b = ctx.bind(param)) { ... }          // 绑定到当前线程,close 恢复上一个绑定(可嵌套)
```

## 4. 核心 API

### 4.1 注解(`core.annotation`)

| 注解 | 属性 |
|---|---|
| `@ExtensionPoint` | `scenarios`、`version`(仅展示用)、— |
| `@Ability` | `code`(缺省类全名)、`requires`/`excludes`(`Class<?>[]`,**同时挂载**语义,不含先后) |
| `@Business` | `code`(缺省类全名)、`abilities`(`Class<?>[]`:能力类或 `Self.class`) |
| `@DefaultImplementation` | 标在类上,或(Spring)标在 `@Bean` 方法上;编程式用 `builder.defaultImplementationFor(point, impl)`。只有 `void` 方法的扩展点无需声明,框架提供空实现 |
| `Self` | `abilities` 里的位置标记类 |

### 4.2 运行时(`core`)

```java
interface ExtensionContext<T> {
    static <T> ExtensionContextBuilder<T> builder();
    Resolution resolve(T param);
    Binding bind(T param);  Binding bind(Resolution resolution);   // 后者用于跨线程传递
    Resolution current();                                           // 无绑定 → ResolutionException(NO_BINDING)
    void clear();                                                   // 清空本线程所有绑定(兜底清理用)
    <E> E first(Class<E> point);  <E> List<E> all(Class<E> point);  // = current().first / all
    <E,R> R invoke(...); <E,R> List<R> invokeAll(...); <E,R> R invokeReduce(...);
    <E> E proxy(Class<E> point);        <E> List<E> proxyAll(Class<E> point);   // 注入用:每次调用读当前绑定
    ExtensionCatalog catalog();
}
interface Resolution { first / all / invoke / invokeAll / invokeReduce / trace() / explain(Class) }
interface Binding extends AutoCloseable { Resolution resolution(); void close(); }
```

### 4.3 SPI(`core.spi`)
- `BusinessResolver<T>`:`Optional<String> resolve(T param)`,按 code 直达业务,O(1)。配置后业务**不必**实现 `Matcher`,且**完全忽略** `Matcher`;返回 empty 不回退线性匹配。
- `BusinessSelector<T>`:多个业务同时匹配(非 strict)时选一个;缺省 `OrderedCodeBusinessSelector`(按配置的 code 顺序,否则取先注册的)。

### 4.4 只读元数据(`core.catalog`)
`ExtensionCatalog` 取代 `IExtensionReader`:`extensionPoints() / abilities() / businesses() / defaultImplementations() / matcherParamType()`。条目是 record,`BusinessInfo` 含有序的 `mounts`(能力 code 或 Self)。

### 4.5 异常(`core.exception`,全部 unchecked)
`ExtensionException`(根)→ `RegistrationException`(构建期)、`ResolutionException`(带 `Reason`:`NO_BINDING / NO_BUSINESS_MATCHED / MULTIPLE_BUSINESSES_MATCHED / BUSINESS_NOT_FOUND / EXTENSION_NOT_FOUND`)。

## 5. 语义规则

**解析链**:`[业务与其挂载的能力,按 abilities 位置]` → 每个扩展点的兜底。
- 业务匹配:有 `BusinessResolver` 则按 code;否则遍历业务 `match(param)`。
- strict(缺省):0 个匹配 → `NO_BUSINESS_MATCHED`;>1 个 → `MULTIPLE_BUSINESSES_MATCHED`。非 strict:选择器选一个,没有则只走兜底。
- 能力是否生效:业务挂载 **且** `ability.match(param)` 为真;`resolve` 时对所有挂载能力**立即求值**,`Resolution` 是该时刻的快照,**不要跨请求缓存**。
- `first(point)`:按链顺序找第一个实现了该扩展点的;都没有则用兜底。每个已注册的扩展点都有兜底,因此不会失败(未注册的扩展点 → `EXTENSION_NOT_FOUND`)。
- `all(point)`:链中所有实现者按序,最后追加兜底。
- `Self` 缺省在最前;至多出现一次;`abilities` 中能力不得重复、必须已注册。

**扩展点推导**:基于**用户类**(Spring 下由 starter 传入 AOP 代理的目标类),遍历父类、接口、接口的父接口,收集被 `@ExtensionPoint` 直接标注的接口。`A extends B` 且都标注:实现 `A` 同时也是 `B` 的实现。能力与默认实现至少实现一个扩展点(业务可以一个都不实现,只负责识别请求,所有扩展点落到兜底);类型层次里出现**未注册**的 `@ExtensionPoint` → `RegistrationException`(提示加入扫描范围)。

**构建期校验**(`build()` 一次性):扩展点是 public 接口;每个扩展点**恰有一个**兜底(同一扩展点多个兜底 → 报错;没有兜底时,只有 `void` 方法的扩展点由框架提供空实现,其余报错);code 唯一;`abilities`/`requires`/`excludes` 引用存在;requires/excludes 在每个业务的挂载集合上成立;能力/业务有 `Matcher`(业务有 `BusinessResolver` 时除外)。

**Binding**:每线程一个栈;`close` 恢复上一个绑定,重复 close 幂等;`clear()` 清空整个栈。`proxy(point)` 每次调用读栈顶;无绑定 → `ResolutionException(NO_BINDING)`(信息含线程名)。`toString()` 在无绑定时返回描述串,不抛异常。

**matcherParamType**:builder 可显式给出;否则从 Provider 的 `Matcher<T>` 泛型尝试推导,不一致则构建失败;推导不出为 `null`(admin 需容忍)。

## 6. 包结构

```
core/              ExtensionContext, ExtensionContextBuilder, Resolution, Binding
core/annotation/   ExtensionPoint, Ability, Business, DefaultImplementation, Self
core/interfaces/   Matcher
core/spi/          BusinessResolver, BusinessSelector, OrderedCodeBusinessSelector
core/definition/   AbilityDefinition, BusinessDefinition, DefaultImplementationDefinition(编程式装配)
core/catalog/      ExtensionCatalog 及 *Info
core/trace/        ResolveTrace, ExtensionExplanation   (priority → position)
core/exception/    ExtensionException, RegistrationException, ResolutionException
core/internal/     实现细节,不承诺兼容;starter/admin 禁止依赖(ArchUnit)
```

## 7. Spring starter

- **构建时机**:`ExtensionContext` 在所有单例初始化完成后由 `SmartInitializingSingleton` 构建;注入代理(`proxy`)调用时才解析 context。→ 能力/业务里使用 `@ExtensionInject` 不再有依赖环。
- **零配置扫描**:一个扫描器,缺省范围 = `AutoConfigurationPackages`,同时识别 `@ExtensionPoint` 接口与 `@Ability/@Business/@DefaultImplementation` 类;`@ExtensionScan(basePackages)` 仅用于补充范围。
- **注入**:`@ExtensionInject` 支持字段、构造器/方法参数(`X` → first,`List<X>` → all)。
- **自动会话**:提供 `MatcherParamResolver<T>` Bean 时,注册 `HandlerInterceptor`:`preHandle` 绑定、`afterCompletion` 解绑;`SessionCleanupFilter` 保留为兜底 `clear()`(仅 REQUEST 分发)。reactive 不自动支持,文档说明显式传递 `Resolution`。
- **matcher 参数类型**:用 `ResolvableType` 从 Provider 的 `Matcher<T>` 汇总;可用 `easy-extension.matcher-param-type` 覆盖。
- **配置**:`allow-unknown-business`、`business-match-order` 保留;`enable-log` 删除(用 logger 级别)。

## 8. admin / 注解处理器 / IntelliJ 插件

随 4.0.0 同版本发布:admin 改读 `ExtensionCatalog`(`priority` 字段输出位置序号,前端不变);处理器输出 `metadata.json` v2(`abilities`/`Self`);插件把三处 `"::"` 解析收敛成一个函数,识别 `abilities`/`Self`。

## 9. 实施阶段

1. core 重写(含测试;删除旧实现)
2. starter 重写
3. admin / 处理器 / 插件
4. README、迁移指南、CHANGELOG、版本号(`/release-prep`,不发布)

每阶段保持所属模块测试全绿;在 `release/4.0` 集成分支上进行,全部完成后再合并到 `main`。
