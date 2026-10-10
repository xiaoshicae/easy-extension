# 从 4.x 升级到 5.0

5.0 只有一处不兼容:**识别业务只剩一种方式——每个业务自己的 `match`**。`BusinessResolver`、`BusinessSelector` 以及 `easy-extension.business-match-order` 被删除;多个业务同时匹配在任何模式下都报错。其余 API 与 4.1 相同。

## 为什么

- **新增业务只改一处**:每个业务用 `match` 认领自己的请求。`BusinessResolver` 在身份需要"翻译"时(例如渠道 + 租户决定业务)会长成一个集中的 `switch`,新增业务要回头改它——这正是扩展点要消灭的 if-else。
- **少一种心智模型**:"match 还是 resolver"二选一、"resolver 下业务可以不实现 Matcher"、"非严格模式下多匹配由 selector 或 match-order 决定"这些分支都没有了。
- **多个业务同时匹配是 `match` 写重叠了**,框架不再替你挑一个。`allow-unknown-business` 的含义也因此与名字一致:只放行"没有业务匹配"。
- 性能不是保留 resolver 的理由:逐个 `match` 是 O(业务数),实测 1000 个业务、匹配到最后一个约 2.6 µs(Go 版同等实现),相对一次 Web 请求可以忽略。

## 步骤

### 1. 删掉 `BusinessResolver`,让每个业务实现 `match`

```java
// 4.x
@Bean BusinessResolver<String> businessResolver() { return Optional::ofNullable; }

@Business(code = "biz.retail")
public class RetailBusiness implements FreightCalcExtension { ... }

// 5.0
@Business(code = "biz.retail")
public class RetailBusiness implements Matcher<String>, FreightCalcExtension {
    @Override
    public boolean match(String code) { return "biz.retail".equals(code); }
    ...
}
```

匹配参数是自定义类型时同理:`return "retail".equals(param.getBizCode());`。

编程式装配:`BusinessDefinition.of(code, matcher, impl)` 的 `matcher` 不能再是 `null`,否则 `build()` 报 `business [code] should implement Matcher`。

### 2. 删掉 `BusinessSelector` 和 `business-match-order`

- 删除 `BusinessSelector` / `OrderedCodeBusinessSelector` 的 Bean 和 `builder.businessSelector(...)` 调用。
- 删除配置 `easy-extension.business-match-order`。它在配置元数据(`spring-configuration-metadata.json`)里标为 `error` 级别的废弃项,读取配置元数据的 IDE 会据此提示;没删的话 Spring Boot 会忽略这个属性,行为按下一条。
- 如果你依赖它在多个匹配中挑一个:收紧各业务的 `match`,让它们互斥。5.0 起多匹配一律 `ResolutionException(MULTIPLE_BUSINESSES_MATCHED)`,异常信息里列出了匹配到的业务 code,方便定位。

### 3. 处理 `BUSINESS_NOT_FOUND`

`ResolutionException.Reason.BUSINESS_NOT_FOUND` 被删除(它只由 resolver / selector 产生)。业务码没有对应的业务时,现在就是"没有业务匹配":严格模式 `NO_BUSINESS_MATCHED`,`allow-unknown-business=true` 时走默认实现。按 `reason()` 判断错误的代码,把 `BUSINESS_NOT_FOUND` 分支并入 `NO_BUSINESS_MATCHED`。删掉它之后 `EXTENSION_NOT_FOUND` 的 `ordinal()` 从 4 变为 3:按枚举常量或 `name()` 判断的代码不受影响,持久化或比较 `ordinal()` 的需要调整。

### 4. 匹配参数类型

业务的 `Matcher<T>` 现在总是参与匹配参数类型的推导。4.x 里配置了 resolver 的应用,如果业务类声明的 `Matcher<X>` 与能力不一致,升级后启动会报 `abilities and businesses match on different parameter types`:统一成同一个类型,或设置 `easy-extension.matcher-param-type`。

## 对照表

| 4.x | 5.0 |
|---|---|
| `BusinessResolver<T>` Bean / `builder.businessResolver(...)` | 删除;每个业务 `match` 自己的 code |
| 业务可以不实现 `Matcher`(配置了 resolver 时) | 业务必须实现 `Matcher<T>` |
| `BusinessSelector<T>` / `OrderedCodeBusinessSelector` / `builder.businessSelector(...)` | 删除 |
| `easy-extension.business-match-order` | 删除;多匹配一律报错 |
| 非严格模式下多个业务匹配:selector / match-order 选一个 | 任何模式下报 `MULTIPLE_BUSINESSES_MATCHED` |
| `Reason.BUSINESS_NOT_FOUND` | 删除;归入 `NO_BUSINESS_MATCHED` |
| 包 `io.github.xiaoshicae.extension.core.spi` | 删除 |
| `DeferredExtensionContext(beanFactory, properties, resolver, selector)` | `DeferredExtensionContext(beanFactory, properties)` |
| `EasyExtensionAutoConfiguration.extensionContext(beanFactory, properties, resolver, selector)` | `extensionContext(beanFactory, properties)` |
| `EasyExtensionConfigurationProperties.getBusinessMatchOrder()` / `setBusinessMatchOrder(...)` | 删除 |
