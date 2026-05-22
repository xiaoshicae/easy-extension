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

**禁止**:

- ❌ `mvn deploy -DskipTests`(`pre-bash-guard.sh` 会拦,且 Maven Central 不接受未测试包)
- ❌ `mvn deploy -Dgpg.skip=true`(Maven Central 要求签名)
- ❌ 不打 tag 直接 deploy
- ❌ 发版前没更新 README dependency 示例版本

## 子项目独立发布

- **IntelliJ 插件**:走 JetBrains Plugin Marketplace,与 Maven 项目独立
- **admin-ui-frontend**:作为 web 资源嵌入 `admin-spring-boot-starter`,不单独发布 npm

## 紧急回滚

如果发到 Maven Central 才发现严重问题:

1. **不能撤回**:Maven Central 不允许删除已发布版本
2. **快速发补丁**:走 `/hotfix` skill,bump patch 版本号
3. **在 README 顶部加 WARNING**:警告 N.N.X 有问题,请升级到 N.N.(X+1)
4. **GitHub Release**:把有问题的版本标为 `Pre-release` 或在描述里警告
