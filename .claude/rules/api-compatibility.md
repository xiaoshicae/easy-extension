# 公共 API 兼容性规则

> easy-extension 是被外部项目依赖的 SDK,**任何破坏性变更都会传递给所有下游用户**。

## 公共 API 的定义

下列在 `easy-extension-core` / `easy-extension-spring-boot-starter` / `easy-extension-admin-spring-boot-starter` 中,**对外可见**的元素,都属于"公共 API":

- `public` 接口 / 类 / 注解 / 枚举 / 异常
- `public` 方法、构造器、字段
- `protected` 方法和字段(可被下游子类化的部分)

约定:`I` 前缀接口(`IExtensionPoint`、`IExtensionInvoker`、`IExtensionReader` 等)是**最稳定的契约**,这些接口的变更要求最严。

## 必须遵守(违反需 bump major version)

### 禁止删除

- ❌ 删除 `public` 接口 / 类 / 异常 / 注解
- ❌ 删除 `public` / `protected` 方法
- ❌ 删除 `public` / `protected` 字段
- ❌ 删除 `public` 枚举值

### 禁止修改签名

- ❌ 修改 `public` 方法的参数数量、类型、顺序
- ❌ 修改 `public` 方法的返回类型(协变除外:子类化 OK)
- ❌ 给 `public` 方法添加新的 checked exception
- ❌ 修改注解元素的 `default` 值(下游编译值会变)

### 禁止接口加方法(无默认实现)

- ❌ 给已发布的 `I*` 接口添加抽象方法(下游实现立即编译失败)
- ✅ 可以加 `default` 方法(Java 8+)
- ✅ 可以加新接口,让旧接口继承新接口

### 禁止抽象类破坏继承

- ❌ 给已发布的 `Abstract*` 类添加抽象方法
- ✅ 可以加具体方法(有默认实现)
- ✅ 可以加 `protected` 钩子方法

## 推荐(良好实践)

- ✅ 新增 `public` 方法 / 类 / 接口(纯增量)
- ✅ 用 `@Deprecated(since = "x.y.z", forRemoval = true)` 标记淘汰路径,**至少保留一个 minor version 再删**
- ✅ 修改 `private` / package-private 实现:随便改
- ✅ 异常类继承层次保持稳定(`RegisterException`、`QueryException` 等)

## 兼容性变更类型与版本影响

| 变更 | 兼容性 | 版本号 |
|---|---|---|
| 新增 `public` 方法 / 类 | ✅ 兼容 | bump **patch** 或 **minor** |
| 新增接口 `default` 方法 | ✅ 兼容 | bump **minor** |
| 给接口加抽象方法 | ❌ 破坏 | bump **major** |
| 删 `public` 方法 / 类 | ❌ 破坏 | bump **major** |
| 修改 `public` 方法签名 | ❌ 破坏 | bump **major** |
| 内部实现重构(行为不变) | ✅ 兼容 | bump **patch** |
| 修复 bug 改变行为 | ⚠️ 半兼容 | bump **patch** 或 **minor**,在 CHANGELOG 说明 |

## 检查方法

`/push` skill 会自动跑这套规则。手动审查时:

```bash
# 列出本分支相对 main 改动的 public 类
git diff origin/main..HEAD --name-only -- 'easy-extension-core/src/main/java/**/*.java' \
  'easy-extension-*-starter/src/main/java/**/*.java'

# 看 public 接口 / 抽象类的具体改动
git diff origin/main..HEAD -- 'easy-extension-core/src/main/java/**/I*.java'
git diff origin/main..HEAD -- 'easy-extension-core/src/main/java/**/Abstract*.java'
```

人工审查时关注:
1. 接口签名变更 → 立即拦下,要求 default 方法 或 新接口策略
2. 删除元素 → 立即拦下,要求 `@Deprecated` 过渡
3. checked exception 增加 → 立即拦下,要求 RuntimeException 包装

## 注释规范

公共 API 必须有 Javadoc:

```java
/**
 * 注册扩展点。
 *
 * @param clazz 扩展点接口类(必须是接口)
 * @throws RegisterException 如果 clazz 为 null、不是接口、或重复注册
 */
public <T> void registerExtensionPoint(Class<T> clazz) throws RegisterException;
```

`/release-prep` skill 会扫描 public class/method 缺少 Javadoc 的情况。
