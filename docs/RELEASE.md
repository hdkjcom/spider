# Spider 发布指南

## 前置条件

### 1. Sonatype JIRA 账号
- 注册 https://issues.sonatype.org
- 提交 ticket 申请 `io.github.spider` groupId 所有权
- 等待审批通过

### 2. GPG 密钥
```bash
# 生成密钥
gpg --gen-key
# 发布公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. Maven settings.xml
编辑 `~/.m2/settings.xml`：
```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>你的Sonatype用户名</username>
      <password>你的Sonatype密码</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>release</id>
      <properties>
        <gpg.keyname>你的GPG密钥ID</gpg.keyname>
        <gpg.passphrase>你的GPG密码</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

## 发布步骤

### 0. 版本号确认
确保所有 pom.xml 使用正式版本号（非 SNAPSHOT）：
```bash
grep '<version>' pom.xml | grep -v xml | head -1
# 应该输出: <version>0.1.0</version>
```

### 1. 运行全部测试
```bash
mvn clean test
# 必须全部通过 (17 modules, BUILD SUCCESS)
```

### 2. 本地验证发布包
```bash
mvn clean verify -Prelease
# 生成 javadoc.jar、sources.jar、.asc 签名文件
```

### 3. 发布到 Sonatype OSSRH
```bash
mvn clean deploy -Prelease -DskipTests
```

### 4. 在 Sonatype 控制台操作
- 登录 https://s01.oss.sonatype.org
- Staging Repositories → 找到刚才上传的仓库
- Close → 验证通过后 → Release
- 等待 30 分钟同步到 Maven Central

### 5. 验证发布
```bash
# 搜索是否已同步
curl -s "https://search.maven.org/solrsearch/select?q=g:io.github.spider" | grep -o '"numFound":[0-9]*'
```

## 发布后

### 版本升级
```bash
# 将版本号升级到下一个 SNAPSHOT
# 例如 0.1.0 → 0.2.0-SNAPSHOT
```

### Git Tag
```bash
git tag v0.1.0
git push origin v0.1.0
```

## 本次发布清单 (0.1.0)

发布模块（15 个）：
- [x] spider-core
- [x] spider-http
- [x] spider-jackson
- [x] spider-metrics
- [x] spider-resilience
- [x] spider-contract
- [x] spider-grpc
- [x] spider-nacos
- [x] spider-admin
- [x] spider-codegen
- [x] spider-telemetry
- [x] spider-config
- [x] spider-messaging
- [x] spider-spring-boot-starter
- [x] spider-root (parent POM)

不发布模块：
- spider-benchmark (开发工具)
- spider-demo (示例代码)
