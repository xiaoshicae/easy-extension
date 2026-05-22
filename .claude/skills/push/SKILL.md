---
description: 推送到远程仓库 (严格质量门 - 测试 + 安全 + API 兼容 + 规范审查)
allowed-tools: Bash(git:*), Bash(mvn:*), Bash(./gradlew:*), Bash(grep:*), Bash(find:*), Bash(CLAUDE_PUSH=1 git push*), Bash(echo:*), Read, Glob, Grep, AskUserQuestion
argument-hint: ""
---

# /push — 推送到远程(严格门)

这是 SDK + Maven Central 项目的**最后一道防线**。比 `/commit` 严格得多。

## 步骤

### 1. 状态检查

```bash
git status
git branch --show-current
git log --oneline origin/main..HEAD
```

如有未提交变更 → 提示用户先 `/commit`。

### 2. 严格质量审查

识别本次 push 涉及的改动模块(对比 `origin/main..HEAD` 的 diff):

```bash
git diff --name-only origin/main..HEAD | awk -F/ '/\.java$/ && /^easy-extension-/ {print $1}' | sort -u
```

**仅当有 Java 改动时**,执行 2.1 - 2.5:

#### 2.1 编译验证(全量)

```bash
mvn -q -pl <changed-modules> -am test-compile -Dgpg.skip=true
```

#### 2.2 测试 + 反向依赖(跨模块影响)

```bash
mvn -q -pl <changed-modules> -amd test -Dgpg.skip=true
```

`-amd` (also-make-dependents) 把改动模块的**下游模块**也一起跑测试。例:改 `easy-extension-core` 会触发 4 个 starter 的测试。

#### 2.3 安全扫描

扫 `git diff origin/main..HEAD` 是否引入硬编码凭证(同 /commit 的模式表)。**push 阶段一旦发现一律阻止**(commit 是询问,push 是硬拦)。

额外扫描:

```bash
# .m2/settings.xml 不应被提交
git diff --name-only origin/main..HEAD | grep -E '(settings\.xml|\.gnupg|secring|\.gpg|\.env)' && exit 1
```

#### 2.4 公共 API 兼容性扫描 (⭐ SDK 关键)

这是 SDK 项目专属的检查 —— **公共 API 的不兼容变更会破坏所有下游用户**。

策略:对比 `origin/main..HEAD` 中 `easy-extension-core/src/main/java/**/*.java` 的变更,识别:

| 变更类型 | 处理 |
|---|---|
| 删除 `public` 类 / `public` 方法 | ❌ **阻止** |
| 修改 `public` 方法签名(参数 / 返回值 / 异常) | ❌ **阻止** |
| 删除 `public` 字段 | ❌ **阻止** |
| 给 `public abstract` 类添加新 `abstract` 方法 | ❌ **阻止** |
| 修改注解的 `value()` 等方法 | ❌ **阻止** |
| `protected` 同上但**警告**(不阻止) | ⚠️ 警告 |
| 新增 `public` 方法/类 | ✅ 通过 |

操作:`git diff origin/main..HEAD -- 'easy-extension-core/src/main/java/**/*.java'`,逐文件审查变更行。

发现不兼容变更 → 输出表格(文件、类、变更类型、说明),询问用户:

- **取消推送,先做迁移方案**(推荐)
- **接受不兼容变更,需要 bump major version**(写到 commit / PR 说明里)
- **标 @Deprecated 而不是直接删除**(保留兼容)

#### 2.5 多模块版本号一致性

```bash
# 所有 pom.xml 的 <version> 必须一致
for p in pom.xml easy-extension-*/pom.xml; do
  grep -E '^[[:space:]]*<version>[0-9]' "$p" | head -1
done | sort -u
```

如果出现 2 行以上版本号 → ❌ 阻止,提示走 `/release-prep`。

```bash
# README.md 中 dependency 版本号必须与根 pom version 一致
ROOT_VERSION=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\([^<]*\)<.*/\1/')
grep -E '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' README.md
```

不一致 → 警告(README 是文档,push 阶段不强拦,但建议在本 PR 内修复)。

#### 2.6 项目规范审查 (vs `.claude/rules/`)

针对变更涉及的文件,对照规则文件逐条检查:

| 规则文件 | 审查内容 |
|---|---|
| `.claude/rules/api-compatibility.md` | 公共 API 兼容性细则(参见 2.4) |
| `.claude/rules/code-style.md` | Java 21 + Spring Boot 4 风格(`var` 慎用、`Optional` 用法、null 处理) |
| `.claude/rules/testing.md` | JUnit 5 注解、Mockito 用法、断言风格 |
| `.claude/rules/multi-module.md` | 模块依赖方向(starter 可依赖 core,反向禁止) |
| `.claude/rules/release.md` | 涉及 pom.xml / README.md 版本时是否一致变更 |

执行:`git diff origin/main..HEAD` → 逐文件读上下文 → 对照规则。

结果:
- **无违规**:`✅ 规范审查通过`
- **有违规**:表格输出,询问:取消 / 强制(忽略违规)

### 3. 推送

所有检查通过(或用户选择强制)后:

```bash
CLAUDE_PUSH=1 git push -u origin <current-branch>
```

**重要**:必须 `CLAUDE_PUSH=1` 前缀,否则 `.githooks/pre-push` 会拒绝。

如果之前已经 push 过(分支已 track 远程),`-u origin <branch>` 可省略,直接 `CLAUDE_PUSH=1 git push`。

### 4. 推送后

输出:
- ✅ 推送成功:`<branch>` → `<remote>`
- 提示:如这是 release 分支,下一步走 `/release-prep` 检查 Maven Central 发布

## 用法

```
/push        # 推送当前分支到远程,跑严格质量门
```
