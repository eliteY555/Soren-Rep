# 泰来云 - 保安公司员工管理系统（后端）

## 架构说明

本项目采用**微服务架构**，职责清晰分离：

- **tailai-file-service**：文件服务（底层服务），负责所有文件操作（OSS上传、PDF生成、签名处理）
- **tailai-admin-web**：管理员后台（业务服务），负责业务逻辑编排，通过HTTP调用文件服务
- **tailai-common**：公共模块，提供统一的返回结果、异常处理、工具类等

### 服务调用关系

```
前端 → 管理员后台(8082) → 文件服务(8083) → 阿里云OSS
         ↓ RestTemplate调用      ↓ OSS SDK
     业务逻辑编排              文件存储实现
```

**优势：**

- ✅ 职责清晰：文件服务专注文件处理，管理员后台专注业务逻辑
- ✅ 复用性高：其他服务（员工小程序、工作流服务）都可调用文件服务
- ✅ 易于扩展：后续可升级为OpenFeign或使用服务注册中心
- ✅ 独立部署：各服务可独立升级和扩容

## 项目结构

```
back/
├── tailai-common/              # 公共模块（实体类、工具类、统一返回结果）
├── tailai-file-service/        # 文件服务（OSS上传下载、PDF生成）- 底层服务
├── tailai-admin-web/           # 管理员后台服务（调用文件服务）- 业务服务
└── pom.xml                     # 父项目Maven配置
```

## 技术栈

- **JDK**: 11+
- **Spring Boot**: 2.7.18
- **阿里云OSS**: 3.17.4
- **Knife4j**: 4.1.0（接口文档）
- **Hutool**: 5.8.25（工具类）
- **Lombok**: 1.18.30
- **FastJson2**: 2.0.45

## 快速开始

### 1. 环境要求

- JDK 11 或更高版本
- Maven 3.6+
- IntelliJ IDEA 或其他IDE

### 2. 编译项目

在`back`目录下执行：

```bash
mvn clean install
```

### 3. 启动服务

#### 方式一：IDEA启动（推荐开发环境）

1. 导入项目到IDEA

2. 等待Maven依赖下载完成

3. 启动`tailai-file-service`服务：
   
   - 运行 `com.tailai.file.FileServiceApplication`
   - 访问：http://localhost:8083/doc.html

4. 启动`tailai-admin-web`服务：
   
   - 运行 `com.tailai.admin.AdminWebApplication`
   - 访问：http://localhost:8082/doc.html

#### 方式二：命令行启动

```bash
# 启动文件服务
cd tailai-file-service
mvn spring-boot:run

# 启动管理员后台服务（新窗口）
cd tailai-admin-web
mvn spring-boot:run
```

#### 方式三：jar包启动（生产环境）

```bash
# 编译打包
mvn clean package -DskipTests

# 启动文件服务
java -jar tailai-file-service/target/tailai-file-service-1.0.0.jar

# 启动管理员后台服务
java -jar tailai-admin-web/target/tailai-admin-web-1.0.0.jar
```

## 接口文档

启动服务后，访问Knife4j接口文档：

- **文件服务**: http://localhost:8083/doc.html
- **管理员后台**: http://localhost:8082/doc.html

## 主要功能

### 文件服务（8083端口）- 底层服务

**职责：** 提供文件操作的基础能力，不涉及业务逻辑

**核心接口：**

- ✅ 通用文件上传：`POST /api/file/upload`
- ✅ 合同模板上传：`POST /api/file/template/upload`（校验Word/HTML格式）
- ✅ 证件图片上传：`POST /api/file/image/upload`（校验图片格式）
- ✅ 文件删除：`DELETE /api/file/delete`
- ✅ 获取临时访问URL：`GET /api/file/download-url`

**特点：**

- 所有文件校验逻辑在此完成
- 直接操作阿里云OSS
- 可被所有业务服务调用

### 管理员后台（8082端口）- 业务服务

**职责：** 业务逻辑编排，通过FileServiceClient调用文件服务

**核心接口：**

- ✅ 上传合同模板：`POST /api/admin/template/upload`
  - 调用文件服务上传
  - 后续保存模板记录到数据库
- ✅ 删除模板文件：`DELETE /api/admin/template/delete`
  - 调用文件服务删除
  - 同步删除数据库记录
- ✅ 获取模板下载链接：`GET /api/admin/template/download-url`
  - 调用文件服务生成URL

**特点：**

- 不直接操作OSS，通过文件服务调用
- 专注于业务逻辑（如数据库操作、权限校验）
- 使用FileServiceClient进行服务间通信

## 测试文件上传功能

### 使用Knife4j测试

1. 打开文件服务文档：http://localhost:8083/doc.html
2. 找到"文件管理"分组
3. 测试"通用文件上传"接口：
   - 点击"调试"
   - 选择文件
   - 输入目录（如：test）
   - 点击"发送"
   - 查看响应结果

### 使用管理员后台测试（推荐）

**说明：** 管理员后台通过RestTemplate调用文件服务，测试服务间通信

1. 打开管理员后台文档：http://localhost:8082/doc.html
2. 找到"模板管理"分组
3. 测试"上传合同模板"接口：
   - 点击"调试"
   - 选择Word或HTML文件
   - 输入模板名称（如：正式员工劳动合同模板）
   - 输入模板类型（如：ONBOARD）
   - 点击"发送"
   - 查看响应结果

**内部流程：**

```
管理员后台Controller 
  ↓ FileServiceClient.uploadTemplate()
  ↓ HTTP POST 请求
文件服务Controller
  ↓ OssService.uploadFile()
  ↓ OSS SDK
阿里云OSS存储
```

4. 测试"删除模板文件"接口：
   
   - 复制上传返回的ossPath
   - 在删除接口中输入ossPath
   - 点击"发送"

5. 测试"获取模板下载链接"接口：
   
   - 输入ossPath
   - 输入过期时间（默认60分钟）
   - 点击"发送"
   - 复制返回的URL在浏览器打开

### 使用Postman测试

```bash
POST http://localhost:8083/api/file/upload
Content-Type: multipart/form-data

参数：
- file: [选择文件]
- directory: templates/contracts
```

## OSS配置

在`tailai-file-service/src/main/resources/application.yml`中配置：

```yaml
aliyun:
  oss:
    endpoint: https://oss-cn-beijing.aliyuncs.com
    accessKeyId: 
    accessKeySecret: 
    bucketName: tailai-1
```

## OSS目录结构

```
tailai-1 (Bucket)
├── templates/
│   └── contracts/          # 合同模板
├── certificates/
│   ├── id-card/           # 身份证
│   ├── diploma/           # 学历证书
│   └── other/             # 其他证件
├── files/                 # 通用文件
└── [按日期分组]
    └── 2025/01/01/        # 每天的文件
```

## 开发说明

### 添加新接口

1. 在对应的Controller中添加方法
2. 使用Swagger注解标注：
   - `@Tag`：接口分组
   - `@Operation`：接口说明
   - `@Parameter`：参数说明
3. 返回值使用`Result<T>`统一封装
4. 异常使用`BusinessException`抛出

### 调用文件服务

在其他服务中通过`FileServiceClient`调用文件服务：

```java
@Autowired
private FileServiceClient fileServiceClient;

public void uploadFile(MultipartFile file) {
    Map<String, Object> result = fileServiceClient.uploadFile(file, "test");
    String ossPath = (String) result.get("ossPath");
    // 保存ossPath到数据库
}
```

## 常见问题

### 1. OSS连接失败

检查OSS配置是否正确，网络是否正常。

### 2. 文件上传失败

检查文件大小是否超限，当前限制：

- 通用文件：100MB
- 图片文件：10MB
- 模板文件：10MB

### 3. 端口被占用

修改`application.yml`中的端口配置：

- 文件服务：`server.port: 8083`
- 管理员后台：`server.port: 8082`

## 下一步计划

- [ ] 添加数据库支持
- [ ] 实现PDF生成功能
- [ ] 实现电子签名功能
- [ ] 添加工作流服务
- [ ] 实现员工小程序服务

## 联系方式

- 项目负责人：泰来云开发团队
- 邮箱：dev@tailai.com
