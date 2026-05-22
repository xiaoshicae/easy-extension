# easy-extension — Claude Code 工作指南

> Java 扩展点框架,以 SDK 方式发布到 Maven Central。本文档是 `.claude/` 体系的入口索引,详细规则在 `.claude/rules/` 下。

## 项目快照

- **类型**: Maven 多模块 SDK + 嵌入式管理后台
- **JDK**: 21
- **Spring**: Spring Boot 4.0.5 / Spring 7.0.6
- **发布**: Maven Central — `io.github.xiaoshicae:easy-extension-*`
- **当前版本**: 见根 `pom.xml` 的 `<version>` 与 `<easy-extension.version>`(写本文时 3.3.6)

### 模块拓扑

| 模块 | 类型 | 角色 |
|---|---|---|
| `easy-extension-core` | Maven | 框架核心,仅依赖 JDK + slf4j-api |
| `easy-extension-annotation-processor` | Maven | 编译期 APT,javaparser |
| `easy-extension-spring-boot-starter` | Maven | Spring 集成(依赖 core) |
| `easy-extension-admin-spring-boot-starter` | Maven | 管理后台(依赖 core + spring-web) |
| `easy-extension-admin-ui-frontend` | npm / React | 后台前端,作为 webjar 嵌入 admin-starter |
| `easy-extension-intellij-plugin` | Gradle / Kotlin | IDE 插件,独立发布到 JetBrains Marketplace |

模块依赖**单向**(starter → core,反向禁止)。详见 `rules/multi-module.md`。

---

## 双层防御模型

这个项目是 SDK,一次错误发布会破坏所有下游用户。`.claude/` 与 `.githooks/` 构成两道防线:

```
┌──────────────────────────────────────────────────────────────┐
│  AI 工作流层  ── .claude/skills/                             │
│    /commit   /push   /release-prep   /hotfix                 │
│       ↓                                                       │
│  物理 git hook 层  ── .githooks/                              │
│    pre-commit / pre-push 校验 CLAUDE_COMMIT=1 / CLAUDE_PUSH=1│
│       ↓                                                       │
│  AI bash guard 层  ── .claude/hooks/pre-bash-guard.sh        │
│    拦 --no-verify / force-push / 密钥访问 / mvn deploy 跳过  │
└──────────────────────────────────────────────────────────────┘
```

含义:

- AI 想绕开 → AI guard 挡(`--no-verify` 等手段在 Claude 这边被拦)。
- AI 或开发者忘记走 skill → 物理 hook 挡(裸 `git commit` 没有 `CLAUDE_COMMIT=1` 就被拒)。
- 用户终端紧急情况 → `git commit --no-verify` 主动逃生(只对人开放,AI 不可用)。

启用物理层(每次新 clone 一次,SessionStart hook 会自动检查):

```bash
git config core.hooksPath .githooks
```

---

## 工作流:何时用什么

### 提交代码

| 场景 | 用什么 | 做了什么 |
|---|---|---|
| 日常提交 | `/commit` | 模块级 `mvn compile` + 硬编码密钥扫描 + `System.out` 警告 |
| 推送到远程 | `/push` | 全量测试(`-amd` 含下游)+ 安全 + **API 兼容性** + 多模块版本一致性 + 规则审查 |
| Maven Central 发布前 | `/release-prep [新版本]` | 版本号一键同步 + Javadoc + 三件套构建 + GPG 签名 + tag 冲突检查 |
| 线上紧急修复 | `/hotfix "事故说明"` | 跳过重审查但保留密钥扫描,强制 audit log |

**不要**直接 `git commit` / `git push` —— 物理 hook 会拒,逼你走 skill。
**绝不要**手工 `mvn deploy` —— AI guard 会挡,Maven Central 只允许走 `/release-prep` 的完整流程。

### 召唤 subagent

| 场景 | agent | 不要让主线程自己做的事 |
|---|---|---|
| 改了 `public` / `protected` / `I*` 接口 | `java-api-compat-reviewer` | 兼容性表 |
| Java 代码改动后的全面增量审查 | `code-reviewer` | 正确性/并发/风格 |
| `mvn compile` 或 `mvn test` 报错 | `build-error-resolver` | 编译错误定位 |

主线程拿 agent 的结论后再决定动作,**不要重复他们的工作**。

---

## 规则索引

详细硬约束写在 `.claude/rules/`,**改对应类型的代码前主动读取**:

| 规则文件 | 适用场景 |
|---|---|
| `rules/api-compatibility.md` | 改 `public` / `protected` / `I*` 接口、注解、异常时 **必读** |
| `rules/code-style.md` | 写新 Java 代码(命名、日志、异常、Java 21 特性) |
| `rules/multi-module.md` | 跨模块改动 / 加新依赖 / 新建子模块 |
| `rules/release.md` | 准备发布到 Maven Central |
| `rules/testing.md` | 写或改测试代码(JUnit 5 + Mockito) |

---

## 硬约束(违反 = 自动拦截)

这些在 hook 层执行,记不住没关系——改错会被自动挡回:

- ❌ 生产代码用 `System.out` / `printStackTrace()` — `post-edit.sh` 直接 exit 2
- ❌ `git commit --no-verify` / `git push --force` 等绕过手段 — AI guard 拦
- ❌ `mvn deploy -DskipTests` / `-Dgpg.skip=true` — Maven Central 不接受
- ❌ 删除已发布的 `public` 元素 / 修改 `public` 方法签名 — 必须 bump major + CHANGELOG 标 BREAKING
- ❌ 读写 `~/.m2/settings.xml`、`~/.gnupg/` 等敏感路径 — AI guard 拦
- ❌ `core` 模块依赖 Spring / 任何 starter — 架构错误,反向依赖
- ❌ 给已发布接口加非 `default` 抽象方法 — 下游编译立即失败

---

## 多模块操作要点

- 改 `core` 的 API 后,**先** `mvn install -pl easy-extension-core -DskipTests`,把新版本装到本地 .m2,下游模块编译才看得到。
- 模块级编译: `mvn -pl <module> -am compile -DskipTests -Dgpg.skip=true`
- 跨模块测试: `mvn -pl <module> -amd test`(`-amd` = also-make-dependents,跑下游测试)
- 本地开发跳过 GPG: `-Dgpg.skip=true`(发布不允许)

`stop-check.sh` 在每轮响应结束自动跑改动模块的 `test-compile` + `-amd test`,失败会让 Claude 继续修。

---

## 子项目独立性

- `easy-extension-intellij-plugin` 用 Gradle / Kotlin,**不进 Maven 构建链**,独立发布。改这个模块时 `stop-check.sh` 会跑 `./gradlew compileKotlin`。
- `easy-extension-admin-ui-frontend` 用 npm,构建产物嵌入 `admin-starter`。前端文件改动只触发 `prettier --write`,不强行编译。

---

## 文件位置速查

```
.claude/
├── CLAUDE.md                ← 本文档,整个 .claude/ 的入口
├── settings.json            权限白/黑名单 + hooks 配置(团队共享)
├── settings.local.json      个人扩展白名单
├── hooks/
│   ├── pre-bash-guard.sh    PreToolUse(Bash) — 拦危险命令
│   ├── post-edit.sh         PostToolUse — Java 模块编译 + 风格扫描 + 前端 prettier
│   ├── session-start.sh     SessionStart — 提示 hooksPath / 保护分支
│   └── stop-check.sh        Stop — 跨模块 test-compile + test -amd
├── skills/
│   ├── commit/SKILL.md      /commit
│   ├── push/SKILL.md        /push
│   ├── release-prep/SKILL.md /release-prep [版本]
│   └── hotfix/SKILL.md      /hotfix "事故"
├── agents/
│   ├── java-api-compat-reviewer.md
│   ├── code-reviewer.md
│   └── build-error-resolver.md
└── rules/
    ├── api-compatibility.md
    ├── code-style.md
    ├── multi-module.md
    ├── release.md
    └── testing.md

.githooks/
├── pre-commit               要求 CLAUDE_COMMIT=1
├── pre-push                 要求 CLAUDE_PUSH=1
└── README.md                双层防御详解
```

---

## 给 Claude 自己的提醒

1. **改代码前先读规则**。改 Java 前读 `rules/code-style.md`;改 public API 前读 `rules/api-compatibility.md`。规则不是装饰。
2. **不要主动跑 `git commit` 或 `git push`**。让用户说"提交"或"推送",然后走对应 skill。
3. **subagent 是同事不是工具**。让 `code-reviewer` 评审、`build-error-resolver` 分析错误,你拿结果再行动,不要重复他们的工作。
4. **改了 `public` API 必须自检兼容性**,主动召唤 `java-api-compat-reviewer`。
5. **生产代码绝不用 `System.out`**。被 hook 挡到说明你没读 `rules/code-style.md`,回去读。
6. **版本号是死契约**。任何涉及 `<version>` 的改动必须走 `/release-prep` 同步全部 9 处(根 pom + 4 子模块 + properties + README + doc + frontend),不要手改其中一个。
