package com.uav.detection.drone.model;

/**
 * 探测任务状态枚举
 */
public enum MissionStatus {
    /** 已创建，等待分配无人机 */
    CREATED,
    /** 已分配无人机，准备起飞 */
    ASSIGNED,
    /** 飞行中，正在采集数据 */
    IN_FLIGHT,
    /** 已着陆，等待数据上传 (离线场景) */
    LANDED,
    /** 数据已上传，任务完成 */
    COMPLETED,
    /** 任务异常终止 */
    ABORTED
}
