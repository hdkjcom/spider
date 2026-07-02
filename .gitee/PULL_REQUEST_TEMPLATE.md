## 这个 PR 做了什么

<!-- 简述改动内容 -->

## 为什么这样做 / 关联 Issue

<!-- 背景、动机；关联 Issue 填 #xxx -->

## 改动类型

- [ ] feat 新功能
- [ ] fix Bug 修复
- [ ] docs 文档
- [ ] refactor 重构
- [ ] test 测试
- [ ] chore 构建/依赖

## 测试

- [ ] 相关模块 `mvn test -pl <module>` 通过
- [ ] 跨模块改动 `mvn test`（全量）通过

## 自查清单

- [ ] PR 目标分支是 `dev`
- [ ] 提交信息符合 `类型: 描述`
- [ ] `spider-core` 未引入 Spring 依赖
- [ ] 保持 Java 8 兼容（未用 `var` / `record` / `sealed` / Java 9+ API）
- [ ] 公开 API 有 Javadoc
- [ ] 新功能配套了单元测试
