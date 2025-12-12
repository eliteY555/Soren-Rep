# 图标说明

## 底部导航栏图标

本目录包含底部导航栏所需的图标文件。

### 图标列表

| 图标名称 | 未选中 | 选中 | 说明 |
|---------|--------|------|------|
| 首页 | home.png | home-active.png | 首页图标 |
| 申请 | application.png | application-active.png | 申请管理图标 |
| 合同 | contract.png | contract-active.png | 合同管理图标 |
| 通知 | notification.png | notification-active.png | 通知消息图标 |
| 我的 | profile.png | profile-active.png | 个人中心图标 |

### 图标规范

- **尺寸**: 81px × 81px（推荐）
- **格式**: PNG（支持透明背景）
- **颜色**: 
  - 未选中: #999999（灰色）
  - 选中: #667eea（主题紫色）

### 使用说明

1. 所有图标文件已放置在 `front-wx/images/` 目录下
2. 在 `app.json` 中的 `tabBar` 配置中引用
3. 如需自定义图标，请替换对应的PNG文件

### 在线图标资源

如需更换图标，可以使用以下资源：
- [iconfont](https://www.iconfont.cn/)
- [iconpark](https://iconpark.oceanengine.com/)
- [flaticon](https://www.flaticon.com/)

### 快速生成工具

可以使用在线工具生成小程序图标：
- [小程序图标生成器](https://www.uupoop.com/)
- [TinyPNG](https://tinypng.com/) - 图标压缩

---

**注意**: 微信小程序对tabBar图标有以下要求：
- 图片大小限制为 40KB
- 建议尺寸为 81px × 81px
- 仅支持本地图片，不支持网络图片
