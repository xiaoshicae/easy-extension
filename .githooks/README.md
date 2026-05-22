# .githooks/

项目级 git hooks,与 `.claude/` 配套构成"双层防御":

| 层 | 作用范围 | 实现 |
|---|---|---|
| **物理层** (本目录) | 任何 `git commit` / `git push` | 检查 `CLAUDE_COMMIT=1` / `CLAUDE_PUSH=1` 环境变量 |
| **AI 软层** (`.claude/hooks/`) | Claude 调用的 Bash | `pre-bash-guard.sh` 拦 `--no-verify` 等绕过手段 |

物理层挡住"忘记走 skill 直接提交";AI 软层挡住"AI 自作主张绕过 skill"。

## 启用

```bash
git config core.hooksPath .githooks
```

只需要执行一次(本仓库)。可以加到 README 的"开发者入门"步骤里,让所有协作者都启用。

## 禁用

```bash
git config --unset core.hooksPath
```

## 合法工作流

| 谁 | 怎么做 |
|---|---|
| **AI (Claude)** | 调用 `/commit` 或 `/push` skill,自动带 `CLAUDE_*=1` |
| **用户本人 / 标准提交** | `CLAUDE_COMMIT=1 git commit -m "..."` |
| **用户本人 / 标准推送** | `CLAUDE_PUSH=1 git push` |
| **用户本人 / 紧急绕过** | `git commit --no-verify` (绕过 pre-commit) 或 `git push --no-verify` (绕过 pre-push) |
| **AI / 紧急绕过** | `/hotfix` skill (`--no-verify` 在 AI 那边被 `.claude/hooks/pre-bash-guard.sh` 拦截) |

## 设计要点

- **AI 不能用 `--no-verify`**:`pre-bash-guard.sh` 拦截这个手段,逼 AI 走 `/hotfix`
- **用户可以用 `--no-verify`**:终端紧急上线场景的逃生口
- **物理 hook 不可被 AI 关闭**:就算 AI 试图修改 `core.hooksPath`,也写在 deny 清单里

## 调试

如果 hook 执行报错:

```bash
bash -x .githooks/pre-commit   # 看脚本到底为啥拦
git commit --no-verify         # 紧急放行
```
