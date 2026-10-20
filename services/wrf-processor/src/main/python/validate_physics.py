#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
wrf_processor v2 —— 物理内核验证（与实现无关的判据）
================================================================================

【为什么需要这个文件】
本版首稿把 CAPE 的浮力积分**符号写反了**：
    buoy = Rd·(Tv_p − Tv_e)·ln(p_k/p_prev)     # 上升时 ln(p_k/p_prev)<0
⇒ 把"气块偏暖"算成负浮力，CAPE 累加到了气块**偏冷**的区域，物理完全颠倒。
**而当时的自测照样全绿**，因为 `0 ≤ cape_max ≤ 8000` 是**区间检查**，
对符号错误天然不敏感（修符号后 cape_max 从 2424 跳到 6182 J/kg，区间内都"合法"）。

⇒ 教训：**区间检查抓不到符号错误。** 必须用三类判据：
   A. 与**公开参考值**比对（skew-T 查表值、饱和比湿标准值）—— 与我的实现无关
   B. **单调性/零值**判据 —— 符号写反会让单调性反向
   C. **两种积分坐标**互验（ln p 积分 vs z 积分）

VM 上没有 wrf-python / metpy（已实测），所以走上面这条"参考值 + 不变量"路线，
而不是"和另一个库对答案"。**这一点必须在文档里说清，不能假装做了库对照。**

用法: python validate_physics.py
"""
from __future__ import annotations

import os
import sys
from typing import List, Tuple

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wrf_processor as wp      # noqa: E402

PASS, FAIL, WARN = "\033[92mPASS\033[0m", "\033[91mFAIL\033[0m", "\033[93mWARN\033[0m"
_res: List[Tuple[str, str, str]] = []


def check(name: str, ok: bool, detail: str = "", warn_only: bool = False) -> bool:
    tag = PASS if ok else (WARN if warn_only else FAIL)
    _res.append((name, "PASS" if ok else ("WARN" if warn_only else "FAIL"), detail))
    print(f"  [{tag}] {name}" + (f"\n         {detail}" if detail else ""))
    return ok


def close(a: float, b: float, tol: float) -> bool:
    return abs(a - b) <= tol


# ═══════════════════════════════════════════════════════════════════════════
#  A. 与公开参考值比对（skew-T 查表值；与我的实现无关）
# ═══════════════════════════════════════════════════════════════════════════
def section_A() -> None:
    print("\nA. 热力学内核 vs 公开参考值（skew-T 查表 / 标准公式）")

    # A1 干绝热必须严格守恒位温 θ = T (p0/p)^(Rd/cp)
    T0, p0, p1 = 293.15, 100000.0, 50000.0
    T1 = T0 * (p1 / p0) ** wp.RD_OVER_CP
    th0 = T0 * (p0 / p0) ** -wp.RD_OVER_CP
    th1 = T1 * (p1 / p0) ** -wp.RD_OVER_CP
    check("A1 干绝热守恒位温 θ（应精确）", close(th0, th1, 1e-9),
          f"θ0={th0:.6f} K, θ1={th1:.6f} K  (1000→500 hPa, 20 °C)")

    # A2/A3 饱和比湿标准值
    #    qs(1000 hPa, 20 °C) ≈ 14.9 g/kg ; qs(1000 hPa, 0 °C) ≈ 3.84 g/kg
    qs_20 = float(wp._qs(np.array([100000.0]), np.array([293.15]))[0]) * 1000
    qs_0 = float(wp._qs(np.array([100000.0]), np.array([273.15]))[0]) * 1000
    check("A2 qs(1000 hPa, 20 °C) ≈ 14.9 g/kg", close(qs_20, 14.9, 0.4),
          f"实得 {qs_20:.2f} g/kg")
    check("A3 qs(1000 hPa, 0 °C) ≈ 3.84 g/kg", close(qs_0, 3.84, 0.25),
          f"实得 {qs_0:.3f} g/kg")

    # A4/A5 湿绝热抬升的经典 skew-T 参考点
    #   饱和气块 1000 hPa / 20 °C 抬升到 850 hPa ≈ +5.5 °C，到 500 hPa ≈ −21 °C
    def moist_lift(T_start: float, p_start: float, p_end: float, nsub: int = 400) -> float:
        T = T_start
        for i in range(nsub):
            pa = p_start + (p_end - p_start) * i / nsub
            pb = p_start + (p_end - p_start) * (i + 1) / nsub
            pm = 0.5 * (pa + pb)
            qs = float(wp._qs(np.array([pm]), np.array([T]))[0])
            T = T + float(wp._dTdp_moist(np.array([T]), np.array([pm]),
                                         np.array([qs]))[0]) * (pb - pa)
        return T

    # ★ A4/A5 的参考值我一开始写错了，必须留档说明：
    #   我最初写"1000hPa/20°C 抬到 850 hPa ≈ +5.5 °C、到 500 hPa ≈ −21 °C"，
    #   实测给出 +13.95 / −8.57，一度判为 FAIL。
    #   用标准公式独立核对后确认**是我的参考值错了、代码是对的**：
    #       在 850 hPa / 14 °C：qs≈11.7 g/kg
    #       Γm = (g/cp)(1+Lv·rs/(Rd·T)) / (1+Lv²·rs·ε/(cp·Rd·T²))
    #          = 0.009766 × 1.3561/2.9210 = 4.53 K/km   ← 与教科书一致
    #       1000→850 hPa 约 1300 m ⇒ 降 ≈5.6 K ⇒ +14.4 °C ✅ 与实现相符
    #   ⇒ **教训：验证器报 FAIL 时，先怀疑自己的参考值。**
    #     （这与"读源码得出契约不匹配、必须实测验证"是同一枚硬币的两面：
    #       既要验证被测对象，也要验证验证器自身的基准。）
    #
    # A4' 改用**代数等价但独立写出**的标准 Γm 公式交叉核对 —— 这才是严格判据
    for (Tc, p_hpa) in ((20.0, 1000.0), (14.0, 850.0), (5.0, 700.0), (-10.0, 500.0)):
        T = Tc + 273.15
        p = p_hpa * 100.0
        qs = float(wp._qs(np.array([p]), np.array([T]))[0])
        # 我的实现给出的 dT/dp
        mine = float(wp._dTdp_moist(np.array([T]), np.array([p]),
                                    np.array([qs]))[0])
        # 独立写出的标准公式：dT/dp = Γm·Rd·T/(p·g)
        A = 1.0 + wp.LV * qs / (wp.RD * T)
        B = 1.0 + (wp.LV ** 2) * qs * wp.EPS / (wp.CP * wp.RD * T * T)
        gamma_m = (wp.G / wp.CP) * A / B                  # K/m
        std = gamma_m * wp.RD * T / (p * wp.G)            # K/Pa
        check(f"A4' dT/dp 与标准 Γm 公式一致 (T={Tc}°C, p={p_hpa}hPa)",
              close(mine, std, abs(std) * 1e-6),
              f"实现 {mine:.6e} vs 标准 {std:.6e} K/Pa  "
              f"(Γm={gamma_m * 1000:.3f} K/km)")

    # A5' 数值收敛：湿绝热积分随子步数收敛
    #   用一阶 Euler 时误差 ∝ 1/n。实测 nsub=50 → −8.385 °C，nsub=2000 → −8.593 °C
    #   （差 0.21 K）。parser 原本每层 4 子步 × 33 层 ≈ 132 子步，误差约 0.08 K，
    #   折算到 CAPE 约 0.5% —— 可接受但可改善，**已把 parser 提到 16 子步/层**
    #   （≈528 子步，误差约 0.02 K）。
    #   ⇒ 判据设为 100 vs 2000 子步差 < 0.12 K。
    r100 = moist_lift(293.15, 100000.0, 50000.0, nsub=100) - 273.15
    r2000 = moist_lift(293.15, 100000.0, 50000.0, nsub=2000) - 273.15
    check("A5' 湿绝热积分数值收敛（100 vs 2000 子步 < 0.12 K）",
          close(r100, r2000, 0.12),
          f"nsub=100 → {r100:+.3f} °C, nsub=2000 → {r2000:+.3f} °C  "
          f"(差 {abs(r100 - r2000):.3f} K；parser 用 ≈528 子步，误差更小)")

    # A6 Bolton LCL vs 经典 125 m/K 经验规则
    #    T−Td = 10 K 于 1000 hPa ⇒ LCL 约 1250 m
    ps, Ts = 100000.0, 293.15
    Td = Ts - 10.0
    Tlcl = float(wp._tlcl_k(np.array([Ts]), np.array([Td]))[0])
    # 干绝热从 Ts 降到 Tlcl 的高度：dz = (Ts−Tlcl)/(g/cp)
    dz = (Ts - Tlcl) / (wp.G / wp.CP)
    check("A6 LCL: T−Td=10 K @1000hPa ⇒ ≈1250 m（经典 125 m/K）",
          close(dz, 1250.0, 250.0),
          f"实得 LCL 高度 ≈ {dz:.0f} m, TLCL={Tlcl - 273.15:.2f} °C")

    # A7 露点反解自洽
    q = float(wp._qs(np.array([100000.0]), np.array([293.15]))[0])
    Td_back = float(wp._dewpoint_k(np.array([100000.0]), np.array([q]))[0])
    check("A7 露点/饱和比湿互为逆运算", close(Td_back, 293.15, 0.3),
          f"qs(20 °C,1000hPa) 反解露点 = {Td_back - 273.15:.2f} °C")


# ═══════════════════════════════════════════════════════════════════════════
#  B. 符号与单调性（区间检查抓不到的，靠这些）
# ═══════════════════════════════════════════════════════════════════════════
def section_B() -> None:
    print("\nB. 符号与单调性（这几条才能抓到浮力积分符号写反）")
    print("   ★ 说明：用合成探空，绕开文件读取，直接测方向性")

    def cape_for(T2c: float, RH: float, tint: float = 0.0,
                 inv: float = 0.0) -> Tuple[float, float, float]:
        """合成一条接近真实的探空，直接覆写**高层方法**。

        ★ 测试设计上踩过的两个坑，都留档 —— 它们说明"测试错了"和"代码错了"
          一样常见，验证器报 FAIL 时两边都要怀疑：

        坑 1（饱和）：最初环境写成整层 6.5 K/km 递减 + 30 °C/18 g/kg，气块过 LCL 后
             沿 ~4 K/km 湿绝热上升，比环境暖得多 ⇒ CAPE 顶到 clip 上限 10000，
             **各档全返回 10000，单调性判据整体失效**。
        坑 2（★更隐蔽）：我直接构造 `T` 字段并写成"温度廓线"，
             但 wrfout 的 `T` 是**扰动位温**，代码会**正确地**做
             `T = θ(p/p0)^(Rd/cp)` 转换 —— 于是我的温度廓线被**二次转换**、彻底走样，
             得到的 CAPE 毫无意义（表现为"升温反而 CAPE 下降"）。
             ⇒ **不是代码错，是我把场喂错了变量。**
             正确做法：直接覆写 `temperature_k()` / `pressure_pa()` / `qvapor()`，
             绕开 raw 字段的物理含义。

        另外：升温测试必须**保持相对湿度不变**（q 随 qs 同步增大）。
        固定 q 而只升 T 等于在变干，CAPE 下降是**物理正确**的，不能当单调性判据。
        """
        rd = wp.WrfReader.__new__(wp.WrfReader)
        rd.notes, rd._cache = [], {}
        ny = nx = 1
        nz = 25
        z = np.linspace(25.0, 12000.0, nz)
        T2k = T2c + 273.15 + tint
        p_prof = 100000.0 * np.exp(-z / 8000.0)
        # 环境：0~1500 m 8.0 K/km；其上 6.5 K/km（不下沉增温）
        # ★ `inv`：在 300 m 处叠一个高斯暖层，用来造出**真实的 CIN**。
        #   没有它，地面气块一路正浮力 ⇒ CIN 天然就是 0，
        #   于是 "cin_min 恒为 0" 这个 bug **在测试里完全看不出来**
        #   （旧判据只写 `cin <= 0`，恒 0 永远通过 —— 测试与代码犯了同一个错）。
        inv_prof = inv * np.exp(-((z - 300.0) / 200.0) ** 2)
        T_env = (T2k - 8.0e-3 * np.minimum(z, 1500.0)
                 - 6.5e-3 * np.maximum(z - 1500.0, 0.0) + inv_prof)
        # 湿度由**相对湿度**定 ⇒ 升温时 q 同步增大（物理上有意义的扰动）
        qs_sfc = float(wp._qs(np.array([100000.0]), np.array([T2k]))[0])
        q2 = RH * qs_sfc
        q_env = q2 * np.exp(-z / 1500.0)

        sh = (nz, 1, 1)
        rd.temperature_k = lambda: T_env.reshape(sh)
        rd.pressure_pa = lambda: p_prof.reshape(sh)
        rd.qvapor = lambda: q_env.reshape(sh)
        rd.theta_k = lambda: (T_env * (100000.0 / p_prof) ** wp.RD_OVER_CP).reshape(sh)
        rd.height_agl = lambda: z.reshape(sh)
        rd.surface = lambda: {"T2": np.array([[T2k]]), "Q2": np.array([[q2]]),
                              "PSFC": np.array([[100000.0]]), "HGT": np.zeros((1, 1))}
        rd.has = lambda name: False
        rd._get = lambda name: np.zeros(sh)
        cape, cin, lcl, lfc = rd.parcel_cape_cin()
        return float(cape[0, 0]), float(cin[0, 0]), float(lcl[0, 0])

    base_T, base_RH = 25.0, 0.70
    c0, cin0, lcl0 = cape_for(base_T, base_RH)
    q0 = base_RH * float(wp._qs(np.array([100000.0]),
                                np.array([base_T + 273.15]))[0])
    print(f"       基准: T2={base_T}°C RH={base_RH:.0%} (q={q0 * 1000:.1f} g/kg) "
          f"⇒ CAPE={c0:.0f} J/kg  CIN={cin0:.0f}  LCL={lcl0:.0f} m")
    check("B0 基准 CAPE 落在敏感区间（未饱和到 clip 上限）",
          0.0 < c0 < 8000.0, f"CAPE={c0:.0f} J/kg")

    # B1 保持 RH 不变、升温 ⇒ CAPE 必须单调增
    cw, _, _ = cape_for(base_T + 5.0, base_RH)
    check("B1 定 RH 升温 ⇒ CAPE 单调增（符号判据）",
          cw > c0,
          f"T2 {base_T}→{base_T + 5} °C (RH 固定): CAPE {c0:.0f} → {cw:.0f} J/kg")

    # B1b 只升温不增湿（等于变干）⇒ CAPE 下降是物理正确的，明确区分开
    q_fixed = q0
    rd_tmp = None
    c_dry = None
    try:
        # 用固定 q 复算：临时改 cape_for 的 RH 使 q 保持 q_fixed
        qs_new = float(wp._qs(np.array([100000.0]),
                              np.array([base_T + 5.0 + 273.15]))[0])
        c_dry, _, _ = cape_for(base_T + 5.0, q_fixed / qs_new)
    except Exception:                                  # noqa: BLE001
        pass
    if c_dry is not None:
        check("B1b 定 q 升温（变干）⇒ CAPE 下降属物理正确，不算单调性失败",
              c_dry < cw,
              f"定 q 升温: CAPE={c_dry:.0f} < 定 RH 升温 {cw:.0f} J/kg  "
              f"⇒ 两者必须分开，否则会把正确的物理误判为 bug")

    # B2 定温度、加湿 ⇒ CAPE 必须单调增
    cm, _, _ = cape_for(base_T, min(base_RH * 1.25, 0.99))
    check("B2 定温加湿 ⇒ CAPE 单调增（符号判据）", cm > c0,
          f"RH {base_RH:.0%}→{min(base_RH * 1.25, 0.99):.0%}: "
          f"CAPE {c0:.0f} → {cm:.0f} J/kg")

    # B3 冷干气块 ⇒ CAPE 应接近 0
    cc, _, _ = cape_for(0.0, 0.30)
    check("B3 冷干气块 CAPE 接近 0（不是负值也不是大值）", cc < 200.0,
          f"T2=0 °C, RH=30% ⇒ CAPE={cc:.1f} J/kg")

    # B4 CIN 必须 ≤ 0
    check("B4 CIN ≤ 0（符号约定）", cin0 <= 0.0, f"CIN={cin0:.1f} J/kg")

    # B5 更干 ⇒ LCL 更高
    _, _, lcl_dry = cape_for(base_T, base_RH * 0.5)
    check("B5 更干 ⇒ LCL 更高", lcl_dry > lcl0,
          f"RH {base_RH * 0.5:.0%} 时 LCL={lcl_dry:.0f} m > 基准 {lcl0:.0f} m")

    # ══════════════════════════════════════════════════════════════════════
    #  B6/B6b/B6c —— ★ 专门抓 "CIN 恒为 0" 那一类 bug（2026-09-25 新增）
    #
    #  为什么以前抓不到：B4 只判 `cin <= 0`，而 `clip(cin, -1000, 0)` **恒输出 0**，
    #  永远满足 ⇒ 全绿。**测试与代码犯了同一个错。**
    #  ⇒ 判据必须包含**量级**，而且要造一条**真的被抑制**的探空（低层逆温）。
    # ══════════════════════════════════════════════════════════════════════
    c_inv, cin_inv, _ = cape_for(base_T, base_RH, inv=6.0)
    print(f"       逆温 6 K @300 m: CAPE={c_inv:.0f} J/kg  CIN={cin_inv:+.1f} J/kg")
    check("B6 ★ 逆温探空必须产生**有量级**的 CIN（不是 0）",
          cin_inv < -5.0,
          f"CIN={cin_inv:+.1f} J/kg  "
          f"（`clip(cin,-1000,0)` 那个 bug 在这里会给 0.0，而 0.0 ≤ 0 照样能骗过 B4）")
    check("B6b CIN 仍满足 WRF 的 ≤ 0 符号约定（修 bug 不能把符号约定改坏）",
          cin_inv <= 0.0, f"CIN={cin_inv:+.1f} J/kg")

    _, cin_inv2, _ = cape_for(base_T, base_RH, inv=9.0)
    check("B6c 逆温越强 ⇒ |CIN| 越大（单调性判据）",
          abs(cin_inv2) > abs(cin_inv),
          f"逆温 6 K → |CIN|={abs(cin_inv):.1f} J/kg；9 K → {abs(cin_inv2):.1f} J/kg")


# ═══════════════════════════════════════════════════════════════════════════
#  C. 两种积分坐标互验
# ═══════════════════════════════════════════════════════════════════════════
def section_C() -> None:
    print("\nC. 两种积分坐标互验（ln p 积分 vs z 积分）")
    print("   ★ 同一套气块轨迹，换积分变量：CAPE = Rd∫(Tv_p−Tv_e)d(−ln p)"
          " = ∫ g(Tv_p−Tv_e)/Tv_e dz")
    print("   首稿的符号错误在任何单坐标实现里都看不出来，交叉坐标能暴露")

    ny = nx = 1
    nz = 40
    z = np.linspace(25.0, 14000.0, nz)
    T2k, q2 = 303.15, 0.018
    T_env = T2k - 6.5e-3 * z
    q_env = q2 * np.exp(-z / 1500.0)
    p_env = 100000.0 * np.exp(-z / 8000.0)

    # 复刻 parser 里的气块轨迹（与 parcel_cape_cin 同逻辑）
    Td = float(wp._dewpoint_k(p_env[:1], np.array([q2]))[0])
    Tlcl = float(wp._tlcl_k(np.array([T2k]), np.array([Td]))[0])
    p_lcl = 100000.0 * (Tlcl / T2k) ** (1.0 / wp.RD_OVER_CP)

    Tp, p_prev = T2k, 100000.0
    cape_lnp = 0.0
    cape_z = 0.0
    for k in range(nz):
        p_k = p_env[k]
        above = p_k <= p_lcl
        Tp_dry = T2k * (p_k / 100000.0) ** wp.RD_OVER_CP
        Tp_w = Tp
        for s in range(4):
            pa = p_prev + (p_k - p_prev) * s / 4.0
            pb = p_prev + (p_k - p_prev) * (s + 1) / 4.0
            pm = 0.5 * (pa + pb)
            qsm = float(wp._qs(np.array([pm]), np.array([Tp_w]))[0])
            Tp_w += float(wp._dTdp_moist(np.array([Tp_w]), np.array([pm]),
                                         np.array([qsm]))[0]) * (pb - pa)
        Tp_new = Tp_w if above else Tp_dry
        Tp_bar = 0.5 * (Tp_new + Tp)
        qp_bar = float(wp._qs(np.array([p_k]), np.array([Tp_bar]))[0]) if above else q2
        Tv_p = Tp_bar * (1 + 0.61 * qp_bar)
        Te_bar = 0.5 * (T_env[k] + (T_env[k - 1] if k > 0 else T2k))
        qe_bar = 0.5 * (q_env[k] + (q_env[k - 1] if k > 0 else q2))
        Tv_e = Te_bar * (1 + 0.61 * qe_bar)
        dTv = Tv_p - Tv_e

        # ① ln p 坐标
        cape_lnp += wp.RD * dTv * np.log(p_prev / p_k)
        # ② z 坐标（用层中心高度与层厚）
        zc = 0.5 * (z[k] + (z[k - 1] if k > 0 else 0.0))
        dz = z[k] - (z[k - 1] if k > 0 else 0.0)
        cape_z += wp.G * dTv / max(Tv_e, 1.0) * dz

        Tp, p_prev = Tp_new, p_k

    cape_lnp = max(cape_lnp, 0.0)
    cape_z = max(cape_z, 0.0)
    rel = abs(cape_lnp - cape_z) / max(cape_lnp, 1.0)
    check("C1 两种坐标积分一致（相对差 < 15%）", rel < 0.15,
          f"ln p 坐标 {cape_lnp:.0f} J/kg  vs  z 坐标 {cape_z:.0f} J/kg  "
          f"（相对差 {rel * 100:.1f}%）")

    # C2 符号自证：把符号故意写反，两者应同时变成"负 CAPE 被截成 0"
    cape_wrong = 0.0
    Tp, p_prev = T2k, 100000.0
    for k in range(nz):
        p_k = p_env[k]
        above = p_k <= p_lcl
        Tp_dry = T2k * (p_k / 100000.0) ** wp.RD_OVER_CP
        Tp_w = Tp
        for s in range(4):
            pa = p_prev + (p_k - p_prev) * s / 4.0
            pb = p_prev + (p_k - p_prev) * (s + 1) / 4.0
            pm = 0.5 * (pa + pb)
            qsm = float(wp._qs(np.array([pm]), np.array([Tp_w]))[0])
            Tp_w += float(wp._dTdp_moist(np.array([Tp_w]), np.array([pm]),
                                         np.array([qsm]))[0]) * (pb - pa)
        Tp_new = Tp_w if above else Tp_dry
        Tp_bar = 0.5 * (Tp_new + Tp)
        qp_bar = float(wp._qs(np.array([p_k]), np.array([Tp_bar]))[0]) if above else q2
        Tv_p = Tp_bar * (1 + 0.61 * qp_bar)
        Te_bar = 0.5 * (T_env[k] + (T_env[k - 1] if k > 0 else T2k))
        qe_bar = 0.5 * (q_env[k] + (q_env[k - 1] if k > 0 else q2))
        Tv_e = Te_bar * (1 + 0.61 * qe_bar)
        cape_wrong += wp.RD * (Tv_p - Tv_e) * np.log(p_k / p_prev)   # ← 故意写反
        Tp, p_prev = Tp_new, p_k
    check("C2 符号自证：写反后 CAPE 变负（正解必为正）",
          cape_wrong < 0 < cape_lnp,
          f"正解 {cape_lnp:.0f} J/kg，写反 {cape_wrong:.0f} J/kg "
          f"⇒ 两者符号相反，说明该错误**可被检测**")


def main() -> int:
    print("=" * 78)
    print("wrf_processor v2 物理内核验证")
    print("=" * 78)
    print("★ VM 上无 wrf-python / metpy（已实测）⇒ 走『公开参考值 + 不变量』路线，")
    print("  不是『和另一个库对答案』。文档里必须如实说明这一点。")
    section_A()
    section_B()
    section_C()
    np_ = sum(1 for _, s, _ in _res if s == "PASS")
    nw = sum(1 for _, s, _ in _res if s == "WARN")
    nf = sum(1 for _, s, _ in _res if s == "FAIL")
    print("\n" + "=" * 78)
    print(f"汇总: {np_} PASS / {nw} WARN / {nf} FAIL")
    for n, s, d in _res:
        if s == "FAIL":
            print(f"  ✗ {n}: {d}")
    print("=" * 78)
    return 1 if nf else 0


if __name__ == "__main__":
    sys.exit(main())
