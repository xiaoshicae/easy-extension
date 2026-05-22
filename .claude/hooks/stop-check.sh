#!/bin/bash
# Stop hook: Claude 本轮响应结束前的整体把关
#
# 触发: 每次 Claude 响应结束前
# 行为:
#   1. 改动模块 mvn test-compile      — 全量编译验证 (跨模块影响)
#   2. 改动模块 mvn test              — 增量测试 (本轮改动模块及其反向依赖)
#   3. (可选) Kotlin 子项目 gradle compileKotlin (仅当 IntelliJ 插件文件被改动)
# 任一项失败 → exit 2,Claude 会继续修复
#
# 可通过环境变量跳过:
#   EASY_EXT_HOOK_SKIP_STOP=1   完全跳过 Stop 检查
#   EASY_EXT_HOOK_SKIP_TEST=1   只编译,不跑测试

set -uo pipefail

cat - >/dev/null 2>&1 || true

[ "${EASY_EXT_HOOK_SKIP_STOP:-0}" = "1" ] && exit 0

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || true)
[ -n "$REPO_ROOT" ] || exit 0
[ -f "$REPO_ROOT/pom.xml" ] || exit 0
cd "$REPO_ROOT" || exit 0
command -v mvn >/dev/null 2>&1 || exit 0

# 找本轮改动的 Java 文件 -> 映射到模块
CHANGED=$( {
  git diff --name-only HEAD 2>/dev/null || true
  git ls-files --others --exclude-standard 2>/dev/null || true
} )

CHANGED_MODULES=$(echo "$CHANGED" | awk -F/ '/\.java$/ && /^easy-extension-/ {print $1}' | sort -u | grep -v '^easy-extension-intellij-plugin$' || true)
KOTLIN_CHANGED=$(echo "$CHANGED" | grep -E '^easy-extension-intellij-plugin/.*\.kt$' | head -1 || true)

# 没有 Java/Kotlin 改动则直接放行
[ -z "$CHANGED_MODULES" ] && [ -z "$KOTLIN_CHANGED" ] && exit 0

FAIL=0
ERRORS=""

# 1. Maven 多模块: 用 -pl ... -am (also-make) 把上游依赖也带上,验证跨模块影响
if [ -n "$CHANGED_MODULES" ]; then
  MODS=$(echo "$CHANGED_MODULES" | tr '\n' ',' | sed 's/,$//')

  # 1a. 编译验证
  if COMPILE_OUT=$(mvn -q -pl "$MODS" -am test-compile -Dgpg.skip=true 2>&1); then :; else
    ERRORS="${ERRORS}[Stop] mvn test-compile 失败 (modules: $MODS):
${COMPILE_OUT}

"
    FAIL=1
  fi

  # 1b. 增量测试 (跑改动模块及其下游) - -amd: also-make-dependents
  if [ "${EASY_EXT_HOOK_SKIP_TEST:-0}" != "1" ] && [ "$FAIL" -eq 0 ]; then
    if TEST_OUT=$(mvn -q -pl "$MODS" -amd test -Dgpg.skip=true 2>&1); then :; else
      ERRORS="${ERRORS}[Stop] mvn test 失败 (modules: $MODS + 下游):
${TEST_OUT}

"
      FAIL=1
    fi
  fi
fi

# 2. Kotlin (IntelliJ 插件) - Gradle 编译验证
if [ -n "$KOTLIN_CHANGED" ] && [ -x "$REPO_ROOT/easy-extension-intellij-plugin/gradlew" ]; then
  if GRADLE_OUT=$(cd "$REPO_ROOT/easy-extension-intellij-plugin" && ./gradlew -q compileKotlin 2>&1); then :; else
    ERRORS="${ERRORS}[Stop] gradle compileKotlin 失败 (IntelliJ 插件):
${GRADLE_OUT}

"
    FAIL=1
  fi
fi

if [ "$FAIL" -eq 1 ]; then
  printf '%s' "$ERRORS" >&2
  exit 2
fi

echo "[Stop] ✓ compile + test (modules: ${CHANGED_MODULES:-none}${KOTLIN_CHANGED:+ + intellij-plugin})" >&2
exit 0
