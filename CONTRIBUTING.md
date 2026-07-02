# Spider 贡献指南

感谢你关注 Spider！Spider 是面向 Java 微服务的声明式服务调用治理中间件
（Declarative Remote Call + Elastic Governance + Contract Protection + Observability）。
本文档帮助你快速参与贡献。项目主要面向中文开发者，文档以中文为主，关键术语保留英文。

## 环境要求

- **JDK 8+**（目标编译版本为 Java 8）
- **Maven 3.6+**
- Git（配置好 Gitee 和 GitHub 两个远程仓库，见 [双仓库同步](#双仓库同步)）

## Fork 与 Pull Request 流程

Spider 采用经典的 Fork → Branch → PR → Review → Merge 协作模型。

### 第一步：Fork 仓库

在 Gitee 或 GitHub 上点击 Fork，将 Spider fork 到你的账号下。然后 clone 并添加上游仓库：

```bash
git clone <your-fork-url>
cd spider
git remote add upstream https://gitee.com/suxuZZR/spider.git  # 或 github.com/hdkjcom/spider
```

### 第二步：切功能分支

**不要直接在 dev 或 master 上提交。** 从上游 dev 切分支：

```bash
git checkout dev
git pull upstream dev
git checkout -b feature/xxx
```

命名：全小写连字符英文，≤4 个单词。`feature/circuit-breaker-dynamic` ✅  `feature/v1` ❌

### 第三步：开发与测试

写代码 → 写测试 → 本地验证：

```bash
mvn test -pl spider-core                     # 改动模块
mvn test                                     # 最终验证，必须全绿
```

### 第四步：保持同步

```bash
git fetch upstream dev
git rebase upstream/dev
```

### 第五步：提交

按 [提交规范](#提交规范) 格式，一个功能点一个提交：

```bash
git commit -m "feat: 动态熔断阈值配置支持"
```

### 第六步：推送并发 PR

```bash
git push origin feature/xxx
```

在 Gitee/GitHub 页面发起 PR，**目标分支选 dev**。描述写清楚：做了什么、为什么、改了什么、测试结果。

### 第七步：Code Review

我会 1-3 天内 review，关注点：spider-core 零依赖边界、Java 8 兼容、强类型 DTO、测试覆盖、线程安全、资源释放。

Review 中提的修改意见在原分支上改并推送，PR 自动更新。

### 第八步：合并

通过后 squash merge 到 dev，你的所有 commit 会合并成一个。然后删分支：

```bash
git branch -d feature/xxx
git push origin --delete feature/xxx
```

### 新手入门

适合第一次贡献的方向：补单元测试、修文档拼写、补 Javadoc、加 demo 示例。从边缘开始，不用一上来就碰核心过滤器链。

## 获取与构建

```bash
git clone <repo-url>        # Gitee 或 GitHub 均可
cd spider
mvn clean install           # 构建全部模块
mvn test                    # 必须全绿
```

常用命令：

```bash
mvn compile                          # 仅编译
mvn test -pl spider-core             # 仅测试某模块
mvn test -pl spider-core -Dtest=SpiderClientFactoryTest   # 跑单个测试类
mvn package -DskipTests              # 打包跳过测试
mvn exec:java -pl spider-console \
    -Dexec.mainClass=io.github.spider.console.SpiderConsoleApplication  # 启动本地控制台
```

## 项目结构

Spider 是多模块 Maven 工程，核心模块 `spider-core` 独立于 Spring，集成能力放在专门的模块：

| 模块 | 职责 |
|---|---|
| `spider-core` | 注解、代理、元数据、Transport SPI、Discovery SPI、Metrics SPI（**零 Spring 依赖**） |
| `spider-http` | 基于 OkHttp 的 HTTP Transport |
| `spider-jackson` | Jackson 编解码器 |
| `spider-metrics` | Micrometer 集成 |
| `spider-resilience` | Resilience4j 熔断器与限流 |
| `spider-contract` | 响应契约校验 |
| `spider-grpc` | gRPC Transport |
| `spider-nacos` | Nacos 服务发现 |
| `spider-console` | 监控控制台与上报 API |
| `spider-codegen` | OpenAPI 客户端生成器 |
| `spider-benchmark` | 基准测试支持 |
| `spider-config` | 动态配置 SPI |
| `spider-messaging` | 消息 Transport SPI |
| `spider-spring-boot-starter` | Spring Boot 自动装配 |
| `spider-demo` | 示例与 E2E 用例 |

新增模块时：在根 `pom.xml` 注册 `<module>spider-<name></module>`，parent 指向
`io.github.hdkjcom.spider:spider:<version>`，并实现 `spider-core` 中对应的 SPI。

## 分支模型

项目采用稳定的双主线分支模型：

```
master        ← 稳定发布版本，只接收 release 与 hotfix 合并
dev           ← 日常开发主线，所有 feature 从这里切出
release/x.y.z ← 版本发布分支，仅修 bug
feature/xxx   ← 新功能分支，从 dev 切出
hotfix/xxx    ← 紧急修复分支，从 master 切出
```

**命名规则**：全小写、连字符分隔、英文描述、不超过 4 个单词；
不要用中文、拼音、数字编号（如 `feature/v1`）。

| 类型 | 格式 | 示例 |
|---|---|---|
| feature | `feature/功能描述` | `feature/grpc-streaming` |
| hotfix | `hotfix/问题描述` | `hotfix/npe-circuit-breaker` |
| release | `release/x.y.z` | `release/0.1.9` |

**禁止操作**：

- 禁止直接在 `master` 上提交
- 禁止直接在 `dev` 上提交功能性代码（仅文档修复例外）
- 禁止 `git push --force` 到 master / dev / release 等共享分支
- 禁止用 `git commit --amend` 修改已推送的提交

新功能开发典型流程：

```bash
git checkout dev && git pull origin dev
git checkout -b feature/xxx
# 开发、提交、推送
git push origin feature/xxx
# 完成后合回 dev
git checkout dev && git pull origin dev
git merge feature/xxx
git push origin dev
git branch -d feature/xxx
```

hotfix 从 `master` 切出，修完后**同时**合并到 master 和 dev。

## 提交规范

格式：`类型: 简短描述`

| 类型 | 用途 |
|---|---|
| `feat:` | 新功能 |
| `fix:` | Bug 修复 |
| `docs:` | 文档更新 |
| `refactor:` | 代码重构（不改变功能） |
| `test:` | 测试相关 |
| `chore:` | 构建、依赖、工具相关 |

示例：

```
feat: 添加 gRPC server-streaming 支持
fix: 修复 CircuitBreaker OPEN 状态下的 NPE
docs: 更新 README 添加 Console 使用说明
refactor: 提取 DashboardDto 替代 Map 返回值
test: 为 SpiderLoadBalancer 补充随机策略单测
chore: 升级 Resilience4j 到 1.7.1
```

合并前要求：

- 至少本地跑通 `mvn compile`
- 合并到 dev 前将功能分支 rebase 到最新 dev
- 合并到 master 前必须跑通 `mvn test`
- 冲突优先保留功能分支的逻辑，格式以目标分支为准

## 代码规范

- **Java 8 兼容**：禁用 `var`、`record`、`sealed`、模块系统及任何 Java 9+ API
- **`spider-core` 零 Spring 依赖**：所有 SPI 放在 core，Spring 集成放在 `spider-spring-boot-starter`
- **强类型 DTO**：表达固定结构时使用类型化 DTO，避免 `Map<String, Object>`
- **日志**：统一使用 SLF4J，禁止 `System.out` / `System.err`
- **代理**：仅使用 JDK 动态代理，不引入 ByteBuddy 或 CGLIB
- **注解**：`@SpiderClient` 等注解只放在接口上
- **Transport 无关**：HTTP / gRPC / messaging 一律通过 `SpiderTransport` SPI
- **复用基础设施**：优先使用 OkHttp、Jackson、Micrometer、Resilience4j、OpenTelemetry、Nacos 等成熟库
- 代码风格：4 空格缩进、camelCase；公开 API 必须有 Javadoc

## 测试要求

- 任何改动都要配套测试，尤其是：元数据解析、请求模板、retry/fallback、
  服务发现与负载均衡、Spring 自动装配、Transport 行为、Codec 语义
- 提交前先跑相关模块，跨模块改动跑全量：

```bash
mvn test -pl spider-core,spider-jackson   # 相关模块
mvn test                                  # 全量（合并到 master 前必须通过）
```

## 双仓库同步

项目同时托管在 **Gitee**（`origin`）和 **GitHub**（`github`）。
每次提交、合并、打 tag、**新建/删除分支**后，**必须同时推送两个仓库**，保持两边一致：

```bash
git push origin dev master release/x.y.z --tags    # origin = Gitee
git push github dev master release/x.y.z --tags    # github = GitHub
```

`dev`、`master`、`release/*` 都属于共享分支，任一变动都要同步两个仓库；删除分支同样两边一起删：

```bash
git push origin --delete <branch>     # origin = Gitee
git push github --delete <branch>     # github = GitHub
```

禁止只推一个仓库。

## 发布流程

版本发布走 `spider-release` 流程，核心是**版本号全局同步**，缺一不可：

1. 从 dev 切 `release/x.y.z` 分支
2. 全局替换版本号，覆盖：
   - 根 `pom.xml` 与所有 15 个子模块 `pom.xml` 的 `<parent><version>`
   - `README.md` / `README_CN.md` / `CLAUDE.md` / `CHANGELOG.md` / `CONTRIBUTING.md` / `docs/*.md`
   - **前端** `spider-console/src/main/resources/templates/console.html` 的 brand-sub 与 footer（最易遗漏）
3. `grep` 验证无旧版本号残留，`mvn compile` 通过
4. 提交 `release: x.y.z 版本号升级`，打 tag `vx.y.z`
5. 合并到 master 并同步推 Gitee + GitHub（含 tags）
6. master 合回 dev，`mvn deploy -DskipTests` 到 Maven Central，去 central.sonatype.com 点 Publish

禁止只更新部分文件就推送；禁止跳过编译验证直接发布。

## 治理理念

项目遵循 Apache Way：Community over code、Meritocracy、Consensus-based decision making。
欢迎通过 Issue 讨论、PR 贡献代码或文档。
