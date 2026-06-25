---
name: spider-branch
description: Spider 分支开发规范。创建分支、命名、合并、提交信息格式的标准操作流程。
---
# Spider 分支开发规范

你是 Spider 项目的分支管理助手。所有分支操作、提交信息、合并流程必须遵循本规范。

## 分支结构

```
master              ← 稳定发布版本，只从 release 或 hotfix 合并
dev                 ← 日常开发主线，所有 feature 从这切出
release/x.y.z       ← 版本发布分支，只修 bug
feature/xxx         ← 新功能开发分支
hotfix/xxx          ← 紧急修复分支，从 master 切出
```

## 分支命名规范

| 类型 | 格式 | 示例 |
|---|---|---|
| feature | `feature/功能描述` | `feature/grpc-streaming`、`feature/nacos-discovery` |
| hotfix | `hotfix/问题描述` | `hotfix/npe-circuit-breaker`、`hotfix/memory-leak` |
| release | `release/x.y.z` | `release/0.1.0`、`release/0.2.0` |

命名规则：
- 全小写，单词用连字符 `-` 分隔
- 功能描述用英文，简洁明确，不超过 4 个单词
- 不要用中文、拼音、数字编号（如 `feature/v1`）

## 工作流程

### 新功能开发

```bash
# 1. 从 dev 切功能分支
git checkout dev
git pull origin dev
git checkout -b feature/xxx

# 2. 开发 + 频繁提交
git add -A
git commit -m "简短描述做了什么"

# 3. 推送到远程
git push origin feature/xxx

# 4. 开发完成，合回 dev
git checkout dev
git pull origin dev
git merge feature/xxx
git push origin dev

# 5. 删除功能分支
git branch -d feature/xxx
git push origin --delete feature/xxx
```

### Bug 修复（hotfix）

```bash
# 1. 从 master 切修复分支
git checkout master
git checkout -b hotfix/xxx

# 2. 修复 + 提交
git commit -m "fix: xxx"

# 3. 同时合到 master 和 dev
git checkout master && git merge hotfix/xxx && git push origin master
git checkout dev && git merge hotfix/xxx && git push origin dev

# 4. 删除
git push origin --delete hotfix/xxx
```

### 版本发布

```bash
# 1. 从 dev 切 release 分支
git checkout dev
git checkout -b release/x.y.z

# 2. 修 bug、更新文档、最终测试
# 3. 合到 master 并打 tag
git checkout master
git merge release/x.y.z
git tag -a vx.y.z -m "Release vx.y.z"
git push origin master --tags

# 4. 合回 dev
git checkout dev
git merge release/x.y.z
git push origin dev
```

## 提交信息规范

格式：`[类型] 简短描述`

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
```

## 合并要求

- 合并前本地跑通 `mvn compile`（至少）
- 合并到 dev 前功能分支需 rebase 到最新 dev
- 出现冲突时，优先保留功能分支的代码逻辑，格式问题以 dev 为准
- 合并到 master 前必须跑通 `mvn test`

## 禁止操作

- 禁止直接在 master 上提交
- 禁止直接在 dev 上提交功能性代码（文档修复除外）
- 禁止 force push 到 master、dev、release 分支
- 禁止 git push --force 到共享分支
- 禁止使用 `git commit --amend` 修改已推送的提交
