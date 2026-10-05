# 从 3.x 迁移到 4.0

4.0 **不向后兼容**,没有桥接版。概念不变(扩展点 / 能力 / 业务 / 默认实现),变的是表达方式。设计背景见 [design-4.0.md](design-4.0.md)。
admin、注解处理器、IntelliJ 插件需与 core 同步升级到 4.0.0。

## 速查表

| 3.x | 4.0 |
|---|---|
| `@ExtensionPointDefaultImplementation`(全局唯一,实现所有扩展点) | `@DefaultImplementation`,每个扩展点至多一个,一个类可兜底多个扩展点 |
| `@Business(priority = 100, abilities = {"ability.x::10"})` | `@Business(uses = {XAbility.class, Self.class})`,数组顺序即优先级 |
| `@Ability(requires = {"code"}, excludes = {"code"})` | `requires` / `excludes` 为 `Class<?>[]`(同时挂载语义) |
| 业务/能力实现 `IBusiness` / `IAbility` / `Identifier` 的方法 | 删除;`code` 写在注解上,缺省为类全名;只需实现 `Matcher<T>` 与扩展点 |
| `IExtensionContext` / `IExtensionReader` / `IExtensionRegister` | `ExtensionContext<T>`(不可变,由 builder 构建);元数据读 `context.catalog()`;**无**运行期注册 |
| `context.initSession(param)` + 隐式 ThreadLocal | `try (Binding b = context.bind(param)) {...}`,或提供 `MatcherParamResolver` Bean 自动绑定;`context.resolve(param)` 得到不可变 `Resolution` |
| `initScopedSession(scope, param)` / named scope | 删除。持有多个 `Resolution`,临时换身份用嵌套 `bind(resolution)` |
| `getLastResolveTrace()` | `resolution.trace()` / `resolution.explain(point)` |
| checked `RegisterException` / `QueryException` | 全部 unchecked:`RegistrationException`(构建期)、`ResolutionException`(带 `Reason`) |
| 业务匹配多个时的隐式顺序 | 严格模式(缺省)报 `MULTIPLE_BUSINESSES_MATCHED`;`business-match-order` 仅在非严格模式下选择 |
| `easy-extension.enable-log` | 删除,用 logger 级别 |
| `@ExtensionScan` 必须配置 | 零配置自动扫描 `AutoConfigurationPackages`;`@ExtensionScan` 仅补充范围 |

## 步骤

1. **升级依赖**到 `4.0.0`(core / starter / admin / annotation-processor 版本一致),IntelliJ 插件升到对应版本。
2. **默认实现**:把原来"实现所有扩展点"的默认实现类保留,改注解为 `@DefaultImplementation`。可以拆成多个类,每个扩展点至多一个兜底;不需要兜底的扩展点标 `@ExtensionPoint(optional = true)`。
3. **业务/能力**:
   - 删除对 `IBusiness` / `IAbility` 方法的重写,`code` 写进注解;
   - `priority` 与 `"code::n"` 字符串换成 `uses` 数组:原来能力优先级数字小于业务自身的,放在 `Self.class` 之前;大于的放之后;
   - `requires` / `excludes` 从 code 字符串改成能力类。
4. **会话**:
   - Web 应用:提供 `MatcherParamResolver<T>` Bean,删掉各处手写的 `initSession`;
   - 其他场景:`try (Binding b = context.bind(param)) { ... }`;跨线程传递 `Resolution`,在目标线程 `bind(resolution)`。
5. **启动期校验**:4.0 在启动时一次性校验全部装配(缺少兜底、code 重复、`uses` 引用不存在、层次里出现未被扫描的 `@ExtensionPoint` 等),原本静默回落到默认实现的错误现在会直接报 `RegistrationException`。
6. **依赖**:代码中不要引用 `io.github.xiaoshicae.extension.core.internal`,该包不承诺兼容。
7. **能力里使用 `@ExtensionInject`**:不再有依赖环,无需 `@Lazy` 之类的绕开写法。

## 行为差异提醒

- 扩展点从实现类的**完整类型层次**推导(父类、接口、父接口),不再只看直接接口。
- `Resolution` 在创建时对所有挂载能力立即求值,是该时刻的快照,不要跨请求缓存。
- `allow-unknown-business=true` 时,未知/无匹配业务只走各扩展点的默认实现。
- 管理后台 JSON 中的 `priority` 现在是 `uses` 中的位置序号(0 最优先)。
- 响应式栈不自动绑定,显式传递 `Resolution`。
