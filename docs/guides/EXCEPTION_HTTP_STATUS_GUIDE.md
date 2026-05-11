# 异常处理与HTTP状态码指南

> **注意**: 此文件为当前有效指南，描述 common-utils 模块的异常处理机制

---

## 概述

本文档说明 common-utils 模块中的异常处理机制，特别是HTTP状态码的支持。

**创建时间**: 2026-05-09  
**版本**: 1.0.0  
**状态**: 已完成  

---

## 改进内容

### BusinessException HTTP状态码支持

**改进前**:
```java
// 只能使用固定500状态码
throw new BusinessException("ERR_CODE", "错误信息");
```

**改进后**:
```java
// 可以使用任意HTTP状态码
throw new BusinessException("ERR_CODE", "错误信息", HttpStatus.NOT_FOUND);

// 或者使用便捷的工厂方法
throw BusinessException.badRequest("ERR_001", "参数不合法");
throw BusinessException.notFound("ERR_002", "数据未找到");
```

### 全局异常处理

GlobalExceptionHandler 覆盖14种异常类型：
- `BusinessException` → 对应HTTP状态码
- `MethodArgumentNotValidException` → 400 Bad Request
- `AccessDeniedException` → 403 Forbidden
- `UsernameNotFoundException` → 401 Unauthorized
- `ResourceNotFoundException` → 404 Not Found
- `DataIntegrityViolationException` → 409 Conflict

---

> **最后更新**: 2026-05-09  
> **版本**: 1.0.0  
> **维护者**: DITHIOTHREITOL