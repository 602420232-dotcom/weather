#ifndef RISK_ASSESSOR_H
#define RISK_ASSESSOR_H

#include <vector>
#include <string>
#include <unordered_map>

namespace uav_sdk {

/**
 * @brief 气象风险等级
 */
enum class RiskLevel {
    LOW = 0,      // 低风险
    MEDIUM = 1,    // 中风险
    HIGH = 2,      // 高风险
    SEVERE = 3     // 严重风险（不建议飞行）
};

/**
 * @brief 气象数据
 */
struct WeatherData {
    double wind_speed;      // 风速 (m/s)
    double wind_direction;  // 风向 (度)
    double temperature;     // 温度 (摄氏度)
    double humidity;        // 湿度 (%)
    double visibility;     // 能见度 (km)
    double precipitation;   // 降水量 (mm/h)
    bool has_thunderstorm;  // 是否有雷暴
};

/**
 * @brief 风险评估结果
 */
struct RiskAssessment {
    RiskLevel level;
    double score;           // 0-100 的风险分数
    std::vector<std::string> warnings;  // 警告信息
};

/**
 * @brief 气象风险评估器
 * 
 * 用于评估无人机飞行的气象风险
 */
class RiskAssessor {
public:
    /**
     * @brief 构造函数
     */
    RiskAssessor();
    
    /**
     * @brief 析构函数
     */
    ~RiskAssessor();
    
    /**
     * @brief 评估气象风险
     * @param weather 气象数据
     * @return 风险评估结果
     */
    RiskAssessment assess(const WeatherData& weather);
    
    /**
     * @brief 批量评估多个气象站数据
     * @param weather_list 气象数据列表
     * @return 风险评估结果列表
     */
    std::vector<RiskAssessment> assess_batch(const std::vector<WeatherData>& weather_list);
    
    /**
     * @brief 获取飞行窗口建议
     * @param weather 气象数据
     * @return 建议信息
     */
    std::string get_flight_window_advice(const WeatherData& weather);
    
    /**
     * @brief 设置自定义风险阈值
     */
    void set_wind_speed_threshold(double threshold);
    void set_visibility_threshold(double threshold);
    void set_temperature_range(double min_temp, double max_temp);

private:
    double wind_speed_threshold_;
    double visibility_threshold_;
    double min_temperature_;
    double max_temperature_;
    
    double calculate_wind_risk(double wind_speed) const;
    double calculate_visibility_risk(double visibility) const;
    double calculate_temperature_risk(double temperature) const;
    double calculate_humidity_risk(double humidity) const;
    double calculate_precipitation_risk(double precipitation) const;
    double calculate_thunderstorm_risk(bool has_thunderstorm) const;
    
    RiskLevel score_to_level(double score) const;
};

}  // namespace uav_sdk

#endif  // RISK_ASSESSOR_H
