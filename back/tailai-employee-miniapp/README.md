# tailai-employee-miniapp - 员工小程序服务

## 模块说明
员工小程序后端服务模块，为微信小程序提供API接口。

## 端口
8081

## 功能模块
- **认证模块**: 微信小程序登录、Token管理
- **用户模块**: 用户信息查询、更新
- **入职模块**: 入职申请、合同签署、申请列表
- **离职模块**: 离职申请、申请列表、离职证明
- **合同模块**: 合同列表、合同详情、合同下载

## 技术栈
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Knife4j 4.1.0

## 启动方式
```bash
mvn spring-boot:run
```

## 接口文档
http://localhost:8081/doc.html

## 目录结构
```
src/main/java/com/tailai/miniapp/
├── EmployeeMiniappApplication.java  # 启动类
├── controller/                       # 控制器层
│   ├── AuthController.java          # 认证控制器
│   ├── UserController.java          # 用户控制器
│   ├── OnboardController.java       # 入职控制器
│   ├── OffboardController.java      # 离职控制器
│   └── ContractController.java      # 合同控制器
├── service/                          # 服务层
│   ├── AuthService.java             # 认证服务
│   ├── UserService.java             # 用户服务
│   ├── OnboardService.java          # 入职服务
│   ├── OffboardService.java         # 离职服务
│   └── ContractService.java         # 合同服务
├── mapper/                           # 数据访问层
│   ├── UserMapper.java              # 用户Mapper
│   ├── ApplicationMapper.java       # 申请Mapper
│   ├── ContractMapper.java          # 合同Mapper
│   └── SignatureMapper.java         # 签名Mapper
├── client/                           # 服务调用客户端
│   └── FileServiceClient.java       # 文件服务客户端
├── config/                           # 配置类
│   ├── Knife4jConfig.java           # 接口文档配置
│   └── WebConfig.java               # Web配置
└── dto/                              # 数据传输对象
    ├── WxLoginRequest.java          # 微信登录请求
    └── WxLoginResponse.java         # 微信登录响应
```

## 注意事项
- 当前模块仅包含基础结构，具体功能待实现
- 需要配置微信小程序的 app-id 和 app-secret
- 调用文件服务时需要确保 tailai-file-service 已启动




