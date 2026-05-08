#ifndef LOGGER_H
#define LOGGER_H

#include <string>
#include <sstream>
#include <ctime>
#include <iostream>

namespace uav_sdk {

enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
};

class Logger {
public:
    static Logger& get_instance() {
        static Logger instance;
        return instance;
    }

    void set_level(LogLevel level) { level_ = level; }

    void debug(const std::string& msg) { log(DEBUG, msg); }
    void info(const std::string& msg) { log(INFO, msg); }
    void warn(const std::string& msg) { log(WARN, msg); }
    void error(const std::string& msg) { log(ERROR, msg); }

private:
    Logger() : level_(INFO) {}

    void log(LogLevel level, const std::string& msg) {
        if (level < level_) return;
        std::string level_str;
        switch (level) {
            case DEBUG: level_str = "DEBUG"; break;
            case INFO:  level_str = "INFO"; break;
            case WARN:  level_str = "WARN"; break;
            case ERROR: level_str = "ERROR"; break;
        }
        std::time_t now = std::time(nullptr);
        char time_buf[20];
        std::strftime(time_buf, sizeof(time_buf), "%Y-%m-%d %H:%M:%S", std::localtime(&now));
        if (level >= WARN) {
            std::cerr << time_buf << " [" << level_str << "] " << msg << std::endl;
        } else {
            std::cout << time_buf << " [" << level_str << "] " << msg << std::endl;
        }
    }

    LogLevel level_;
};

}  // namespace uav_sdk

#endif  // LOGGER_H
