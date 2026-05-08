#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/chrono.h>

#include "path_planner.h"
#include "risk_assessor.h"
#include "flight_controller.h"

namespace py = pybind11;

PYBIND11_MODULE(edge_sdk_cpp, m) {
    m.doc() = R"pbdoc(
        UAV Edge SDK - C++ Core Module
        --------------------------------
        
        This module provides the C++ core implementation for UAV edge computing,
        including path planning, risk assessment, and flight controller interfaces.
        
        .. currentmodule:: edge_sdk_cpp
        
        .. autosummary::
           :toctree: _generate
           
           PathPlanner
           RiskAssessor
           FlightController
    )pbdoc";

#ifdef VERSION_INFO
    m.attr("__version__") = VERSION_INFO;
#else
    m.attr("__version__") = "1.0.0";
#endif

    // ========================================
    // PathPlanner 绑定
    // ========================================
    py::class_<uav_sdk::PathPlanner>(m, "PathPlanner")
        .def(py::init<int, int, double>(),
             py::arg("grid_width") = 100,
             py::arg("grid_height") = 100,
             py::arg("resolution") = 1.0,
             R"pbdoc(
                Create a PathPlanner instance.
                
                Args:
                    grid_width (int): Grid width in meters
                    grid_height (int): Grid height in meters
                    resolution (float): Grid resolution (meters per cell)
             )pbdoc")
        
        .def("plan", &uav_sdk::PathPlanner::plan,
             R"pbdoc(
                Plan a path from start to goal avoiding obstacles.
                
                Args:
                    start (tuple): Start position (x, y)
                    goal (tuple): Goal position (x, y)
                    obstacles (list): List of obstacle positions [(x1,y1), (x2,y2), ...]
                
                Returns:
                    list: List of path points [(x1,y1), (x2,y2), ...]
             )pbdoc",
             py::arg("start"), py::arg("goal"), py::arg("obstacles"))
        
        .def("set_grid_size", &uav_sdk::PathPlanner::set_grid_size,
             py::arg("width"), py::arg("height"),
             R"pbdoc(Set the grid dimensions.)pbdoc")
        
        .def("set_resolution", &uav_sdk::PathPlanner::set_resolution,
             py::arg("resolution"),
             R"pbdoc(Set the grid resolution.)pbdoc")
        
        .def("is_valid", &uav_sdk::PathPlanner::is_valid,
             py::arg("point"),
             R"pbdoc(Check if a point is within grid bounds.)pbdoc")
        
        .def("is_obstacle", &uav_sdk::PathPlanner::is_obstacle,
             py::arg("point"),
             R"pbdoc(Check if a point is an obstacle.)pbdoc")
        
        .def("clear_obstacles", &uav_sdk::PathPlanner::clear_obstacles,
             R"pbdoc(Clear all obstacles.)pbdoc");

    // ========================================
    // Point 结构体绑定
    // ========================================
    py::class_<uav_sdk::Point>(m, "Point")
        .def(py::init<>())
        .def(py::init<int, int>(), py::arg("x"), py::arg("y"))
        .def_readwrite("x", &uav_sdk::Point::x)
        .def_readwrite("y", &uav_sdk::Point::y)
        .def("__repr__", [](const uav_sdk::Point& p) {
            return py::str("({}, {})").format(p.x, p.y);
        });

    // ========================================
    // RiskAssessor 绑定
    // ========================================
    py::class_<uav_sdk::RiskAssessor>(m, "RiskAssessor")
        .def(py::init<>(),
             R"pbdoc(Create a RiskAssessor instance.)pbdoc")
        
        .def("assess", &uav_sdk::RiskAssessor::assess,
             R"pbdoc(
                Assess weather risk.
                
                Args:
                    weather (dict): Weather data with keys:
                        - wind_speed (float): Wind speed in m/s
                        - wind_direction (float): Wind direction in degrees
                        - temperature (float): Temperature in Celsius
                        - humidity (float): Humidity in %
                        - visibility (float): Visibility in km
                        - precipitation (float): Precipitation in mm/h
                        - has_thunderstorm (bool): Whether there's a thunderstorm
                
                Returns:
                    dict: Risk assessment with keys:
                        - level (str): 'LOW', 'MEDIUM', 'HIGH', or 'SEVERE'
                        - score (float): Risk score from 0 to 100
                        - warnings (list): List of warning messages
             )pbdoc",
             py::arg("weather"))
        
        .def("assess_batch", &uav_sdk::RiskAssessor::assess_batch,
             R"pbdoc(
                Assess weather risk for multiple locations.
                
                Args:
                    weather_list (list): List of weather data dictionaries
                
                Returns:
                    list: List of risk assessment dictionaries
             )pbdoc",
             py::arg("weather_list"))
        
        .def("get_flight_window_advice", &uav_sdk::RiskAssessor::get_flight_window_advice,
             R"pbdoc(
                Get flight window advice based on weather.
                
                Args:
                    weather (dict): Weather data
                
                Returns:
                    str: Advice string
             )pbdoc",
             py::arg("weather"))
        
        .def("set_wind_speed_threshold", &uav_sdk::RiskAssessor::set_wind_speed_threshold,
             py::arg("threshold"),
             R"pbdoc(Set wind speed risk threshold.)pbdoc")
        
        .def("set_visibility_threshold", &uav_sdk::RiskAssessor::set_visibility_threshold,
             py::arg("threshold"),
             R"pbdoc(Set visibility risk threshold.)pbdoc")
        
        .def("set_temperature_range", &uav_sdk::RiskAssessor::set_temperature_range,
             py::arg("min_temp"), py::arg("max_temp"),
             R"pbdoc(Set temperature risk range.)pbdoc");

    // ========================================
    // FlightController 绑定
    // ========================================
    py::class_<uav_sdk::FlightController>(m, "FlightController")
        .def(py::init<const std::string&, int>(),
             py::arg("device") = "COM3",
             py::arg("baudrate") = 57600,
             R"pbdoc(
                Create a FlightController instance.
                
                Args:
                    device (str): Serial device path (e.g., 'COM3' on Windows, '/dev/ttyUSB0' on Linux)
                    baudrate (int): Serial baudrate (default: 57600)
             )pbdoc")
        
        .def("connect", &uav_sdk::FlightController::connect,
             R"pbdoc(Connect to the flight controller.)pbdoc")
        
        .def("disconnect", &uav_sdk::FlightController::disconnect,
             R"pbdoc(Disconnect from the flight controller.)pbdoc")
        
        .def("is_connected", &uav_sdk::FlightController::is_connected,
             R"pbdoc(Check if connected to flight controller.)pbdoc")
        
        .def("get_state", &uav_sdk::FlightController::get_state,
             R"pbdoc(
                Get current UAV state.
                
                Returns:
                    dict: UAV state with keys:
                        - latitude, longitude, altitude, abs_altitude
                        - heading, speed, battery
                        - mode, armed, flying
             )pbdoc")
        
        .def("arm", &uav_sdk::FlightController::arm,
             R"pbdoc(Arm the motors.)pbdoc")
        
        .def("disarm", &uav_sdk::FlightController::disarm,
             R"pbdoc(Disarm the motors.)pbdoc")
        
        .def("set_mode", [](uav_sdk::FlightController& self, const std::string& mode) {
            if (mode == "MANUAL") return self.set_mode(uav_sdk::FlightMode::MANUAL);
            if (mode == "STABILIZE") return self.set_mode(uav_sdk::FlightMode::STABILIZE);
            if (mode == "ALT_HOLD") return self.set_mode(uav_sdk::FlightMode::ALT_HOLD);
            if (mode == "POSITION") return self.set_mode(uav_sdk::FlightMode::POSITION);
            if (mode == "AUTO") return self.set_mode(uav_sdk::FlightMode::AUTO);
            if (mode == "RTL") return self.set_mode(uav_sdk::FlightMode::RTL);
            if (mode == "LAND") return self.set_mode(uav_sdk::FlightMode::LAND);
            if (mode == "TAKEOFF") return self.set_mode(uav_sdk::FlightMode::TAKEOFF);
            if (mode == "GUIDED") return self.set_mode(uav_sdk::FlightMode::GUIDED);
            return false;
        }, py::arg("mode"),
             R"pbdoc(
                Set flight mode.
                
                Args:
                    mode (str): One of 'MANUAL', 'STABILIZE', 'ALT_HOLD', 'POSITION',
                               'AUTO', 'RTL', 'LAND', 'TAKEOFF', 'GUIDED'
             )pbdoc")
        
        .def("takeoff", &uav_sdk::FlightController::takeoff,
             py::arg("altitude"),
             R"pbdoc(Takeoff to specified altitude.)pbdoc")
        
        .def("land", &uav_sdk::FlightController::land,
             R"pbdoc(Land the UAV.)pbdoc")
        
        .def("return_to_launch", &uav_sdk::FlightController::return_to_launch,
             R"pbdoc(Return to launch point.)pbdoc")
        
        .def("goto_position", &uav_sdk::FlightController::goto_position,
             py::arg("lat"), py::arg("lon"), py::arg("alt"),
             R"pbdoc(Goto a specific position.)pbdoc")
        
        .def("upload_mission", &uav_sdk::FlightController::upload_mission,
             R"pbdoc(
                Upload a mission (list of waypoints).
                
                Args:
                    waypoints (list): List of waypoint dictionaries with keys:
                        - latitude, longitude, altitude, speed, action
             )pbdoc",
             py::arg("waypoints"))
        
        .def("execute_mission", &uav_sdk::FlightController::execute_mission,
             R"pbdoc(Execute the uploaded mission.)pbdoc")
        
        .def("pause_mission", &uav_sdk::FlightController::pause_mission,
             R"pbdoc(Pause the current mission.)pbdoc");

    // ========================================
    // 枚举类型绑定
    // ========================================
    py::enum_<uav_sdk::RiskLevel>(m, "RiskLevel")
        .value("LOW", uav_sdk::RiskLevel::LOW)
        .value("MEDIUM", uav_sdk::RiskLevel::MEDIUM)
        .value("HIGH", uav_sdk::RiskLevel::HIGH)
        .value("SEVERE", uav_sdk::RiskLevel::SEVERE)
        .export_values();

    py::enum_<uav_sdk::FlightMode>(m, "FlightMode")
        .value("MANUAL", uav_sdk::FlightMode::MANUAL)
        .value("STABILIZE", uav_sdk::FlightMode::STABILIZE)
        .value("ALT_HOLD", uav_sdk::FlightMode::ALT_HOLD)
        .value("POSITION", uav_sdk::FlightMode::POSITION)
        .value("AUTO", uav_sdk::FlightMode::AUTO)
        .value("RTL", uav_sdk::FlightMode::RTL)
        .value("LAND", uav_sdk::FlightMode::LAND)
        .value("TAKEOFF", uav_sdk::FlightMode::TAKEOFF)
        .value("GUIDED", uav_sdk::FlightMode::GUIDED)
        .export_values();
}
