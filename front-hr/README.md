# 泰来云员工管理系统 - PC管理后台

基于 Vue 3 + TypeScript + Element Plus 构建的现代化管理后台系统。

## 技术栈

- **框架**: Vue 3.3 + TypeScript
- **UI组件**: Element Plus 2.4
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.2
- **HTTP**: Axios 1.5
- **构建工具**: Vite 4.4
- **图表**: ECharts 5.4
- **样式**: SCSS

## 项目结构

```
front-pc/
├── src/
│   ├── api/              # API接口
│   │   ├── request.ts    # Axios封装
│   │   ├── auth.ts       # 认证接口
│   │   ├── contract.ts   # 合同接口
│   │   └── template.ts   # 模板接口
│   ├── stores/           # Pinia状态管理
│   │   └── user.ts       # 用户状态
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── views/            # 页面组件
│   │   ├── Login.vue             # 登录页
│   │   ├── Dashboard.vue         # 首页
│   │   ├── onboard/              # 入职模块
│   │   │   ├── ApprovalList.vue  # 审批列表
│   │   │   └── ApprovalDetail.vue # 审批详情
│   │   ├── offboard/             # 离职模块
│   │   ├── contract/             # 合同模块
│   │   └── template/             # 模板模块
│   ├── components/       # 公共组件
│   ├── styles/           # 全局样式
│   ├── utils/            # 工具函数
│   ├── App.vue
│   └── main.ts
├── public/
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 功能模块

### 1. 登录认证
- 管理员账号密码登录
- JWT Token认证
- 自动Token刷新
- 退出登录

### 2. 首页Dashboard
- 数据概览统计
- 快捷操作入口
- 图表数据展示
- 待办事项提醒

### 3. 入职审批
- 入职申请列表
- 多条件筛选
- 审批详情查看
- 领导签字审批
- 合同生效处理

### 4. 离职审批
- 离职申请列表
- 审批流程管理
- 离职清算办理
- 离职证明生成

### 5. 合同管理
- 合同列表查询
- 合同详情查看
- 合同状态管理
- PDF预览下载

### 6. 模板管理
- 模板列表
- 模板上传
- 模板启用/禁用
- 模板删除

## API接口对接

所有接口请求统一代理到后端服务：

- 开发环境: `http://localhost:8082`
- 生产环境: 配置在 `vite.config.ts` 中

### 接口格式

**请求头:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**响应格式:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1696147200000
}
```

## 测试账号

- 用户名: `admin`
- 密码: `admin123`

## 样式规范

- 主题色: `#667eea` → `#764ba2` (渐变)
- 圆角: 8px / 12px / 16px
- 阴影: `0 2px 12px rgba(0,0,0,0.08)`
- 间距: 8px / 16px / 20px / 24px

## 开发注意事项

1. 所有页面组件必须使用 TypeScript
2. API调用必须添加错误处理
3. 表单提交前必须验证
4. 敏感信息需要脱敏显示
5. 所有操作需要用户确认

## 浏览器支持

- Chrome >= 90
- Edge >= 90
- Firefox >= 88
- Safari >= 14

## 许可证

MIT License

