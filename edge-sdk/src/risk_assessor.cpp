#include "risk_assessor.h"
#include <algorithm>
#include <sstream>

namespace uav_sdk {

RiskAssessor::RiskAssessor()
    : wind_speed_threshold_(10.0),      // 10 m/s
      visibility_threshold_(3.0),       // 3 km
      min_temperature_(-20.0),         // -20°C
      max_temperature_(50.0) {          // 50°C
}

RiskAssessor::~RiskAssessor() {
}

double RiskAssessor::calculate_wind_risk(double wind_speed) const {
    if (wind_speed < 5.0) return 0.0;
    if (wind_speed < 10.0) return (wind_speed - 5.0) * 10.0;  // 0-50
    if (wind_speed < 15.0) return 50.0 + (wind_speed - 10.0) * 10.0;  // 50-100
    return 100.0;
}

double RiskAssessor::calculate_visibility_risk(double visibility) const {
    if (visibility > 10.0) return 0.0;
    if (visibility > 5.0) return (10.0 - visibility) * 10.0;  // 0-50
    if (visibility > 3.0) return 50.0 + (5.0 - visibility) * 25.0;  // 50-100
    return 100.0;
}

double RiskAssessor::calculate_temperature_risk(double temperature) const {
    // 最优温度范围: 15-25°C
    if (temperature >= 15.0 && temperature <= 25.0) return 0.0;
    
    if (temperature < 15.0) {
        double diff = 15.0 - temperature;
        return std::min(100.0, diff * 5.0);  // 每降低1°C增加5分
    } else {
        double diff = temperature - 25.0;
        return std::min(100.0, diff * 5.0);  // 每升高1°C增加5分
    }
}

double RiskAssessor::calculate_humidity_risk(double humidity) const {
    if (humidity < 80.0) return 0.0;
    if (humidity < 95.0) return (humidity - 80.0) * 5.0;  // 0-75
    return 75.0 + (humidity - 95.0) * 5.0;  // 75-100
}

double RiskAssessor::calculate_precipitation_risk(double precipitation) const {
    if (precipitation < 0.1) return 0.0;
    if (precipitation < 2.5) return precipitation * 20.0;  // 0-50 (小雨)
    if (precipitation < 7.5) return 50.0 + (precipitation - 2.5) * 10.0;  // 50-100 (中雨)
    return 100.0;  // 大雨及以上
}

double RiskAssessor::calculate_thunderstorm_risk(bool has_thunderstorm) const {
    return has_thunderstorm ? 100.0 : 0.0;
}

RiskLevel RiskAssessor::score_to_level(double score) const {
    if (score < 25.0) return RiskLevel::LOW;
    if (score < 50.0) return RiskLevel::MEDIUM;
    if (score < 75.0) return RiskLevel::HIGH;
    return RiskLevel::SEVERE;
}

RiskAssessment RiskAssessor::assess(const WeatherData& weather) {
    RiskAssessment result;
    
    // 计算各因素风险分数
    double wind_risk = calculate_wind_risk(weather.wind_speed);
    double vis_risk = calculate_visibility_risk(weather.visibility);
    double temp_risk = calculate_temperature_risk(weather.temperature);
    double hum_risk = calculate_humidity_risk(weather.humidity);
    double precip_risk = calculate_precipitation_risk(weather.precipitation);
    double thunder_risk = calculate_thunderstorm_risk(weather.has_thunderstorm);
    
    // 加权平均
    double total_score = wind_risk * 0.30 +    // 风速权重30%
                        vis_risk * 0.25 +      // 能见度权重25%
                        temp_risk * 0.15 +     // 温度权重15%
                        hum_risk * 0.10 +      // 湿度权重10%
                        precip_risk * 0.15 +   // 降水权重15%
                        thunder_risk * 0.05;   // 雷暴权重5%
    
    result.score = std::min(100.0, std::max(0.0, total_score));
    result.level = score_to_level(result.score);
    
    // 生成警告信息
    if (wind_risk > 50.0) {
        if (wind_risk >= 75.0) {
            result.warnings.push_back("严重警告: 风速过高，建议推迟飞行");
        } else {
            result.warnings.push_back("警告: 风速较高，谨慎飞行");
        }
    }
    
    if (vis_risk > 50.0) {
        if (vis_risk >= 75.0) {
            result.warnings.push_back("严重警告: 能见度极差，禁止飞行");
        } else {
            result.warnings.push_back("警告: 能见度较低");
        }
    }
    
    if (thunder_risk > 0.0) {
        result.warnings.push_back("严重警告: 检测到雷暴天气，禁止飞行");
    }
    
    if (precip_risk > 50.0) {
        result.warnings.push_back("警告: 有降水，建议推迟飞行");
    }
    
    if (temp_risk > 50.0) {
        result.warnings.push_back("警告: 温度超出适宜范围");
    }
    
    return result;
}

std::vector<RiskAssessment> RiskAssessor::assess_batch(
    const std::vector<WeatherData>& weather_list) {
    
    std::vector<RiskAssessment> results;
    results.reserve(weather_list.size());
    
    for (const auto& weather : weather_list) {
        results.push_back(assess(weather));
    }
    
    return results;
}

std::string RiskAssessor::get_flight_window_advice(const WeatherData& weather) {
    RiskAssessment assessment = assess(weather);
    
    std::ostringstream advice;
    
    switch (assessment.level) {
        case RiskLevel::LOW:
            advice << "适宜飞行";
            break;
        case RiskLevel::MEDIUM:
            advice << "可以飞行，但需谨慎";
            break;
        case RiskLevel::HIGH:
            advice << "不建议飞行";
            break;
        case RiskLevel::SEVERE:
            advice << "禁止飞行";
            break;
    }
    
    advice << " (风险分数: " << assessment.score << "/100)";
    
    return advice.str();
}

void RiskAssessor::set_wind_speed_threshold(double threshold) {
    wind_speed_threshold_ = threshold;
}

void RiskAssessor::set_visibility_threshold(double threshold) {
    visibility_threshold_ = threshold;
}

void RiskAssessor::set_temperature_range(double min_temp, double max_temp) {
    min_temperature_ = min_temp;
    max_temperature_ = max_temp;
}

}  // namespace uav_sdk
