#!/usr/bin/env python3
"""
Python类型注解自动生成工具

自动为Python函数和方法添加类型注解。
使用方法:
    python auto_add_type_annotations.py <目录或文件路径>
    python auto_add_type_annotations.py --dry-run <目录或文件路径>  # 仅预览

Author: AI Code Audit System
Date: 2026-05-08
"""

import ast
import os
import sys
import logging
import argparse
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


class TypeAnnotationVisitor(ast.NodeVisitor):
    """AST访问器，分析函数定义并生成类型注解"""

    def __init__(self, source_lines: List[str]):
        self.source_lines = source_lines
        self.changes: List[Dict[str, Any]] = []

    def visit_FunctionDef(self, node: ast.FunctionDef) -> None:
        """访问函数定义"""
        self._check_function(node)
        self.generic_visit(node)

    def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef) -> None:
        """访问异步函数定义"""
        self._check_function(node)
        self.generic_visit(node)

    def _check_function(self, node: Union[ast.FunctionDef, ast.AsyncFunctionDef]) -> None:
        """检查函数并记录需要添加的类型注解"""
        # 跳过私有函数
        if node.name.startswith('_'):
            return

        # 检查返回值类型注解
        if node.returns is None:
            line_content = self.source_lines[node.lineno - 1]
            if '-> None' not in line_content:
                self.changes.append({
                    'type': 'return',
                    'lineno': node.lineno,
                    'name': node.name,
                    'node': node
                })

        # 检查参数类型注解
        for arg in node.args.args:
            if arg.annotation is None:
                self.changes.append({
                    'type': 'arg',
                    'lineno': node.lineno,
                    'name': f"{node.name}({arg.arg})",
                    'arg': arg.arg,
                    'node': node
                })


class TypeAnnotationInserter:
    """类型注解插入器"""

    # 常见类型映射
    TYPE_MAPPING = {
        'str': 'str',
        'int': 'int',
        'float': 'float',
        'bool': 'bool',
        'list': 'List[Any]',
        'dict': 'Dict[str, Any]',
        'tuple': 'Tuple[Any, ...]',
        'set': 'Set[Any]',
        'None': 'None',
        'object': 'Any',
    }

    @staticmethod
    def infer_type_from_name(name: str) -> str:
        """根据变量名推断类型"""
        name_lower = name.lower()

        if 'id' in name_lower or 'count' in name_lower or 'num' in name_lower:
            return 'int'
        elif 'name' in name_lower or 'title' in name_lower or 'desc' in name_lower:
            return 'str'
        elif 'flag' in name_lower or 'is_' in name_lower or 'has_' in name_lower:
            return 'bool'
        elif 'list' in name_lower or 'ids' in name_lower:
            return 'List[Any]'
        elif 'dict' in name_lower or 'map' in name_lower:
            return 'Dict[str, Any]'
        elif 'time' in name_lower or 'date' in name_lower:
            return 'str'  # 或 datetime
        elif 'path' in name_lower or 'file' in name_lower:
            return 'str'  # 或 Path
        else:
            return 'Any'

    @classmethod
    def generate_annotation(cls, name: str, is_arg: bool = True) -> str:
        """生成类型注解"""
        if is_arg:
            inferred = cls.infer_type_from_name(name)
            return cls.TYPE_MAPPING.get(inferred, inferred)
        else:
            return '-> None'


def analyze_file(file_path: Path, dry_run: bool = False) -> List[Dict[str, Any]]:
    """分析单个文件"""
    try:
        content = file_path.read_text(encoding='utf-8')
        tree = ast.parse(content, filename=str(file_path))
        source_lines = content.split('\n')

        visitor = TypeAnnotationVisitor(source_lines)
        visitor.visit(tree)

        changes = visitor.changes
        for change in changes:
            change['file'] = str(file_path)

        return changes

    except Exception as e:
        logger.info(f"Error analyzing {file_path}: {e}")
        return []


def generate_annotated_code(file_path: Path) -> Optional[str]:
    """为文件生成带类型注解的代码"""
    try:
        content = file_path.read_text(encoding='utf-8')
        tree = ast.parse(content, filename=str(file_path))
        source_lines = content.split('\n')

        # 分析需要的更改
        visitor = TypeAnnotationVisitor(source_lines)
        visitor.visit(tree)

        if not visitor.changes:
            return None

        # 生成新代码（简化版本，仅处理简单情况）
        lines = source_lines.copy()

        for change in sorted(visitor.changes, key=lambda x: x['lineno'], reverse=True):
            if change['type'] == 'arg' and 'arg' in change:
                # 在参数后添加类型注解
                arg_name = change['arg']
                annotation = TypeAnnotationInserter.generate_annotation(arg_name, True)
                line_idx = change['lineno'] - 1
                line = lines[line_idx]

                # 简单处理：添加 annotation
                if f': {annotation}' not in line:
                    lines[line_idx] = f"{line}: {annotation}  # TODO: 确认类型"

        return '\n'.join(lines)

    except Exception as e:
        logger.info(f"Error generating annotated code for {file_path}: {e}")
        return None


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Python类型注解自动生成工具')
    parser.add_argument('path', nargs='?', default='.', help='要分析的目录或文件路径')
    parser.add_argument('--dry-run', action='store_true', help='仅预览，不实际修改')
    parser.add_argument('--recursive', '-r', action='store_true', help='递归处理子目录')
    parser.add_argument('--extensions', default='.py', help='文件扩展名过滤（默认.py）')

    args = parser.parse_args()

    path = Path(args.path)
    if not path.exists():
        logger.info(f"Path does not exist: {path}")
        sys.exit(1)

    # 收集所有Python文件
    if path.is_file():
        files = [path]
    else:
        pattern = '**/*' + args.extensions if args.recursive else '*' + args.extensions
        files = list(path.glob(pattern))

    logger.info(f"Found {len(files)} Python files")

    all_changes = []
    for file_path in files:
        if file_path.name.startswith('.'):
            continue
        changes = analyze_file(file_path, args.dry_run)
        all_changes.extend(changes)

    # 统计
    arg_changes = [c for c in all_changes if c['type'] == 'arg']
    return_changes = [c for c in all_changes if c['type'] == 'return']

    logger.info(f"\n=== 分析结果 ===")
    logger.info(f"缺少类型注解的参数: {len(arg_changes)}")
    logger.info(f"缺少返回类型注解: {len(return_changes)}")

    if arg_changes:
        logger.info(f"\n需要添加参数类型注解的函数:")
        seen = set()
        for change in arg_changes:
            if change['name'] not in seen:
                print(f"  - {change['file']}:{change['lineno']} {change['name']}")
                seen.add(change['name'])

    if return_changes:
        logger.info(f"\n需要添加返回类型注解的函数:")
        seen = set()
        for change in return_changes:
            if change['name'] not in seen:
                print(f"  - {change['file']}:{change['lineno']} {change['name']}")
                seen.add(change['name'])

    if args.dry_run:
        logger.info("\n[Dry Run] 未进行任何修改")
    else:
        logger.info("\n使用 --dry-run 参数预览修改")

    # 生成报告文件
    report_path = Path('type_annotation_report.md')
    with report_path.open('w', encoding='utf-8') as f:
        f.write("# Python类型注解分析报告\n\n")
        f.write(f"分析时间: 2026-05-08\n\n")
        f.write(f"## 统计\n\n")
        f.write(f"- 缺少参数类型注解: {len(arg_changes)}\n")
        f.write(f"- 缺少返回类型注解: {len(return_changes)}\n\n")
        f.write(f"## 详细列表\n\n")

        if arg_changes:
            f.write("### 需要添加参数类型注解\n\n")
            seen = set()
            for change in arg_changes:
                key = f"{change['file']}:{change['lineno']}"
                if key not in seen:
                    f.write(f"- `{change['file']}` 行{change['lineno']}: `{change['name']}`\n")
                    seen.add(key)

        if return_changes:
            f.write("\n### 需要添加返回类型注解\n\n")
            seen = set()
            for change in return_changes:
                key = f"{change['file']}:{change['lineno']}"
                if key not in seen:
                    f.write(f"- `{change['file']}` 行{change['lineno']}: `{change['name']}`\n")
                    seen.add(key)

    logger.info(f"\n详细报告已保存到: {report_path}")


if __name__ == '__main__':
    main()
