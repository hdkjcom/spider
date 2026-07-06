# Spider 升级路线图

> 2026-07 校准版。基于对 1.0.1 代码库的实际核对，剔除文档中已过时的计划项，保留真实待办。
> 每项标注验证方式与改动范围，按"价值 × 可行性"排序。

## 校准说明：以下"计划项"实际已落地，不再列入待办

对照 git 历史与代码核对，`docs/evolution-plan.md` 与 `docs/optimization-plan.md` 中这些条目已完成，两份文档需更新以免误导后续开发：

| 计划项 | 实际状态 | 证据 |
|---|---|---|
| 异步调用 `CompletableFuture` | 已完成 | `SpiderInvocationHandler` 已支持异步线程池提交 |
| 退避 ±50% jitter | 已完成 | `RetryFilter.computeBackoff` 已实现抖动 |
| 动态配置覆盖退避/超时 | 已完成 | `ConfigOverrideFilter` + `RetryFilter` 读取 `config.*` 属性 |
| Spring Cloud LoadBalancer 适配 | 已完成 | starter 已集成 `spring-cloud-loadbalancer` |
| OkHttp 连接池可观测 | 已完成 | Actuator 已暴露连接池状态 |
| Banner 默认关闭 | 已完成 | `SpiderClientFactory` 默认不开 banner |
| CONTRIBUTING 实质内容 | 已完成 | 多版迭代，含双仓库 PR 流程 |
| demo E2E 长期失败 | 已不成立 | 全量 `mvn test` 14 个 demo 用例全绿 |

## 已完成（本次）

- CI 启用：`ci.yml` 进入版本库（此前被 `.gitignore` 的 `.github/` 挡掉），修正触发分支名为 `dev/master`，加 `-Dgpg.skip=true` 规避无密钥签名失败，已推送 Gitee + GitHub。

## P0 — CI 与测试基线

**目标**：让 CI 真正覆盖全量模块，成为可靠的回归保护网。

- [x] 启用 GitHub Actions，修正分支名与 gpg 问题
- [ ] ci.yml 去掉 `-pl '!spider-demo'`，改为全量构建。demo 已验证全绿，无需排除，排除反而让 demo 失去回归保护。
- [ ] README 接入 CI 状态 badge
- [ ] 更新 CLAUDE.md 的 Known Issues，移除已不成立的 demo 失败条目（本地文件，不进仓）

验证：push 后 GitHub Actions 在 Java 8/11/17 三个矩阵全绿。

## P1 — 动态治理补全

**目标**：兑现"运行时不重启调参"承诺。当前只做了一半。

代码核对结论：
- 重试最大次数：已可从 `ctx.attribute("config.maxAttempts")` 动态读取（部分完成）
- **熔断阈值：`CountingCircuitBreaker` 的 `failureRateThreshold` / `slidingWindowSize` / `waitDurationInOpenStateMillis` 全是 `final`，构造后不可变**
- 限流许可数：`RateLimiterInterceptor` 未支持运行时刷新

改动范围：
- `CountingCircuitBreaker`：阈值字段改为 `volatile`（或 `AtomicReference`），新增 `reconfigure(...)` 方法
- `RateLimiterInterceptor`：许可数支持运行时刷新
- `ConfigOverrideFilter`：扩展可覆盖的键（`circuitBreaker.threshold`、`rateLimit.permits`）
- 测试：验证运行时改阈值后行为实时变化

风险：低，纯 filter / 策略对象扩展，不动管道结构。

## P2 — 契约保护实质化

**目标**：让"Contract Protection"从空壳变实质，拉开与 OpenFeign 的差异。

现状：`ContractInterceptor` 只是把校验委托给用户传入的 `ResponseValidator` lambda，框架本身不带任何校验器。

改动范围：
- `spider-contract` 内置 JSON Schema 校验器（接入 `everit-org/json-schema` 或 `networknt`）
- 注解驱动：`@ValidateResponse(schema = "...")` 从 classpath 加载 schema
- 契约违反抛 `SpiderContractViolationException`（已存在），归入 retry/metrics 的 `CONTRACT` 分类
- `spider-codegen` 接口 → OpenAPI 反向生成（补契约来源）

风险：中，引入新依赖需评估体积与 Java 8 兼容。

## P3 — 性能与可观测数据

**目标**：用数据支撑"不劣于直连 OkHttp"的承诺。

- `spider-benchmark`：补 Spider vs OkHttp vs Retrofit 的 JMH 对比，公开结果
- 方法级统计下沉 Micrometer：当前 per-method 数据只在 `SpiderRuntime` 内存，应进 `spider.client.requests` 的 histogram（已有 method 标签，补 p99）
- 验证：Prometheus 抓取到 per-method histogram

## P4 — 工程收尾

CLAUDE.md 已列的 follow-up，逐项清掉：
- [ ] console 版本号硬编码 → 改读 build properties
- [ ] gRPC 流控 magic strings → 类型化
- [ ] per-client CircuitBreaker/Retry 配置进 Spring properties（`spider.clients.<name>.*`）

## 守住不做

- Reactive/WebFlux（异步双链路维护负担过大）
- 多语言客户端、编排能力（Saga/工作流，越界成框架）
- 自研连接池/JSON/序列化/断路器算法
- 引入 ByteBuddy/CGLIB，让 core 依赖 Spring

## 执行原则

- 每项独立 feature 分支，独立可发布，不堆积
- 高风险项（P2 引入新依赖）单独验证
- 每项改动配测试，`mvn test` 全绿方可合并
- 遵循分支规范：`dev → feature/xxx → dev`，双仓库同步推送
