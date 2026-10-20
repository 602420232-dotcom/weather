#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""由样例 wrfout 造一份**输入不同**的副本，用于「输出随输入变」的判据。

用法：
    python tools/verify/make_perturbed_wrfout.py <src.nc> <dst.nc>

改动（刻意只动会被三个诊断量用到的物理量）：
    T2     +8.0 K     （影响 CAPE、能见度：温度→饱和比湿→RH）
    Q2     ×1.5       （影响能见度：湿度）
    QVAPOR ×1.5       （同上，3D）
    U / V  ×1.35      （影响 TKE：风切变）

★ 为什么需要它：C2 的判据是「三项都**从实际数据算出**」。
只对着一个文件断言"不等于旧常量"是弱证据（常量恰好相等也可能蒙对）；
换成**两份输入 + 要求输出跟着变**，才能把"常量/写死"排除掉。
"""
import os
import sys

import netCDF4 as nc

PERTURB = {
    "T2": ("add", 8.0),
    "Q2": ("mul", 1.5),
    "QVAPOR": ("mul", 1.5),
    "U": ("mul", 1.35),
    "V": ("mul", 1.35),
}


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    src_path, dst_path = sys.argv[1], sys.argv[2]
    if not os.path.isfile(src_path):
        print("源文件不存在: %s" % src_path)
        return 2
    os.makedirs(os.path.dirname(os.path.abspath(dst_path)), exist_ok=True)

    src = nc.Dataset(src_path, "r")
    dst = nc.Dataset(dst_path, "w", format="NETCDF4")
    changed = []
    try:
        dst.setncatts({k: src.getncattr(k) for k in src.ncattrs()})
        for name, dim in src.dimensions.items():
            dst.createDimension(name, None if dim.isunlimited() else len(dim))
        for name, var in src.variables.items():
            out = dst.createVariable(name, var.dtype, var.dimensions,
                                     zlib=True, complevel=4)
            out.setncatts({k: var.getncattr(k) for k in var.ncattrs()})
            data = var[:]
            if name in PERTURB:
                op, val = PERTURB[name]
                data = data + val if op == "add" else data * val
                changed.append("%s(%s%g)" % (name, "+" if op == "add" else "×", val))
            out[:] = data
    finally:
        src.close()
        dst.close()
    print("已写出 %s；改动: %s" % (dst_path, ", ".join(changed)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
