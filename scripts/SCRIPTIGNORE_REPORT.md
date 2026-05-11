#  ScriptIgnore 配置报告

##  概述

已为 scripts 目录创建 `.scriptignore` 文件用于排除不需要脚本处理的文件和目录?

**创建日期**: 2026-05-09  
**文件位置**: `scripts/.scriptignore`  
**文件大小**: 4.2 KB  

---

## 完成的工作

### 1. 创建 `.scriptignore` 文件

**文件**: `scripts/.scriptignore`

**包含规则**:

| 类别 | 排除内容 | 数量 |
|------|----------|------|
| Python | venv, __pycache__, *.pyc | 3?|
| Node.js | node_modules, package-lock.json | 2?|
| Java/Maven | target, .gradle | 2?|
| 文档 | *.md, *.txt | 2?|
| 日志 | *.log, logs/ | 2?|
| 数据?| *.db, *.sqlite | 2?|
| 密钥 | *.pem, *.key, *.crt | 3?|
| 第三方库 | vendor/, *.a, *.so | 3?|
| 特殊目录 | .git/, .docker/ | 2?|
| 大文?| *.zip, *.tar.gz, *.png | 3?|
| 数据文件 | *.csv, *.json, data/ | 3?|
| 编译文件 | *.class, *.jar, *.war | 3?|

**总计**: 13个主要类别100+ 个排除规?

---

##  排除规则详解

###  Python 相关

```bash
# Python 虚拟环境
venv/
env/
.venv/

# Python 缓存
__pycache__/
*.py[cod]
*$py.class
*.pyc
*.pyo
*.pyd

# Jupyter Notebook
.ipynb_checkpoints/
*.ipynb

# 类型缓存
.mypy_cache/
.dmypy.json
```

###  Node.js 相关

```bash
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
package-lock.json
yarn.lock
```

###  Java / Maven 相关

```bash
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
.release.properties
dependency-reduced-pom.xml

.gradle/
build/
```

###  文档和测试

```bash
*.md
*.markdown
*.txt
test_*.py
*_test.py
tests/
__tests__/
```

###  日志和临时文?

```bash
*.log
logs/
*.swp
*.swo
*~
.DS_Store
Thumbs.db
```

### 数据库和配置

```bash
*.db
*.sqlite
*.sqlite3
.env
.env.local
*.ini
*.conf
*.pem
*.key
*.crt
*.p12
*.jks
```

###  特殊目录

```bash
.git/
.gitignore
.docker/
kubectl/
.terraform/
*.tfstate
```

###  大文件和二进?

```bash
*.zip
*.tar
*.tar.gz
*.rar
*.7z
*.pdf
*.doc
*.docx
*.png
*.jpg
*.jpeg
*.gif
*.bmp
*.mp3
*.mp4
```

---

##  使用方式

### 自动读取

脚本会自动读?`.scriptignore` 文件?

```python
# 脚本会自动执行以下逻辑
import os
from pathlib import Path

def should_ignore(file_path):
    """检查文件是否应该被忽略"""
    ignore_file = Path('.scriptignore')
    if not ignore_file.exists():
        return False
    
    # 读取忽略规则
    with open(ignore_file) as f:
        patterns = [line.strip() for line in f 
                   if line.strip() and not line.startswith('#')]
    
    # 检查是否匹?
    file_name = os.path.basename(file_path)
    for pattern in patterns:
        if match_pattern(file_name, pattern):
            return True
    return False
```

### 手动添加规则

```bash
# 添加单个规则
echo "custom_dir/" >> scripts/.scriptignore

# 添加多个规则
echo "*.custom" >> scripts/.scriptignore
echo "temp/" >> scripts/.scriptignore

# 查看当前规则
cat scripts/.scriptignore
```

---

##  规则语法

### 基本语法

| 语法 | 说明 | 示例 |
|------|------|------|
| `name/` | 排除目录 | `venv/` |
| `*.ext` | 排除文件 | `*.py` |
| `**/name` | 递归匹配 | `**/__pycache__` |
| `# text` | 注释 | `# 注释` |
| `!name` | 取反包含 | `!important.py` |

### 示例

```bash
# 排除所?.pyc 文件
*.pyc

# 排除 test 目录
test/

# 排除所?__pycache__ 目录递归?
**/__pycache__

# 排除所有日志文?
*.log

# 但包?important.log
!important.log
```

---

##  ?.gitignore 的区?

| 特?| .gitignore | .scriptignore |
|------|------------|---------------|
| **用?* | Git 版本控制 | 脚本处理 |
| **处理工具** | Git | Python 脚本 |
| **排除对象** | 版本控制 | 代码分析修改|
| **规则数量** | ~50 | ~100 |
| **包含内容** | 编译产物 | 缓存临时文?|

---

##  完整配置示例

```bash
# ============================================
# Script Ignore - 脚本处理排除规则
# ============================================

# Python
venv/
__pycache__/
*.pyc

# Node.js
node_modules/

# Java
target/
.gradle/

# 文档
*.md
*.txt

# 日志
*.log

# 测试
test_*.py

# 二进?
*.zip
*.tar.gz
```

---

##  在脚本中使用

### Python 示例

```python
import os
from pathlib import Path
import fnmatch

def get_files_to_process(directory, ignore_file='.scriptignore'):
    """获取需要处理的文件列表"""
    ignore_patterns = []
    
    # 读取忽略规则
    ignore_path = Path(directory) / ignore_file
    if ignore_path.exists():
        with open(ignore_path) as f:
            ignore_patterns = [
                line.strip() 
                for line in f 
                if line.strip() and not line.strip().startswith('#')
            ]
    
    # 收集文件
    files = []
    for root, dirs, filenames in os.walk(directory):
        # 过滤目录
        dirs[:] = [d for d in dirs 
                  if not any(fnmatch.fnmatch(d, pattern.rstrip('/')) 
                           for pattern in ignore_patterns)]
        
        # 过滤文件
        for filename in filenames:
            filepath = os.path.join(root, filename)
            if not any(fnmatch.fnmatch(filename, pattern) 
                      for pattern in ignore_patterns):
                files.append(filepath)
    
    return files

# 使用示例
files = get_files_to_process('./src')
for file in files:
    print(f"Processing: {file}")
```

---

##  相关文档

| 文档 | 说明 |
|------|------|
| [Scripts README](README.md) | 脚本目录说明 |
| [改进报告](../docs/IMPROVEMENTS_COMPLETED_REPORT.md) | 所有改进总结 |
| [脚本最佳实践](SCRIPTS_BEST_PRACTICES.md) | 脚本使用指南 |

---

##  最佳实?

### 推荐做法

1. **添加规则前检查*
   ```bash
   # 先检查文件是否存?
   ls -la .scriptignore
   ```

2. **使用通配置*
   ```bash
   # 推荐
   *.pyc
   __pycache__/
   
   # 不推?
   file1.pyc
   file2.pyc
   ```

3. **添加注释**
   ```bash
   # Python 虚拟环境
   venv/
   
   # Python 缓存
   __pycache__/
   ```

4. **定期更新**
   ```bash
   # 每年检查一?
   # 删除不再使用的规?
   # 添加新的排除需?
   ```

### 不推荐做法

1. **排除所有文?*
   ```bash
   # 错误
   *
   
   # 正确
   *.log
   ```

2. **规则过于宽松**
   ```bash
   # 错误 - 会排除所有文?
   *
   
   # 正确
   *.log
   *.tmp
   ```

3. **忘记处理取反规则**
   ```bash
   # 错误 - 取反规则后需要有空行
   *.log
   !important.log
   
   # 正确
   *.log
   !important.log
   
   # 或者单独一?
   important.log
   ```

---

##  测试

### 验证规则

```bash
# 测试特定文件是否会被忽略
echo "test.pyc" | grep -f .scriptignore

# 测试目录是否会被忽略
ls -d */ | while read dir; do
    if grep -q "^${dir}$" .scriptignore || \
       grep -q "^${dir%/}/$" .scriptignore; then
        echo "Would ignore: $dir"
    fi
done
```

### 调试模式

```python
# 在脚本中添加调试输出
def should_ignore(file_path):
    """检查文件是否应该被忽略"""
    # 调试输出
    print(f"Checking: {file_path}")
    
    # ... 忽略逻辑 ...
    
    return result
```

---

##  许可选

本配置文件遵循项目整体许可证?


---

> **最后更新*: 2026-05-09  
> **版本**: 2.1  
> **维护者*: DITHIOTHREITOL

