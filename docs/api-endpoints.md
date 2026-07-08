# 笔灵 AI - API 端点清单

> 前端发起的所有 API 请求路径，后端 Spring Boot 端口 **6790**。
> Vite 开发服务器（端口 15173）已配置 `/v3` 和 `/api` 代理转发到 6790。

---

## 1. 对话模块

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/v3/api/kbs` | 无 | 获取笔记库列表 |
| `GET` | `/api/config/llm-profiles` | 无 | 获取模型列表 |
| `GET` | `/v3/api/chat/messages` | `kbId`, `offset`, `limit` | 获取对话历史 |
| `POST` | `/v3/api/chat/send` | FormData: `message`, `kbId`, `modelName`, `mode` | 发送消息（SSE 流式返回） |
| `DELETE` | `/v3/api/chat/clear` | `kbId` | 清空对话 |

## 2. 数据中心

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/datacenter/modules` | 模块列表 |
| `POST` | `/api/datacenter/modules` | 新建模块 |
| `GET` | `/api/datacenter/modules/{id}` | 获取模块 |
| `PUT` | `/api/datacenter/modules/{id}` | 编辑模块 |
| `DELETE` | `/api/datacenter/modules/{id}` | 删除模块 |
| `GET` | `/api/datacenter/modules/{id}/datasets` | 获取模块下的数据集列表 |
| `GET` | `/api/datacenter/datasets` | 全量数据集列表（用于合并统计） |
| `POST` | `/api/datacenter/datasets` | 新建数据集 |
| `PUT` | `/api/datacenter/datasets/{id}` | 编辑数据集 |
| `DELETE` | `/api/datacenter/datasets/{id}` | 删除数据集 |
| `GET` | `/api/datacenter/datasets/{id}/records` | 记录列表（支持 `page`/`size`/`keyword`） |
| `POST` | `/api/datacenter/datasets/{id}/records` | 新增记录 |
| `PUT` | `/api/datacenter/datasets/{id}/records/{rid}` | 更新记录 |
| `DELETE` | `/api/datacenter/datasets/{id}/records/{rid}` | 删除记录 |
| `POST` | `/api/datacenter/datasets/{id}/import/json` | JSON 导入 |
| `POST` | `/api/datacenter/datasets/{id}/import/url` | URL 导入 |
| `GET` | `/api/datacenter/modules/{moduleId}/analysis` | 获取/生成 AI 分析 |
| `POST` | `/api/datacenter/modules/{moduleId}/analysis` | 提交数据生成 AI 分析 |
| `DELETE` | `/api/datacenter/modules/{moduleId}/analysis` | 清除 AI 分析缓存 |

## 3. 洞察

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/ai/kb-stats` | 笔记库统计（`kbId`） |
| `GET` | `/api/ai/search` | 智能搜索（`kbId`, `query`, `limit`） |
| `GET` | `/api/ai/projects` | 子项目列表（`kbId`） |
| `GET` | `/api/ai/tags` | 标签云（`kbId`） |
| `GET` | `/api/ai/heatmap` | 热力图（`kbId`） |
| `POST` | `/api/ai/analyze-project` | 分析项目（`kbId`, `projectName`） |
| `POST` | `/api/ai/quick-action` | 快速操作（`kbId`, `action`） |

## 4. 笔记

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/v3/api/notes/tree` | 文件树（`kbId`） |
| `GET` | `/v3/api/notes/read` | 读取笔记（`kbId`, `path`） |

## 5. 任务提醒

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/tasks` | 任务列表（`kbId`） |
| `POST` | `/api/tasks/add` | 新增任务 |
| `POST` | `/api/tasks/update` | 更新任务 |
| `POST` | `/api/tasks/delete` | 删除任务（`id`） |
| `GET` | `/api/reminders` | 提醒列表（`kbId`） |
| `POST` | `/api/reminders/add` | 新增提醒 |
| `POST` | `/api/reminders/update` | 更新提醒 |
| `POST` | `/api/reminders/toggle` | 开关提醒（`id`） |
| `POST` | `/api/reminders/delete` | 删除提醒（`id`） |

## 6. 工具箱

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/tools` | 工具列表（可选，不存在则使用前端硬编码默认工具） |

---

## Vite 代理配置

```ts
// vite.config.ts
server: {
  port: 15173,
  proxy: {
    '/v3': { target: 'http://localhost:6790', changeOrigin: true },
    '/api': { target: 'http://localhost:6790', changeOrigin: true }
  }
}
```