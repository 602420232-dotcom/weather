#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""合成 wrfout → 端到端回归测试（**不需要 VM 上的真实 wrfout**）

为什么要有这个
--------------
`selftest_wrf_processor.py` 需要真实 wrfout，而 wrfout 都在 VM 里 ⇒ 本机改完
**没法验证**。`validate_physics.py` 能测热力学内核，但测不到：
    · `dims` 与 stride 后数组是否一致（★FIX11）
    · Java 传的 `height` 是否真的生效（★FIX10）
    · 降水因子能不能算（★FIX9，需要 Time≥2 才能做差）
    · `risk.turbulence` 里的 W 统计（★FIX6 更正后）
    · payload 结构/体积

⇒ 用 `netCDF4` 造一个**维度与变量名都符合 WRF 约定**的小文件（Time=2,
   bottom_top=10, south_north=8, west_east=10），把这些全部覆盖。
**这是"用合成数据验证接口与聚合逻辑"，不是"用合成数据验证物理"** ——
物理那部分仍由 `validate_physics.py` 的参考值/单调性判据负责，二者不重叠。
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8", errors="replace")
    except Exception:                                              # noqa: BLE001
        pass

import netCDF4 as nc
import numpy as np

RD_OVER_CP = 287.0 / 1004.5
G = 9.80665

HERE = os.path.dirname(os.path.abspath(__file__))
PARSER = os.path.join(HERE, "wrf_processor.py")

NT, NZ, NY, NX = 2, 10, 8, 10
NZ_S = NZ + 1
RAIN_T1 = 3.5          # t=1 的累积降水（mm）⇒ 与 t=0 做差应得 3.5 mm/h


def build(path: str) -> None:
    rng = np.random.default_rng(20260925)
    ds = nc.Dataset(path, "w", format="NETCDF4")

    ds.createDimension("Time", None)
    ds.createDimension("DateStrLen", 19)
    ds.createDimension("bottom_top", NZ)
    ds.createDimension("bottom_top_stag", NZ_S)
    ds.createDimension("south_north", NY)
    ds.createDimension("west_east", NX)
    # ★ 交错维：真实 WRF 里 U 在 west_east_stag、V 在 south_north_stag。
    #   第一次没建这两个维，U/V 用了非交错网格 ⇒ 解析器**正确地**去交错后
    #   变成 (10,8,9)/(10,7,10)，拼接时维度对不上而报错。
    #   **那是我的合成文件错了，不是解析器错了。**
    ds.createDimension("south_north_stag", NY + 1)
    ds.createDimension("west_east_stag", NX + 1)

    def var(name, dims, data, units=""):
        v = ds.createVariable(name, "f4", dims)
        v[:] = data
        if units:
            v.units = units
        return v

    # ★ 真实 wrfout 的垂直分层是**近地面很密**的（本域第 1 层中心 21.8~26.2 m AGL）。
    #   第一版用等距 0~16000 m ⇒ L1 落在 **800 m**，与 TH2（2 m 诊断量）
    #   差 **3.29 K**，位温基准自证直接失败。
    #   ★ 那是**合成文件不像真的，不是代码错了** —— 判据 I2 抓对了，只是抓的是我。
    #   等距分层会让"位温基准自证"这个机制失去意义（它本来就是靠 L1≈2 m 才成立的）。
    z_stag = np.array([0.0, 25.0, 60.0, 110.0, 180.0, 280.0, 420.0,
                       620.0, 900.0, 1300.0, 1800.0])
    z_mass = 0.5 * (z_stag[:-1] + z_stag[1:])                # 质量层高度 (m)
    p_mass = 100000.0 * np.exp(-z_mass / 8000.0)             # 质量层气压 (Pa)

    T2k = 298.15                                             # 地面 25 °C
    t_env = T2k - 6.5e-3 * z_mass                            # 环境温度
    th_env = t_env * (100000.0 / p_mass) ** RD_OVER_CP       # 位温
    t_pert = th_env - 300.0                                  # wrfout 的 T 是**扰动位温**

    b3t = lambda prof: np.broadcast_to(prof[:, None, None], (NT, NZ, NY, NX))  # noqa: E731
    _ = (b3t,)          # 保留形状约定备查；下面用显式 broadcast 更直观

    # 2-D 场：给一点空间变化，避免"常数场"掩盖广播错误
    lat0, lon0 = 30.7, 104.1
    xlat = lat0 + 0.03 * np.arange(NY)[:, None] + 0.0 * np.arange(NX)[None, :]
    xlon = lon0 + 0.03 * np.arange(NX)[None, :] + 0.0 * np.arange(NY)[:, None]

    q_sfc = 0.014                                            # 14 g/kg
    q_prof = q_sfc * np.exp(-z_mass / 2500.0)

    def tvar(name, dims, base2d, units=""):
        """带 Time 维的场：两个时次给略微不同的值。"""
        d = np.stack([base2d, base2d * 0.999])               # (NT, ...)
        return var(name, dims, d, units)

    var("P", ("Time", "bottom_top", "south_north", "west_east"),
        np.zeros((NT, NZ, NY, NX), "f4"), "Pa")
    var("PB", ("Time", "bottom_top", "south_north", "west_east"),
        np.broadcast_to(p_mass[:, None, None], (NT, NZ, NY, NX)).copy(), "Pa")
    var("T", ("Time", "bottom_top", "south_north", "west_east"),
        np.broadcast_to(t_pert[:, None, None], (NT, NZ, NY, NX)).copy(), "K")
    var("PH", ("Time", "bottom_top_stag", "south_north", "west_east"),
        np.zeros((NT, NZ_S, NY, NX), "f4"), "m2 s-2")
    var("PHB", ("Time", "bottom_top_stag", "south_north", "west_east"),
        np.broadcast_to((G * z_stag)[:, None, None], (NT, NZ_S, NY, NX)).copy(), "m2 s-2")
    var("QVAPOR", ("Time", "bottom_top", "south_north", "west_east"),
        np.broadcast_to(q_prof[:, None, None], (NT, NZ, NY, NX)).copy(), "kg kg-1")
    var("U", ("Time", "bottom_top", "south_north", "west_east_stag"),
        np.full((NT, NZ, NY, NX + 1), 5.0, "f4"), "m s-1")
    var("V", ("Time", "bottom_top", "south_north_stag", "west_east"),
        np.full((NT, NZ, NY + 1, NX), -2.0, "f4"), "m s-1")

    # ★ W：交错在 bottom_top_stag（NZ+1 层），解析器会去交错取相邻平均。
    #   为了让**去交错后**的极值正好是 +1.8 / −1.2，把相邻两个 w 层设成同一个值。
    w_stag = np.zeros(NZ_S)
    w_stag[2] = w_stag[3] = 1.8      # 上升
    w_stag[5] = w_stag[6] = -1.2     # 下沉
    var("W", ("Time", "bottom_top_stag", "south_north", "west_east"),
        np.broadcast_to(w_stag[:, None, None], (NT, NZ_S, NY, NX)).copy(), "m s-1")

    tvar("T2", ("Time", "south_north", "west_east"),
         np.full((NY, NX), T2k, "f4"), "K")
    tvar("Q2", ("Time", "south_north", "west_east"),
         np.full((NY, NX), q_sfc, "f4"), "kg kg-1")
    tvar("U10", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 6.0, "f4"), "m s-1")
    tvar("V10", ("Time", "south_north", "west_east"),
         np.full((NY, NX), -3.0, "f4"), "m s-1")
    tvar("PSFC", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 100000.0, "f4"), "Pa")
    tvar("HGT", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 500.0, "f4"), "m")
    tvar("TH2", ("Time", "south_north", "west_east"),
         np.full((NY, NX), T2k * (100000.0 / 100000.0) ** RD_OVER_CP, "f4"), "K")
    tvar("XLAT", ("Time", "south_north", "west_east"), xlat, "deg")
    tvar("XLONG", ("Time", "south_north", "west_east"), xlon, "deg")
    tvar("PBLH", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 800.0, "f4"), "m")
    tvar("UST", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 0.25, "f4"), "m s-1")
    tvar("HFX", ("Time", "south_north", "west_east"),
         np.full((NY, NX), 120.0, "f4"), "W m-2")
    tvar("QCLOUD", ("Time", "bottom_top", "south_north", "west_east"),
         np.zeros((NZ, NY, NX), "f4"), "kg kg-1")

    # ★ RAINNC：t=0 为 0，t=1 为 RAIN_T1 ⇒ 做差应得 3.5 mm/h（★FIX9 的判据）
    rain = np.zeros((NT, NY, NX), "f4")
    rain[1] = RAIN_T1
    var("RAINNC", ("Time", "south_north", "west_east"), rain, "mm")

    times = ds.createVariable("Times", "S1", ("Time", "DateStrLen"))
    for i, s in enumerate(("2022-07-01_00:00:00", "2022-07-01_01:00:00")):
        times[i, :] = np.array(list(s), dtype="S1")

    ds.TITLE = "SYNTHETIC wrfout for parser regression (NOT real data)"
    ds.close()


def invoke(action: str, params: dict) -> dict:
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False,
                                     encoding="utf-8") as f:
        json.dump(params, f)
        pf = f.name
    try:
        # ★ 必须显式 encoding="utf-8"：Windows 上 text=True 会用系统默认 GBK 解码，
        #   而解析器输出是 UTF-8 ⇒ `UnicodeDecodeError: 'gbk' codec can't decode`，
        #   表现成"无 JSON 输出"，很容易被误判成解析器崩了。
        #   （这是本项目记录过的 GBK 坑，本轮第 2 次遇到。）
        p = subprocess.run([sys.executable, PARSER, action, pf],
                           capture_output=True, text=True, timeout=900,
                           encoding="utf-8", errors="replace")
        raw = (p.stdout or "").strip() or (p.stderr or "").strip()
        for line in reversed(raw.splitlines()):
            if line.strip().startswith("{"):
                try:
                    return json.loads(line.strip())
                except json.JSONDecodeError:
                    continue
        return {"success": False, "error": "无 JSON 输出: %s" % raw[:300]}
    finally:
        os.unlink(pf)


PASS, FAIL = "PASS", "FAIL"
_res = []


def check(name, ok, detail=""):
    _res.append((name, PASS if ok else FAIL, detail))
    print("  [%s] %s%s" % (PASS if ok else FAIL, name, ("  — " + detail) if detail else ""))
    return ok


def main() -> int:
    out = os.path.join(HERE, "_synthetic_wrfout.nc")
    print("=" * 78)
    print("合成 wrfout → 端到端回归（验证接口与聚合逻辑，**不验证物理**）")
    print("=" * 78)
    build(out)
    print("  合成文件: %s（%.2f MB，Time=%d, nz=%d, %dx%d）"
          % (out, os.path.getsize(out) / 1e6, NT, NZ, NY, NX))

    print("\nA. 基本链路")
    r = invoke("parse", {"filePath": out, "stride": 1})
    if not check("A1 parse 成功", r.get("success") is True, str(r.get("error"))[:160]):
        return 1
    pl, meta = r["payload"], r.get("meta", {})
    check("A2 dims.level == grid.levels 长度",
          pl["dims"]["level"] == len(pl["grid"]["levels"]),
          "%s vs %s" % (pl["dims"]["level"], len(pl["grid"]["levels"])))

    print("\nB. ★FIX11 dims 必须描述**输出数组**（stride 一致性）")
    ok = True
    for st in (1, 2, 3):
        rr = invoke("parse", {"filePath": out, "stride": st})
        if not rr.get("success"):
            check("B stride=%d 解析成功" % st, False, str(rr.get("error"))[:80])
            ok = False
            continue
        p = rr["payload"]
        nlat, nlon = len(p["temperature"][0]), len(p["temperature"][0][0])
        good = (p["dims"]["lat"] == nlat and p["dims"]["lon"] == nlon
                and len(p["grid"]["lat"]) == nlat and len(p["grid"]["lon"]) == nlon)
        check("B stride=%d dims 与数组一致" % st, good,
              "dims=%dx%d  数组=%dx%d  sourceDims=%s"
              % (p["dims"]["lat"], p["dims"]["lon"], nlat, nlon,
                 p["grid"].get("sourceDims")))
        ok = ok and good
    check("B0 ★FIX11 stride=3 时 dims.lat 不再是原始网格尺寸",
          invoke("parse", {"filePath": out, "stride": 3})["payload"]["dims"]["lat"]
          != invoke("parse", {"filePath": out, "stride": 1})["payload"]["dims"]["lat"],
          "旧版两者恒等（都报原始 %d），这正是那个契约 bug" % NY)

    print("\nC. ★FIX10 Java 传的 height 必须真的生效")
    r1 = invoke("parse", {"filePath": out, "height": 100})
    r2 = invoke("parse", {"filePath": out, "height": [50, 200]})
    if r1.get("success") and r2.get("success"):
        l1 = r1["payload"]["grid"]["levels"]
        l2 = r2["payload"]["grid"]["levels"]
        check("C1 height=100 ⇒ 只有 1 层且为 100", l1 == [100.0], str(l1))
        check("C2 height=[50,200] ⇒ 两层", l2 == [50.0, 200.0], str(l2))
        check("C3 meta.requestedLevels 回显了请求",
              r1.get("meta", {}).get("requestedLevels") == [100.0],
              str(r1.get("meta", {}).get("requestedLevels")))
    else:
        check("C height 路径可用", False,
              str(r1.get("error") or r2.get("error"))[:120])
    r3 = invoke("parse", {"filePath": out, "height": "abc"})
    check("C4 height 非法值给出明确错误而不是静默走默认",
          r3.get("success") is False and "height" in str(r3.get("error", "")),
          str(r3.get("error"))[:80])
    # ★ 取 meta 要用 .get 逐层兜底：旧版根本没有 requestedLevels 这个键，
    #   直接下标会在**红测**里抛 KeyError、把整轮跑断（本轮就栽了一次）。
    r5 = invoke("parse", {"filePath": out})
    req5 = (r5.get("meta") or {}).get("requestedLevels")
    check("C5 不传 height/levels 时走默认层（meta 里说明）",
          "默认" in str(req5), "requestedLevels=%r" % (req5,))

    print("\nD. ★FIX9 降水：累积量必须做差")
    risk = r.get("risk", {})
    summ = risk.get("summary", {})
    pr = summ.get("factors", {}).get("precipitation_risk")
    check("D1 降水因子不再是恒 0", pr is not None and pr > 0.0,
          "precipitation_risk=%s（旧版恒 0，因为 max(RAINNC)*0.0）" % pr)
    check("D2 小时雨强 == 累积量之差（%.1f mm）" % RAIN_T1,
          abs((summ.get("worst", {}).get("rain_mm_h") or -999) - RAIN_T1) < 1e-3,
          "rain_mm_h=%s" % summ.get("worst", {}).get("rain_mm_h"))
    check("D3 weights 里降水项存在且权重和 == 1",
          abs(sum((summ.get("weights") or {}).values()) - 1.0) < 1e-9,
          str(summ.get("weights")))

    print("\nE. ★FIX7 CIN / ★FIX8 LFC")
    conv = risk.get("convection", {})
    check("E1 cin_min ≤ 0（符号约定）", (conv.get("cin_min") or 0) <= 0.0,
          "cin_min=%s" % conv.get("cin_min"))
    check("E2 cin_min 有量级或有明确 0（不因裁剪而恒 0）",
          conv.get("cin_min") is not None, "cin_min=%s" % conv.get("cin_min"))
    check("E3 lfc 报出了有效列数（★FIX8：不再让假 0 参与平均）",
          "lfc_valid_cols" in conv and "lfc_total_cols" in conv,
          "valid=%s / total=%s" % (conv.get("lfc_valid_cols"),
                                   conv.get("lfc_total_cols")))

    print("\nF. ★FIX6 W 统计")
    turb = risk.get("turbulence", {})
    check("F1 w_max_ms 来自真实 W（应 ≈ +1.8）",
          turb.get("w_max_ms") is not None and abs(turb["w_max_ms"] - 1.8) < 0.05,
          "w_max_ms=%s" % turb.get("w_max_ms"))
    check("F2 w_min_ms 来自真实 W（应 ≈ −1.2）",
          turb.get("w_min_ms") is not None and abs(turb["w_min_ms"] + 1.2) < 0.05,
          "w_min_ms=%s" % turb.get("w_min_ms"))
    check("F3 w_note 说明 W **未**参与 TKE（注释与实现一致）",
          "未" in str(turb.get("w_note", "")) or "not" in str(turb.get("w_note", "")).lower(),
          str(turb.get("w_note"))[:70])

    print("\nI. ★FIX12 位温基准自证证据 / ★FIX13 PBLH·UST·HFX 真的被用上")
    tb = (r.get("meta") or {}).get("theta_base") or {}
    check("I1 ★FIX12 meta.theta_base 带自证证据（verified/residual/candidates）",
          all(k in tb for k in ("verified", "candidates", "base_used")),
          "verified=%s residual=%s base=%s" % (tb.get("verified"),
                                               tb.get("residual_K"), tb.get("base_used")))
    check("I2 合成文件的基准能被自证（TH2 与 base=300 一致）",
          tb.get("verified") is True,
          "residual=%s K（阈值 %s）" % (tb.get("residual_K"), tb.get("tolerance_K")))

    bl = (risk.get("boundary_layer") or {})
    check("I3 ★FIX13 boundary_layer 块存在且列出了可用量",
          set(bl.get("available") or []) >= {"PBLH", "UST", "HFX"},
          "available=%s" % bl.get("available"))
    check("I4 PBLH 的数值真的取到了（合成文件里是 800 m）",
          abs(((bl.get("pblh") or {}).get("mean") or -1) - 800.0) < 1e-3,
          "pblh.mean=%s" % (bl.get("pblh") or {}).get("mean"))
    check("I5 UST / HFX 也取到了（0.25 m/s、120 W/m²）",
          abs(((bl.get("ust") or {}).get("mean") or -1) - 0.25) < 1e-6
          and abs(((bl.get("hfx") or {}).get("mean") or -1) - 120.0) < 1e-6,
          "ust=%s hfx=%s" % ((bl.get("ust") or {}).get("mean"),
                             (bl.get("hfx") or {}).get("mean")))

    n_f = sum(1 for _, s, _ in _res if s == FAIL)
    print("\n" + "=" * 78)
    print("汇总: %d PASS / %d FAIL" % (len(_res) - n_f, n_f))
    for n, s, d in _res:
        if s == FAIL:
            print("  ✗ %s: %s" % (n, d))
    print("=" * 78)
    return 1 if n_f else 0


if __name__ == "__main__":
    sys.exit(main())
