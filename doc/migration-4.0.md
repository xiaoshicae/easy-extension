# 从 3.x 迁移到 4.0

4.0 **不向后兼容**,没有桥接版。概念不变(扩展点 / 能力 / 业务 / 默认实现),变的是表达方式。设计背景见 [design-4.0.md](design-4.0.md)。
admin、注解处理器、IntelliJ 插件需与 core 同步升级到 4.0.0。

## 速查表

| 3.x | 4.0 |
|---|---|
| `@ExtensionPointDefaultImplementation`(全局唯一,实现所有扩展点) | `@DefaultImplementation`,每个扩展点至多一个,一个类可兜底多个扩展点 |
| `@Business(priority = 100, abilities = {"ability.x::10"})` | `@Business(abilities = {XAbility.class, Self.class})`:属性名不变,但元素从字符串变成能力**类**,数组顺序即优先级,`Self.class` 是业务自身的位置 |
| `@Ability(requires = {"code"}, excludes = {"code"})` | `requires` / `excludes` 为 `Class<?>[]`(同时挂载语义) |
| 业务/能力实现 `IBusiness` / `IAbility` / `Identifier` 的方法 | 删除;`code` 写在注解上,缺省为类全名;只需实现 `Matcher<T>` 与扩展点 |
| `IExtensionContext` / `IExtensionReader` / `IExtensionRegister` | `ExtensionContext<T>`(不可变,由 builder 构建);元数据读 `context.catalog()`;**无**运行期注册 |
| `context.initSession(param)` + 隐式 ThreadLocal | `try (Binding b = context.bind(param)) {...}`,或提供 `MatcherParamResolver` Bean 自动绑定;`context.resolve(param)` 得到不可变 `Resolution` |
| `initScopedSession(scope, param)` / named scope | 删除。持有多个 `Resolution`,临时换身份用嵌套 `bind(resolution)` |
| `getLastResolveTrace()` | `resolution.trace()` / `resolution.explain(point)` |
| checked `RegisterException` / `QueryException` | 全部 unchecked:`RegistrationException`(构建期)、`ResolutionException`(带 `Reason`) |
| 业务匹配多个时的隐式顺序 | 严格模式(缺省)报 `MULTIPLE_BUSINESSES_MATCHED`;`business-match-order` 仅在非严格模式下选择 |
| `easy-extension.enable-log` | 删除;把 `io.github.xiaoshicae.extension.core.internal.Resolver` 的日志级别调到 DEBUG 看每次请求的匹配过程 |
| `@ExtensionScan` 必须配置(`scanPackages`) | 零配置自动扫描 `AutoConfigurationPackages`;`@ExtensionScan(basePackages = ...)` 仅补充范围(扩展点/业务不在应用包下时需要) |
| `@MatcherParam`(标注参数类) | 删除;参数类型由 `Matcher<T>` 的泛型推导,或用 `easy-extension.matcher-param-type` 指定 |
| 非 Spring:`DefaultExtensionContext` + `ExtensionContextRegisterHelper` + `AbstractBusiness`/`AbstractAbility` | `ExtensionContext.<T>builder().extensionPoint(..).defaultImplementation(..).ability(..).business(..).build()`;扩展点接口必须标注 `@ExtensionPoint`,业务/能力用注解而不是重写 `code()`/`priority()`/`usedAbilities()` |
| JDK 17 | JDK 21 |

## 步骤

1. **升级依赖**到 `4.0.0`(core / starter / admin / annotation-processor 版本一致),IntelliJ 插件升到对应版本。
2. **基线**:JDK 21、Spring Boot 4.x(Spring 7)。
3. **默认实现**:把原来"实现所有扩展点"的默认实现类保留,改注解为 `@DefaultImplementation`。可以拆成多个类,每个扩展点至多一个兜底。只有 `void` 方法的扩展点不必写兜底(框架提供空实现);单方法扩展点可用 `@Bean @DefaultImplementation` 的 lambda。
4. **业务/能力**:
   - 删除对 `IBusiness` / `IAbility` 方法的重写,`code` 写进注解;
   - `priority` 与 `"code::n"` 字符串换成 `abilities` 数组:原来能力优先级数字小于业务自身的,放在 `Self.class` 之前;大于的放之后;
   - `requires` / `excludes` 从 code 字符串改成能力类。
5. **会话**:
   - Web 应用:提供 `MatcherParamResolver<T>` Bean,删掉各处手写的 `initSession`;
   - 其他场景:`try (Binding b = context.bind(param)) { ... }`;跨线程传递 `Resolution`,在目标线程 `bind(resolution)`。
6. **启动期校验**:4.0 在启动时一次性校验全部装配(缺少兜底、code 重复、`abilities` 引用不存在、层次里出现未被扫描的 `@ExtensionPoint` 等),原本静默回落到默认实现的错误现在会直接报 `RegistrationException`。
7. **依赖**:代码中不要引用 `io.github.xiaoshicae.extension.core.internal`,该包不承诺兼容。
8. **能力里使用 `@ExtensionInject`**:不再有依赖环,无需 `@Lazy` 之类的绕开写法。

## 迁移示例

```java
// 3.x
@Business(code = "biz.retail", priority = 100,
        abilities = {"ability.free-shipping::10", "ability.return-7d::20"})
public class RetailBusiness implements Matcher<OrderMatchParam>, OrderValidateExtension { ... }

// 4.0: 能力的数字 < 100,所以排在业务自身(Self)之前
@Business(code = "biz.retail",
        abilities = {FreeShippingAbility.class, Return7DaysAbility.class, Self.class})
public class RetailBusiness implements Matcher<OrderMatchParam>, OrderValidateExtension { ... }
```

```java
// 3.x: 手写 Interceptor + IExtensionSession.initSession/removeSession
// 4.0: 一个 Bean 即可,框架自动绑定、解绑
@Bean
MatcherParamResolver<OrderMatchParam> resolver() {
    return request -> new OrderMatchParam(request.getParameter("bizCode"));
}
```

完整的迁移范例见示例仓库 [easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)(`master` 分支已升级到 4.0)。

## 行为差异提醒

- 一个业务可以**不实现任何扩展点**(只负责识别请求,所有扩展点走默认实现);能力和默认实现至少要实现一个。
- 扩展点从实现类的**完整类型层次**推导(父类、接口、父接口),不再只看直接接口。
- `Resolution` 在创建时对所有挂载能力立即求值,是该时刻的快照,不要跨请求缓存。
- `allow-unknown-business=true` 时,未知/无匹配业务只走各扩展点的默认实现。
- 管理后台 JSON 中的 `priority` 现在是 `abilities` 中的位置序号(0 最优先)。
- 不要按名字引用 starter 为扩展点注册的代理 Bean(`xxx#FirstMatchedExtensionProxy` 等,现在用接口全限定名);用 `@ExtensionInject` 或 `ExtensionContext.proxy(...)`。
- 对未注册的扩展点调用 `proxy()` 抛 `ResolutionException(EXTENSION_NOT_FOUND)`。
- 响应式栈不自动绑定,显式传递 `Resolution`。
