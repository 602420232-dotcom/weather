#include "path_planner.h"
#include <limits>
#include <unordered_map>

namespace uav_sdk {

PathPlanner::PathPlanner(int grid_width, int grid_height, double resolution)
    : grid_width_(grid_width), grid_height_(grid_height), resolution_(resolution) {
}

PathPlanner::~PathPlanner() {
}

void PathPlanner::set_grid_size(int width, int height) {
    grid_width_ = width;
    grid_height_ = height;
}

void PathPlanner::set_resolution(double resolution) {
    resolution_ = resolution;
}

bool PathPlanner::is_valid(const Point& point) const {
    return point.x >= 0 && point.x < grid_width_ &&
           point.y >= 0 && point.y < grid_height_;
}

bool PathPlanner::is_obstacle(const Point& point) const {
    for (const auto& obs : obstacles_) {
        if (obs == point) {
            return true;
        }
    }
    return false;
}

void PathPlanner::clear_obstacles() {
    obstacles_.clear();
}

double PathPlanner::heuristic(const Point& a, const Point& b) const {
    // 使用曼哈顿距离
    return std::abs(a.x - b.x) + std::abs(a.y - b.y);
}

std::vector<Point> PathPlanner::get_neighbors(const Point& point) const {
    std::vector<Point> neighbors;
    
    // 4连通邻居
    std::vector<Point> candidates = {
        Point(point.x - 1, point.y),
        Point(point.x + 1, point.y),
        Point(point.x, point.y - 1),
        Point(point.x, point.y + 1)
    };
    
    for (const auto& p : candidates) {
        if (is_valid(p) && !is_obstacle(p)) {
            neighbors.push_back(p);
        }
    }
    
    return neighbors;
}

int PathPlanner::point_to_id(const Point& p) const {
    return p.y * grid_width_ + p.x;
}

std::vector<Point> PathPlanner::reconstruct_path(
    const std::unordered_map<int, Node>& came_from,
    const Point& current) const {
    
    std::vector<Point> path;
    Point node = current;
    
    while (came_from.find(point_to_id(node)) != came_from.end()) {
        path.push_back(node);
        node = came_from.at(point_to_id(node)).parent;
    }
    
    path.push_back(node);
    std::reverse(path.begin(), path.end());
    
    return path;
}

std::vector<Point> PathPlanner::plan(
    const Point& start,
    const Point& goal,
    const std::vector<Point>& obstacles) {
    
    // 清空旧障碍物并添加新障碍物
    obstacles_ = obstacles;
    
    // 检查起点和终点是否有效
    if (!is_valid(start) || !is_valid(goal)) {
        return {};
    }
    
    if (is_obstacle(start) || is_obstacle(goal)) {
        return {};
    }
    
    // 如果起点和终点相同
    if (start == goal) {
        return {start};
    }
    
    // A* 算法
    std::priority_queue<Node> open_set;
    std::unordered_map<int, Node> came_from;
    std::unordered_map<int, double> g_score;
    
    Node start_node;
    start_node.point = start;
    start_node.g_cost = 0;
    start_node.h_cost = heuristic(start, goal);
    
    open_set.push(start_node);
    g_score[point_to_id(start)] = 0;
    
    while (!open_set.empty()) {
        Node current = open_set.top();
        open_set.pop();
        
        // 检查是否到达终点
        if (current.point == goal) {
            return reconstruct_path(came_from, current.point);
        }
        
        // 遍历邻居
        for (const auto& neighbor : get_neighbors(current.point)) {
            double tentative_g = current.g_cost + 1;  // 假设每步代价为1
            
            int neighbor_id = point_to_id(neighbor);
            
            if (g_score.find(neighbor_id) == g_score.end() ||
                tentative_g < g_score[neighbor_id]) {
                
                // 更新路径
                came_from[neighbor_id] = current;
                g_score[neighbor_id] = tentative_g;
                
                Node neighbor_node;
                neighbor_node.point = neighbor;
                neighbor_node.g_cost = tentative_g;
                neighbor_node.h_cost = heuristic(neighbor, goal);
                neighbor_node.parent = current.point;
                
                open_set.push(neighbor_node);
            }
        }
    }
    
    // 没有找到路径
    return {};
}

}  // namespace uav_sdk
