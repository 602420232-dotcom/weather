import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/path-planning',
    name: 'pathPlanning',
    component: () => import('../views/PathPlanningView.vue')
  },
  {
    path: '/weather',
    name: 'weather',
    component: () => import('../views/WeatherView.vue')
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: () => import('../views/TasksView.vue')
  },
  {
    path: '/drones',
    name: 'drones',
    component: () => import('../views/DronesView.vue')
  },
  {
    path: '/history',
    name: 'history',
    component: () => import('../views/HistoryView.vue')
  },
  {
    path: '/monitoring',
    name: 'monitoring',
    component: () => import('../views/MonitoringView.vue')
  },
  {
    path: '/data-sources',
    name: 'dataSources',
    component: () => import('../views/DataSourceView.vue')
  },
  {
    path: '/example',
    name: 'example',
    component: () => import('../views/ExampleView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router