#!/bin/bash
# SessionStart hook: 会话开始时的轻量提示 (advisory)
#
# 行为:
#   1. 提示物理 git hooks 是否启用
#   2. 提示是否在 main/release 分支 (不建议直接改动)
# 永不阻塞 - 仅输出提示信息到 stderr

set -uo pipefail

cat - >/dev/null 2>&1 || true

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || true)
[ -n "$REPO_ROOT" ] || exit 0
cd "$REPO_ROOT" || exit 0

# 1. 检测 core.hooksPath 是否指向 .githooks
HOOKS_PATH=$(git config --local core.hooksPath 2>/dev/null || echo "")
if [ "$HOOKS_PATH" != ".githooks" ] && [ -d .githooks ]; then
  echo "[Session] 提示: 物理 git hooks 未启用,可执行:" >&2
  echo "          git config core.hooksPath .githooks" >&2
fi

# 2. 提示当前分支
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
case "$BRANCH" in
  main|master|release/*|release-*)
    echo "[Session] ⚠️  当前在保护分支 $BRANCH — 建议切到 feature 分支" >&2
    ;;
esac

exit 0
