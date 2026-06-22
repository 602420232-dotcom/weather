package com.uav.detection.drone.model;

/**
 * 探测任务类型枚举
 */
public enum MissionType {
    /** 水平网格扫描：指定区域按经纬度网格逐行飞行 */
    GRID_SCAN,
    /** 垂直剖面扫描：指定点从低空到高空的螺旋上升 */
    VERTICAL_PROFILE,
    /** 气象现象追踪：跟随指定气象目标移动 */
    TARGET_TRACKING,
    /** 自由采集：按指定航线自由飞行采集 */
    FREE_COLLECTION
}
