#!/bin/bash
# PreToolUse(Bash) hook: 拦截 AI 绕过质量门禁/物理强制的危险操作
#
# 重要: 本 hook 只拦截 Claude(AI) 的 Bash 调用,不影响用户在终端直接操作
# 用户紧急上线场景: 在终端直接跑 git commit --no-verify 完全正常工作

set -uo pipefail

command -v jq >/dev/null 2>&1 || exit 0

INPUT=$(cat -)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
[ -n "$CMD" ] || exit 0

# 1. 拦 --no-verify (AI 绕过 git hook 的常见手段)
if echo "$CMD" | grep -qE '(^|[^A-Za-z_-])--no-verify($|[^A-Za-z_-])'; then
  cat >&2 <<'EOF'
BLOCKED: 禁止 AI 使用 --no-verify 绕过 git hook
合法路径:
  - 标准提交: /commit
  - 标准推送: /push
  - 线上紧急: /hotfix (显式紧急通道,跳过质量门禁但保留审计)
说明: 用户本人在终端直接 git commit --no-verify 不受此 hook 影响
EOF
  exit 1
fi

# 2. 拦 force push 到保护分支
if echo "$CMD" | grep -qE 'git[[:space:]]+push.*(-f|--force|--force-with-lease)(\b|$)'; then
  CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  case "$CURRENT_BRANCH" in
    main|master|release/*|release-*)
      echo "BLOCKED: 禁止 force push 到保护分支 ($CURRENT_BRANCH)" >&2
      echo "如需修正历史,在 feature 分支上操作后再合并" >&2
      exit 1
      ;;
  esac
fi

# 3. 拦敏感文件访问 (GPG 私钥 / .env / 密钥文件 / Maven settings)
if echo "$CMD" | grep -qE '(secring\.|\.gpg(\b|$)|\.env(\b|$|\.)|\.pem(\b|$)|\.p12(\b|$)|/\.m2/settings\.xml|/\.gnupg/)'; then
  # 例外: 只读操作可放行 (cat/head/grep --list/list-secret-keys 等元数据查询)
  if echo "$CMD" | grep -qE '^(gpg[[:space:]]+--list|gpg[[:space:]]+--version|chmod[[:space:]]+600[[:space:]])'; then
    :
  else
    echo "BLOCKED: 检测到访问敏感文件 (GPG 私钥 / .env / Maven settings)" >&2
    echo "如确需访问,在终端中由用户本人执行" >&2
    exit 1
  fi
fi

# 4. 拦 git reset --hard
if echo "$CMD" | grep -qE 'git[[:space:]]+reset[[:space:]]+--hard'; then
  echo "BLOCKED: git reset --hard 会丢失未提交工作,请显式确认意图" >&2
  echo "替代方案: git stash 暂存,或 git revert 安全回退" >&2
  exit 1
fi

# 5. 拦 rm -rf 危险路径
if echo "$CMD" | grep -qE 'rm[[:space:]]+-[a-zA-Z]*r[a-zA-Z]*f?[[:space:]]+(/[[:space:]]|/$|\.git[[:space:]]|\.git$|~|\$HOME)'; then
  echo "BLOCKED: 检测到危险的 rm 操作" >&2
  exit 1
fi

# 6. 拦 mvn deploy 跳过测试/GPG (Maven Central 发布最容易出错的点)
if echo "$CMD" | grep -qE 'mvn[[:space:]].*deploy'; then
  if echo "$CMD" | grep -qE '(-DskipTests|-Dmaven\.test\.skip|-Dgpg\.skip)'; then
    echo "BLOCKED: 禁止 mvn deploy 跳过测试或 GPG 签名 (Maven Central 强要求)" >&2
    echo "发布前请走 /release-prep 做完整检查" >&2
    exit 1
  fi
fi

# 7. 提醒 (不阻止): 直接修改 pom.xml <version> 时建议走 /release-prep
if echo "$CMD" | grep -qE '(sed|perl).*-i.*<version>.*pom\.xml'; then
  echo "[Guard] 提醒: 修改 <version> 会涉及多模块同步,建议走 /release-prep" >&2
fi

exit 0
