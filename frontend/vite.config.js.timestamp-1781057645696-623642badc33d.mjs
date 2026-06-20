var __require = /* @__PURE__ */ ((x) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x, {
  get: (a, b) => (typeof require !== "undefined" ? require : a)[b]
}) : x)(function(x) {
  if (typeof require !== "undefined") return require.apply(this, arguments);
  throw Error('Dynamic require of "' + x + '" is not supported');
});

// vite.config.js
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "file:///mnt/d/Developer/workplace/py/iteam/trae/uav-path-planning-system/frontend-vue/node_modules/vite/dist/node/index.js";
import vue from "file:///mnt/d/Developer/workplace/py/iteam/trae/uav-path-planning-system/frontend-vue/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import vueJsx from "file:///mnt/d/Developer/workplace/py/iteam/trae/uav-path-planning-system/frontend-vue/node_modules/@vitejs/plugin-vue-jsx/dist/index.mjs";
var __vite_injected_original_import_meta_url = "file:///mnt/d/Developer/workplace/py/iteam/trae/uav-path-planning-system/frontend-vue/vite.config.js";
var pwaOptions = {
  registerType: "autoUpdate",
  includeAssets: ["favicon.ico", "robots.txt", "apple-touch-icon.png"],
  manifest: {
    name: "\u65E0\u4EBA\u673A\u8DEF\u5F84\u89C4\u5212\u7CFB\u7EDF",
    short_name: "UAV Path Planning",
    description: "\u57FA\u4E8E WRF \u6C14\u8C61\u9A71\u52A8\u7684\u65E0\u4EBA\u673A VRP \u667A\u80FD\u8DEF\u5F84\u89C4\u5212\u7CFB\u7EDF",
    theme_color: "#409EFF",
    background_color: "#ffffff",
    display: "standalone",
    start_url: "/",
    lang: "zh-CN",
    icons: [
      { src: "/vite.svg", sizes: "192x192", type: "image/svg+xml", purpose: "any" },
      { src: "/vite.svg", sizes: "512x512", type: "image/svg+xml", purpose: "any maskable" }
    ]
  },
  workbox: {
    globPatterns: ["**/*.{js,css,html,ico,png,svg,woff2}"],
    runtimeCaching: [
      {
        urlPattern: ({ request }) => ["style", "script", "worker", "image"].includes(request.destination),
        handler: "CacheFirst",
        options: {
          cacheName: "uav-static-assets",
          expiration: { maxEntries: 60, maxAgeSeconds: 60 * 60 * 24 * 30 }
        }
      },
      {
        urlPattern: ({ url, request }) => request.method === "GET" && /\/v1\/weather\b/.test(url.pathname),
        handler: "StaleWhileRevalidate",
        options: {
          cacheName: "uav-weather-api",
          expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 }
        }
      },
      {
        urlPattern: ({ url, request }) => request.method === "GET" && /\/v1\/(tasks|planning|assimilation)\b/.test(url.pathname),
        handler: "NetworkFirst",
        options: {
          cacheName: "uav-task-api",
          expiration: { maxEntries: 30, maxAgeSeconds: 5 * 60 }
        }
      },
      {
        urlPattern: ({ url }) => url.origin !== self.location.origin,
        handler: "CacheFirst",
        options: {
          cacheName: "uav-cross-origin",
          expiration: { maxEntries: 30, maxAgeSeconds: 60 * 60 * 24 * 7 }
        }
      }
    ]
  },
  devOptions: { enabled: false }
};
function tryLoadPwa() {
  try {
    const { VitePWA } = __require("file:///mnt/d/Developer/workplace/py/iteam/trae/uav-path-planning-system/frontend-vue/node_modules/vite-plugin-pwa/dist/index.js");
    console.info("[vite] PWA \u63D2\u4EF6\u5DF2\u542F\u7528\uFF08vite-plugin-pwa\uFF09");
    return VitePWA(pwaOptions);
  } catch (_) {
    console.warn("[vite] vite-plugin-pwa \u672A\u5B89\u88C5\uFF0C\u8DF3\u8FC7 PWA \u63D2\u4EF6\uFF1B\u6267\u884C npm install vite-plugin-pwa --save-dev \u540E\u53EF\u542F\u7528");
    return null;
  }
}
var vite_config_default = defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    tryLoadPwa(),
    // CSP meta 标签注入（生产/开发均启用，确保安全策略一致）
    {
      name: "html-csp",
      transformIndexHtml(html) {
        return html.replace(
          "<head>",
          `<head>
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' ws: wss: https:; frame-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self';">
    <meta http-equiv="X-Content-Type-Options" content="nosniff">
    <meta http-equiv="Referrer-Policy" content="strict-origin-when-cross-origin">`
        );
      }
    }
  ].filter(Boolean),
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  server: {
    port: 3e3,
    proxy: {
      "/api": {
        target: "http://localhost:8088",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, "")
      },
      "/actuator": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/nacos": {
        target: "http://localhost:8848",
        changeOrigin: true
      }
    }
  },
  build: {
    minify: "esbuild",
    chunkSizeWarningLimit: 1e3,
    target: ["es2020", "edge88", "firefox78", "chrome87", "safari14"],
    rollupOptions: {
      // STOMP/SockJS 为可选依赖：仅在生产 WebSocket 模式下需要
      // 动态 import() 已有 try-catch 容错，标记 external 避免构建时解析失败
      external: ["@stomp/stompjs", "sockjs-client"],
      output: {
        manualChunks: {
          vendor: ["vue", "vue-router", "pinia", "axios"],
          ui: ["element-plus"],
          chart: ["echarts"],
          map: ["leaflet"]
        }
      }
    },
    cacheDir: "./node_modules/.vite-cache"
  },
  optimizeDeps: {
    include: ["vue", "vue-router", "pinia", "axios", "leaflet", "echarts", "element-plus"],
    exclude: []
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvbW50L2QvRGV2ZWxvcGVyL3dvcmtwbGFjZS9weS9pdGVhbS90cmFlL3Vhdi1wYXRoLXBsYW5uaW5nLXN5c3RlbS9mcm9udGVuZC12dWVcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9tbnQvZC9EZXZlbG9wZXIvd29ya3BsYWNlL3B5L2l0ZWFtL3RyYWUvdWF2LXBhdGgtcGxhbm5pbmctc3lzdGVtL2Zyb250ZW5kLXZ1ZS92aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vbW50L2QvRGV2ZWxvcGVyL3dvcmtwbGFjZS9weS9pdGVhbS90cmFlL3Vhdi1wYXRoLXBsYW5uaW5nLXN5c3RlbS9mcm9udGVuZC12dWUvdml0ZS5jb25maWcuanNcIjtpbXBvcnQgeyBmaWxlVVJMVG9QYXRoLCBVUkwgfSBmcm9tICdub2RlOnVybCdcbmltcG9ydCB7IGRlZmluZUNvbmZpZyB9IGZyb20gJ3ZpdGUnXG5pbXBvcnQgdnVlIGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZSdcbmltcG9ydCB2dWVKc3ggZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlLWpzeCdcblxuLy8gPT09PT0gVml0ZVBXQSBcdTkxNERcdTdGNkVcdUZGMDhcdTY3MkFcdTVCODlcdTg4QzVcdTY1RjZcdThERjNcdThGQzdcdUZGMENcdTRFMERcdTk2M0JcdTU4NUVcdTY3ODRcdTVFRkFcdUZGMDlcbmNvbnN0IHB3YU9wdGlvbnMgPSB7XG4gIHJlZ2lzdGVyVHlwZTogJ2F1dG9VcGRhdGUnLFxuICBpbmNsdWRlQXNzZXRzOiBbJ2Zhdmljb24uaWNvJywgJ3JvYm90cy50eHQnLCAnYXBwbGUtdG91Y2gtaWNvbi5wbmcnXSxcbiAgbWFuaWZlc3Q6IHtcbiAgICBuYW1lOiAnXHU2NUUwXHU0RUJBXHU2NzNBXHU4REVGXHU1Rjg0XHU4OUM0XHU1MjEyXHU3Q0ZCXHU3RURGJyxcbiAgICBzaG9ydF9uYW1lOiAnVUFWIFBhdGggUGxhbm5pbmcnLFxuICAgIGRlc2NyaXB0aW9uOiAnXHU1N0ZBXHU0RThFIFdSRiBcdTZDMTRcdThDNjFcdTlBNzFcdTUyQThcdTc2ODRcdTY1RTBcdTRFQkFcdTY3M0EgVlJQIFx1NjY3QVx1ODBGRFx1OERFRlx1NUY4NFx1ODlDNFx1NTIxMlx1N0NGQlx1N0VERicsXG4gICAgdGhlbWVfY29sb3I6ICcjNDA5RUZGJyxcbiAgICBiYWNrZ3JvdW5kX2NvbG9yOiAnI2ZmZmZmZicsXG4gICAgZGlzcGxheTogJ3N0YW5kYWxvbmUnLFxuICAgIHN0YXJ0X3VybDogJy8nLFxuICAgIGxhbmc6ICd6aC1DTicsXG4gICAgaWNvbnM6IFtcbiAgICAgIHsgc3JjOiAnL3ZpdGUuc3ZnJywgc2l6ZXM6ICcxOTJ4MTkyJywgdHlwZTogJ2ltYWdlL3N2Zyt4bWwnLCBwdXJwb3NlOiAnYW55JyB9LFxuICAgICAgeyBzcmM6ICcvdml0ZS5zdmcnLCBzaXplczogJzUxMng1MTInLCB0eXBlOiAnaW1hZ2Uvc3ZnK3htbCcsIHB1cnBvc2U6ICdhbnkgbWFza2FibGUnIH1cbiAgICBdXG4gIH0sXG4gIHdvcmtib3g6IHtcbiAgICBnbG9iUGF0dGVybnM6IFsnKiovKi57anMsY3NzLGh0bWwsaWNvLHBuZyxzdmcsd29mZjJ9J10sXG4gICAgcnVudGltZUNhY2hpbmc6IFtcbiAgICAgIHtcbiAgICAgICAgdXJsUGF0dGVybjogKHsgcmVxdWVzdCB9KSA9PiBbJ3N0eWxlJywgJ3NjcmlwdCcsICd3b3JrZXInLCAnaW1hZ2UnXS5pbmNsdWRlcyhyZXF1ZXN0LmRlc3RpbmF0aW9uKSxcbiAgICAgICAgaGFuZGxlcjogJ0NhY2hlRmlyc3QnLFxuICAgICAgICBvcHRpb25zOiB7XG4gICAgICAgICAgY2FjaGVOYW1lOiAndWF2LXN0YXRpYy1hc3NldHMnLFxuICAgICAgICAgIGV4cGlyYXRpb246IHsgbWF4RW50cmllczogNjAsIG1heEFnZVNlY29uZHM6IDYwICogNjAgKiAyNCAqIDMwIH1cbiAgICAgICAgfVxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgdXJsUGF0dGVybjogKHsgdXJsLCByZXF1ZXN0IH0pID0+IHJlcXVlc3QubWV0aG9kID09PSAnR0VUJyAmJiAvXFwvdjFcXC93ZWF0aGVyXFxiLy50ZXN0KHVybC5wYXRobmFtZSksXG4gICAgICAgIGhhbmRsZXI6ICdTdGFsZVdoaWxlUmV2YWxpZGF0ZScsXG4gICAgICAgIG9wdGlvbnM6IHtcbiAgICAgICAgICBjYWNoZU5hbWU6ICd1YXYtd2VhdGhlci1hcGknLFxuICAgICAgICAgIGV4cGlyYXRpb246IHsgbWF4RW50cmllczogNTAsIG1heEFnZVNlY29uZHM6IDYwICogNjAgfVxuICAgICAgICB9XG4gICAgICB9LFxuICAgICAge1xuICAgICAgICB1cmxQYXR0ZXJuOiAoeyB1cmwsIHJlcXVlc3QgfSkgPT4gcmVxdWVzdC5tZXRob2QgPT09ICdHRVQnICYmIC9cXC92MVxcLyh0YXNrc3xwbGFubmluZ3xhc3NpbWlsYXRpb24pXFxiLy50ZXN0KHVybC5wYXRobmFtZSksXG4gICAgICAgIGhhbmRsZXI6ICdOZXR3b3JrRmlyc3QnLFxuICAgICAgICBvcHRpb25zOiB7XG4gICAgICAgICAgY2FjaGVOYW1lOiAndWF2LXRhc2stYXBpJyxcbiAgICAgICAgICBleHBpcmF0aW9uOiB7IG1heEVudHJpZXM6IDMwLCBtYXhBZ2VTZWNvbmRzOiA1ICogNjAgfVxuICAgICAgICB9XG4gICAgICB9LFxuICAgICAge1xuICAgICAgICB1cmxQYXR0ZXJuOiAoeyB1cmwgfSkgPT4gdXJsLm9yaWdpbiAhPT0gc2VsZi5sb2NhdGlvbi5vcmlnaW4sXG4gICAgICAgIGhhbmRsZXI6ICdDYWNoZUZpcnN0JyxcbiAgICAgICAgb3B0aW9uczoge1xuICAgICAgICAgIGNhY2hlTmFtZTogJ3Vhdi1jcm9zcy1vcmlnaW4nLFxuICAgICAgICAgIGV4cGlyYXRpb246IHsgbWF4RW50cmllczogMzAsIG1heEFnZVNlY29uZHM6IDYwICogNjAgKiAyNCAqIDcgfVxuICAgICAgICB9XG4gICAgICB9XG4gICAgXVxuICB9LFxuICBkZXZPcHRpb25zOiB7IGVuYWJsZWQ6IGZhbHNlIH1cbn1cblxuLy8gXHU2NzYxXHU0RUY2XHU1RjBGXHU1MkEwXHU4RjdEIHZpdGUtcGx1Z2luLXB3YVx1RkYxQVx1NjcyQVx1NUI4OVx1ODhDNVx1NjVGNlx1OTc1OVx1OUVEOFx1OERGM1x1OEZDN1x1RkYwQ1x1NEUwRFx1OTYzQlx1NTg1RVx1Njc4NFx1NUVGQVxuZnVuY3Rpb24gdHJ5TG9hZFB3YSgpIHtcbiAgdHJ5IHtcbiAgICBjb25zdCB7IFZpdGVQV0EgfSA9IHJlcXVpcmUoJ3ZpdGUtcGx1Z2luLXB3YScpXG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIG5vLWNvbnNvbGVcbiAgICBjb25zb2xlLmluZm8oJ1t2aXRlXSBQV0EgXHU2M0QyXHU0RUY2XHU1REYyXHU1NDJGXHU3NTI4XHVGRjA4dml0ZS1wbHVnaW4tcHdhXHVGRjA5JylcbiAgICByZXR1cm4gVml0ZVBXQShwd2FPcHRpb25zKVxuICB9IGNhdGNoIChfKSB7XG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIG5vLWNvbnNvbGVcbiAgICBjb25zb2xlLndhcm4oJ1t2aXRlXSB2aXRlLXBsdWdpbi1wd2EgXHU2NzJBXHU1Qjg5XHU4OEM1XHVGRjBDXHU4REYzXHU4RkM3IFBXQSBcdTYzRDJcdTRFRjZcdUZGMUJcdTYyNjdcdTg4NEMgbnBtIGluc3RhbGwgdml0ZS1wbHVnaW4tcHdhIC0tc2F2ZS1kZXYgXHU1NDBFXHU1M0VGXHU1NDJGXHU3NTI4JylcbiAgICByZXR1cm4gbnVsbFxuICB9XG59XG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHBsdWdpbnM6IFtcbiAgICB2dWUoKSxcbiAgICB2dWVKc3goKSxcbiAgICB0cnlMb2FkUHdhKCksXG4gICAgLy8gQ1NQIG1ldGEgXHU2ODA3XHU3QjdFXHU2Q0U4XHU1MTY1XHVGRjA4XHU3NTFGXHU0RUE3L1x1NUYwMFx1NTNEMVx1NTc0N1x1NTQyRlx1NzUyOFx1RkYwQ1x1Nzg2RVx1NEZERFx1NUI4OVx1NTE2OFx1N0I1Nlx1NzU2NVx1NEUwMFx1ODFGNFx1RkYwOVxuICAgIHtcbiAgICAgIG5hbWU6ICdodG1sLWNzcCcsXG4gICAgICB0cmFuc2Zvcm1JbmRleEh0bWwoaHRtbCkge1xuICAgICAgICByZXR1cm4gaHRtbC5yZXBsYWNlKFxuICAgICAgICAgICc8aGVhZD4nLFxuICAgICAgICAgIGA8aGVhZD5cbiAgICA8bWV0YSBodHRwLWVxdWl2PVwiQ29udGVudC1TZWN1cml0eS1Qb2xpY3lcIiBjb250ZW50PVwiZGVmYXVsdC1zcmMgJ3NlbGYnOyBzY3JpcHQtc3JjICdzZWxmJyAndW5zYWZlLWlubGluZScgJ3Vuc2FmZS1ldmFsJzsgc3R5bGUtc3JjICdzZWxmJyAndW5zYWZlLWlubGluZSc7IGltZy1zcmMgJ3NlbGYnIGRhdGE6IGh0dHBzOjsgZm9udC1zcmMgJ3NlbGYnIGRhdGE6OyBjb25uZWN0LXNyYyAnc2VsZicgd3M6IHdzczogaHR0cHM6OyBmcmFtZS1zcmMgJ25vbmUnOyBvYmplY3Qtc3JjICdub25lJzsgYmFzZS11cmkgJ3NlbGYnOyBmb3JtLWFjdGlvbiAnc2VsZic7XCI+XG4gICAgPG1ldGEgaHR0cC1lcXVpdj1cIlgtQ29udGVudC1UeXBlLU9wdGlvbnNcIiBjb250ZW50PVwibm9zbmlmZlwiPlxuICAgIDxtZXRhIGh0dHAtZXF1aXY9XCJSZWZlcnJlci1Qb2xpY3lcIiBjb250ZW50PVwic3RyaWN0LW9yaWdpbi13aGVuLWNyb3NzLW9yaWdpblwiPmBcbiAgICAgICAgKVxuICAgICAgfVxuICAgIH1cbiAgXS5maWx0ZXIoQm9vbGVhbiksXG4gIHJlc29sdmU6IHtcbiAgICBhbGlhczoge1xuICAgICAgJ0AnOiBmaWxlVVJMVG9QYXRoKG5ldyBVUkwoJy4vc3JjJywgaW1wb3J0Lm1ldGEudXJsKSlcbiAgICB9XG4gIH0sXG4gIHNlcnZlcjoge1xuICAgIHBvcnQ6IDMwMDAsXG4gICAgcHJveHk6IHtcbiAgICAgICcvYXBpJzoge1xuICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODgnLFxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXG4gICAgICAgIHJld3JpdGU6IChwYXRoKSA9PiBwYXRoLnJlcGxhY2UoL15cXC9hcGkvLCAnJylcbiAgICAgIH0sXG4gICAgICAnL2FjdHVhdG9yJzoge1xuICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODAnLFxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWVcbiAgICAgIH0sXG4gICAgICAnL25hY29zJzoge1xuICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0Ojg4NDgnLFxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWVcbiAgICAgIH1cbiAgICB9XG4gIH0sXG4gIGJ1aWxkOiB7XG4gICAgbWluaWZ5OiAnZXNidWlsZCcsXG4gICAgY2h1bmtTaXplV2FybmluZ0xpbWl0OiAxMDAwLFxuICAgIHRhcmdldDogWydlczIwMjAnLCAnZWRnZTg4JywgJ2ZpcmVmb3g3OCcsICdjaHJvbWU4NycsICdzYWZhcmkxNCddLFxuICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgIC8vIFNUT01QL1NvY2tKUyBcdTRFM0FcdTUzRUZcdTkwMDlcdTRGOURcdThENTZcdUZGMUFcdTRFQzVcdTU3MjhcdTc1MUZcdTRFQTcgV2ViU29ja2V0IFx1NkEyMVx1NUYwRlx1NEUwQlx1OTcwMFx1ODk4MVxuICAgICAgLy8gXHU1MkE4XHU2MDAxIGltcG9ydCgpIFx1NURGMlx1NjcwOSB0cnktY2F0Y2ggXHU1QkI5XHU5NTE5XHVGRjBDXHU2ODA3XHU4QkIwIGV4dGVybmFsIFx1OTA3Rlx1NTE0RFx1Njc4NFx1NUVGQVx1NjVGNlx1ODlFM1x1Njc5MFx1NTkzMVx1OEQyNVxuICAgICAgZXh0ZXJuYWw6IFsnQHN0b21wL3N0b21wanMnLCAnc29ja2pzLWNsaWVudCddLFxuICAgICAgb3V0cHV0OiB7XG4gICAgICAgIG1hbnVhbENodW5rczoge1xuICAgICAgICAgIHZlbmRvcjogWyd2dWUnLCAndnVlLXJvdXRlcicsICdwaW5pYScsICdheGlvcyddLFxuICAgICAgICAgIHVpOiBbJ2VsZW1lbnQtcGx1cyddLFxuICAgICAgICAgIGNoYXJ0OiBbJ2VjaGFydHMnXSxcbiAgICAgICAgICBtYXA6IFsnbGVhZmxldCddXG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9LFxuICAgIGNhY2hlRGlyOiAnLi9ub2RlX21vZHVsZXMvLnZpdGUtY2FjaGUnXG4gIH0sXG4gIG9wdGltaXplRGVwczoge1xuICAgIGluY2x1ZGU6IFsndnVlJywgJ3Z1ZS1yb3V0ZXInLCAncGluaWEnLCAnYXhpb3MnLCAnbGVhZmxldCcsICdlY2hhcnRzJywgJ2VsZW1lbnQtcGx1cyddLFxuICAgIGV4Y2x1ZGU6IFtdXG4gIH1cbn0pXG5cbi8vID09PT09IFN0b3J5Ym9vayBcdThCRjRcdTY2MEVcdUZGMDhcdTRFMERcdTVGNzFcdTU0Q0RcdTc1MUZcdTRFQTdcdTY3ODRcdTVFRkFcdUZGMDk9PT09PVxuLy8gXHU2NzJDXHU5ODc5XHU3NkVFXHU3Njg0XHU3RUM0XHU0RUY2XHU2NTg3XHU2ODYzXHU1N0ZBXHU0RThFIEBzdG9yeWJvb2svdnVlMy12aXRlIFx1Njc4NFx1NUVGQVx1RkYwQ1xuLy8gXHU5MTREXHU3RjZFXHU2NTg3XHU0RUY2XHU0RjREXHU0RThFIC5zdG9yeWJvb2svXHVGRjA4bWFpbi5qcyAvIHByZXZpZXcuanMgLyB2aXRlLmNvbmZpZy5qc1x1RkYwOVx1RkYwQ1xuLy8gXHU3RUM0XHU0RUY2IHN0b3JpZXMgXHU0RjREXHU0RThFIHNyYy9zdG9yaWVzLyBcdTc2RUVcdTVGNTVcdTRFMEJcdTMwMDJcbi8vIFx1NTk4Mlx1OTcwMFx1NjcyQ1x1NTczMFx1NTQyRlx1NTJBOCBTdG9yeWJvb2sgXHU2RDRGXHU4OUM4XHU3RUM0XHU0RUY2XHU2NTg3XHU2ODYzXHVGRjBDXHU4QkY3XHU2MjY3XHU4ODRDXHVGRjFBXG4vLyAgIG5wbSBpbnN0YWxsIEBzdG9yeWJvb2svdnVlMy12aXRlIEBzdG9yeWJvb2svYWRkb24tZXNzZW50aWFscyBAc3Rvcnlib29rL2FkZG9uLWludGVyYWN0aW9ucyAtLXNhdmUtZGV2XG4vLyAgIG5weCBzdG9yeWJvb2sgaW5pdFxuLy8gXHU0RTRCXHU1NDBFXHU3NkY0XHU2M0E1XHU4OTg2XHU3NkQ2IC5zdG9yeWJvb2svIFx1NEUwQlx1NzY4NFx1OTE0RFx1N0Y2RVx1NjU4N1x1NEVGNlx1NTM3M1x1NTNFRlx1RkYxQlxuLy8gXHU4RkQwXHU4ODRDXHVGRjFBbnB4IHN0b3J5Ym9vayBkZXYgLXAgNjAwNlxuLy8gXHU2Q0U4XHVGRjFBU3Rvcnlib29rIFx1NzZGOFx1NTE3M1x1NEY5RFx1OEQ1Nlx1NEUwRFx1N0VCM1x1NTE2NSBwYWNrYWdlLmpzb24gXHU3NTFGXHU0RUE3XHU0RjlEXHU4RDU2XHVGRjBDXG4vLyAgICAgbnBtIHJ1biBidWlsZCBcdTRFNUZcdTRFMERcdTRGMUFcdTY3ODRcdTVFRkEgU3Rvcnlib29rIFx1NjcyQ1x1OEVBQlx1MzAwMlxuIl0sCiAgIm1hcHBpbmdzIjogIjs7Ozs7Ozs7QUFBNFosU0FBUyxlQUFlLFdBQVc7QUFDL2IsU0FBUyxvQkFBb0I7QUFDN0IsT0FBTyxTQUFTO0FBQ2hCLE9BQU8sWUFBWTtBQUhpUCxJQUFNLDJDQUEyQztBQU1yVCxJQUFNLGFBQWE7QUFBQSxFQUNqQixjQUFjO0FBQUEsRUFDZCxlQUFlLENBQUMsZUFBZSxjQUFjLHNCQUFzQjtBQUFBLEVBQ25FLFVBQVU7QUFBQSxJQUNSLE1BQU07QUFBQSxJQUNOLFlBQVk7QUFBQSxJQUNaLGFBQWE7QUFBQSxJQUNiLGFBQWE7QUFBQSxJQUNiLGtCQUFrQjtBQUFBLElBQ2xCLFNBQVM7QUFBQSxJQUNULFdBQVc7QUFBQSxJQUNYLE1BQU07QUFBQSxJQUNOLE9BQU87QUFBQSxNQUNMLEVBQUUsS0FBSyxhQUFh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
