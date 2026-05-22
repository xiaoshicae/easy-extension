#!/bin/bash
# PostToolUse hook: 文件改完后的增量质量检查 (Java + 前端 + Kotlin 分发)
#
# 触发: Edit / Write / MultiEdit
# 行为:
#   .java   → 扫 System.out/printStackTrace + 模块级 mvn compile
#   .ts/.tsx/.js/.jsx → 前端 prettier 自动格式化
#   .kt     → 提示 (Gradle 项目编译慢,Stop hook 时统一处理)
# 任一项失败 → exit 2 + stderr,Claude 会自修
#
# 可通过环境变量跳过昂贵步骤:
#   EASY_EXT_HOOK_SKIP_COMPILE=1  跳过 mvn compile

set -uo pipefail

command -v jq >/dev/null 2>&1 || exit 0

INPUT=$(cat -)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty')

[ -n "$FILE_PATH" ] || exit 0
[ -f "$FILE_PATH" ] || exit 0

REPO_ROOT=$(git -C "$(dirname "$FILE_PATH")" rev-parse --show-toplevel 2>/dev/null || true)
[ -n "$REPO_ROOT" ] || exit 0
cd "$REPO_ROOT" || exit 0

REL_PATH="${FILE_PATH#$REPO_ROOT/}"
MODULE=$(echo "$REL_PATH" | cut -d/ -f1)

FAIL=0
ERRORS=""

case "$REL_PATH" in
  # ===== Java =====
  *.java)
    # 必须在 easy-extension-* 模块下
    case "$MODULE" in
      easy-extension-core|easy-extension-annotation-processor|easy-extension-spring-boot-starter|easy-extension-admin-spring-boot-starter) ;;
      *) exit 0 ;;
    esac

    # 1. 拦生产代码 System.out / err / printStackTrace (豁免测试)
    case "$REL_PATH" in
      */src/test/*|*Test.java|*Tests.java) ;;
      *)
        PRINT_HITS=$(grep -nE 'System\.(out|err)\.(print|println|printf)\(' "$REL_PATH" 2>/dev/null || true)
        STACK_HITS=$(grep -nE '\.printStackTrace\(\s*\)' "$REL_PATH" 2>/dev/null || true)
        if [ -n "$PRINT_HITS" ] || [ -n "$STACK_HITS" ]; then
          ERRORS="${ERRORS}[Hook] 禁止在生产代码使用 System.out/err.print* 或 e.printStackTrace(),应使用 SLF4J:
文件: $REL_PATH
${PRINT_HITS}
${STACK_HITS}

"
          FAIL=1
        fi
        ;;
    esac

    # 2. 模块级 mvn compile
    if [ "${EASY_EXT_HOOK_SKIP_COMPILE:-0}" != "1" ] && command -v mvn >/dev/null 2>&1; then
      if BUILD_OUT=$(mvn -q -pl "$MODULE" -am compile -DskipTests -Dgpg.skip=true 2>&1); then :; else
        ERRORS="${ERRORS}[Hook] mvn compile 失败 ($MODULE):
${BUILD_OUT}

"
        FAIL=1
      fi
    fi
    ;;

  # ===== 前端 =====
  easy-extension-admin-ui-frontend/src/*.ts|easy-extension-admin-ui-frontend/src/*.tsx|easy-extension-admin-ui-frontend/src/*.js|easy-extension-admin-ui-frontend/src/*.jsx|easy-extension-admin-ui-frontend/src/*.less|easy-extension-admin-ui-frontend/src/*.json)
    if command -v npx >/dev/null 2>&1 && [ -d easy-extension-admin-ui-frontend/node_modules ]; then
      (cd easy-extension-admin-ui-frontend && npx --no-install prettier --write "../$REL_PATH" 2>/dev/null) || true
    fi
    ;;

  # ===== Kotlin (IntelliJ 插件) =====
  easy-extension-intellij-plugin/*.kt|easy-extension-intellij-plugin/**/*.kt)
    echo "[Hook] $REL_PATH — Kotlin/Gradle 文件,跳过快速编译 (Gradle 启动慢,Stop hook 时统一处理)" >&2
    ;;

  *)
    exit 0
    ;;
esac

if [ "$FAIL" -eq 1 ]; then
  printf '%s' "$ERRORS" >&2
  exit 2
fi

echo "[Hook] $REL_PATH ✓ ($MODULE)" >&2
exit 0
