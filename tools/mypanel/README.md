# MyPanel

## 概述

MyPanel 是一个轻量级的 Docker 容器管理与磁盘监控面板。基于 Node.js + Express 构建，提供 REST API 接口用于查询 Docker 容器状态和磁盘使用情况。

## 技术栈

| 技术 | 版本 | 用途 |
|------|:----:|------|
| Node.js | 18+ | 运行环境 |
| Express | 5.x | Web 框架 |
| Dockerode | 5.x | Docker Remote API 客户端 |
| Diskusage | 1.x | 磁盘使用情况检测 |

## 安装

```bash
cd tools/mypanel
npm install
```

## 启动

```bash
node app.js
```

## 功能

- Docker 容器列表查询
- 容器启停控制
- 磁盘空间监控
- 健康检查 API

## 相关文档

- [项目 README](../../README.md)
- [Docker 部署指南](../../docs/DOCKER.md)

---

> **最后更新**: 2026-05-30
> **版本**: 1.0
> **维护者**: UAV Platform Team
