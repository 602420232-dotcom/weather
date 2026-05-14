# scripts

算法核心的运维脚本目录，提供 Docker 构建与命令行快捷启动等自动化工具。

## 主要文件

| 文件 | 说明 |
|------|------|
| `build_docker.ps1` | Windows PowerShell Docker 镜像构建脚本 |
| `build_docker.bat` | Windows 批处理 Docker 镜像构建脚本 |
| `run_cli.ps1` | PowerShell CLI 快捷启动脚本 |
| `run_cli.bat` | 批处理 CLI 快捷启动脚本 |

## 使用方法

### 构建 Docker 镜像

```powershell
# PowerShell
.\scripts\build_docker.ps1
```

```cmd
REM 命令提示符
scripts\build_docker.bat
```

### 启动 CLI

```powershell
.\scripts\run_cli.ps1
```

```cmd
scripts\run_cli.bat
```

## 说明

- `build_docker.*` 脚本会自动定位项目根目录并使用 `docker/Dockerfile` 构建镜像
- `run_cli.*` 脚本提供一键启动同化命令行界面的快捷方式
- 镜像构建成功后可通过 `docker run -p 8000:8000 bayesian_assimilation:latest api` 启动 API 服务

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
