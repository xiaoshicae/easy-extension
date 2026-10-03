# ADR-0002:用户面简化

| | |
|---|---|
| 状态 | **提议**(2026-10-03),待维护者评审;通过后改为"已采纳",并生效对 ADR-0001 的修订 |
| 适用 | 4.0 的用户面:注解、`Identity`、`Extensions`、Web 绑定、异步、测试 |
| 修订 | [ADR-0001](0001-v4-architecture.md) 的 D1、D3;缩小 D4、D7 的范围 |
| 配套 | [4.0 API 草图](../design/v4-api-sketch.md)(已按本文重写)、README「4.0 新用法预览」 |
| 触发 | 维护者对 4.0 预览的反馈:"看起来有点复杂"。要求:暴露给用户的尽量简单,复杂留给框架 |

## 1. 背景

ADR-0001 把**内部**做简单了,但配套预览里,用户**要先理解的概念并没有变少**,只是换了名字:

| 预览里的写法 | 用户被迫理解的概念 | 实际需求 |
|---|---|---|
| `@Business(chain = {"ability.x", Business.SELF})` | "链"、哨兵常量 `SELF` | 绝大多数业务不需要排序:业务先答,能力补充。只有少数能力要"压过"业务 |
| `class X implements IdentityResolver<HttpServletRequest>` | resolver | 多数 Web 应用的身份就是一个请求头 |
| `Identity.of(biz, abilitiesOf(req))` | "本次启用哪些能力" | 能力已经由业务挂载,启用关系不必每次重述 |
| `Session.require()`、`session.wrap(...)` | 会话、线程绑定 | 线程池和 `@Async` 沿用请求身份,框架自己能做 |
| `ExtensionsTest`、`assertRoute(...).selects(...)` | 一套测试断言 DSL | 指定身份、调用、用 JUnit 断言 |
| `reloadableExtensions.replace(...)` | 注册表会变、代际 | 没有用例催着要 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 三个属性 | `mandatory` 已被"抽象方法即必选"取代;另两个只给 admin 展示(`ExtensionInfoService`),而 admin 已拆出主线 |

跑通第一个请求,预览要求 **4 个类**(扩展点、能力、业务、resolver)和 **7 个概念**(`@ExtensionPoint`、`@Ability`、`@Business`、`chain`、`SELF`、`IdentityResolver`、`Identity`)。

用户面的预算按**最常见的路径**算,不按"能表达的全部场景"算:**常见情况零代码,少见情况才引入概念。**

## 2. 决策

| # | 决策 | 与 ADR-0001 的关系 |
|---|---|---|
| S1 | 用户面分三层;每个用户只需要第 1 层的 5 个类型(`@ExtensionPoint`、`@Business`、`@Ability`、`Identity`、`Extensions`)。主路径上不出现会话、resolver、"链" | 新增 |
| S2 | `@Business(code, abilities, first)`:业务先于它的能力回答;`first` 列出要先于业务回答的能力 | 取代 D3 的 `chain` + `SELF` |
| S3 | `Identity.of(business)` 默认启用业务挂载的**全部**能力;`only(...)` / `without(...)` 收窄 | 修订 D1:不再每次列出启用的能力 |
| S4 | Web 绑定零代码:配置请求头名即可。身份不在请求头里,才写 `IdentityResolver`(第 2 层) | 修订 D1 |
| S5 | 默认扫描 Boot 的自动配置包(`@SpringBootApplication` 所在包树);`@ExtensionScan` 只在追加包时用 | 新增 |
| S6 | 异步沿用身份是框架的事:Spring 的线程池和 `@Async` 自动处理;其余用 `Extensions.wrap(...)`。`Session` 降到第 3 层 | 取代预览里的 `Session.require()` |
| S7 | 测试:`Extensions.of(...)` 与 `@WithIdentity`。不做路由断言 DSL | 取代预览里的 `ExtensionsTest` |
| S8 | 4.0 不提供:热替换;`@ExtensionPoint` 的任何属性 | 缩小 D4、D7 的范围 |

回答顺序固定,不可配置:**启用的 `first` → 业务自己 → 启用的 `abilities` → `@DefaultProvider` → 接口 `default`。**

## 3. 复杂留给框架

| 以前要用户做或知道 | 现在 |
|---|---|
| 写 resolver 读请求头 | Web filter 读配置的头 |
| 每次列出本次启用的能力 | `Identity.of(business)` 默认全部挂载 |
| 声明扫描包 | 默认扫 Boot 的自动配置包 |
| 把身份带进线程池 | `TaskDecorator` 自动;自建 `Executor` 一行 `wrap` |
| 注入 `Extensions` 时顾虑循环依赖、用 `ObjectProvider` | 注入的是门面,冻结的注册表在全部单例创建后接上;业务 Bean 直接注入 |
| 启动期问题逐个排查 | 一次聚合报出全部(`RegistryException#problems()`) |
| 弄清"为什么是它回答" | 顺序规则固定;细节只在 `Explanation` 里暴露 |
| AOP 代理、注入歧义、接口 `default` 方法的调用 | 感知 AOP;路由 Bean 为 `@Primary`;`invokeDefault` |

## 4. 后果

### 好处

- 第一个请求:**2 个类**(扩展点、业务)+ **1 行配置**,**2 个概念**(`@ExtensionPoint`、`@Business`)。能力、resolver、拦截器、会话都是用到再学。
- 第 1 层 5 个类型。顶层公开类型共 16 个,另有可选的 `matching` 子包 2 个;ADR-0001 D7 的预算"约 15"定为 **18**,用 ArchUnit 守住。
- 迁移规则更机械:`@Business(priority, abilities = {"a::10"})` 中,优先级小于业务自身的能力进 `first`,其余进 `abilities`,各自按数字升序。

### 代价与待办

1. **能力的启用语义变了。** 3.x 每个能力在每次请求里用 `match(param)` 决定是否启用;4.0 的 `Identity.of(business)` 默认挂载的全部启用。要"同一个业务、不同请求启用不同能力"的用户用 `only` / `without`,或继续用 Matcher 风格:`MatcherIdentityResolver` 把各能力 `match()` 的结果映射成 `only(...)`(只在业务挂载的范围内取交集)。迁移里最需要人看的一处,写进迁移指南。
2. **请求头缺失时不绑定身份。** 忘了带头的请求,在调用扩展点处才失败,不是在入口处 400;错误消息说明怎么办。要入口即拒绝,写一个 `IdentityResolver` Bean。是否加开关见草图 §9。
3. **自动沿用身份只覆盖 Spring 管理的线程池和 `@Async`。** `new Thread`、自建的 `ExecutorService`、`CompletableFuture` 的默认池要自己 `Extensions.wrap(...)`。没有身份时的报错消息指向 `wrap`,文档在"异步"一节写明。另外,Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用它(多于一个则都不生效),Boot 4 会把多个组合起来(两个版本的自动配置已核对):应用已有自己的 `TaskDecorator` 时,starter 在 3.5 上于启动期给出提示并说明怎么组合,P2 的容器测试覆盖这个组合。
4. **`first` 这个名字**可能被读成"`abilities` 里的第一个"。备选 `precedence`、`overriding`;改名不影响语义,评审时定。
5. **4.0 不提供运行时热替换。** 注册表不可变、路由代理与注册表无关,以后加这个能力不需要改 API。
6. **默认只扫 Boot 的自动配置包。** 包树之外的业务和能力(例如别的 jar 里的另一个包),要用 `@ExtensionScan(scanPackages = ...)` 追加,与 Spring Data 的约定一致。

## 5. 对 ADR-0001 的修订

| ADR-0001 | 变化 |
|---|---|
| D1 | `Identity.of(biz, abilities…)` → `Identity.of(biz)`(全部挂载的能力)加 `only` / `without`;Web 绑定零代码;`IdentityResolver` 降到第 2 层。显式身份、`Matcher` 降级为可选,不变 |
| D3 | `chain` + `SELF` → `abilities` + `first`。声明顺序代替数字,不变 |
| D4 | `@ExtensionPoint` 的 `scenarios` / `version` 一并去掉(§8 原写"保持为提示性元数据",它们只被 admin 展示用到) |
| D7 | 公开类型预算:"约 15" → 18;第 1 层 5 个 |
| §4 差异 2 | 改为:`only` / `without` 里列了业务没挂载的能力 → `ResolutionException` |
| §4 新增差异 10 | 能力默认全部启用(原由各能力的 `match()` 决定) |
| §5 风险"业务 Bean 注入 `Extensions`" | 缓解改为:注入的是门面,业务侧不需要 `ObjectProvider` |
| §9 开放问题 1 | "取全部实现":现在是 `Extensions.all`(第 1 层),`Session.all` 在第 3 层 |

D0、D2、D5、D6、D8、D9 不受影响。

## 6. 否决的方案

- **保留 `chain` + `SELF`,只给默认值("不写 = 业务在前")。** 哨兵常量和两套写法还在;`first` 把"例外"直接命名,默认情况什么都不用写。
- **只提供 resolver,不做零代码的 Web 绑定。** 每个 Web 应用都要写同样的几行代码。
- **`Identity` 继续列出启用的能力。** 重复表达业务已经声明的挂载关系。
- **路由断言 DSL(`assertRoute`)。** `Explanation` 是普通 record,用 JUnit 断言即可;DSL 是又一套 API。
- **把"能力是否启用"仍交给每个能力的 `match()`。** `Matcher` 回到核心,ADR-0001 D1 的收益落空。
- **请求头缺失时自动绑定 `Identity.none()`。** 忘带头的请求会被默认层静默服务,问题被藏起来。

## 7. 重新评估的触发条件

- 出现"忘了带头"类的线上问题 → 加开关(草图 §9)或改默认。
- 出现按请求属性给同一业务选能力的主流用例 → 扩展 `Identity`(仍用 `only` / `without`),不把 `Matcher` 放回核心。
- 用户确有运行时换实现的需求 → 加热替换(不改现有 API)。
