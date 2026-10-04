# ADR-0002:用户面简化

| | |
|---|---|
| 状态 | **已撤回**(2026-10-04):维护者决定保留 3.x 核心设计,见 [ADR-0003](0003-keep-the-3x-core-design.md)。从未被采纳,对 ADR-0001 的修订没有生效。下文保留当时的提议和理由 |
| 原状态 | **提议**(2026-10-03),待维护者评审;2026-10-04 据维护者的倾向改写了 S12(恢复 Matcher,待确认);通过后改为"已采纳",并生效对 ADR-0001 的修订 |
| 维护者的意见 | 2026-10-03:请求头缺失或业务码未知时**不加**"回落到默认层"的开关(已确认)。2026-10-04:看到 RPC 样例后,"这个太不优雅了，还是之前的matcher优雅，business要不要也恢复成matcher的形式呢?",随后"感觉还是matcher实现更优雅"。这是倾向,不是逐条的决定:S12 的"业务和能力都恢复 Matcher",以及 S3、S4、S9、§4 里关于匹配的具体规则,都是我据此写的**提议**,待确认 |
| 待确认 | 业务和能力都恢复 Matcher(S12);匹配规则(严格单命中、无开关);`@WithIdentity` 的两种写法与取消表达式语言、`only` / `without` 属性;`@MatcherParam` 改标参数;显式身份下带 `Matcher` 的能力默认不启用(`Identity.with` 点名);请求头已绑定的业务与匹配得到的业务冲突时报错;兜底业务(草图 §9 第 10 项);`@WithIdentity` 的名字(建议保留);`overridingAbilities` 这个名字(原 `first`,维护者觉得 `first` 不合适);首个 GA 是否瘦身(§4 末尾) |
| 适用 | 4.0 的用户面:注解、`Identity`、`Extensions`、入口绑定、异步、测试 |
| 修订 | [ADR-0001](0001-v4-architecture.md) 的 D1、D3;缩小 D4、D7 的范围 |
| 配套 | [4.0 API 草图](../design/v4-api-sketch.md)(已按本文重写)、README「4.0 新用法预览」 |
| 触发 | 维护者对 4.0 预览的反馈:(1)"看起来有点复杂",暴露给用户的要尽量简单,复杂留给框架;(2)"很多 Spring Boot 应用是 RPC,根本没有 HTTP",身份不能只靠请求头;(3)2026-10-04,看到 RPC 样例里 `only = "#request.abilityCodes"` 之后:"这个太不优雅了，还是之前的matcher优雅",并倾向业务也恢复成 Matcher |

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
| S1 | 用户面分三层;新手只需要第 1 层的 6 个类型(`@ExtensionPoint`、`@Business`、`@Ability`、`Matcher`、`@WithIdentity`、`Extensions`)。主路径上不出现会话、resolver、"链"、`Identity` | 新增 |
| S2 | `@Business(code, abilities, overridingAbilities)`:业务先于它的能力回答;`overridingAbilities` 列出要覆盖业务自己逻辑的能力。(原名 `first`,易被读成"`abilities` 里的第一个") | 取代 D3 的 `chain` + `SELF` |
| S3 | 显式身份 `Identity.of(business)` 启用业务挂载的、**没有 `Matcher`** 的能力(带 `Matcher` 的能力要有请求才能判断,默认不启用);`only(...)` / `with(...)` / `without(...)` 点名。Matcher 得到的身份,启用哪些能力由各能力的 `match` 决定(S12)。没有任何能力带 `Matcher` 时,就是"挂载的全部" | 修订 D1:不再每次列出启用的能力 |
| S4 | **入口绑定与传输无关。** `@WithIdentity`:无值 = 匹配模式(方法的匹配参数交给业务和能力的 `Matcher`,默认第一个参数,可用 `@MatcherParam` 指定);有值 = 业务码字面量(定时任务、固定业务、测试)。适用于 RPC、MQ、定时任务、HTTP 请求体和测试。网关设在请求头里的业务码,配一行 `easy-extension.web.business-header` 即可,这是可选便利,只在 servlet 应用里,绑定的是显式身份。`IdentityResolver` 降到第 2 层(只被 HTTP 过滤器使用),可返回 `null` 弃权,里面可以调用 `identityOf` 用上 Matcher | 修订 D1 |
| S5 | 默认扫描 Boot 的自动配置包(`@SpringBootApplication` 所在包树);`@ExtensionScan` 只在追加包时用 | 新增 |
| S6 | **异步显式。** 换线程要带身份,规则只有一条:`Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`。starter 不自动贡献 `TaskDecorator`。`Session` 降到第 3 层 | 取代预览里的 `Session.require()`,并取消上一版"自动覆盖 `applicationTaskExecutor`"的设想 |
| S7 | 测试:`Extensions.of(...)`,以及同一个 `@WithIdentity`(字面量形式;Spring 测试里由自动注册的 `TestExecutionListener` 处理);测 `Matcher` 用 `identityOf`。不做路由断言 DSL | 取代预览里的 `ExtensionsTest` |
| S8 | 4.0 不提供:热替换;`@ExtensionPoint` 的任何属性 | 缩小 D4、D7 的范围 |
| S9 | 启用集合在请求期确定之后(`only` / `with` / `without` 收窄,或由 Matcher 得到),启用的能力 `requires` 的能力也必须启用,否则 `ResolutionException`。3.x 只在挂载时校验,这是更严的地方 | 补充 D1 |
| S10 | HTTP 请求头是信任边界,规则(缺失、空白、重复、未知)与状态码写进草图 §5.6;应由网关或认证层设置并覆盖客户端传来的值。请求头缺失或业务码未知时不绑定、不回落到默认层,**不加开关**(维护者已确认) | 补充 D1 |
| S11 | **一次执行只有一个业务身份。** 跨业务的订单,要么拆开执行再汇总(嵌套的 `extensions.call(biz, ...)`),要么把组合本身建成一个业务。不做"复合身份" | 新增 |
| S12 | **恢复 `Matcher<P>`,业务和能力都可选实现**(据维护者 2026-10-04 的倾向,待确认;取代 2026-10-03 的"不带 Matcher")。它只是得到 `Identity` 的一种方式:核心仍是显式的 `Identity`(非泛型,链按 `Identity` 缓存)。**严格、无开关**:业务恰好一个命中,无命中 / 多命中都是 `ResolutionException`;没有 `BusinessMatchSelector`、`business-match-order`、策略枚举、`allow-unknown-business`,也没有 3.x 的全局匹配参数类(原来标了 `@MatcherParam` 的那个类)。P 由每个类自己的泛型解析。`@WithIdentity` 无值 = 匹配模式,有值 = 业务码字面量;**取消表达式语言**和 `only` / `without` 属性。显式身份(字面量、请求头、`extensions.run`)不运行 Matcher,只启用没有 `Matcher` 的能力(S3)。规则见草图 §5.8 | 接近 ADR-0001 D1 的"`Matcher` 降级为可选的默认 resolver",区别:`Matcher<P>` 接口在 core、匹配在 `Extensions.identityOf`、没有配置;也不是 D1 否决的"保持 `Matcher` 在核心"(没有全局 `T` 和策略) |

回答顺序固定,不可配置:**启用的 `overridingAbilities` → 业务自己 → 启用的 `abilities` → `@DefaultProvider` → 接口 `default`。**

## 3. 复杂留给框架

| 以前要用户做或知道 | 现在 |
|---|---|
| 在每个入口手写 `initSession` / `ExtensionSessionScope.run`,并保证清理 | `@WithIdentity`:绑定、还原、清理都是框架的 |
| 每个业务各写一个 `match()`,路由逻辑散在各处 | 保留 `match()`(业务和能力各写自己的),但判定规则只有一套:严格单命中,没有无命中 / 多命中两套策略和选择器;框架按请求类建索引、缓存链;入口一个注解 |
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

- 第一个请求(口径:新手路径里出现的东西):**引入依赖 + 2 个类**(扩展点、业务,业务里写一个 `match`)**+ 入口绑定**(入口方法上一个注解;业务码在网关设的请求头里的,一行配置),要认识的注解 **3 个**(`@ExtensionPoint`、`@Business`、`@WithIdentity`;业务码在请求头里的不需要最后一个)加一个接口(`Matcher`)。另有两条要知道的规则:接口的 `default` 方法是兜底、没有 `default` 的方法必选;业务类要在扫描包树内。能力、resolver、拦截器、会话都是用到再学。
- 公开类型:新手要认识 6 个。`Extensions` 有十几个成员,第 1 层只用 `all` / `wrap` / `of`。**顶层**公开类型共 19 个(不含嵌套类型、starter、test-kit)。ADR-0001 D7 的预算"约 15"定为 **20**,按顶层类型计,用 ArchUnit 守住。
- 迁移规则:`@Business(priority, abilities = {"a::10"})` 按 3.x **实际解析出的数字**比较,小于业务自身优先级的能力进 `overridingAbilities`,其余进 `abilities`,各自按数字升序。适用范围:注解方式注册的类;recipe 要复现 3.x 对未写数字的能力自动编为 1、2……的规则(业务默认是 0,所以默认业务在前;但业务写了 `priority = 100` 而能力不写数字时,能力在前)。手写 `IBusiness` 仍是人工。

### 代价与待办

1. **能力的启用有两种来源。** 入口有请求对象时(匹配模式),由各能力的 `Matcher` 决定,与 3.x 相同(`requires` 的请求期校验除外,见代价 7);显式身份(字面量、请求头、`extensions.run`)没有请求对象,不运行 Matcher,启用的是挂载的、没有 `Matcher` 的能力,带 `Matcher` 的能力默认不启用,用 `with` / `only` 点名。这样按请求才该启用的能力(包邮、折扣)不会在没有请求的入口里意外生效;必须一直执行的能力(风控)不要给它 `Matcher`。后者是 3.x 里没有的情形(3.x 的每个入口都要构造一个匹配参数),写进迁移指南;配了请求头而能力带 `Matcher` 时,启动时打 WARN 日志,并出现在 `describe().warnings()`。
2. **身份缺失时不回落到默认层。** HTTP 请求头缺失是弃权:不绑定,到调用扩展点处才失败,不是在入口处 400;没人处理的 `ResolutionException` 是 HTTP 500。`@WithIdentity` 的匹配模式没有弃权:无命中、多命中直接在入口(方法体之前)抛 `ResolutionException`。错误消息说明怎么办。**不加全局的"回落到默认层"开关**(2026-10-03 已确认,针对请求头缺失和业务码未知;Matcher 的无命中按同一精神处理,待确认):要默认层,HTTP 里让 resolver 返回 `Identity.none()`,其余入口用 `extensions.run(Identity.none(), ...)`;匹配模式下没有这个出口,要兜底见草图 §9 第 10 项(`fallback` 业务,待定)。
3. **换线程要自己带身份,没有自动覆盖。** `@Async` 的默认执行器、`new Thread`、自建的 `ExecutorService`、`CompletableFuture` 的默认池、并行流的工作线程,都要 `Extensions.wrap(...)`,或给线程池设置 `ExtensionTaskDecorator`。比"自动沿用"多一行代码,但规则只有一条。放弃自动方案的原因(两个 Boot 版本的自动配置已核对):Boot 3.5 只在 `TaskDecorator` Bean 唯一时才采用,多于一个则都不生效,Boot 4 会组合;应用自己声明任何 `Executor` Bean 时 Boot 不再创建 `applicationTaskExecutor`;`ThreadPoolTaskExecutor` 读不到已有的 decorator,框架没法替应用自己的线程池组合式加装。自动贡献只能覆盖一部分线程池,还会在 3.5 上让应用已有的 decorator 失效。没有身份时的报错消息指向 `wrap`。
4. **`overridingAbilities` 这个名字偏长**,但方向不会读反:`first` 会被读成"`abilities` 里的第一个",`overrides` 会被读成"业务覆盖能力"(那恰恰是默认行为,写反了不会报错)。备选 `overriddenBy`。
5. **4.0 不提供运行时热替换。** 注册表不可变、路由代理与注册表无关,以后加这个能力不需要改 API。
6. **默认只扫 Boot 的自动配置包。** 包树之外的业务和能力(例如别的 jar 里的另一个包),要用 `@ExtensionScan(scanPackages = ...)` 追加,与 Spring Data 的约定一致。症状是"没有业务认领"(匹配模式)或"业务码未知"(请求头模式);包树之外的能力,启动时就报错。
7. **`requires` 的请求期校验比 3.x 更严**(S9)。3.x 只在挂载时校验,请求期按 `match()` 启用的组合不检查(已核对 3.3.6 的 `DefaultExtensionContext` 与稳定化分支的 `ChainResolver`)。迁移时 `match` 本身不用改,但启用的组合违反 `requires` 的请求,现在会失败。
8. **`all(E)` 不含接口 `default` 体。** 3.x 的默认实现是链上的一个对象,`invokeAll` / `invokeReduce` 会遍历到;4.0 的 `all(E)` 只含链上的对象和 `@DefaultProvider`,每个元素仍经过拦截器。聚合时 `default` 体的值不再参与。
9. **默认顺序(业务先答)是设计假设,不是统计结果。** 它与 3.x 未写数字时的行为一致;但如果样例工程和用户反馈显示多数能力都要覆盖业务,应当重审默认(见 §7)。
10. **一次执行只有一个业务(S11,有意的限制)。** 组合订单要拆开执行,或把组合建成业务。不做复合身份,是因为:两个业务实现同一方法时,"谁回答"没有合理的默认,"先注册的赢"会把整单悄悄按一个业务处理(3.x 的 select 策略就是这样);要合并,每个扩展点得各定一套策略(求和、取大、拼接),这是业务策略,不是框架该替用户定的;身份缓存的键、`explain`、校验都会随之变复杂。聚合需要时,`extensions.all(E)` 加应用里的 reduce 已经够用。3.x 当前的代码用命名 scope 或 `resolve` + `runWith` 已经能这样做(已用一次性程序验证,两种方式各自答各自的,并行也成立)。
11. **`@WithIdentity` 的限制。** Spring AOP 的常规限制:同类内部调用不经代理,`final` 类和方法、`private` 方法不生效;只对 Spring Bean 生效,不是 Bean 的入口用 `extensions.run(...)`。取消表达式语言之后没有 `-parameters` 的要求,匹配参数的位置(第一个参数或 `@MatcherParam`)编译器看得见。机制已用一次性程序验证:AOP 部分(Spring 6.2.19 与 7.0.6,JDK 代理与 CGLIB 代理、类上与方法上、嵌套还原);匹配模式(Spring 6.2.19,两个服务、7 个场景:能力各自判断、无命中、多命中、缺必选方法)。
12. **传输层元数据里的身份,核心不提供适配器。** RPC 的 attachment、gRPC 的 metadata、MQ 的消息头里的身份,写在 RPC 框架自己的服务端拦截点里,调用 `extensions.call(...)`;配方放文档。需求多了再出独立模块。
13. **`Matcher` 的代价(恢复它时已知的)。** ① 业务是 N 选 1:重叠只在重叠的那类请求上才报错(运行期、数据相关),典型数据的测试测不出来;用有代表性的请求调用 `identityOf` 做单测。② "没命中"的消息不如 `business [x] not found` 点名:框架不知道业务码在请求的哪个字段;消息给出类型和候选数,DEBUG 日志逐个打印 `match` 的结果。③ 一个类只能有一个 P(javac 拒绝同一接口的两种参数化,已验证):同一个业务要认领几种请求时,让它们实现同一个小接口,`Matcher` 取这个接口,否则入口先转换。④ 样板:每个业务一个 `"biz.retail".equals(r.bizCode())`,重复了 `@Business(code=…)` 里的码。⑤ 显式身份不运行 Matcher:没有请求的入口(定时任务、测试、请求头)只启用没有 `Matcher` 的能力,带 `Matcher` 的能力要 `with` / `only` 点名;配了请求头而能力带 `Matcher` 时,启动时打 WARN 日志。⑥ 匹配用的是调用方给的字段:选业务的依据要由网关或认证层确定,影响价格的能力不要让调用方"点名",依据用服务端能核实的订单事实(草图 §5.8)。⑦ 所有候选的 `match` 都会被评估,任何一个抛异常,都让这类请求整体失败。⑧ 请求头已绑定的业务与匹配得到的业务不同 → `ResolutionException`,不是静默覆盖(草图 §5.7 第 5 条,也是我的提议)。⑨ `requires` 比 3.x 更严,见代价 7。⑩ 迁移:3.x 的默认(`allow-unknown-business=false`)就是严格匹配,不受影响;设置了 `allow-unknown-business=true`(无命中走默认实现、多命中按 `business-match-order` 选一个)的应用要人工处理,草图 §8、§9。成本:线性于候选业务数,但很小。设计原型的粗测(一次性程序,单线程,非 JMH,预热后,`match` 只是字符串相等,不能由仓库复现):每个业务的 `match` 约 5–6 ns,另有约 0.1 µs 的固定开销,10 个业务约 0.15 µs,100 个约 0.6 µs,1000 个约 6 µs,显式身份的链查找约 30 ns,远小于一次 RPC。3.x 的 `initSession` 在稳定化分支上的实测(ADR-0001 D1,普通循环,1000 个业务约 3.0 µs)与它同量级。所以本文最初把"线性扫描"当作反对 Matcher 的理由,这条站不住:线性是真的,但便宜。

### 留给维护者决定的一项取舍

- **首个 GA 瘦身。** 只发 core + starter + test-kit;编译期注解处理器、BOM、迁移 recipe 放到 4.x(处理器输出的唯一消费者是 admin,admin 已拆出主线;运行期校验已经聚合报出全部问题)。取舍:首个 GA 更小、风险更低,对少了编译期检查和自动迁移。我倾向这样做。草图和 README 的正文没有按它来写,等你决定。

## 5. 对 ADR-0001 的修订

| ADR-0001 | 变化 |
|---|---|
| D1 | `Identity.of(biz, abilities…)` → `Identity.of(biz)`(显式身份:挂载的、没有 `Matcher` 的能力)加 `only` / `with` / `without`;入口绑定与传输无关(`@WithIdentity`,HTTP 请求头是可选便利);`IdentityResolver` 降到第 2 层并可弃权;**`Matcher` 保留为"请求对象 → 身份"的默认方式**(据维护者 2026-10-04 的倾向,待确认):严格单命中、无策略、无开关。显式身份不变 |
| D3 | `chain` + `SELF` → `abilities` + `overridingAbilities`。声明顺序代替数字,不变 |
| D4 | `@ExtensionPoint` 的 `scenarios` / `version` 一并去掉(§8 原写"保持为提示性元数据",它们只被 admin 展示用到) |
| D7 | 公开类型预算:"约 15" → 20,按 core 顶层类型计(现 19);第 1 层 6 个 |
| §4 差异 2 | 改为:`only` / `without` 里列了业务没挂载的能力 → `ResolutionException` |
| §4 新增差异 10 | 显式身份:启用没有 `Matcher` 的挂载能力;Matcher 得到的身份:由各能力的 `match()` 决定(与 3.x 相同) |
| §4 新增差异 11 | 请求期收窄后校验 `requires`(原来只在挂载时校验) |
| §4 新增差异 12 | `all(E)` 不含接口 `default` 体(原来的默认实现是链上的对象,会被 `invokeAll` 遍历到) |
| §4 新增差异 13 | 无命中 / 多命中:一律 `ResolutionException`(3.x 的默认严格模式);没有 `allow-unknown-business`、策略枚举、选择器、`business-match-order` |
| §5 风险"业务 Bean 注入 `Extensions`" | 缓解改为:注入的是门面,业务侧不需要 `ObjectProvider` |
| §6 P2 | "Web 绑定"扩展为"入口绑定":`@WithIdentity` 的 AOP 加可选的 HTTP 请求头 filter;`TaskDecorator` 不再自动贡献,只提供 `ExtensionTaskDecorator` |
| §9 开放问题 1、2、4 | 1:草图暂定 `Extensions.all`(第 1 层),`Session.all` 在第 3 层。2:`Matcher<P>` 接口在 core,匹配在 `Extensions.identityOf`,不另建模块。4:缓存默认 10000 条 LRU,不对外配置 |

D0、D2、D5、D6、D8、D9 不受影响。

## 6. 否决的方案

- **保留 `chain` + `SELF`,只给默认值("不写 = 业务在前")。** 哨兵常量和两套写法还在;`overridingAbilities` 把"例外"直接命名,默认情况什么都不用写。
- **只提供 resolver,不做零代码的入口绑定。** 每个应用的每个入口都要写同样的几行代码,而且清理(`removeSession`)要靠自觉。
- **把请求头配置当作唯一的零代码路径。** 只对 HTTP 成立;RPC、MQ、定时任务没有请求头,HTTP 应用的身份也常在请求体里。
- **在核心里为每个 RPC 框架维护适配器。** 数量没有尽头,每个框架的线程模型还不同(gRPC 的监听器回调、Dubbo 的异步提供者)。`@WithIdentity` 覆盖"身份在请求对象里"这个主流情形,传输层元数据靠配方。
- **自动贡献 `TaskDecorator` Bean、自动覆盖 `applicationTaskExecutor`。** 见 §4 代价 3:覆盖面窄,还会让应用已有的 decorator 失效。
- **不提供 `Matcher`(2026-10-03 的 S12;2026-10-04 起维护者倾向恢复,待确认)。** 当时的理由是:判定逻辑散在每个业务里、要线性扫描、要无命中 / 多命中两套策略加选择器、泛型参数贯穿接口;而身份本来就是请求里的显式数据。复核:线性扫描存在但很便宜(见 §4 代价 13);两套策略和选择器靠严格单命中去掉了(3.x 的默认本来就是严格);泛型靠每个类自己解析 P、核心非泛型去掉了。更重要的是写法:判定逻辑跟着业务和能力走(开闭、类型安全、不靠注解里的字符串),"能力自己说这个请求要不要我"比"调用方列出能力码"更自然。留下的真实代价列在 §4 代价 13。
- **`@WithIdentity` 的表达式语言(`#request.bizCode`、`only = "#request.abilityCodes"`)与 `Matcher` 并存。** 选业务、选能力各有两种写法;注解里的字符串没有类型检查,重构不跟随,还带来 `-parameters`、`@bean` 调用、表达式结果为 `null` 怎么办等规则。取消,只留字面量和匹配模式。业务码相等的样板太多时再评估(§7)。
- **`Identity` 继续列出启用的能力。** 重复表达业务已经声明的挂载关系。
- **路由断言 DSL(`assertRoute`)。** `Explanation` 是普通 record,用 JUnit 断言即可;DSL 是又一套 API。
- **身份缺失时自动绑定 `Identity.none()`,或加一个回落到默认层的开关。** 忘带头的请求会被默认层静默服务,问题被藏起来。弃权(`null`)与它不同:只是不绑定,调用扩展点时照样报错。
- **请求期收窄后自动补上被 `requires` 的能力。** 隐式改变启用集合,`explain` 之外看不出来;抛 `ResolutionException` 更显式。
- **复合身份(`Identity.of(b1, b2)`,一条链里同时有两个业务)。** 见 §4 代价 10。

## 7. 重新评估的触发条件

- 出现"忘了带头"类的线上问题 → 重新评估入口检查(已确认暂不加回落到默认层的开关)。
- 大量业务的 `match` 只是码相等,样板太多 → 评估在 `@Business` 上给按码匹配的简写(草图 §9 第 13 项)。
- 样例工程或用户反馈显示多数能力都写了 `overridingAbilities` → 重审默认顺序。
- 用户确有运行时换实现的需求 → 加热替换(不改现有 API)。
- 出现重叠造成的线上问题,或用户要求"无命中走默认实现" → 评估 `@Business(fallback = true)`(草图 §9 第 10 项),不加全局开关。
- 排查"为什么没命中"的反馈差 → 提供匹配追踪 API(草图 §9 第 11 项)。
- 传输层元数据的配方被反复抄写 → 出 Dubbo、gRPC 的独立适配模块。
- 出现必须在一次调用里合并两个业务回答的用例,且 `all(E)` 加 reduce 满足不了 → 重新评估复合身份。
