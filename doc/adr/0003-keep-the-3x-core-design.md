# ADR-0003:保持 3.x 核心设计,只修内部

| | |
|---|---|
| 状态 | **已采纳**(2026-10-04)。§5(优先级怎么表示)待维护者确认 |
| 决定 | 维护者,2026-10-04,原话:"按3.x 核心设计来吧，内部各种问题帮我优化，然后abilities的code用数字表示优先级确实有点小小问题,尤其是business本身也用数字表示优先级，看下有更优雅的实现吗" |
| 取代 | [ADR-0001](0001-v4-architecture.md)(4.0 架构方向)全部;[ADR-0002](0002-simplify-user-facing-api.md)(用户面简化)撤回 |
| 适用 | 公共 API 与模型的走向;分支 `feat/stability-and-resolution-model` 的发布方式 |

## 1. 背景

分支里有两类东西:

- **稳定化**:修 3.x 模型上的真问题(异常被包四层、AOP 代理的实现被静默跳过、Bean 实例化两次、换线程丢身份……)。公共 API 只做增量,japicmp 判定为 MINOR。
- **4.0 设计**:ADR-0001、ADR-0002、API 草图、README 预览,主张换掉用户面(显式 `Identity`、`overridingAbilities`、`@WithIdentity`、接口 `default` 代替默认实现类、普通注入……)。

维护者看到 RPC 完整样例后的反馈,原话:"怎么感觉比3.x版本易用和简洁性倒退了很多呢，我感觉3.x设计的对称性和暴露的功能的易用性都很好呢，只是内部实现有很多细节不到位呢"。

## 2. 决策

1. **3.x 的模型和公共 API 保持。** 业务和能力是同一种东西(`code` + `Matcher<T>` + 实现扩展点);业务用名字挂载能力;实现从一条按优先级排好的链上取;应用自己决定入口怎么得到匹配参数(`initSession(param)`,或 3.3.6 就有的 `ExtensionSessionScope.run`),框架负责解析和路由;`@ExtensionInject` 注入。
2. **内部问题照修,公共 API 只做增量**(japicmp 门禁守着)。行为变化写进 CHANGELOG 的 "Behavior changes"。本分支的稳定化按 **3.4** 发布:代码里的标签是 `@since 3.4`,pom 里的版本号等 `/release-prep` 时再改,最终叫 3.4.0 还是别的在那时定。
3. **以后新增 API:一次一个,只做增量,与现有概念对称,由真实痛点驱动,先写进 ADR 再实现。**
4. **撤回 4.0 的 API 重设计**:ADR-0002 撤回,ADR-0001 被取代,API 草图不再代表计划,README 的"4.0 新用法预览"删除。这些文档留在仓库里作为记录,各自带有状态说明。

## 3. 理由

4.0 稿在**对称性**上丢了东西:

| | 3.x | 4.0 稿 |
|---|---|---|
| 业务和能力 | 同一种东西:`code` + `Matcher<T>` + 扩展点实现 | 业务有 `abilities` / `overridingAbilities` 两个列表;能力的 `Matcher` 可有可无,显式身份下两种能力行为不同 |
| 谁先回答 | 一条规则:按数字优先级排序(业务默认 0,能力 `::n`) | 固定四段顺序 + 按方法逐个找 + 接口 `default` + `@DefaultProvider` |
| 身份从哪来 | 一条路:`initSession(param)` → 各个 `Matcher` | 两条路(匹配 / 显式),各有规则,外加 `Identity.with / only / without` |
| 参数类型 P | 全局一个(`@MatcherParam` 标在类上) | 每个类自己的 P,一个类只能有一个 |
| 入口 | 应用自己决定,框架不碰传输 | 框架接管入口:`@WithIdentity`(AOP)、请求头配置、`IdentityResolver`、RPC 配方 |

- **衡量简洁的标准用错了。** 4.0 稿用"公开类型数量"和"最短示例的行数"当指标;用户付出的成本是要同时记住多少条规则、模型是否一致。按后者,4.0 稿是倒退。入口这一项尤其明显:3.x 把"入口怎么拿到身份"留给应用,边界划得好;4.0 稿把它收进框架,边界一扩大,规则就跟着多了。
- **3.x 的问题在内部,不在用户面。** ADR-0001 §1.2 表里的问题(异常包装、AOP 丢失、双实例……)都是实现细节,修它们不需要换模型。本分支已经证明了这一点:公共 API 只增量。

## 4. 后果

- (+)用户不用迁移,也不用重新学一套模型。
- (+)ADR-0001 的 D0(并入 4.0,不单发 3.4.0)反转:这批变更按 3.4 发布。
- (−)行为变化(异常原样抛出、继承来的扩展点算数、被 AOP 代理的业务开始参与匹配……)在次版本里发布。CHANGELOG 的 "Behavior changes" 是写给用户的契约;要不要因此叫 4.0.0,是发版时的决定,与 API 设计无关。
- (−)ADR-0001 的 D2、D3 想解决的问题还在:默认实现要写一个类(已由 `mandatory` 和多默认实现缓解),数字优先级(见 §5)。

## 5. 待定:优先级怎么表示

**现状。** 链上每个成员一个数字:业务自己 `@Business(priority)`,默认 0;挂载的能力 `"code::N"`,没写数字时从 1 起按声明顺序自动编号(前一个数字加 1);默认实现是 `Integer.MAX_VALUE`。按数字升序排,相同数字报错。管理后台、`ResolveTrace` / `explain`、IDE 插件显示的都是这个数字。

**维护者指出的问题。** 能力的数字挤在 code 字符串里(`"ability.x::10"`),业务自己又有一个数字,两处要互相对得上。

**约束。** 只做增量,旧写法继续有效;`IBusiness#priority()`、`UsedAbility`、`ResolveTrace`、管理后台里的数字保持不变,手写的 `IBusiness`、管理后台和 IDE 插件不受影响。已有的两个备选:ADR-0001 D3 的 `chain` + `SELF`(单列表),ADR-0002 S2 的 `abilities` + `overridingAbilities`(两个列表)。确认后单独写 ADR。

## 6. 4.0 稿里留下的想法

都不是决定;每一项独立、增量,有真实痛点才做。

- 入口注解:`ExtensionSessionScope.run(ctx, param, body)` 的 AOP 语法糖。
- 一行把身份带进线程池(3.4 已有 `currentChain()` 加 `runWith`)。
- 工程化:`${revision}` 加 flatten(现在要手工同步 9 处版本号)、BOM、JMH 基准、ArchUnit(ADR-0001 D9)。与 API 无关。

## 7. 重新评估的触发条件

- 出现 3.x 模型表达不了、又不能用增量 API 解决的需求。
- 修内部问题需要破坏公共 API。
- 用户反馈入口样板(每个入口手写 `initSession` 并保证清理)确实是痛点 → 评估 §6 的入口注解。

## 8. 相关文件

- [ADR-0001](0001-v4-architecture.md)(已取代)、[ADR-0002](0002-simplify-user-facing-api.md)(已撤回)、[4.0 API 草图](../design/v4-api-sketch.md)(已撤回)
- `CHANGELOG.md`("Behavior changes" 与发布计划)
