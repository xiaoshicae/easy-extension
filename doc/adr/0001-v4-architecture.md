# ADR-0001:4.0 架构方向

| | |
|---|---|
| 状态 | **已采纳**(2026-10-03)。待定项 D0–D5 由维护者交给推荐默认值决定,本文如实记录取舍和理由;D0 经维护者确认:并入 4.0 发布 |
| 适用 | easy-extension 4.0 主线:core、Spring starter、周边模块、工程化 |
| 配套 | [4.0 API 草图](../design/v4-api-sketch.md)(P0 评审通过后冻结) |
| 修订 | 提议中的 [ADR-0002](0002-simplify-user-facing-api.md)(用户面简化)修订 D1、D3,缩小 D4、D7 的范围;通过后生效,下文保留原决策 |
| 基线 | 本文数据取自 `feat/stability-and-resolution-model` 分支,提交 `18e0dd0`(下称"稳定化分支") |

## 1. 背景

3.x 的模型(`Matcher` + 数字优先级 + 一个默认实现大类 + JDK 代理包装用户对象)可以工作,但复杂度主要来自模型本身,不是实现细节。

稳定化分支实测(约数):

| 项 | 数据 | 问题 |
|---|---|---|
| core | 76 个文件、5.4K 行;`DefaultExtensionContext` 841 行 | 注册、会话、查找、解释全塞在一个类里 |
| 公共 API 表面 | 76 个顶层类型,68 个 `public` | Manager、Factory、Utils、Helper 全公开,任何重构都成了"破坏性变更"(3.3.5 就这样出过事) |
| 接口 | 16 个 `I*`;`IExtensionContext` 由 5 个接口合体,另有 6 个已弃用的 `scopedXxx` | 使用者要先理解一堆分层 |
| 代理层 | `proxy` + `util` 约 900 行 | 只是为了把元数据挂到用户对象上 |
| 异常 | 13 个类,12 个是受检异常 | 调用方到处 `throws SessionException` |
| Spring starter | 18 个文件、1.1K 行 | 3 个自写扫描器、3 种 Holder Bean、一个重写字段注入的 BPP |
| admin | 2.1K 行 Java + 2.0K 行前端,运行时依赖 JavaParser | 展示源码要靠 sources.jar、文件系统遍历、`metadata.json`,生产环境很脆弱 |
| 注解处理器 | 575 行 | 唯一职责是把源码和 Javadoc 抽出来给 admin 展示 |

### 1.1 三个根因

1. **用代理给用户对象挂元数据。** 注解类被 JDK 代理包成 `IBusiness` / `IAbility`,各有一套 Template 加反射转发。
2. **注册是可变、无序、校验分散的。** register 方法、两个 Helper、四对 Manager、事后再 `validateRegistration()`,于是有了锁、COW 快照、版本指纹、注册顺序依赖。
3. **`Matcher<T>` 把"我是谁"和"怎么路由"缠在一起。** 每个业务自带 `match()`,于是有线性扫描、"无命中 / 多命中"两套策略、选择器、`@MatcherParam`,泛型 `T` 还贯穿 16 个接口。

### 1.2 为什么不继续在 3.x 上修补

稳定化分支修了一批真问题,每一个的根都在上面三条里:

| 问题 | 根因 |
|---|---|
| 异常被包四层,声明的受检异常抓不到 | 1:代理层层转发 |
| Spring AOP 代理的实现被静默跳过;Bean 被实例化两次 | 1:代理包装用户对象;自写扫描器 |
| 继承来的扩展点算不算,会改变路由;未注册的继承扩展点会让启动失败(评审发现) | 1:要在注册期猜"这个类实现了哪些扩展点" |
| 默认实现必须"实现全部扩展点",于是补了 `mandatory`、多默认实现、`validateRegistration()` | 2 |
| 不同 code 的默认实现同优先级:注册通过,每个请求都失败(评审发现) | 2 + 数字优先级 |
| `runWith` 在内联执行时抹掉调用线程自己的会话(评审的高危项) | 会话是可变 ThreadLocal,外加命名 scope |
| 无命中 / 多命中两套策略加旧开关 | 3 |

继续修补只会把复杂度搬进更多的兼容开关。这一轮同时留下了一份**可执行的行为说明**(容器级、语义级测试),这是 4.0 敢于重写的前提,见 §7。

## 2. 决策总览

| # | 决策 | 采纳 | 备选 |
|---|---|---|---|
| D0 | 稳定化分支的去向 | **并入 4.0 主线,不单发 3.4.0;留作行为基准(oracle)和保底发布点** | 作为最后一个 3.x 发布 |
| D1 | 身份判定 | **核心用显式 `Identity`;`Matcher` 降级为可选的默认 resolver** | `Matcher` 留在核心 |
| D2 | 默认实现 | **接口 `default` 方法;抽象方法 = 必选;可选 `@DefaultProvider` Bean** | 保留大类模型加 `mandatory` |
| D3 | 优先级 | **声明顺序 `chain = {"ability.x", SELF}`** | 保留数字 |
| D4 | Admin | **拆出主线;主线只给 Actuator JSON;处理器改做编译期校验** | 保留内嵌 |
| D5 | JDK 基线 | **17**(Spring Boot 3.5 / 4.0) | 21 |
| D6 | 异常 | **全部 unchecked:3 个具体类加 1 个基类** | 保留受检 |
| D7 | 公开 API | **收缩到约 15 个类型,其余放 `internal`,ArchUnit 守住** | —— |
| D8 | Spring starter | **重写:一个注册器 + 从容器收集 + 标准注入** | 修补现有扫描器 |
| D9 | 工程化 | **`${revision}` + flatten、BOM、test-kit、JMH、迁移 recipe** | —— |

D6–D9 没有悬念,直接做。D0–D5 的理由如下。

## 3. 决策详述

### D0 稳定化分支的去向

- **背景。** 分支里有两类东西:真修复(AOP 代理丢失、双实例、异常包装、热路径、japicmp 门禁空转),以及为兼容而生的新增 API(`mandatory`、两个策略枚举加旧开关、legacy 异常开关、`addExtensionPointDefaultImplementation`、一批 `default` 方法)。
- **决策。** 整体并入 4.0 主线,**不单独发 3.4.0**。它同时是:(a)4.0 行为契约的来源,(b)新旧实现差分比对的基准(oracle),(c)4.0 迟到时的保底发布点。
- **理由。** 分支里的行为变更(继承的扩展点按路由计入、AOP 代理 Bean 开始参与匹配、异常原样抛出、作用域关闭语义)按 SemVer 属于 breaking,兼容性评审也把其中的路由变化判为 breaking(启动期的几处回归已经修复)。而兼容用的新增 API 会被 4.0 的模型取代,放进 3.x 只会让用户迁移两次。
- **后果。**
  - (+)用户只迁移一次;主线不再背兼容开关(legacy 异常开关随代理层一起消失)。
  - (−)用户要等 4.0 才拿到修复。缓解:`18e0dd0` 始终保持可发布状态,CHANGELOG 里已有"Behavior changes"一节;若 4.0 GA 明显延期,直接从该提交发 3.x(把代码里的 4.0 标签机械替换回 3.4 即可),无需返工;P4 的 RC 给早期使用者。
  - 代码里的 `@since`、`@Deprecated(since = ...)` 等标签已统一改为 `4.0`(见提交 `chore: label the 4.0 base as 4.0`),对里程碑版(如 `4.0.0-M1`)和正式版都成立。pom 里的版本号不动,发版准备(`/release-prep`)时再定。
- **重新评估的触发条件。** P1 结束时对 GA 的预计仍然很远,或有用户明确需要这批修复。

### D1 身份判定显式化

> **提议中的修订([ADR-0002](0002-simplify-user-facing-api.md) S3、S4、S12):** `Identity.of(biz)`(显式身份)默认启用业务挂载的全部能力;入口绑定与传输无关(`@WithIdentity`),`IdentityResolver` 降到第 2 层;**`Matcher` 保留**:2026-10-03 曾决定不带,2026-10-04 被维护者推翻,恢复为"请求对象 → 身份"的默认方式,严格单命中、无策略、无开关。下文"`Matcher` 风格保留为可选的默认 resolver"处按此理解:`Matcher<P>` 是业务和能力直接实现的接口,匹配在 `Extensions.identityOf`,不是独立的 `MatcherIdentityResolver`。

- **背景。** README 里的 matcher 基本是 `"retail".equals(param.getBizCode())` 和 `param.getAbilityCodes().contains(...)`:身份本来就是请求里的显式数据,`match()` 只是样板。`Matcher<T>` 带来:`initSession` 随业务数线性增长(实测 100 个业务 0.65 µs、1000 个 3.0 µs)、"无命中 / 多命中"两套策略加选择器加 `business-match-order`、泛型 `T` 贯穿 16 个接口、`@MatcherParam`。
- **决策。** 核心只做"身份 → 链"。输入是显式的 `Identity`(业务码 + 本次启用的能力码),判定交给 `IdentityResolver<Req>`。`Matcher` 风格保留为可选的默认 resolver(`MatcherIdentityResolver`),多命中怎么处理(报错 / 取首个 / 按顺序)是它自己的配置,不再是核心概念。
- **后果。**
  - (+)核心非泛型;身份稳定时解析是一次 Map 查找(链按 `Identity` 缓存);单测里直接传 `Identity`。
  - (+)删 `@MatcherParam`、选择器、两个策略枚举、`T`。
  - (−)习惯 `match()` 的用户要多一个 resolver。Matcher 风格下由 starter 自动提供,迁移成本低。
  - **注意:** O(1) 只对"显式身份"成立。Matcher 风格的默认 resolver 仍要逐个业务调用 `match()`,与 3.x 同阶。
- **否决的方案。** 保持 `Matcher` 在核心:泛型 `T` 和线性扫描都留下,后面的简化落空。同时支持两套核心 API:测试面翻倍。
- **重新评估的触发条件。** 出现无法用字符串码表达的路由维度(例如要按请求属性给"同一业务的同一能力"选不同实现)。目前没有这种用例。

### D2 默认实现下沉到接口

- **背景。** 3.x 的默认实现是"一个类实现全部扩展点",放在链尾兜底。发票、支付渠道这类没有合理默认值的扩展点,只能写占位实现,"没人实现"这个本该立刻暴露的错误被吞掉。稳定化分支为此补了 `mandatory`、多默认实现、`validateRegistration()`,等于给错误的模型打补丁。而 `@ExtensionPoint(version)` 的 Javadoc 和 README 本来就在用 `default` 方法做接口演进。
- **决策。**
  - 扩展点接口的 `default` 方法就是默认实现;没有 `default` 的抽象方法就是"必选"。
  - 路由代理对"链上没人实现"的 `default` 方法,用 `InvocationHandler.invokeDefault`(JDK 16+)调用接口自己的默认体;对抽象方法抛 `ExtensionNotFoundException`,消息列出整条链。
  - 默认逻辑需要注入 Bean 时,用可选的 `@DefaultProvider` Bean,每个扩展点至多一个。
- **后果。**
  - (+)删 `@ExtensionPointDefaultImplementation`、`mandatory`、"默认实现必须覆盖全部扩展点"校验、`DefaultsRegistry`。
  - (+)接口演进规则统一:加方法必须带 `default`。
  - (+)"某个类通过父类或父接口间接实现了扩展点,算不算、没注册怎么办"这类问题不再需要在注册期猜,路由时按方法判断即可。
  - (−)**路由按方法而不是按扩展点接口**(见 §4 差异 1)。对带 `default` 方法的扩展点,行为与 3.x 不同。
  - (−)`default` 方法体拿不到注入的 Bean,所以有 `@DefaultProvider`。
  - (−)Kotlin 需要 `-Xjvm-default=all`,否则接口默认体不是 JVM 的 default 方法。构建期检测并给出提示。
- **否决的方案。** 保留大类模型加 `mandatory`:每个扩展点继续要一个占位类。新增 `@DefaultFor(Ext.class)`:又一套注册概念。
- **重新评估的触发条件。** JMH 显示 `invokeDefault` 成本不可接受(预期远低于一次反射调用);或需要 Bean 的默认逻辑成了主流。

### D3 声明顺序代替数字优先级

> **提议中的修订([ADR-0002](0002-simplify-user-facing-api.md) S2):** `chain` + `SELF` 改为 `@Business(abilities, overridingAbilities)`。

- **背景。** 优先级只在"一个业务自己"与"它挂载的能力"之间比较(`ability.free-shipping::10`,数字越小越优先)。数字带来:`::` 字符串解析、自动编号、冲突检测。有些冲突要到每次 `initSession` 才暴露(业务与默认实现之间、不同 code 的默认实现之间)。评审就发现一例:不同 code 的默认实现同优先级,注册通过、每个请求都失败。
- **决策。** 用有序列表表达:`@Business(code = "biz.retail", chain = {"ability.free-shipping", SELF})`。越靠前越先响应,`SELF` 代表业务自身,必须恰好出现一次。`@DefaultProvider` 和接口 `default` 永远排在链尾。
- **后果。**
  - (+)删字符串解析、自动编号、优先级冲突这一整类错误。
  - (−)迁移要把数字转成顺序。对注解方式注册的类可以机械完成:按数字升序排列,业务自身的优先级决定 `SELF` 的位置。手写 `IBusiness` 的要人工处理。
- **否决的方案。** 保留数字:保留一类运行期错误。数字加顺序混用:两套规则。

### D4 Admin 拆出主线

> **提议中的修订([ADR-0002](0002-simplify-user-facing-api.md) S8):** `@ExtensionPoint` 的 `scenarios` / `version` 一并去掉(它们只被 admin 展示用到)。

- **背景。** admin 是 2.1K 行 Java 加 2.0K 行前端,运行时依赖 JavaParser。"展示源码"靠 sources.jar、文件系统遍历和 `metadata.json`,生产环境脆弱。"冲突检测"的价值在 D3 和构建期校验之后大部分已被取代。
- **决策。** 主线只提供 Actuator endpoint(JSON:`Extensions.describe()` 与按身份的 `explain`)。UI 另起仓库消费该 JSON。"展示源码"砍掉(IDE 插件已有导航)。注解处理器改做编译期校验。
- **后果。**
  - (+)主线少约 2K 行 Java 和一个重依赖。处理器从"给 admin 抽数据"变成"在编译期拦错",更有价值。
  - (−)依赖内嵌 admin 的用户要迁到新 UI;3.x 的 admin 继续可用。
- **否决的方案。** 内嵌保留:继续背依赖和脆弱特性。

### D5 JDK 17 基线

- **背景。** Spring Boot 3.5 与 4.0 都支持 17。`invokeDefault` 需要 JDK 16+。稳定化分支已按 `--release 17` 编译,并在 JDK 17/21 × Boot 3.5/4.0 的矩阵上跑过。
- **决策。** 4.0 基线 17,CI 矩阵保持 17/21 × Boot 3.5/4.0。不使用 JDK 21 的 API;不支持 Boot 2.x 与 JDK 8–11。
- **否决的方案。** 21:没有它才有的必需 API,只会缩小用户面。

### D6–D9(无悬念)

- **D6 异常。** `ExtensionException`(基类)下三个具体类:`RegistryException`(构建期,聚合全部问题)、`ResolutionException`(开会话 / 解析身份)、`ExtensionNotFoundException`(调用期,没有实现者)。所有 API 去掉 `throws`。实现类抛出的异常原样透传,这是契约,有专门的契约测试。
- **D7 公开 API。** 约 15 个类型公开,其余放 `internal` 包;ArchUnit 守三条:`core` 不依赖 Spring、`internal` 不外泄、公开类型总数设预算上限。(ADR-0002 提议:预算改为 20,按 core 顶层类型计,不含嵌套类型、starter、test-kit。)
- **D8 starter。** 一个注册器(`ClassPathBeanDefinitionScanner` 加 include filter)把 `@Business` / `@Ability` / `@DefaultProvider` 类注册成普通 Bean,把 `@ExtensionPoint` 接口注册成 `@Primary` 的路由 Bean;`Extensions` 从容器收集(`getBeansWithAnnotation`,AOP 代理感知)。不能用"元注解 `@Component`",因为 core 零依赖,它的注解带不了 Spring 的注解。
- **D9 工程化。** 版本号改 `${revision}` 加 flatten(现在要手工同步 9 处),开发期用 `-SNAPSHOT`,顺带不再踩"japicmp 基线被 reactor 吞掉"的坑;加 BOM、`easy-extension-test`;加 ArchUnit 与 JMH 基准模块;发 OpenRewrite 迁移 recipe。

## 4. 与 3.x 的已知行为差异(有意的)

这些是新旧实现的差分比对里允许出现的差异,每一条都要在迁移指南里写明。

| # | 差异 | 3.x | 4.0 |
|---|---|---|---|
| 1 | **路由粒度** | 按扩展点接口:链上第一个"实现了该接口"的对象回答它的所有方法,包括它继承来的 `default` 体 | 按方法:一个对象只回答它**自己实现**的方法,没覆盖 `default` 方法就继续往下找。只影响带 `default` 方法的扩展点 |
| 2 | 身份里的能力 | 由各能力的 `match()` 决定 | 身份里列了业务没挂载的能力 → `ResolutionException` |
| 3 | 命名空间 | 业务码和能力码混在一个 map,同 code 后者静默覆盖前者 | 分属两个命名空间,各自内部重复才算错 |
| 4 | 优先级 | 数字;冲突在每次 `initSession` 才报 | 不存在 |
| 5 | 默认实现 | 一个类覆盖全部扩展点;`mandatory` | 接口 `default` / `@DefaultProvider`;抽象方法即必选 |
| 6 | 没有会话时调用 | `InvokeException` | `ResolutionException`,消息说明如何打开会话 |
| 7 | 命名 scope | 同一线程多个命名会话 | 同一请求里多个独立身份 = 多个 `Session` 对象,显式持有;线程上只绑定"当前"一个 |
| 8 | 异常 | 13 个类,12 个受检 | 3 个具体类,全部 unchecked |
| 9 | 注入 | `@ExtensionInject` 字段 | 普通注入(路由 Bean 为 `@Primary`) |

> [ADR-0002](0002-simplify-user-facing-api.md)(提议中)修订差异 2,并新增差异 10(能力默认全部启用)、11(请求期校验 `requires`)、12(`all(E)` 不含接口 `default` 体)。

## 5. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 路由的反射成本 | JMH 基线,以稳定化分支的实测为底线(落到默认实现 55 ns、链头命中 32 ns、`initSession` 100 个业务 0.65 µs,均为普通循环测得,**不是 JMH**,P1 先用 JMH 重测);`MethodHandle` 缓存;每个会话持有预计算的路由表 |
| Spring AOP / CGLIB 代理 | 注册表同时持有"实现类"和"实例"。starter 传入目标类读注解,调用走 Bean 本身,增强照常生效。沿用稳定化分支的 AOP 容器测试 |
| 路由 Bean 与业务 Bean 同为某扩展点类型,按类型注入有歧义 | 路由 Bean 设为 `@Primary`;P2 的容器测试覆盖 |
| 业务 Bean 注入 `Extensions` 造成循环依赖 | `Extensions` 在全部单例实例化之后构建(`SmartInitializingSingleton`);业务侧注入 `ObjectProvider<Extensions>`(ADR-0002 提议:注入的是门面,业务侧不需要 `ObjectProvider`) |
| 线程绑定与异步 | `Session.wrap(...)`;嵌套时恢复进入前的绑定(稳定化分支评审的高危项,做成契约测试) |
| 大爆炸重写 | 阶段出口标准(§6);契约测试加黄金文件(§7);新旧差分 |
| 迁移成本 | OpenRewrite recipe 加迁移指南;3.x 维护分支只收关键修复 |
| 用户依赖 admin UI | 3.x admin 继续可用;新 UI 仓库消费 JSON |
| Kotlin / Groovy 的接口默认方法 | 构建期检测 `Method.isDefault()` 并提示;文档说明 |

## 6. 路线图与出口标准

规模为粗估(S / M / L),不是承诺;每个阶段可以独立评审和回滚。

| 阶段 | 内容 | 规模 | 出口标准 |
|---|---|---|---|
| **P0 契约先行** | 本 ADR 与 API 草图;从稳定化分支提取行为契约(§7) | S | 草图评审通过并冻结;黄金文件已生成 |
| **P1 新 core(零依赖)** | `Extensions` 构建与冻结、`Identity`、链、`Session`、路由代理、拦截器、`explain`、`describe` | L | 契约测试通过(允许清单内的差异除外);JMH 不低于上述底线;ArchUnit 通过;core 覆盖率 ≥ 70% |
| **P2 新 starter** | 注册器、容器收集、路由 Bean、Web 绑定、属性、`TaskDecorator` | M | 容器测试在 Boot 3.5 / 4.0 × JDK 17 / 21 上全绿 |
| **P3 周边** | Actuator、test-kit、编译期校验处理器、BOM、`${revision}`、迁移 recipe、文档 | M | recipe 在样例工程上跑通;迁移指南覆盖 §4 全部差异 |
| **P4 试点到 GA** | sample 加一个真实业务线试点,RC,GA。IDE 插件和 Admin UI 各自跟进 | M | 试点业务线无阻塞问题 |

不要求 P1–P3 严格串行,但 P2 依赖 P1 的接口冻结。

> [ADR-0002](0002-simplify-user-facing-api.md)(提议中)修订 P2:"Web 绑定"扩展为"入口绑定"(`@WithIdentity` 的 AOP,加可选的 HTTP 请求头 filter);`TaskDecorator` 不再自动贡献,只提供 `ExtensionTaskDecorator`。同一文档还提出两项留给维护者决定的范围取舍(是否不带 Matcher 兼容层、首个 GA 是否瘦身),会影响 P1–P3 的内容。

## 7. 验证策略

1. **行为契约。** 稳定化分支的语义测试是验收集,结构性测试(Manager、Proxy 层)不迁移:

   | 现有测试 | 去向 |
   |---|---|
   | `SpringIntegrationTest`(JDK / CGLIB 代理、单实例) | 改写到新 starter,语义原样 |
   | `ProxyExceptionTransparencyTest` | 改写为路由代理的"异常透明"契约 |
   | `ChainHandOffTest`、`InterceptorRegistrationConcurrencyTest` | 改写为 `Session.run / wrap` 的嵌套恢复与并发契约 |
   | `InterceptorTest` | 原样(拦截器语义不变) |
   | `SpringWiringTest`、`SpringUpgradeCompatibilityTest` | 容器能启动、拦截器、不因多余 Bean 失败的部分保留;策略、selector、`@Primary` 默认实现随模型消失 |
   | `ResolutionPolicyTest`、`MultiDefaultImplementationTest`、`InheritedExtensionPointRegistrationTest` | 不迁移,意图由新的 resolver / `default` 方法测试覆盖 |
   | `RegistrySnapshotConcurrencyTest` 与各 Manager 测试 | 不迁移(结构性) |

2. **黄金文件。** 在稳定化分支上写一个小工具:给定场景(业务、能力、默认实现、扩展点)和一批请求,记录每个扩展点由哪个 code 回答、链是什么,输出成 `src/test/resources/contract/*.json`,提交进主线。新 core 的契约测试读它比对;只覆盖"方法全是抽象方法"的扩展点,带 `default` 方法的情形(§4 差异 1)用手写测试。这样不需要让新旧两套同名包同时出现在一个 classpath 里。
3. **JMH。** 场景:链头命中、落到默认实现、`open(identity)`(命中缓存 / 未命中)、多线程扩展性。
4. **ArchUnit。** `core` 不依赖 Spring 与 servlet;`internal` 只被 `core` 引用;公开类型数量设预算上限。
5. **japicmp。** 4.0 GA 后基线改为 4.0.0;开发期跳过(此前已对 3.x 基线验证过)。

## 8. 不做什么

类加载隔离与热部署;自带熔断(留拦截器,给 Resilience4j 写适配即可);表达式 DSL 匹配器;Boot 2 与 JDK 8–11;把 `scenarios` / `version` 做成强校验(它们保持为提示性元数据;ADR-0002 提议:这两个属性随 admin 一起去掉)。

## 9. 待 P1 内决定的开放问题

1. "取全部实现"(3.x 的 `invokeAll` / `invokeReduce`)的 API 形态:`Session.all(Class<E>)` 返回实现列表(草图里的做法),还是注入时标注。(草图暂定:`Extensions.all`,`Session.all` 在第 3 层。)
2. `Matcher` 放哪:`Matcher<P>` 接口在 core(业务、能力直接实现),匹配逻辑在 `Extensions.identityOf`,不另建模块(ADR-0002 S12,2026-10-04 修订)。
3. 包名:沿用 `io.github.xiaoshicae.extension.core`(推荐,迁移 recipe 简单),还是改根包。(草图暂定:沿用。)
4. `Extensions` 内按 `Identity` 缓存链的容量上限与淘汰策略。(草图暂定:默认 10000 条,LRU。)
5. 是否提供响应式(Reactor Context)适配。
6. Actuator 之外,是否保留一个最小的只读页面。

## 10. 相关文件

- [ADR-0002 用户面简化](0002-simplify-user-facing-api.md)(提议中)
- [4.0 API 草图](../design/v4-api-sketch.md)
- `CHANGELOG.md`(稳定化分支的变更与"Behavior changes"一节)
- `.claude/rules/api-compatibility.md`、`release.md`(4.0 GA 前,发版与兼容性规则按 major 版本处理)
