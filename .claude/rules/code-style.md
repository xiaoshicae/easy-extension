# Java 代码风格规范

> 基于项目实际代码风格抽取。Java 21 + Spring Boot 4.0.5 + Spring 7.0.6。

## 命名约定

| 类型 | 规则 | 示例 |
|---|---|---|
| 包名 | 小写单词,层级化 | `io.github.xiaoshicae.extension.core.proxy` |
| 接口 | **`I` 前缀** + PascalCase | `IExtensionPoint`, `IExtensionInvoker` |
| 抽象类 | **`Abstract` 前缀** + PascalCase | `AbstractExtensionPointDefaultImplementation` |
| 默认实现 | **`Default` 前缀** | `DefaultExtensionContext` |
| 异常 | **`Exception` 后缀** | `RegisterException`, `QueryException` |
| 工厂类 | **`Factory` 后缀** | `FirstMatchedExtPointProxyFactory` |
| 测试类 | **`Test` 后缀**(单数,**不要** `Tests`) | `DefaultExtContextTest` |
| 测试方法 | `testXxx()` | `testRegisterExtensionPoint()` |

## 包结构(`easy-extension-core`)

| 子包 | 职责 |
|---|---|
| `core` | 顶层 API(`I*` 接口)、`DefaultExtensionContext` |
| `core/extension` | 扩展点契约(`IExtensionPoint*`、`AbstractExtension*`) |
| `core/ability` | Ability 抽象 |
| `core/business` | Business 抽象 |
| `core/proxy` | 代理工厂(`*ProxyFactory`) |
| `core/annotation` | 注解定义 |
| `core/exception` | 自定义异常 |
| `core/interfaces` | 公共标记接口(如 `Priority`) |
| `core/session` | 会话管理 |
| `core/trace` | 调用链跟踪 |
| `core/util` | 工具类 |

新增类时**先确定包的归属**,不要往 `core/util` 倒。

## 强制要求

### 日志:用 SLF4J,不用 System.out

```java
// ✅ 正确
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.warn("extension {} not found", name);

// ❌ 错误
System.out.println("extension not found");
e.printStackTrace();
```

`.claude/hooks/post-edit.sh` 在生产代码(`src/main/`)里发现 `System.out/err.print*` 或 `printStackTrace()` 会直接 exit 2 让 Claude 自修。

### 异常:用项目自定义异常

参考已有:`RegisterException`、`QueryException`、`ExtensionException`。

```java
// ✅ 正确
throw new RegisterException("clazz should not be null");

// ❌ 避免
throw new RuntimeException("...");
throw new IllegalStateException("...");
```

公共 API 抛出新 **checked exception** 是破坏性变更(见 `api-compatibility.md`)。

### 输入校验放在方法入口

```java
public <T> void registerExtensionPoint(Class<T> clazz) throws RegisterException {
    if (clazz == null) {
        throw new RegisterException("clazz should not be null");
    }
    if (!clazz.isInterface()) {
        throw new RegisterException("clazz should be an interface type");
    }
    // ... 业务逻辑
}
```

错误消息**小写开头**,**简短描述事实**(看现有代码:`clazz should not be null`、`clazz should be an interface type`)。

## 推荐

### Java 21 特性

- ✅ `var` —— 局部变量类型推导,**只在右侧类型显而易见时用**
- ✅ `Pattern matching for switch` —— 处理多类型分支
- ✅ `Record` —— 不可变 DTO / value object
- ⚠️ `Sealed class` —— 可用于约束扩展点层次,但**对外公共 API 慎用**(下游无法扩展)

### Optional 用法

- ✅ 返回类型:`Optional<T> findExtension(String name)`
- ❌ 参数类型:**不要** `void foo(Optional<String> name)`
- ❌ 字段类型:**不要** 把 Optional 当 nullable 字段

### final 关键字

- ✅ 局部变量:**省略**(`var` 自带 final-ish 语义,显式 final 噪音)
- ✅ 方法参数:**可省**(项目已有风格保持一致)
- ✅ 字段:不可变字段加 `final`
- ✅ 工具类:`public final class Utils { private Utils() {} }`

## 禁止

- ❌ 在生产代码用 `System.out` / `System.err` / `printStackTrace()`(hook 拦)
- ❌ 静默吞 exception:`catch (Exception e) {}` 至少要 log
- ❌ `throw new RuntimeException("...")` 当成自定义异常用 —— 用项目已有异常类
- ❌ 给已发布的接口加非 default 抽象方法(见 `api-compatibility.md`)
- ❌ 给已发布的抽象类加抽象方法

## 文件级约定

- 单文件不超过 500 行(超了说明该拆类了)
- 单方法不超过 50 行(超了说明该拆方法了)
- 公共 API(`I*` 接口、`public` 方法)必须有 Javadoc
- import 顺序按 IDEA 默认 / Maven 风格(项目跟随 IDEA 设置即可,不强制 spotless)
