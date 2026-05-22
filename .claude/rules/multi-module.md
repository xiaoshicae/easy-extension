# 多模块依赖方向规则

> easy-extension 是 4 模块 Maven 工程(+ 1 个独立 Gradle 子项目 + 1 个前端)。
> **模块依赖方向是 SDK 项目的生死线**,反向依赖会导致循环或破坏可移植性。

## 模块拓扑

```
                            easy-extension (pom, BOM)
                                    │
                ┌───────────────────┼─────────────────────┐
                ▼                   ▼                     ▼
       easy-extension-core    annotation-processor   (其他独立模块)
                ▲                                          
                │                                          
       依赖于 core 的下游:                                  
                │                                          
       ┌────────┴─────────────────────────┐               
       ▼                                  ▼               
  easy-extension-                   easy-extension-       
  spring-boot-starter         admin-spring-boot-starter   
```

**独立子项目**(不在 Maven 根模块声明里):
- `easy-extension-intellij-plugin` — Gradle / Kotlin,IDE 插件,完全独立
- `easy-extension-admin-ui-frontend` — npm / React,管理后台 UI

## 强制依赖规则

### 允许的依赖方向

| 模块 | 可依赖 |
|---|---|
| `easy-extension-core` | 仅 JDK + slf4j-api |
| `easy-extension-annotation-processor` | JDK + javaparser(注解处理时编译期工具) |
| `easy-extension-spring-boot-starter` | core + spring-boot-starter |
| `easy-extension-admin-spring-boot-starter` | core + spring-boot-starter-web + spring-boot-starter |

### 禁止的依赖方向

- ❌ `core` 依赖任何 starter(starter 是上层适配,反向依赖破坏可移植性)
- ❌ `core` 依赖 Spring 任何 artifact
- ❌ `annotation-processor` 依赖运行时模块(注解处理只在编译期)
- ❌ `spring-boot-starter` 依赖 `admin-spring-boot-starter`(同层禁止互相依赖)
- ❌ 任意模块依赖 IntelliJ 插件 / 前端

### 检查命令

```bash
# 看 core 的实际依赖(应当极少)
mvn -pl easy-extension-core dependency:tree -DskipTests

# 全模块依赖图
mvn dependency:tree -DskipTests | grep -E '^\['
```

如果发现 core 依赖了 Spring → 立即修复,这是破坏性的架构错误。

## 版本号同步规则

所有 4 个 Maven 模块的 `<version>` 必须**完全一致**:

```bash
# 检查所有 pom.xml 的 version 是否一致
for p in pom.xml easy-extension-*/pom.xml; do
  echo -n "$p: "
  grep -m1 -oE '<version>[^<]+</version>' "$p" | head -1
done
```

期待输出 4 行(根 + 3 个子模块)+ 父引用,**版本号必须相同**。

### 版本号引用方式

子模块的 `<version>` 不应硬编码,而是用 properties:

```xml
<!-- pom.xml (root) -->
<properties>
    <easy-extension.version>3.3.6</easy-extension.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.xiaoshicae</groupId>
            <artifactId>easy-extension-core</artifactId>
            <version>${easy-extension.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`/release-prep` skill 会扫这个一致性。

## 模块边界与重构

### 加新功能时

1. 确定该功能属于**哪一层**:
   - 框架抽象 → `core`
   - Spring 集成 → `spring-boot-starter`
   - 管理后台 → `admin-spring-boot-starter`
   - 编译期处理 → `annotation-processor`
2. 如果跨模块,**先把抽象沉到 `core`,再在上层做 Spring/Web 适配**
3. 严禁在 starter 里写"通用扩展点逻辑" —— 那是 core 的活

### 跨模块改动 = 跨模块测试

`stop-check.sh` 用 `mvn -pl <changed> -amd test`(also-make-dependents)自动跑下游模块测试。

举例:改 `easy-extension-core` 的接口 → 自动跑 4 个模块的测试,因为所有 starter 都依赖 core。

### IntelliJ 插件 / 前端是独立体

- IntelliJ 插件用 Gradle,**不参与 mvn 构建链**
- 前端 npm 模块,**不参与 mvn 构建链**
- 主体 Maven 项目的发布(Maven Central)**不包含**这两个 —— 它们各自的发布通道独立

## 包名约束(架构防腐)

| 模块 | 允许的根包 |
|---|---|
| core | `io.github.xiaoshicae.extension.core.*` |
| annotation-processor | `io.github.xiaoshicae.extension.annotation.*` |
| spring-boot-starter | `io.github.xiaoshicae.extension.spring.*` |
| admin-spring-boot-starter | `io.github.xiaoshicae.extension.admin.*` |

不要跨模块共用包名(避免 split package,Java 9+ module system 会炸)。
