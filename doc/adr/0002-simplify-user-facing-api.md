# ADR-0002:用户面简化

| | |
|---|---|
| 状态 | **提议**(2026-10-03),待维护者评审;通过后改为"已采纳",并生效对 ADR-0001 的修订 |
| 已确认 | 维护者已确认(2026-10-03):4.0 **不带 Matcher 兼容层**(S12);请求头缺失或业务码未知时**不加**"回落到默认层"的开关 |
| 待确认 | `@WithIdentity` 的名字(建议保留);`overridingAbilities` 这个名字(原 `first`,维护者觉得 `first` 不合适);首个 GA 是否瘦身(§4 末尾) |
| 适用 | 4.0 的用户面:注解、`Identity`、`Extensions`、入口绑定、异步、测试 |
| 修订 | [ADR-0001](0001-v4-architecture.md) 的 D1、D3;缩小 D4、D7 的范围 |
| 配套 | [4.0 API 草图](../design/v4-api-sketch.md)(已按本文重写)、README「4.0 新用法预览」 |
| 触发 | 维护者对 4.0 预览的反馈:(1)"看起来有点复杂",暴露给用户的要尽量简单,复杂留给框架;(2)"很多 Spring Boot 应用是 RPC,根本没有 HTTP",身份不能只靠请求头 |

## 1. 背景

ADR-0001 把**内部**做简单了,但配套预览里,用户**要先理解的概念并没有变少**,只是换了名字。再加上它默认了 HTTP:

| 预览里的写法 | 用户被迫理解的概念 | 实际需求 |
|---|---|---|
| `@Business(chain = {"ability.x", Business.SELF})` | "链"、哨兵常量 `SELF` | 要不要压过业务,因能力而异(3.x 的包邮例子就压过业务)。默认(业务先答)与 3.x 未写数字时一致;例外直接命名,不必每个业务都写一遍完整顺序 |
| `class X implements IdentityResolver<HttpServletRequest>` | resolver | 多数 HTTP 应用的身份就是一个请求头 |
| `easy-extension.web.business-header` 是"一行配置"的主路径 | 假设有 HTTP | 很多 Spring Boot 应用是 RPC、MQ 消费者、定时任务,没有 servlet,那个 filter 根本不注册;HTTP 应用的身份也常在请求体里,过滤器读不到。入口绑定应当与传输无关 |
| `Identity.of(biz, abilitiesOf(req))` | "本次启用哪些能力" | 能力已经由业务挂载,启用关系不必每次重述 |
| `Session.require()`、`session.wrap(...)` | 会话、线程绑定 | 换线程要带身份,只需要一条规则 |
| `ExtensionsTest`、`assertRoute(...).selects(...)` | 一套测试断言 DSL | 指定身份、调用、用 JUnit 断言 |
| `reloadableExtensions.replace(...)` | 注册表会变、代际 | 没有用例催着要 |
| `@ExtensionPoint(mandatory / scenarios / version)` | 三个属性 | `mandatory` 已被"抽象方法即必选"取代;另两个只给 admin 展示(`ExtensionInfoService`),而 admin 已拆出主线 |

跑通第一个请求,旧预览要求:引入依赖、**4 个类**(扩展点、能力、业务、resolver)和 **7 个要认识的类型或概念**(`@ExtensionPoint`、`@Ability`、`@Business`、`chain`、`SELF`、`IdentityResolver`、`Identity`),而且只在 HTTP 下成立。

用户面的预算按**最常见的路径**算,不按"能表达的全部场景"算:**常见情况零代码,少见情况才引入概念。**

## 2. 决策

| # | 决策 | 与 ADR-0001 的关系 |
|---|---|---|
| S1 | 用户面分三层;新手只需要第 1 层的 6 个类型(`@ExtensionPoint`、`@Business`、`@Ability`、`@WithIdentity`、`Identity`、`Extensions`)。主路径上不出现会话、resolver、"链" | 新增 |
| S2 | `@Business(code, abilities, overridingAbilities)`:业务先于它的能力回答;`overridingAbilities` 列出要覆盖业务自己逻辑的能力。(原名 `first`,易被读成"`abilities` 里的第一个") | 取代 D3 的 `chain` + `SELF` |
| S3 | `Identity.of(business)` 默认启用业务挂载的**全部**能力;`only(...)` / `without(...)` 收窄 | 修订 D1:不再每次列出启用的能力 |
| S4 | **入口绑定与传输无关。** `@WithIdentity` 声明身份:业务码字面量,或从方法参数取的 SpEL。适用于 RPC、MQ、定时任务、HTTP 请求体和测试。网关设在请求头里的身份,配一行 `easy-extension.web.business-header` 即可,这是可选便利,只在 servlet 应用里。`IdentityResolver` 降到第 2 层(只被 HTTP 过滤器使用),可返回 `null` 弃权 | 修订 D1 |
| S5 | 默认扫描 Boot 的自动配置包(`@SpringBootApplication` 所在包树);`@ExtensionScan` 只在追加包时用 | 新增 |
| S6 | **异步显式。** 换线程要带身份,规则只有一条:`Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`。starter 不自动贡献 `TaskDecorator`。`Session` 降到第 3 层 | 取代预览里的 `Session.require()`,并取消上一版"自动覆盖 `applicationTaskExecutor`"的设想 |
| S7 | 测试:`Extensions.of(...)`,以及同一个 `@WithIdentity`(Spring 测试里由自动注册的 `TestExecutionListener` 处理)。不做路由断言 DSL | 取代预览里的 `ExtensionsTest` |
| S8 | 4.0 不提供:热替换;`@ExtensionPoint` 的任何属性 | 缩小 D4、D7 的范围 |
| S9 | 请求期收窄(`only` / `without`)之后,启用的能力 `requires` 的能力也必须启用,否则 `ResolutionException` | 补充 D1 |
| S10 | HTTP 请求头是信任边界,规则(缺失、空白、重复、未知)与状态码写进草图 §5.6;应由网关或认证层设置并覆盖客户端传来的值。请求头缺失或业务码未知时不绑定、不回落到默认层,**不加开关**(维护者已确认) | 补充 D1 |
| S11 | **一次执行只有一个业务身份。** 跨业务的订单,要么拆开执行再汇总(嵌套的 `extensions.call(biz, ...)`),要么把组合本身建成一个业务。不做"复合身份" | 新增 |
| S12 | **4.0 不带 Matcher 兼容层**(维护者已确认)。`Matcher<P>`、`@MatcherParam`、无命中 / 多命中策略、`BusinessMatchSelector`、`allow-unknown-business` 都没有对应物。身份永远是显式的,`match()` 里的判断挪到入口:`@WithIdentity` 的表达式,或一个 Bean 方法 | 修订 D1:"`Matcher` 降级为可选的默认 resolver"改为"不提供" |

回答顺序固定,不可配置:**启用的 `overridingAbilities` → 业务自己 → 启用的 `abilities` → `@DefaultProvider` → 接口 `default`。**

## 3. 复杂留给框架

| 以前要用户做或知道 | 现在 |
|---|---|
| 在每个入口手写 `initSession` / `ExtensionSessionScope.run`,并保证清理 | `@WithIdentity`:绑定、还原、清理都是框架的 |
| 每个业务各写一个 `match()`,路由逻辑散在各处 | 路由判断集中在入口(一个表达式或一个 Bean 方法);没有无命中 / 多命中两套策略 |
| 写 resolver 读请求头 | Web filter 读配置的头 |
| 每次列出本次启用的能力 | `Identity.of(business)` 默认全部挂载 |
| 声明扫描包 | 默认扫 Boot 的自动配置包 |
| 把身份带进线程池 | `Extensions.wrap(...)` 一行,或给线程池设一行装饰器;规则处处一样,没有"某个线程池例外" |
| 注入 `Extensions` 时顾虑循环依赖、用 `ObjectProvider` | 注入的是门面,冻结的注册表在全部单例创建后接上;业务 Bean 直接注入 |
| 启动期问题逐个排查 | 一次聚合报出全部(`RegistryException#problems()`),包括 `@WithIdentity` 里写错的业务码 |
| 弄清"为什么是它回答" | 顺序规则固定;细节只在 `Explanation` 里暴露 |
| AOP 代理、注入歧义、接口 `default` 方法的调用 | 感知 AOP;路由 Bean 为 `@Primary`;`invokeDefault` |

## 4. 后果

### 好处

- 第一个请求(口径:新手路径里出现的东西):**引入依赖 + 2 个类**(扩展点、业务)**+ 入口绑定**(HTTP 请求头一行配置,或入口方法上一个注解),要认识的注解 **2 个**(`@ExtensionPoint`、`@Business`);RPC、MQ、定时任务再加 `@WithIdentity`。另有两条要知道的规则:接口的 `default` 方法是兜底、没有 `default` 的方法必选;业务类要在扫描包树内。能力、resolver、拦截器、会话都是用到再学。
- 公开类型:新手要认识 6 个。`Extensions` 有十几个成员,第 1 层只用 `run` / `call` / `all` / `wrap` / `of`。**顶层**公开类型共 17 个(不含嵌套类型、starter、test-kit)。ADR-0001 D7 的预算"约 15"定为 **20**,按顶层类型计,用 ArchUnit 守住。
- 迁移规则:`@Business(priority, abilities = {"a::10"})` 按 3.x **实际解析出的数字**比较,小于业务自身优先级的能力进 `overridingAbilities`,其余进 `abilities`,各自按数字升序。适用范围:注解方式注册的类;recipe 要复现 3.x 对未写数字的能力自动编为 1、2……的规则(业务默认是 0,所以默认业务在前;但业务写了 `priority = 100` 而能力不写数字时,能力在前)。手写 `IBusiness` 仍是人工。

### 代价与待办

1. **能力的启用语义变了。** 3.x 每个能力在每次请求里用 `match(param)` 决定是否启用;4.0 的 `Identity.of(business)` 默认挂载的全部启用。要"同一个业务、不同请求启用不同能力"的用户用 `only` / `without`(`@WithIdentity` 的 `only` / `without` 可以从参数里取)。迁移里最需要人看的一处,写进迁移指南。
2. **身份缺失时不绑定,也不回落到默认层。** HTTP 请求头缺失、`@WithIdentity` 的表达式结果为 `null`,都是弃权:不绑定,到调用扩展点处才失败,不是在入口处 400;没人处理的 `ResolutionException` 是 HTTP 500。错误消息说明怎么办。要入口即拒绝,表达式里自己抛 `ResolutionException`,或写一个 `IdentityResolver` Bean。**不加"回落到默认层"的开关**(维护者已确认):要默认层,就显式 `Identity.none()`,或建一个没有实现的 `biz.default` 业务。
3. **换线程要自己带身份,没有自动覆盖。** `@Async` 的默认执行器、`new Thread`、自建的 `ExecutorService`、`CompletableFuture` 的默认池、并行流的工作线程,都要 `Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`。比"自动沿用"多一行代码,但规则只有一条。放弃自动方案的原因(两个 Boot 版本的自动配置已核对):Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用,多于一个则都不生效,Boot 4 会组合;应用自己声明任何 `Executor` Bean 时 Boot 不再创建 `applicationTaskExecutor`;`ThreadPoolTaskExecutor` 读不到已有的 decorator,框架没法替应用自己的线程池组合式加装。自动贡献只能覆盖一部分线程池,还会在 3.5 上让应用已有的 decorator 失效。没有身份时的报错消息指向 `wrap`。
4. **`overridingAbilities` 这个名字偏长**,但方向不会读反:`first` 会被读成"`abilities` 里的第一个",`overrides` 会被读成"业务覆盖能力"(那恰恰是默认行为,写反了不会报错)。备选 `overriddenBy`。
5. **4.0 不提供运行时热替换。** 注册表不可变、路由代理与注册表无关,以后加这个能力不需要改 API。
6. **默认只扫 Boot 的自动配置包。** 包树之外的业务和能力(例如别的 jar 里的另一个包),要用 `@ExtensionScan(scanPackages = ...)` 追加,与 Spring Data 的约定一致。症状是"业务码未知"。
7. **`requires` 的请求期校验比 3.x 更严**(S9)。3.x 只在挂载时校验,请求期按 `match()` 启用的组合不检查。
8. **`all(E)` 不含接口 `default` 体。** 3.x 的默认实现是链上的一个对象,`invokeAll` / `invokeReduce` 会遍历到;4.0 的 `all(E)` 只含链上的对象和 `@DefaultProvider`,每个元素仍经过拦截器。聚合时 `default` 体的值不再参与。
9. **默认顺序(业务先答)是设计假设,不是统计结果。** 它与 3.x 未写数字时的行为一致;但如果样例工程和用户反馈显示多数能力都要覆盖业务,应当重审默认(见 §7)。
10. **一次执行只有一个业务(S11,有意的限制)。** 组合订单要拆开执行,或把组合建成业务。不做复合身份,是因为:两个业务实现同一方法时,"谁回答"没有合理的默认,"先注册的赢"会把整单悄悄按一个业务处理(3.x 的 select 策略就是这样);要合并,每个扩展点得各定一套策略(求和、取大、拼接),这是业务策略,不是框架该替用户定的;身份缓存的键、`explain`、校验都会随之变复杂。聚合需要时,`extensions.all(E)` 加应用里的 reduce 已经够用。3.x 当前的代码用命名 scope 或 `resolve` + `runWith` 已经能这样做(已用一次性程序验证,两种方式各自答各自的,并行也成立)。
11. **`@WithIdentity` 的限制。** Spring AOP 的常规限制:同类内部调用不经代理,`final` 类和方法、`private` 方法不生效;只对 Spring Bean 生效,不是 Bean 的入口用 `extensions.run(...)`。SpEL 里用参数名要 `-parameters` 编译(Boot 的 parent 默认开),否则用 `#p0`;表达式是字符串,编译器检查不了,所以 starter 在启动期只校验字面量的业务码、能力码,以及"用了参数名而参数名不可用"。机制已用一次性程序验证(Spring 6.2.19 与 7.0.6,JDK 代理与 CGLIB 代理、类上与方法上、弃权、嵌套还原、`only` 来自表达式都成立;没有 `-parameters` 时 `#request` 求不出值,`#p0` 可用)。
12. **传输层元数据里的身份,核心不提供适配器。** RPC 的 attachment、gRPC 的 metadata、MQ 的消息头里的身份,写在 RPC 框架自己的服务端拦截点里,调用 `extensions.call(...)`;配方放文档。需求多了再出独立模块。
13. **没有 Matcher 兼容层,迁移更费劲。** 每个 `match()` 都要迁到入口。典型情形(`bizCode` 相等、能力码在请求里)是机械的,可以出 recipe;复杂的 `match()`(按地区、金额等)要人工,把判断写成一个 Bean 方法,在 `@WithIdentity` 的表达式里调用。原来靠 `allow-unknown-business` 回落到默认实现的,要建一个没有实现的 `biz.default` 业务,让路由方法把未知的码映射到它。好处是路由逻辑从每个业务里收拢到入口一处,线性扫描和无命中 / 多命中两套策略一并消失。

### 留给维护者决定的一项取舍

- **首个 GA 瘦身。** 只发 core + starter + test-kit;编译期注解处理器、BOM、迁移 recipe 放到 4.x(处理器输出的唯一消费者是 admin,admin 已拆出主线;运行期校验已经聚合报出全部问题)。取舍:首个 GA 更小、风险更低,对少了编译期检查和自动迁移。我倾向这样做。草图和 README 的正文没有按它来写,等你决定。

## 5. 对 ADR-0001 的修订

| ADR-0001 | 变化 |
|---|---|
| D1 | `Identity.of(biz, abilities…)` → `Identity.of(biz)`(全部挂载的能力)加 `only` / `without`;入口绑定与传输无关(`@WithIdentity`,HTTP 请求头是可选便利);`IdentityResolver` 降到第 2 层并可弃权;**`Matcher` 不再提供**(原为可选的默认 resolver)。显式身份不变 |
| D3 | `chain` + `SELF` → `abilities` + `overridingAbilities`。声明顺序代替数字,不变 |
| D4 | `@ExtensionPoint` 的 `scenarios` / `version` 一并去掉(§8 原写"保持为提示性元数据",它们只被 admin 展示用到) |
| D7 | 公开类型预算:"约 15" → 20,按 core 顶层类型计(现 17);第 1 层 6 个 |
| §4 差异 2 | 改为:`only` / `without` 里列了业务没挂载的能力 → `ResolutionException` |
| §4 新增差异 10 | 能力默认全部启用(原由各能力的 `match()` 决定) |
| §4 新增差异 11 | 请求期收窄后校验 `requires`(原来只在挂载时校验) |
| §4 新增差异 12 | `all(E)` 不含接口 `default` 体(原来的默认实现是链上的对象,会被 `invokeAll` 遍历到) |
| §4 新增差异 13 | 没有无命中 / 多命中策略和 `allow-unknown-business`:业务码未知就是 `ResolutionException` |
| §5 风险"业务 Bean 注入 `Extensions`" | 缓解改为:注入的是门面,业务侧不需要 `ObjectProvider` |
| §6 P2 | "Web 绑定"扩展为"入口绑定":`@WithIdentity` 的 AOP 加可选的 HTTP 请求头 filter;`TaskDecorator` 不再自动贡献,只提供 `ExtensionTaskDecorator` |
| §9 开放问题 1、2、4 | 1:草图暂定 `Extensions.all`(第 1 层),`Session.all` 在第 3 层。2:取消(不带 Matcher)。4:缓存默认 10000 条 LRU,不对外配置 |

D0、D2、D5、D6、D8、D9 不受影响。

## 6. 否决的方案

- **保留 `chain` + `SELF`,只给默认值("不写 = 业务在前")。** 哨兵常量和两套写法还在;`overridingAbilities` 把"例外"直接命名,默认情况什么都不用写。
- **只提供 resolver,不做零代码的入口绑定。** 每个应用的每个入口都要写同样的几行代码,而且清理(`removeSession`)要靠自觉。
- **把请求头配置当作唯一的零代码路径。** 只对 HTTP 成立;RPC、MQ、定时任务没有请求头,HTTP 应用的身份也常在请求体里。
- **在核心里为每个 RPC 框架维护适配器。** 数量没有尽头,每个框架的线程模型还不同(gRPC 的监听器回调、Dubbo 的异步提供者)。`@WithIdentity` 覆盖"身份在请求对象里"这个主流情形,传输层元数据靠配方。
- **自动贡献 `TaskDecorator` Bean、自动覆盖 `applicationTaskExecutor`。** 见 §4 代价 3:覆盖面窄,还会让应用已有的 decorator 失效。
- **保留 `Matcher` 风格(放在核心、`core.matching` 子包或独立模块)。** 判定逻辑散在每个业务里,要线性扫描,要无命中 / 多命中两套策略加选择器,泛型参数贯穿接口;而身份本来就是请求里的显式数据。迁移靠指南和 recipe。真有大量复杂 `match()` 需要兼容,以后出独立模块。
- **`Identity` 继续列出启用的能力。** 重复表达业务已经声明的挂载关系。
- **路由断言 DSL(`assertRoute`)。** `Explanation` 是普通 record,用 JUnit 断言即可;DSL 是又一套 API。
- **身份缺失时自动绑定 `Identity.none()`,或加一个回落到默认层的开关。** 忘带头的请求会被默认层静默服务,问题被藏起来。弃权(`null`)与它不同:只是不绑定,调用扩展点时照样报错。
- **请求期收窄后自动补上被 `requires` 的能力。** 隐式改变启用集合,`explain` 之外看不出来;抛 `ResolutionException` 更显式。
- **复合身份(`Identity.of(b1, b2)`,一条链里同时有两个业务)。** 见 §4 代价 10。

## 7. 重新评估的触发条件

- 出现"忘了带头"类的线上问题 → 重新评估入口检查(已确认暂不加回落到默认层的开关)。
- 出现按请求属性给同一业务选能力的主流用例 → 扩展 `Identity`(仍用 `only` / `without`)。
- 样例工程或用户反馈显示多数能力都写了 `overridingAbilities` → 重审默认顺序。
- 用户确有运行时换实现的需求 → 加热替换(不改现有 API)。
- 迁移反馈显示大量复杂的 `match()` 需要兼容 → 出独立的兼容模块,不放回核心。
- 用 `@WithIdentity` 的用户普遍嫌 SpEL 不安全或难调试 → 补类型安全的入口(`resolver` 属性,草图 §9)。
- 传输层元数据的配方被反复抄写 → 出 Dubbo、gRPC 的独立适配模块。
- 出现必须在一次调用里合并两个业务回答的用例,且 `all(E)` 加 reduce 满足不了 → 重新评估复合身份。
