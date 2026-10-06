# 发布规范

> easy-extension 发布到 **Maven Central**,要求严格(GPG 签名 + source + javadoc 三件套)。
> 历史 5 个 commit 都在加固发布流程 —— 这里把约定固化下来。

## 发布前检查清单

`/release-prep` skill 会自动跑这些。手动操作时也按这个顺序。

### 1. 版本号一致性

所有需要同步版本号的位置:

| 位置 | 写法 |
|---|---|
| `pom.xml`(根模块) `<version>` | `4.0.0` |
| `pom.xml` `<properties><easy-extension.version>` | `4.0.0` |
| `easy-extension-core/pom.xml` 父引用 | `4.0.0` |
| `easy-extension-annotation-processor/pom.xml` 父引用 | `4.0.0` |
| `easy-extension-spring-boot-starter/pom.xml` 父引用 | `4.0.0` |
| `easy-extension-admin-spring-boot-starter/pom.xml` 父引用 | `4.0.0` |
| `README.md` 依赖示例 `<version>` | `4.0.0` |
| `doc/` 下文档引用 | `4.0.0` |
| `easy-extension-admin-ui-frontend/package.json` `version` | `4.0.0` |

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

发布到 Maven Central 走 **Sonatype Central Portal**(根 pom 里的 `central-publishing-maven-plugin`,GPG 签名由 `-Prelease` 激活)。
**发布必须由有凭证的人在自己的机器上执行**:Claude/CI 会话没有也不应接触 `~/.m2/settings.xml` 里的 Portal token 和 `~/.gnupg/` 里的私钥(`pre-bash-guard.sh` 会拦)。

### 一次性准备

- Central Portal 账号,并已验证命名空间 `io.github.xiaoshicae`
- 在 Portal 生成 **User Token**,写入 `~/.m2/settings.xml`(`chmod 600`),server id 必须是 `ossrh`:

  ```xml
  <server><id>ossrh</id><username>TOKEN_USER</username><password>TOKEN_PASSWORD</password></server>
  ```

- 本机有可用的 GPG 私钥(`gpg --list-secret-keys --keyid-format=long`),公钥已上传到 keyserver

### 每次发布

```bash
# 1. 准备:同步版本号 + 检查(不会发布)。在 Claude 里跑 /release-prep 4.0.0,
#    产出一个版本号提交,走 PR 合并到 main
# 2. 回到干净的 main,打 tag
git checkout main && git pull
git tag -a v4.0.0 -m "Release 4.0.0"
git push origin v4.0.0

# 3. 构建、测试、签名并上传到 Central Portal(要求 JDK 21)
mvn -B clean deploy -Prelease

# 4. 打开 https://central.sonatype.com/publishing/deployments
#    等校验通过(VALIDATED),确认文件无误后点 Publish。
#    几分钟后 https://central.sonatype.com/artifact/io.github.xiaoshicae/easy-extension-core 可见
```

说明:

- 插件默认不会自动发布(没配 `autoPublish`),所以第 4 步需要你在 Portal 里手动点 Publish;确认无误前随时可以 Drop。
- 根 pom 的 `distributionManagement` 仍指向旧的 `oss.sonatype.org`(OSSRH 已下线),使用 `central-publishing-maven-plugin` 时不会走它,可在后续清理中删除。
- 同一个版本一旦 Publish **不可撤回、不可覆盖**,有问题只能发补丁版本(见"紧急回滚")。
- 一次 `deploy` 会上传 4 个 Maven 模块(core / annotation-processor / spring-boot-starter / admin-spring-boot-starter)。admin 必须与 core 同版本发布。

### 发布顺序(4.x 这类带 IDE 插件的大版本)

1. **IntelliJ 插件先发**:`easy-extension-intellij-plugin` 独立发布到 JetBrains Marketplace(`gradle.properties` 的 `pluginVersion` 与库版本无关,建议为 4.x-only 的插件 bump 主版本;`./gradlew buildPlugin` 产出 zip,上传到 Marketplace 或用 `publishPlugin` 并配置 token)。
2. **再发库**(上面的 Maven 流程)。
3. **最后更新示例仓库** `easy-extension-sample` 的 `easy-extension-version` 为 `4.0.0` 并合并迁移 PR,更新 Wiki。
4. 在 GitHub 上基于 tag 创建 Release,内容取自 `CHANGELOG.md`,**BREAKING CHANGE** 放在最前,并链接 `doc/migration-4.0.md`。

**禁止**:

- ❌ `mvn deploy -DskipTests`(`pre-bash-guard.sh` 会拦,且 Maven Central 不接受未测试包)
- ❌ 不带 `-Prelease` 的 `mvn deploy`(artifact 没签名,Portal 会拒绝)
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
