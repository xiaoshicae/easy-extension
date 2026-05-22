# 测试规范

> 项目使用 **JUnit 5 (Jupiter) + Mockito**。已有 38 个测试,保持现有风格。

## 框架

- JUnit Jupiter API 5.10.3
- Mockito 5.14.2
- 测试位置:`<module>/src/test/java/...`,**包结构与 main 镜像一致**

## 命名约定

| 类型 | 规则 | 示例 |
|---|---|---|
| 测试类 | 被测类名 + **`Test` 后缀**(单数) | `DefaultExtContextTest` |
| 测试方法 | `testXxx()`,`Xxx` 描述行为 | `testRegisterExtensionPoint()` |

**不要**用 `Tests`(复数)、`*IT`(集成测试)、`Spec`(BDD)、或 `should_*` 风格 —— 与现有不一致。

## 测试结构

参考 `DefaultExtContextTest`:

```java
package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtContextTest {

    @Test
    public void testRegisterExtensionPoint() throws Exception {
        // 1. 构造被测对象
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        // 2. 异常路径(用 assertThrows + 断言异常 message)
        RegisterException e = assertThrows(
            RegisterException.class,
            () -> context.registerExtensionPoint(null)
        );
        assertEquals("clazz should not be null", e.getMessage());

        // 3. 正常路径(直接调用,无异常即通过)
        interface ExtensionPoint1 {}
        context.registerExtensionPoint(ExtensionPoint1.class);
    }
}
```

## 强制要求

### 异常测试必须断言 message

```java
// ✅ 正确:同时断言异常类型 + message
RegisterException e = assertThrows(RegisterException.class, () -> ctx.foo(null));
assertEquals("clazz should not be null", e.getMessage());

// ❌ 不够:只断言异常类型,message 改了测试还能过
assertThrows(RegisterException.class, () -> ctx.foo(null));
```

理由:错误消息是公共 API 的一部分(下游可能 parse / log / display)。

### 并发测试单独成类

参考 `ConcurrencyTest.java`,涉及并发的测试单独建测试类,标注 `@Tag("concurrency")` 方便选择性跑。

### 不在测试代码里硬编码 sleep

```java
// ❌ 错误
Thread.sleep(1000);  // 等异步完成,flaky

// ✅ 正确
Awaitility.await().atMost(5, SECONDS).until(() -> context.isReady());
// 或者用 CountDownLatch / CompletableFuture
```

## Mockito 使用

```java
import static org.mockito.Mockito.*;

@Test
public void testInvoke() {
    IExtensionPoint mock = mock(IExtensionPoint.class);
    when(mock.execute(any())).thenReturn("ok");

    invoker.invoke(mock, request);

    verify(mock).execute(eq(request));
}
```

- 推荐用 `Mockito.mock()` + `when().thenReturn()`
- 避免 PowerMock / static mock(架构紧耦合信号)
- 不要 mock 项目自定义异常 —— 直接 new 一个

## 覆盖率

- **目标**:`easy-extension-core` >= 70% (核心框架,关键模块)
- **目标**:starter 模块 >= 50% (Spring 集成,边界用例)
- 工具:**不强制** Jacoco 集成(项目目前未配置),手动跑:

```bash
mvn test
# 看 surefire 输出 + IDEA 自带覆盖率工具
```

`/push` skill 暂不强制覆盖率门禁(项目未配 Jacoco),但发布前 `/release-prep` 会检查测试是否全部通过。

## 不要

- ❌ 测试类放在 `src/main/`(JUnit 不会跑,会被打包进 jar)
- ❌ 测试依赖外部资源(数据库 / 网络) —— 用 mock
- ❌ 测试之间共享状态(每个 `@Test` 应独立)
- ❌ 用 `@Disabled` 关测试 —— 修好或删掉,不要留烂代码

## CI / Hook 关联

- `.claude/hooks/stop-check.sh` 在 Claude 改完代码后跑 `mvn -pl <modules> -amd test`
- `/push` skill 会跑完整测试
- `/commit` 不跑测试(轻量)
