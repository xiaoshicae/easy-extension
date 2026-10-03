# 发布规范

> easy-extension 发布到 **Maven Central**,要求严格(GPG 签名 + source + javadoc 三件套)。
> 历史 5 个 commit 都在加固发布流程 —— 这里把约定固化下来。

## 发布前检查清单

`/release-prep` skill 会自动跑这些。手动操作时也按这个顺序。

### 1. 版本号一致性

所有需要同步版本号的位置:

| 位置 | 写法 |
|---|---|
| `pom.xml`(根模块) `<version>` | `3.3.6` |
| `pom.xml` `<properties><easy-extension.version>` | `3.3.6` |
| `easy-extension-core/pom.xml` 父引用 | `3.3.6` |
| `easy-extension-annotation-processor/pom.xml` 父引用 | `3.3.6` |
| `easy-extension-spring-boot-starter/pom.xml` 父引用 | `3.3.6` |
| `easy-extension-admin-spring-boot-starter/pom.xml` 父引用 | `3.3.6` |
| `README.md` 依赖示例 `<version>` | `3.3.6` |
| `doc/` 下文档引用 | `3.3.6` |
| `easy-extension-admin-ui-frontend/package.json` `version` | `3.3.6` |

**所有位置必须一致**,否则 Maven Central 会拒绝或下游用错版本。

### 2. CHANGELOG / Release Notes

每次发布要在 `CHANGELOG.md`(或 GitHub Release)记录:

- **Added**:新增 API / 功能
- **Changed**:行为改变(向后兼容)
- **Deprecated**:标记为废弃的 API
- **Removed**:删除的 API(必须 bump major)
- **Fixed**:bug 修复
- **Security**:安全修复

涉及破坏性变更必须在显眼位置标 `BREAKING CHANGE:`。

### 3. 公共 API 兼容性

参见 `api-compatibility.md`。`/push` 时已经查过一次,发布前最后确认:

```bash
git diff <last-tag>..HEAD -- 'easy-extension-core/src/main/java/**/*.java' \
  'easy-extension-*-starter/src/main/java/**/*.java'
```

任何破坏性变更必须:
- bump **major version**
- CHANGELOG 显式标记
- README 给迁移指引

### 4. Javadoc 完整性

所有 `public` 类、`public` 方法必须有 Javadoc。检查:

```bash
mvn -q javadoc:javadoc -DskipTests
# 警告会出现在 target/site/apidocs/...
```

`/release-prep` skill 会扫这个。

### 5. 测试全部通过

```bash
mvn clean test
```

要求:**所有模块所有测试通过**,无 `@Disabled`,无 flaky。

### 6. 编译三件套验证

Maven Central 要求 source、javadoc、签名:

```bash
mvn clean install -DskipTests=false   # 完整测试 + 安装到本地 .m2
```

构建产物应包含(每个模块):

- `<artifactId>-<version>.jar`
- `<artifactId>-<version>-sources.jar`
- `<artifactId>-<version>-javadoc.jar`
- 各自的 `.asc` 签名文件

### 7. GPG 签名验证

```bash
# 列出本机 GPG key
gpg --list-secret-keys --keyid-format=long

# 测试签名(非交互模式)
echo test | gpg --batch --no-tty --local-user <KEY_ID> --output /tmp/test.sig --detach-sign

# 验证签名
gpg --verify /tmp/test.sig
```

如果 GPG 没配,看 README 或之前 commit `a07b920 refactor: harden source extraction & isolate gpg signing`。

### 8. settings.xml(权限确认)

Maven Central 凭证写在 `~/.m2/settings.xml`,**权限必须是 600**:

```bash
chmod 600 ~/.m2/settings.xml
ls -la ~/.m2/settings.xml
```

`settings.xml` 不应进入 git(已在 `.gitignore`)。**绝不能**把 Sonatype 密码 commit。

## 发布流程

完成上述检查后:

```bash
# 1. 打 tag
git tag -a v3.3.6 -m "Release 3.3.6"
git push origin v3.3.6

# 2. 部署到 Maven Central(走 Sonatype 中央仓库)
mvn -B clean deploy -DskipTests=false

# 3. 到 Sonatype OSSRH 检查 staging repo,确认无误后 release
# https://oss.sonatype.org/
```

### 发布后校验(必做)

```bash
# 1. Central 上必须能看到新版本(返回 200;Central 同步可能有延迟,几分钟后重试)
V=4.0.0
for a in core annotation-processor spring-boot-starter admin-spring-boot-starter; do
  printf "%s -> " "$a"
  curl -s -o /dev/null -w "%{http_code}\n" \
    "https://repo.maven.apache.org/maven2/io/github/xiaoshicae/easy-extension-$a/$V/easy-extension-$a-$V.pom"
done

# 2. 把 API 兼容基线更新为刚发布的版本(根 pom 的 easy-extension.baseline.version),
#    这样下一个版本的 japicmp 门禁对比的就是它
```

> 历史教训:3.3.3 有 tag 但 Central 上没有(返回 404)。tag 与 Central 不一致会让下游困惑,发布后务必校验。

## API 兼容门禁(japicmp)

`mvn verify` 会用 japicmp 对比**上一个已发布版本**(`easy-extension.baseline.version`),
遇到二进制或源码不兼容的变更直接失败。CI 的 `api-compat` job 也会跑。

- 基线 jar 由 `maven-antrun-plugin`(`fetch-api-baseline`,package 阶段)直接从 Central 下载并校验 SHA-1,放在各模块
  `target/japicmp-baseline/`。**不能**让 japicmp 把基线当依赖解析:在版本号还没 bump 时(开发分支一直是已发布的版本号),
  Maven 会把同 GAV 的依赖解析成 reactor 里自己刚打出来的 jar,门禁就变成"自己和自己比",永远通过
- 本地离线构建时加 `-Djapicmp.skip=true`(此时不会下载基线)
- 给已发布的注解**新增带默认值的元素**是兼容的,但 japicmp 会报 `METHOD_ABSTRACT_ADDED_TO_CLASS`(它不知道没人实现注解接口)。
  这类新增要逐个在根 pom 的 japicmp `<excludes>` 里登记(目前只有 `@ExtensionPoint#mandatory()`),登记本身就是一次评审点
- 补丁版 / 小版本只允许**增量**变更(新增类、新增 `default` 方法、新增带默认值的注解元素)
- 确需破坏性变更 → 升 major,并在 `CHANGELOG.md` 显式标 `BREAKING`
- 3.3.5 曾把 6 个 `I*` 接口的抽象方法改名并当作补丁版发布(japicmp 判定 6 项二进制 + 12 项源码不兼容),
  这道门就是为了避免再次发生

**禁止**:

- ❌ `mvn deploy -DskipTests`(`pre-bash-guard.sh` 会拦,且 Maven Central 不接受未测试包)
- ❌ `mvn deploy -Dgpg.skip=true`(Maven Central 要求签名)
- ❌ 不打 tag 直接 deploy
- ❌ 发版前没更新 README dependency 示例版本
- ❌ 为了通过 japicmp 门禁而调整基线版本(基线只在发布完成后更新)

## 子项目独立发布

- **IntelliJ 插件**:走 JetBrains Plugin Marketplace,与 Maven 项目独立
- **admin-ui-frontend**:作为 web 资源嵌入 `admin-spring-boot-starter`,不单独发布 npm

## 紧急回滚

如果发到 Maven Central 才发现严重问题:

1. **不能撤回**:Maven Central 不允许删除已发布版本
2. **快速发补丁**:走 `/hotfix` skill,bump patch 版本号
3. **在 README 顶部加 WARNING**:警告 N.N.X 有问题,请升级到 N.N.(X+1)
4. **GitHub Release**:把有问题的版本标为 `Pre-release` 或在描述里警告
