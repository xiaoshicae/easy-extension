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
| `@Business(chain = {"ability.x", Business.SELF})` | "链"、哨兵常量 `SELF` | 要不要压过业务,因能力而异(3.x 的包邮例子就压过业务)。默认(业务先答)与 3.x 未写数字时一致;例外直接命名,不必每个业务都写一遍完整顺序 |
| `class X implements IdentityResolver<HttpServletRequest>` | resolver | 多数 Web 应用的身份就是一个请求头 |
| `Identity.of(biz, abilitiesOf(req))` | "本次启用哪些能力" | 能力已经由业务挂载,启用关系不必每次重述 |
| `Session.require()`、`session.wrap(...)` | 会话、线程绑定 | 线程池沿用请求身份,框架自己能做(范围见 §4 代价 3) |
| `ExtensionsTest`、`assertRoute(...).selects(...)` | 一套测试断言 DSL | 指定身份、调用、用 JUnit 断言 |
| `reloadableExtensions.replace(...)` | 注册表会变、代际 | 没有用例催着要 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 三个属性 | `mandatory` 已被"抽象方法即必选"取代;另两个只给 admin 展示(`ExtensionInfoService`),而 admin 已拆出主线 |

跑通第一个请求,旧预览要求:引入依赖、**4 个类**(扩展点、能力、业务、resolver)和 **7 个要认识的类型或概念**(`@ExtensionPoint`、`@Ability`、`@Business`、`chain`、`SELF`、`IdentityResolver`、`Identity`)。

用户面的预算按**最常见的路径**算,不按"能表达的全部场景"算:**常见情况零代码,少见情况才引入概念。**

## 2. 决策

| # | 决策 | 与 ADR-0001 的关系 |
|---|---|---|
| S1 | 用户面分三层;新手只需要第 1 层的 5 个类型(`@ExtensionPoint`、`@Business`、`@Ability`、`Identity`、`Extensions`)。主路径上不出现会话、resolver、"链" | 新增 |
| S2 | `@Business(code, abilities, first)`:业务先于它的能力回答;`first` 列出要先于业务回答的能力 | 取代 D3 的 `chain` + `SELF` |
| S3 | `Identity.of(business)` 默认启用业务挂载的**全部**能力;`only(...)` / `without(...)` 收窄 | 修订 D1:不再每次列出启用的能力 |
| S4 | Web 绑定零代码:配置请求头名即可。身份不在请求头里,才写 `IdentityResolver`(第 2 层)。resolver 可返回 `null` 弃权,即不绑定 | 修订 D1 |
| S5 | 默认扫描 Boot 的自动配置包(`@SpringBootApplication` 所在包树);`@ExtensionScan` 只在追加包时用 | 新增 |
| S6 | 异步沿用身份是框架的事,范围是 Boot 自动配置的 `applicationTaskExecutor`(`@Async` 的默认执行器);其余线程用 `Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`。`Session` 降到第 3 层 | 取代预览里的 `Session.require()` |
| S7 | 测试:`Extensions.of(...)` 与 `@WithIdentity`。不做路由断言 DSL | 取代预览里的 `ExtensionsTest` |
| S8 | 4.0 不提供:热替换;`@ExtensionPoint` 的任何属性 | 缩小 D4、D7 的范围 |
| S9 | 请求期收窄(`only` / `without`、Matcher 风格的 `match()`)之后,启用的能力 `requires` 的能力也必须启用,否则 `ResolutionException` | 补充 D1 |
| S10 | Web 请求头是信任边界,规则(缺失、空白、重复、未知)与状态码写进草图 §5.6;应由网关或认证层设置 | 补充 D1 |

回答顺序固定,不可配置:**启用的 `first` → 业务自己 → 启用的 `abilities` → `@DefaultProvider` → 接口 `default`。**

## 3. 复杂留给框架

| 以前要用户做或知道 | 现在 |
|---|---|
| 写 resolver 读请求头 | Web filter 读配置的头 |
| 每次列出本次启用的能力 | `Identity.of(business)` 默认全部挂载 |
| 声明扫描包 | 默认扫 Boot 的自动配置包 |
| 把身份带进线程池 | Boot 自动配置的线程池(`@Async` 默认执行器)自动;其余一行 `wrap` |
| 注入 `Extensions` 时顾虑循环依赖、用 `ObjectProvider` | 注入的是门面,冻结的注册表在全部单例创建后接上;业务 Bean 直接注入 |
| 启动期问题逐个排查 | 一次聚合报出全部(`RegistryException#problems()`) |
| 弄清"为什么是它回答" | 顺序规则固定;细节只在 `Explanation` 里暴露 |
| AOP 代理、注入歧义、接口 `default` 方法的调用 | 感知 AOP;路由 Bean 为 `@Primary`;`invokeDefault` |

## 4. 后果

### 好处

- 第一个请求(口径:新手路径里出现的东西):**引入依赖 + 2 个类**(扩展点、业务)**+ 1 行配置**,要认识的注解 **2 个**(`@ExtensionPoint`、`@Business`)。另有两条要知道的规则:接口的 `default` 方法是兜底、没有 `default` 的方法必选;业务类要在扫描包树内。能力、resolver、拦截器、会话都是用到再学。
- 公开类型:新手要认识 5 个。`Extensions` 有十几个成员,第 1 层只用 `run` / `call` / `all` / `wrap` / `of`。**顶层**公开类型共 16 个,另有可选的 `matching` 子包 2 个(不含嵌套类型、starter、test-kit)。ADR-0001 D7 的预算"约 15"定为 **20**(现 18,留出余量),按顶层类型计,用 ArchUnit 守住。
- 迁移规则:`@Business(priority, abilities = {"a::10"})` 按 3.x **实际解析出的数字**比较,小于业务自身优先级的能力进 `first`,其余进 `abilities`,各自按数字升序。适用范围:注解方式注册的类;recipe 要复现 3.x 对未写数字的能力自动编为 1、2……的规则(业务默认是 0,所以默认业务在前;但业务写了 `priority = 100` 而能力不写数字时,能力在前)。手写 `IBusiness` 仍是人工。

### 代价与待办

1. **能力的启用语义变了。** 3.x 每个能力在每次请求里用 `match(param)` 决定是否启用;4.0 的 `Identity.of(business)` 默认挂载的全部启用。要"同一个业务、不同请求启用不同能力"的用户用 `only` / `without`,或继续用 Matcher 风格:`MatcherIdentityResolver` 把各能力 `match()` 的结果映射成 `only(...)`(只在业务挂载的范围内取交集)。迁移里最需要人看的一处,写进迁移指南。
2. **请求头缺失时不绑定身份。** 忘了带头的请求,在调用扩展点处才失败,不是在入口处 400;没人处理的 `ResolutionException` 是 HTTP 500。错误消息说明怎么办。要入口即拒绝,写一个 `IdentityResolver` Bean。是否加开关见草图 §9。
3. **自动沿用身份的范围很窄。** 只覆盖 Boot 自动配置的 `applicationTaskExecutor`;应用自己声明任何 `Executor` Bean 时 Boot 不再创建它,那种线程池要设置 `ExtensionTaskDecorator`。`new Thread`、自建的 `ExecutorService`、`CompletableFuture` 的默认池、并行流的工作线程要自己 `Extensions.wrap(...)`。没有身份时的报错消息指向 `wrap`。另外,Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用它(多于一个则都不生效),Boot 4 会把多个组合起来(两个版本的自动配置已核对):starter 保证不让应用已有的 `TaskDecorator` 失效,在 3.5 上让位并 WARN。`ThreadPoolTaskExecutor` 读不到已有的 decorator,所以 starter 不能替应用声明的线程池组合式加装。P2 的容器测试覆盖这些组合。
4. **`first` 这个名字**可能被读成"`abilities` 里的第一个"。备选 `precedence`、`overriding`;改名不影响语义,评审时定。
5. **4.0 不提供运行时热替换。** 注册表不可变、路由代理与注册表无关,以后加这个能力不需要改 API。
6. **默认只扫 Boot 的自动配置包。** 包树之外的业务和能力(例如别的 jar 里的另一个包),要用 `@ExtensionScan(scanPackages = ...)` 追加,与 Spring Data 的约定一致。症状是"业务码未知"。
7. **`requires` 的请求期校验比 3.x 更严**(S9)。3.x 只在挂载时校验,请求期按 `match()` 启用的组合不检查。Matcher 风格的用户可能因此在请求期得到 `ResolutionException`。
8. **`all(E)` 不含接口 `default` 体。** 3.x 的默认实现是链上的一个对象,`invokeAll` / `invokeReduce` 会遍历到;4.0 的 `all(E)` 只含链上的对象和 `@DefaultProvider`,每个元素仍经过拦截器。聚合时 `default` 体的值不再参与。
9. **默认顺序(业务先答)是设计假设,不是统计结果。** 它与 3.x 未写数字时的行为一致;但如果样例工程和用户反馈显示多数能力都要压过业务,应当重审默认(见 §7)。

## 5. 对 ADR-0001 的修订

| ADR-0001 | 变化 |
|---|---|
| D1 | `Identity.of(biz, abilities…)` → `Identity.of(biz)`(全部挂载的能力)加 `only` / `without`;Web 绑定零代码;`IdentityResolver` 降到第 2 层并可弃权。显式身份、`Matcher` 降级为可选,不变 |
| D3 | `chain` + `SELF` → `abilities` + `first`。声明顺序代替数字,不变 |
| D4 | `@ExtensionPoint` 的 `scenarios` / `version` 一并去掉(§8 原写"保持为提示性元数据",它们只被 admin 展示用到) |
| D7 | 公开类型预算:"约 15" → 20,按 core 顶层类型计(现 18);第 1 层 5 个 |
| §4 差异 2 | 改为:`only` / `without` 里列了业务没挂载的能力 → `ResolutionException` |
| §4 新增差异 10 | 能力默认全部启用(原由各能力的 `match()` 决定) |
| §4 新增差异 11 | 请求期收窄后校验 `requires`(原来只在挂载时校验) |
| §4 新增差异 12 | `all(E)` 不含接口 `default` 体(原来的默认实现是链上的对象,会被 `invokeAll` 遍历到) |
| §5 风险"业务 Bean 注入 `Extensions`" | 缓解改为:注入的是门面,业务侧不需要 `ObjectProvider` |
| §9 开放问题 1、4 | 草图暂定:取全部实现 = `Extensions.all`(第 1 层),`Session.all` 在第 3 层;缓存默认 10000 条 LRU |

D0、D2、D5、D6、D8、D9 不受影响。

## 6. 否决的方案

- **保留 `chain` + `SELF`,只给默认值("不写 = 业务在前")。** 哨兵常量和两套写法还在;`first` 把"例外"直接命名,默认情况什么都不用写。
- **只提供 resolver,不做零代码的 Web 绑定。** 每个 Web 应用都要写同样的几行代码。
- **`Identity` 继续列出启用的能力。** 重复表达业务已经声明的挂载关系。
- **路由断言 DSL(`assertRoute`)。** `Explanation` 是普通 record,用 JUnit 断言即可;DSL 是又一套 API。
- **把"能力是否启用"仍交给每个能力的 `match()`。** `Matcher` 回到核心,ADR-0001 D1 的收益落空。
- **请求头缺失时自动绑定 `Identity.none()`。** 忘带头的请求会被默认层静默服务,问题被藏起来。resolver 返回 `null`(弃权)与它不同:只是不绑定,调用扩展点时照样报错。
- **请求期收窄后自动补上被 `requires` 的能力。** 隐式改变启用集合,`explain` 之外看不出来;抛 `ResolutionException` 更显式。

## 7. 重新评估的触发条件

- 出现"忘了带头"类的线上问题 → 加开关(草图 §9)或改默认。
- 出现按请求属性给同一业务选能力的主流用例 → 扩展 `Identity`(仍用 `only` / `without`),不把 `Matcher` 放回核心。
- 样例工程或用户反馈显示多数能力都写了 `first` → 重审默认顺序。
- 用户确有运行时换实现的需求 → 加热替换(不改现有 API)。
- Matcher 风格的用户因 S9 在请求期频繁被拒 → 改为自动补依赖,或放宽到警告。
