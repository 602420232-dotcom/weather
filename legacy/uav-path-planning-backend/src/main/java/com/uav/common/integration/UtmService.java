package com.uav.common.integration;

import com.uav.common.integration.dto.AirspaceStatus;
import com.uav.common.integration.dto.DroneInfo;
import com.uav.common.integration.dto.EmergencyResponse;
import com.uav.common.integration.dto.FlightApproval;
import com.uav.common.integration.dto.FlightPlan;

/**
 * UTM (UAS Traffic Management) 服务接口
 * 
 * 提供无人机交通管理系统的集成能力，包括：
 * - 无人机注册
 * - 飞行计划提交与审批
 * - 空域状态查询
 * - 紧急情况报警
 */
public interface UtmService {

    /**
     * 注册无人机到UTM系统
     *
     * @param droneInfo 无人机信息
     * @return 注册ID
     */
    String registerDrone(DroneInfo droneInfo);

    /**
     * 提交飞行计划
     *
     * @param plan 飞行计划
     * @return 是否提交成功
     */
    boolean submitFlightPlan(FlightPlan plan);

    /**
     * 获取飞行审批状态
     *
     * @param planId 飞行计划ID
     * @return 飞行审批结果
     */
    FlightApproval getFlightApproval(String planId);

    /**
     * 查询空域状态
     *
     * @param lat 纬度
     * @param lon 经度
     * @param radius 半径(米)
     * @return 空域状态
     */
    AirspaceStatus queryAirspace(double lat, double lon, double radius);

    /**
     * 发送紧急报警
     *
     * @param droneId 无人机ID
     * @param type 紧急情况类型
     * @return 紧急响应
     */
    EmergencyResponse sendEmergencyAlert(String droneId, EmergencyType type);

    /**
     * 紧急情况类型枚举
     */
    enum EmergencyType {
        BATTERY_LOW,           // 电量低
        SIGNAL_LOST,           // 信号丢失
        GEOFENCE_VIOLATION,    // 地理围栏违规
        MECHANICAL_FAILURE,    // 机械故障
        WEATHER_EMERGENCY,     // 天气紧急情况
        COLLISION_RISK,        // 碰撞风险
        OTHER                  // 其他
    }
}
