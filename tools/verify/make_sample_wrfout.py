#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""由真实 wrfout 裁出一份**小体积样例**（进仓库，供集成测试离线复现）。

用法：
    python tools/verify/make_sample_wrfout.py <src_wrfout.nc> <dst.nc> [--ny 36] [--nx 44]

为什么要裁：真实样例 20.6 MB，直接入库太重；而集成测试必须能**离线重跑**
（不能依赖某台机器上的临时文件）。裁切只动水平范围，垂直层、变量、属性全部保留
⇒ 解析器的全部代码路径（θ→T、RH、k-l TKE、气块 CAPE、能见度参数化）都还会被走到。

★ 判据注意：裁切后 **水平维度就变了** ⇒ 测试里绝不能把 129/149 写死，
必须用 netCDF4 独立读一遍样例再比对（`WrfPlatformIntegrationTest.assertC1Payload` 就是这么做的）。
"""
import argparse
import os
import sys

import netCDF4 as nc


def target_size(dim_name: str, current: int, ny: int, nx: int) -> int:
    """把维度名映射到裁切后的长度；不认识的维度原样保留。"""
    if dim_name == "south_north":
        return min(current, ny)
    if dim_name == "south_north_stag":
        return min(current, ny + 1)
    if dim_name == "west_east":
        return min(current, nx)
    if dim_name == "west_east_stag":
        return min(current, nx + 1)
    return current


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("dst")
    ap.add_argument("--ny", type=int, default=36)
    ap.add_argument("--nx", type=int, default=44)
    ap.add_argument("--complevel", type=int, default=6)
    args = ap.parse_args()

    if not os.path.isfile(args.src):
        print("源文件不存在: %s" % args.src)
        return 2
    os.makedirs(os.path.dirname(os.path.abspath(args.dst)), exist_ok=True)

    src = nc.Dataset(args.src, "r")
    dst = nc.Dataset(args.dst, "w", format="NETCDF4")
    try:
        # 全局属性
        dst.setncatts({k: src.getncattr(k) for k in src.ncattrs()})

        # 维度
        for name, dim in src.dimensions.items():
            size = target_size(name, len(dim), args.ny, args.nx)
            dst.createDimension(name, None if dim.isunlimited() else size)

        # 变量（按维度索引裁切）
        for name, var in src.variables.items():
            slices = []
            for d in var.dimensions:
                cur = len(src.dimensions[d])
                slices.append(slice(0, target_size(d, cur, args.ny, args.nx)))
            chunksizes = None
            if var.ndim >= 3 and all(
                    dst.dimensions[d].size and dst.dimensions[d].size > 0 for d in var.dimensions):
                chunksizes = None
            out = dst.createVariable(
                name, var.dtype, var.dimensions,
                zlib=True, complevel=args.complevel, chunksizes=chunksizes)
            out.setncatts({k: var.getncattr(k) for k in var.ncattrs()})
            data = var[tuple(slices)]
            out[tuple(slices)] = data
    finally:
        src.close()
        dst.close()

    size_mb = os.path.getsize(args.dst) / 1024 / 1024
    print("已写出 %s（%.2f MB，ny=%d nx=%d）" % (args.dst, size_mb, args.ny, args.nx))
    return 0


if __name__ == "__main__":
    sys.exit(main())
