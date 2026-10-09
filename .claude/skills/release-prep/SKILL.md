---
description: 发布前完整检查 - 版本号一致性 + Javadoc + 三件套 + GPG + 测试
allowed-tools: Bash(git:*), Bash(mvn:*), Bash(grep:*), Bash(find:*), Bash(awk:*), Bash(sed:*), Bash(diff:*), Bash(ls:*), Bash(chmod:*), Bash(gpg --list-secret-keys:*), Bash(gpg --batch:*), Bash(gpg --verify:*), Bash(echo:*), Bash(cat:*), Read, Glob, Grep, AskUserQuestion
argument-hint: "[版本号 如 3.3.7]"
---

# /release-prep — 发布前完整检查

> 跑这个之前应已经过了 `/push`。这是 Maven Central 发布前的**最后一道防线**。

## 输入

- 用户传入 `<新版本号>`(如 `3.3.7`),不传则询问。

## 步骤

### 1. 版本号一致性扫描

```bash
ROOT_VER=$(grep -m1 -oE '<version>[^<]+</version>' pom.xml | head -1 | sed 's/<[^>]*>//g')
PROP_VER=$(grep -A0 'easy-extension.version' pom.xml | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)

echo "Root version:       $ROOT_VER"
echo "Property version:   $PROP_VER"
echo "Sub-modules:"
for p in easy-extension-*/pom.xml; do
  # 子模块的父引用是 <parent><version>,不是顶层 <version>
  v=$(grep -A2 '<parent>' "$p" | grep -oE '<version>[^<]+</version>' | head -1 | sed 's/<[^>]*>//g')
  printf "  %-50s %s\n" "$p" "$v"
done

echo "README dependency examples:"
grep -E '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' README.md

echo "doc/:"
find doc -type f \( -name '*.md' -o -name '*.html' \) -exec grep -lE '[0-9]+\.[0-9]+\.[0-9]+' {} \;

echo "Frontend package.json:"
grep -m1 '"version"' easy-extension-admin-ui-frontend/package.json
```

**规则**:

- 所有 Maven 模块的版本号(root + 4 子模块的 parent ref + properties)必须**完全一致**
- README 的 dependency 示例必须 = root version
- doc/ 下的版本号必须 = root version
- frontend package.json version 应 = root version

发现不一致 → 询问用户是否一键修正(用 sed/awk 批量改),还是手动修。

### 2. 如果传入了新版本号 — 升级

如果用户传了 `<新版本号>`,在 `main` 之上新建 `release/<新版本号>` 分支后自动改。

**只改带版本号的那 9 处**,逐行替换并断言,不要做全文 `sed`:

- pom 里的 `xmlns` / `schemaLocation` 含 `POM/4.0.0`,`<modelVersion>4.0.0</modelVersion>` 也是 `4.0.0`,全文替换会误伤;
- `sed '0,/re/…'` 是 GNU 语法,macOS 自带的 BSD sed 不支持;
- `doc/` 下描述历史版本的文字(例如 `design-4.0.md`、`migration-4.0.md` 里的 `4.0.0`)不是"当前版本",不改。

```bash
NEW_VER="<新版本号>"
python3 - "$ROOT_VER" "$NEW_VER" <<'PY'
import pathlib, re, sys
old, new = sys.argv[1], sys.argv[2]
targets = {  # 文件 -> 需要替换的行的特征
    'pom.xml': [r'^    <version>', r'<easy-extension\.version>'],
    'easy-extension-core/pom.xml': [r'^        <version>'],
    'easy-extension-annotation-processor/pom.xml': [r'^        <version>'],
    'easy-extension-spring-boot-starter/pom.xml': [r'^        <version>'],
    'easy-extension-admin-spring-boot-starter/pom.xml': [r'^        <version>'],
    'README.md': [r'^    <version>'],
    'easy-extension-admin-ui-frontend/package.json': [r'^  "version":'],
}
count = 0
for f, patterns in targets.items():
    p = pathlib.Path(f); lines = p.read_text().split('\n')
    for i, line in enumerate(lines):
        if old in line and 'xmlns' not in line and any(re.search(pat, line) for pat in patterns):
            lines[i] = line.replace(old, new); count += 1
    p.write_text('\n'.join(lines))
print(f'replaced {count} lines')   # 期望 9:根 pom 2 + 子模块 4 + README 2 + package.json 1
PY
```

操作完后再跑一次步骤 1 做核对,并确认 `git diff --stat` 只涉及这 7 个文件、共 9 行。

admin 页面显示的版本号运行时读 jar 里的 `pom.properties`,前端 `package.json` 的版本号不会打进 bundle,**改它不需要重新打包前端**。

### 3. CHANGELOG / Release Notes 检查

```bash
# 项目根目录看是否有 CHANGELOG
ls CHANGELOG.md CHANGELOG.txt HISTORY.md 2>/dev/null
```

如果有:确认最新版本号(`NEW_VER`)有对应 section。

如果没有:**询问用户**是否想在本次发布前先建一个 CHANGELOG,或者改用 GitHub Release notes。

### 4. 公共 API 兼容性最终确认

```bash
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)
echo "Comparing against $LAST_TAG..."

git diff "$LAST_TAG..HEAD" -- 'easy-extension-core/src/main/java/**/*.java' \
  'easy-extension-spring-boot-starter/src/main/java/**/*.java' \
  'easy-extension-admin-spring-boot-starter/src/main/java/**/*.java' \
  | grep -E '^-\s+(public|protected)' \
  | head -50
```

如果发现删除/修改的 public / protected 方法,**强制询问**用户:

- 是否已 bump major version?
- 是否在 CHANGELOG / Release Notes 标了 BREAKING CHANGE?
- 是否给出了迁移指引?

参考 `.claude/rules/api-compatibility.md`。

### 5. Javadoc 完整性

```bash
mvn -q -DskipTests javadoc:javadoc 2>&1 | grep -E '(WARNING|error)' | head -30
```

- `WARNING: no comment for ...` → 公共类/方法缺 Javadoc,**应当补上**
- `error` → Javadoc 编译失败,**必须修**(发布会一并失败)

### 6. 全量测试

```bash
mvn -q clean test
```

任何模块测试失败 → **终止**,提示用户先修复。

### 7. 三件套构建验证

```bash
mvn -q clean install -DskipTests
```

构建完后检查 `target/`:

```bash
for m in easy-extension-core easy-extension-annotation-processor easy-extension-spring-boot-starter easy-extension-admin-spring-boot-starter; do
  echo "=== $m ==="
  ls -la $m/target/ | grep -E "\\.(jar|asc)$"
done
```

每个模块应当看到:

- `<artifact>-<ver>.jar`
- `<artifact>-<ver>-sources.jar`
- `<artifact>-<ver>-javadoc.jar`
- 对应的 `.asc` 签名文件(如果 GPG 已配)

### 8. GPG 配置检查

```bash
# 列 secret keys
gpg --list-secret-keys --keyid-format=long

# 测试签名能力(非交互)
echo test | gpg --batch --no-tty --output /tmp/test.sig --detach-sign 2>&1
echo $?
```

- 没有 secret key → 提示用户先生成,不能发 Maven Central
- 签名报错 → 检查 `~/.gnupg/` 权限、`gpg-agent` 配置

### 9. Maven settings.xml 权限

```bash
ls -la ~/.m2/settings.xml 2>/dev/null
```

权限不是 600 → 提示 `chmod 600 ~/.m2/settings.xml`(避免凭证泄漏)。

### 10. Git tag 不冲突

```bash
git tag -l "v${NEW_VER}" "${NEW_VER}"
```

如果已存在 tag → **终止**,询问是否要复用(不推荐)或换个版本号。

### 11. 总结报告

输出表格:

```
✅ Version sync                   PASS
✅ CHANGELOG                      PASS (or N/A)
✅ API compatibility              PASS (no breaking) / WARN (X breaking)
⚠️ Javadoc                        WARN (3 public methods missing javadoc)
✅ Tests                          PASS (38 tests, 0 failures)
✅ Build artifacts (jar+src+doc)  PASS
✅ GPG signing                    PASS (key: 33E5EFCC...)
✅ ~/.m2/settings.xml perms       PASS (600)
✅ Tag v${NEW_VER} available      PASS

下一步:
  1. CHANGELOG:把 `## ${NEW_VER} (unreleased)` 改成发布日期
  2. /commit + /push(release/${NEW_VER} 分支),开 PR 合并到 main
  3. 合并后在 main 上:
     git checkout main && git pull
     git tag -a v${NEW_VER} -m "Release ${NEW_VER}" && git push origin v${NEW_VER}
     git checkout v${NEW_VER} && mvn -B clean deploy -Prelease; git checkout main
  4. https://central.sonatype.com/publishing/deployments 等 VALIDATED 后点 Publish
  5. GitHub Release(内容取自 CHANGELOG)
```

`-Prelease` 不能省:GPG 签名在这个 profile 里,不带它 Portal 会因为缺签名拒收。

## 用法

```
/release-prep            # 询问目标版本号
/release-prep 3.3.7      # 直接升级到 3.3.7
```

## 注意

- 这个 skill **不会**直接执行 `mvn deploy` —— 那是手动操作,发布走 Central Portal(`central-publishing-maven-plugin`),上传后在 Portal 里手动 Publish
- GPG 签名使用 `--batch --no-tty`,需要图形化 pinentry:`~/.gnupg/gpg-agent.conf` 里配 `pinentry-program /opt/homebrew/bin/pinentry-mac`,否则签名报 `No pinentry`
- 也**不会**自动打 tag —— 留给用户手动确认
- 但会改文件(版本号同步),改完后**必须 commit**
