---
description: 创建 Git 提交 (轻量质量门 - 编译 + 扫密钥 + 静态检查)
allowed-tools: Bash(git:*), Bash(mvn:*), Bash(grep:*), Bash(find:*), Bash(CLAUDE_COMMIT=1 git commit*), Bash(echo:*), Read, Glob, Grep, AskUserQuestion
argument-hint: "[提交信息]"
---

# /commit — 创建 Git 提交(轻量门)

## 步骤

### 1. 检查变更范围

```bash
git status
git diff --stat
git diff --cached --stat
```

识别变更涉及的模块(从路径前缀):

| 路径前缀 | 模块 |
|---|---|
| `easy-extension-core/` | core |
| `easy-extension-annotation-processor/` | annotation-processor |
| `easy-extension-spring-boot-starter/` | spring-boot-starter |
| `easy-extension-admin-spring-boot-starter/` | admin-spring-boot-starter |
| `easy-extension-admin-ui-frontend/` | 前端 (npm) |
| `easy-extension-intellij-plugin/` | IntelliJ 插件 (Gradle) |
| 顶层文件 (`pom.xml`, `README.md`, `doc/`) | 文档/全局 |

### 2. 快速质量检查

**仅当 `.java` 有变更时执行:**

#### 2.1 编译验证(模块级)

```bash
mvn -q -pl <changed-modules> -am compile -DskipTests -Dgpg.skip=true
```

编译失败 → 终止,把错误反馈给用户。

#### 2.2 快速代码扫描

读取 `git diff --cached`(或 `git diff`),扫描以下问题(仅生产代码,即 `src/main` 下):

| 检查项 | 模式 | 处理 |
|---|---|---|
| **硬编码密码** | `password\s*[:=]\s*"[^"]{8,}` | 阻止 |
| **硬编码密钥** | `secret\s*[:=]\s*"[^"]{8,}` | 阻止 |
| **硬编码 token** | `token\s*[:=]\s*"[^"]{8,}` | 阻止 |
| **API key** | `api[_-]?key\s*[:=]\s*"[^"]{8,}` | 阻止 |
| **OpenAI / Anthropic key** | `sk-[a-zA-Z0-9]{20,}` | 阻止 |
| **AWS access key** | `AKIA[0-9A-Z]{16}` | 阻止 |
| **GPG passphrase** | `gpg\.passphrase` | 阻止 |
| **Maven Central token** | `oss\.sonatype.*password` | 阻止 |
| **System.out 输出** | `System\.(out\|err)\.(print\|println\|printf)\(` | 警告 |
| **printStackTrace** | `\.printStackTrace\(\s*\)` | 警告 |
| **忽略 error** | `}\s*catch\s*\([^)]+\)\s*\{\s*\}` | 警告 |

**前端有变更时**:`grep` 同一组密钥模式扫描 `easy-extension-admin-ui-frontend/src/` 下的 `.ts/.tsx/.js/.jsx`。

- 发现**阻止级**问题 → 输出列表,询问用户:取消提交 / 强制提交。
- 发现**警告级**问题 → 输出列表,提示用户但继续。
- 全通过 → 输出 `✅ 快速检查通过`。

### 3. 暂存并提交

如果用户没传提交信息,根据 diff 自动生成。**遵循项目现有 commit 风格**:

```
<type>: <短描述>
```

类型(参考最近 commit 历史):`feat` / `fix` / `refactor` / `docs` / `style` / `test` / `chore`

```bash
git add -A
CLAUDE_COMMIT=1 git commit -m "$(cat <<'EOF'
<type>: <描述>

可选: 详细说明 (如有)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

**重要**:必须 `CLAUDE_COMMIT=1` 前缀,否则 `.githooks/pre-commit` 会拒绝。

## 不做什么

- ❌ 不跑测试(留给 /push)
- ❌ 不做公共 API 兼容性审查(留给 /push)
- ❌ 不做覆盖率检查(留给 /push)
- ❌ 不做 Javadoc 完整性(留给 /release-prep)

commit 是高频操作,**只做秒级检查**;严格审查在 push 时统一做。

## 用法

```
/commit                          # 自动生成提交信息
/commit "fix: handle null SPI"   # 用户提供信息
```
