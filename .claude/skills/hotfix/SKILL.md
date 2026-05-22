---
description: 线上紧急修复通道 - 跳过质量门禁但保留物理强制和审计
allowed-tools: Bash(git:*), Bash(CLAUDE_COMMIT=1 git commit*), Bash(CLAUDE_PUSH=1 git push*), Bash(echo:*), Read, AskUserQuestion
argument-hint: "<事故简述>"
---

# /hotfix — 线上紧急通道

**只用于真正的线上事故**:已发布到 Maven Central 的版本有严重 bug、需要立即出补丁。

## 设计哲学

- **不绕过物理 git hooks**:仍走 `CLAUDE_COMMIT=1 / CLAUDE_PUSH=1`,留下"经由 hotfix"的审计
- **跳过 `/commit` `/push` 的质量门**:不跑全套测试,但仍做最小安全扫描
- **强制 audit log**:在 commit message / push 说明里显式声明 `[HOTFIX]`

## 适用场景

✅ **适用**:
- 已发布版本有 NPE / 数据丢失 / 安全漏洞
- 客户报告核心扩展点功能完全不可用
- CI/CD 阻塞所有协作者(且根因在本仓库)

❌ **不适用**:
- "我赶时间"
- "测试太慢"
- "审查太严"
- "想跳过 API 兼容性检查"

如果你属于"不适用"场景而想用 hotfix,**先停下来想想**。SDK 项目的兼容性是承诺,跳过 = 破坏承诺。

## 步骤

### 1. 强制声明事故

询问用户:

- **事故描述**(必填,会写进 commit message)
- **影响范围**:已发布版本号 / 受影响用户群
- **是否已通知下游**(yes/no)
- **预计后续何时补质量门检查**(必填)

如用户给出的事故描述不像真事故 → 拒绝,引导走 `/commit` + `/push`。

### 2. 最小安全扫描

即便 hotfix 也**绝不允许**:

```bash
# 仍然扫密钥(死线)
grep -rE 'password\s*[:=]\s*"[^"]{8,}|secret\s*[:=]\s*"[^"]{8,}|sk-[a-zA-Z0-9]{20,}|AKIA[0-9A-Z]{16}' <changed-files>
```

发现 → 直接终止,提示用户。**密钥泄漏比线上事故更糟糕**。

### 3. 编译验证(最小)

```bash
mvn -q -pl <changed-modules> -am compile -DskipTests -Dgpg.skip=true
```

编译都过不了的 hotfix 别提交。

### 4. 提交 + 推送

```bash
git add -A
CLAUDE_COMMIT=1 git commit -m "$(cat <<'EOF'
[HOTFIX] <事故简述>

跳过的检查: full tests, coverage, API compat, rules audit
影响版本: <版本号>
通知下游: <yes/no>
后续质量门: <预计时间>

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
CLAUDE_PUSH=1 git push
```

### 5. 后续提醒

输出醒目提醒:

```
⚠️  Hotfix 已推送,记得在 <预计时间> 之前补做:
   - mvn -pl <modules> -amd test
   - /push 完整规范审查
   - 公共 API 兼容性检查
   - 如改了 public API,bump version 并同步 README/doc

跟踪: 在 GitHub issue 里记录本次 hotfix 的善后任务
```

## 用法

```
/hotfix "ExtensionContext NPE causing all SPI calls to fail in 3.3.6"
```

不传参数会强制询问事故描述。
