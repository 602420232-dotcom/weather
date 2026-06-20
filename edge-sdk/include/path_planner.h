#ifndef PATH_PLANNER_H
#define PATH_PLANNER_H

#include <vector>
#include <utility>
#include <unordered_set>
#include <queue>
#include <cmath>
#include <algorithm>

namespace uav_sdk {

/**
 * @brief 2D坐标点
 */
struct Point {
    int x;
    int y;
    
    Point() : x(0), y(0) {}
    Point(int x_, int y_) : x(x_), y(y_) {}
    
    bool operator==(const Point& other) const {
        return x == other.x && y == other.y;
    }
    
    bool operator!=(const Point& other) const {
        return !(*this == other);
    }
};

/**
 * @brief 路径规划器 - A* 算法实现
 * 
 * 用于无人机的离线路径规划，支持静态障碍物避障
 */
class PathPlanner {
public:
    /**
     * @brief 构造函数
     * @param grid_width 网格宽度（米）
     * @param grid_height 网格高度（米）
     * @param resolution 分辨率（米/格）
     */
    PathPlanner(int grid_width = 100, int grid_height = 100, double resolution = 1.0);
    
    /**
     * @brief 析构函数
     */
    ~PathPlanner();
    
    /**
     * @brief 规划路径
     * @param start 起点坐标
     * @param goal 终点坐标
     * @param obstacles 障碍物坐标列表
     * @return 规划好的路径点列表
     */
    std::vector<Point> plan(const Point& start, const Point& goal, 
                           const std::vector<Point>& obstacles);
    
    /**
     * @brief 设置网格参数
     */
    void set_grid_size(int width, int height);
    void set_resolution(double resolution);
    
    /**
     * @brief 检查点是否在网格内
     */
    bool is_valid(const Point& point) const;
    
    /**
     * @brief 检查点是否为障碍物
     */
    bool is_obstacle(const Point& point) const;
    
    /**
     * @brief 清除所有障碍物
     */
    void clear_obstacles();

private:
    int grid_width_;
    int grid_height_;
    double resolution_;
    std::vector<Point> obstacles_;
    
    struct Node {
        Point point;
        double g_cost;  // 从起点到当前点的代价
        double h_cost;  // 从当前点到终点的启发式代价
        double f_cost() const { return g_cost + h_cost; }
        Point parent;
        
        bool operator<(const Node& other) const {
            return f_cost() > other.f_cost();  // 用于最小堆
        }
    };
    
    double heuristic(const Point& a, const Point& b) const;
    std::vector<Point> get_neighbors(const Point& point) const;
    std::vector<Point> reconstruct_path(const std::unordered_map<int, Node>& came_from, 
                                        const Point& current) const;
    int point_to_id(const Point& p) const;
};

}  // namespace uav_sdk

#endif  // PATH_PLANNER_H
