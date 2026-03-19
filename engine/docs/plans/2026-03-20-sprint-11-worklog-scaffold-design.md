# Sprint-11 Worklog Scaffold Design

**Date:** 2026-03-20

## Goal

在 `worklog/v1.0.0` 下新增 `sprint-11` 标准目录骨架，遵循当前 sprint 目录的命名和最小文件规范，为后续任务拆分和 IT 验收留出固定位置。

## Scope

- 新增 `worklog/v1.0.0/sprint-11/README.md`
- 新增 `worklog/v1.0.0/sprint-11/tasks/`
- 新增 `worklog/v1.0.0/sprint-11/it/README.md`
- 用可跟踪文件保留空目录

## Constraints

- 不修改现有 sprint 内容
- 不预设具体业务主题，只提供占位信息
- 不碰当前仓库里未提交的业务代码改动

## Structure

`sprint-11` 采用与现有 sprint 一致的最小结构：

- 根目录 README：标题、前缀、状态、目标、背景、任务列表、完成标准
- `tasks/`：保留为空目录，等待后续任务文档进入
- `it/README.md`：说明 IT 目录用途和后续应补充的测试资产

## Verification

- `find worklog/v1.0.0/sprint-11 -maxdepth 2`
- `git status --short`

