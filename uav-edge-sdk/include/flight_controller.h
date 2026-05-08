#ifndef FLIGHT_CONTROLLER_H
#define FLIGHT_CONTROLLER_H

#include <vector>
#include <string>
#include <chrono>

namespace uav_sdk {

/**
 * @brief 飞行模式
 */
enum class FlightMode {
    MANUAL = 0,           // 手动
    STABILIZE = 1,        // 增稳
    ALT_HOLD = 2,         // 高度保持
    POSITION = 3,         // 位置模式
    AUTO = 4,             // 自动
    RTL = 5,              // 返回发射点
    LAND = 6,             // 降落
    TAKEOFF = 7,          // 起飞
    GUIDED = 8            // 引导模式
};

/**
 * @brief 无人机状态
 */
struct UAVState {
    double latitude;       // 纬度
    double longitude;      // 经度
    double altitude;       // 相对高度 (m)
    double abs_altitude;  // 绝对高度 (m)
    double heading;       // 航向角 (度)
    double speed;         // 地速 (m/s)
    double battery;        // 电池电量 (%)
    FlightMode mode;       // 当前飞行模式
    bool armed;            // 是否上锁
    bool flying;           // 是否在飞行
};

/**
 * @brief 航点
 */
struct Waypoint {
    double latitude;
    double longitude;
    double altitude;
    double speed;          // 速度 (m/s)
    bool action;           // 到达后的动作 (true=继续, false=悬停)
};

/**
 * @brief PX4/ArduPilot 飞控通信接口
 * 
 * 通过 MAVLink 协议与飞控通信
 */
class FlightController {
public:
    /**
     * @brief 构造函数
     * @param device 串口设备路径 (如 "/dev/ttyUSB0" 或 "COM3")
     * @param baudrate 波特率 (默认 57600)
     */
    FlightController(const std::string& device = "COM3", int baudrate = 57600);
    
    /**
     * @brief 析构函数
     */
    ~FlightController();
    
    /**
     * @brief 连接到飞控
     * @return 是否连接成功
     */
    bool connect();
    
    /**
     * @brief 断开连接
     */
    void disconnect();
    
    /**
     * @brief 检查连接状态
     * @return 是否已连接
     */
    bool is_connected() const;
    
    /**
     * @brief 获取无人机当前状态
     * @return 无人机状态
     */
    UAVState get_state();
    
    /**
     * @brief 解锁电机
     * @return 是否成功
     */
    bool arm();
    
    /**
     * @brief 上锁电机
     * @return 是否成功
     */
    bool disarm();
    
    /**
     * @brief 设置飞行模式
     * @param mode 飞行模式
     * @return 是否成功
     */
    bool set_mode(FlightMode mode);
    
    /**
     * @brief 起飞到指定高度
     * @param altitude 目标高度 (m)
     * @return 是否成功
     */
    bool takeoff(double altitude);
    
    /**
     * @brief 降落
     * @return 是否成功
     */
    bool land();
    
    /**
     * @brief 返回发射点
     * @return 是否成功
     */
    bool return_to_launch();
    
    /**
     * @brief 飞向指定位置
     * @param lat 目标纬度
     * @param lon 目标经度
     * @param alt 目标高度
     * @return 是否成功
     */
    bool goto_position(double lat, double lon, double alt);
    
    /**
     * @brief 上传航点列表
     * @param waypoints 航点列表
     * @return 是否成功
     */
    bool upload_mission(const std::vector<Waypoint>& waypoints);
    
    /**
     * @brief 执行任务
     * @return 是否成功
     */
    bool execute_mission();
    
    /**
     * @brief 暂停任务
     * @return 是否成功
     */
    bool pause_mission();

private:
    std::string device_;
    int baudrate_;
    bool connected_;
    int system_id_;
    int component_id_;
    
    bool send_message(int message_id, const std::vector<uint8_t>& payload);
    std::vector<uint8_t> encode_mavlink(int message_id, const std::vector<uint8_t>& payload);
    bool decode_mavlink(const std::vector<uint8_t>& data);
};

}  // namespace uav_sdk

#endif  // FLIGHT_CONTROLLER_H
