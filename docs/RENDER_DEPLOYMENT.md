# Render Web Service 部署与测试流程

本文档说明如何把 `java-calc` 部署到 Render Web Service，作为 Java 后端 API 服务供未来前端调用。当前项目已经具备部署条件：

- Spring Boot 后端入口：`com.example.javacalc.JavaCalcApplication`
- Docker 构建文件：`Dockerfile`
- Render Blueprint 文件：`render.yaml`
- 健康检查接口：`GET /api/v1/health`
- 核心计算接口：`POST /api/v1/simplify`

Render Web Service 默认 HTTP 端口是 `10000`。项目的 `Dockerfile` 已经设置 `PORT=10000`，`application.yml` 也支持用环境变量 `PORT` 覆盖端口。

## 一、部署前检查

在项目根目录执行：

```bash
mvn test
mvn -q -DskipTests package
```

预期结果：

- `mvn test` 最后显示 `BUILD SUCCESS`。
- `target/java-calc-0.0.1-SNAPSHOT.jar` 被生成。

如果本机有 Docker，也可以先本地构建镜像：

```bash
docker build -t java-calc .
```

再启动容器验证：

```bash
docker run --rm -p 8080:10000 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173 \
  java-calc
```

另开一个终端测试：

```bash
curl http://localhost:8080/api/v1/health
```

预期响应：

```json
{"status":"UP"}
```

## 二、推送代码到 Git 仓库

Render 通过 GitHub、GitLab 或 Bitbucket 拉取代码构建服务。部署前需要先把项目推送到远程仓库。

如果当前目录还不是 Git 仓库，可以初始化：

```bash
git init
git add .
git commit -m "Prepare Render Docker deployment"
```

然后在 GitHub 等平台创建仓库，并按平台提示添加远程地址：

```bash
git remote add origin <your-repository-url>
git branch -M main
git push -u origin main
```

如果项目已经在 Git 仓库里，只需要提交并推送当前改动：

```bash
git add Dockerfile .dockerignore render.yaml README.md docs/RENDER_DEPLOYMENT.md src/main/resources/application.yml src/main/java/com/example/javacalc/api/CorsConfig.java src/main/java/com/example/javacalc/api/CalculatorController.java
git commit -m "Add Render deployment support"
git push
```

## 三、方式 A：Render Dashboard 手动创建 Web Service

适合第一次部署、想在网页界面里确认每个选项的情况。

1. 登录 Render Dashboard。
2. 点击 `New`，选择 `Web Service`。
3. 连接项目所在的 GitHub、GitLab 或 Bitbucket 仓库。
4. 选择要部署的分支，通常是 `main`。
5. 服务名称建议填写 `java-calc`。
6. Runtime / Language 选择 `Docker`。
7. Dockerfile Path 填写：

```text
./Dockerfile
```

8. Health Check Path 填写：

```text
/api/v1/health
```

9. Environment Variables 中添加：

```text
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173
```

当前还没有前端，所以可以先保留 localhost。后续前端上线后，把它改成真实前端域名，例如：

```text
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,https://www.your-domain.com
```

10. 创建服务并等待 Render 构建、部署。

部署成功后，Render 会分配一个 HTTPS 地址，形如：

```text
https://java-calc.onrender.com
```

后文统一用 `<SERVICE_URL>` 表示这个地址。

## 四、方式 B：使用 render.yaml Blueprint

项目根目录已经包含 `render.yaml`：

```yaml
services:
  - type: web
    name: java-calc
    runtime: docker
    plan: free
    healthCheckPath: /api/v1/health
    envVars:
      - key: CORS_ALLOWED_ORIGINS
        value: http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173
```

使用 Blueprint 时，Render 会根据这个文件创建 Docker Web Service，并自动配置健康检查路径。

操作步骤：

1. 登录 Render Dashboard。
2. 点击 `New`，选择 `Blueprint`。
3. 连接包含本项目的 Git 仓库。
4. Render 会读取根目录下的 `render.yaml`。
5. 确认服务名、计划、环境变量后创建。

后续如果修改 `render.yaml` 并推送到绑定分支，Render 会根据 Blueprint 更新服务配置。

## 五、部署后的无前端测试流程

当前还没有前端，所以测试重点是确认后端服务在线、接口可用、错误响应稳定、CORS 配置可用。

先设置服务地址变量，后续命令直接复用：

```bash
export SERVICE_URL="https://your-service.onrender.com"
```

请把 `https://your-service.onrender.com` 替换成 Render 实际分配的地址。

### 1. 健康检查

```bash
curl -i "$SERVICE_URL/api/v1/health"
```

预期要点：

- HTTP 状态码是 `200`。
- 响应体是：

```json
{"status":"UP"}
```

### 2. 最小表达式化简

```bash
curl -i -X POST "$SERVICE_URL/api/v1/simplify" \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": [],
    "recursiveFunctions": [],
    "expression": "x+x"
  }'
```

预期响应：

```json
{"result":"2*x"}
```

### 3. 普通函数调用

```bash
curl -i -X POST "$SERVICE_URL/api/v1/simplify" \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": ["f(x,y)=x+y"],
    "recursiveFunctions": [],
    "expression": "f(x,2)"
  }'
```

预期响应：

```json
{"result":"x+2"}
```

### 4. 递推函数调用

```bash
curl -i -X POST "$SERVICE_URL/api/v1/simplify" \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": [],
    "recursiveFunctions": [
      [
        "g{0}(x)=x",
        "g{1}(x)=x^2",
        "g{n}(x)=g{n-1}(x)+g{n-2}(x)"
      ]
    ],
    "expression": "g{3}(x)"
  }'
```

预期响应：

```json
{"result":"2*x^2+x"}
```

### 5. 求导表达式

```bash
curl -i -X POST "$SERVICE_URL/api/v1/simplify" \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": [],
    "recursiveFunctions": [],
    "expression": "dx(x^2+sin(x))"
  }'
```

预期响应：

```json
{"result":"2*x+cos(x)"}
```

### 6. 错误响应测试

测试缺少 `expression` 的情况：

```bash
curl -i -X POST "$SERVICE_URL/api/v1/simplify" \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": [],
    "recursiveFunctions": []
  }'
```

预期要点：

- HTTP 状态码应为客户端错误，通常是 `400`。
- 响应体包含结构化错误字段：

```json
{
  "code": "INVALID_REQUEST",
  "message": "请求参数错误"
}
```

`detail` 字段可能随具体校验逻辑变化，不建议前端强依赖完整文本。

### 7. CORS 预检测试

虽然 curl 不受浏览器 CORS 限制，但可以模拟浏览器预检请求：

```bash
curl -i -X OPTIONS "$SERVICE_URL/api/v1/simplify" \
  -H 'Origin: http://localhost:3000' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
```

如果 `CORS_ALLOWED_ORIGINS` 包含 `http://localhost:3000`，预期响应头包含：

```text
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,OPTIONS
```

如果后续前端部署在 `https://your-frontend.vercel.app`，需要先把 Render 后端服务里的 `CORS_ALLOWED_ORIGINS` 改成包含该域名，再用类似命令测试：

```bash
curl -i -X OPTIONS "$SERVICE_URL/api/v1/simplify" \
  -H 'Origin: https://your-frontend.vercel.app' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
```

## 六、Postman / Apifox 测试

没有前端时，也可以用 Postman、Apifox 或 Insomnia 调接口。

请求配置：

- Method：`POST`
- URL：`<SERVICE_URL>/api/v1/simplify`
- Headers：`Content-Type: application/json`
- Body：raw JSON

示例 Body：

```json
{
  "normalFunctions": ["f(x,y)=x+y"],
  "recursiveFunctions": [],
  "expression": "f(x,2)"
}
```

预期响应：

```json
{
  "result": "x+2"
}
```

## 七、前端上线后的接入方式

前端只需要把 API base URL 指向 Render 服务地址：

```js
const API_BASE_URL = "https://your-service.onrender.com";

const response = await fetch(`${API_BASE_URL}/api/v1/simplify`, {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    normalFunctions: ["f(x,y)=x+y"],
    recursiveFunctions: [],
    expression: "f(x,2)"
  })
});

const data = await response.json();
console.log(data.result);
```

前端域名确定后，需要同步更新 Render 后端服务的环境变量：

```text
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

多个域名用英文逗号分隔，不要加空格也可以，加空格项目也会自动 trim。

## 八、常见问题排查

### 1. Render 部署后健康检查失败

检查点：

- `Health Check Path` 是否是 `/api/v1/health`。
- 服务日志里是否显示 Tomcat started。
- 日志里的端口是否为 `10000`，或是否正确读取了 Render 的 `PORT`。
- `Dockerfile` 是否在仓库根目录，Dashboard 里 Dockerfile Path 是否为 `./Dockerfile`。

### 2. curl 能调通，但浏览器前端报 CORS

检查点：

- Render 服务环境变量 `CORS_ALLOWED_ORIGINS` 是否包含前端页面的完整 origin。
- origin 必须包含协议，例如 `https://example.com`，不要只写 `example.com`。
- 端口也算 origin 的一部分，本地 `http://localhost:5173` 和 `http://localhost:3000` 是两个不同 origin。
- 修改环境变量后，需要让 Render 重新部署或重启服务。

### 3. Free 实例第一次访问很慢

Render 免费实例可能会休眠。长时间无人访问后，第一次请求可能需要等待服务冷启动。健康检查和 API 本身如果最终返回正常，就是可用的。

### 4. 构建很慢

第一次 Docker 构建会下载 Maven 依赖和基础镜像，耗时较长是正常的。后续构建会利用缓存，通常会快很多。

### 5. 输出结果和数学书写顺序不同

核心算法不保证输出项按人类阅读习惯排序。例如同一个数学等价式可能输出为不同项顺序。测试时应优先使用 README 中已确认的示例。

## 九、验收清单

部署完成后，至少确认以下项目：

- Render 服务状态是 Live。
- `GET /api/v1/health` 返回 `{"status":"UP"}`。
- `POST /api/v1/simplify` 普通表达式返回正确结果。
- 普通函数、递推函数、求导各跑通一个样例。
- 错误请求返回结构化 JSON，而不是 HTML 错误页或 Java 堆栈。
- CORS 预检请求对目标前端 origin 返回 `Access-Control-Allow-Origin`。

## 十、参考资料

- Render Docker 文档：https://render.com/docs/docker
- Render Web Services 文档：https://render.com/docs/web-services
- Render Health Checks 文档：https://render.com/docs/health-checks
- Render Environment Variables 文档：https://render.com/docs/configure-environment-variables
- Render Blueprint 文档：https://render.com/docs/blueprint-spec
