# Java Calc 使用文档

这是一个数学表达式化简后端服务。项目已经从普通 Java 命令行程序迁移为 Spring Boot 项目，同时保留了原课程作业的 stdin 命令行入口。

目前项目可以使用，已验证：

- Maven 可以编译和运行测试。
- HTTP 接口可以接收 JSON 请求并返回化简结果。
- 普通函数、递推函数、求导示例与旧命令行输出保持一致。
- 每次请求都会创建独立计算上下文，函数定义不会跨请求共享。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.8 或更高版本。

当前 `pom.xml` 使用 Spring Boot 3.3.5。

## 项目结构

```text
src/main/java/com/example/javacalc/
  JavaCalcApplication.java        Spring Boot 启动类
  api/
    CalculatorController.java     HTTP 接口
    GlobalExceptionHandler.java   全局错误处理
    dto/                          请求和响应 DTO
  service/
    CalculatorService.java        计算服务入口
    CalculatorApi.java            业务接口
  core/                           表达式解析和化简核心
  cli/
    MainClass.java                旧 stdin 格式兼容入口
    Input.java                    命令行输入适配
```

旧的根目录 `src/*.java` 已迁移到 Maven 标准目录 `src/main/java/...`。

## 编译和测试

在项目根目录执行：

```bash
mvn test
```

测试覆盖：

- `CalculatorService` 的普通函数、递推函数、求导和混合调用。
- `CalculatorController` 的 HTTP 成功响应、错误响应和健康检查。
- 旧输入格式示例的回归结果。

## 启动后端服务

```bash
mvn spring-boot:run
```

默认端口是 `8080`，也可以通过环境变量覆盖：

```bash
PORT=10000 mvn spring-boot:run
```

配置文件在：

```text
src/main/resources/application.yml
```

启动后服务地址：

```text
http://localhost:8080
```

## 线上服务

当前 Render Web Service 已部署：

```text
https://java-formula-simplifier-service.onrender.com
```

线上健康检查：

```bash
curl https://java-formula-simplifier-service.onrender.com/api/v1/health
```

预期响应：

```json
{"status":"UP"}
```

线上表达式化简测试：

```bash
curl -X POST https://java-formula-simplifier-service.onrender.com/api/v1/simplify \
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

## 健康检查

```bash
curl http://localhost:8080/api/v1/health
```

响应：

```json
{
  "status": "UP"
}
```

## 表达式化简接口

```http
POST /api/v1/simplify
Content-Type: application/json
```

请求体：

```json
{
  "normalFunctions": [
    "f(x,y)=x+y"
  ],
  "recursiveFunctions": [
    [
      "g{0}(x)=x",
      "g{1}(x)=x^2",
      "g{n}(x)=g{n-1}(x)+g{n-2}(x)"
    ]
  ],
  "expression": "f(x,2)+g{3}(x)"
}
```

成功响应：

```json
{
  "result": "2*x+2+2*x^2"
}
```

curl 示例：

```bash
curl -X POST http://localhost:8080/api/v1/simplify \
  -H 'Content-Type: application/json' \
  -d '{
    "normalFunctions": ["f(x,y)=x+y"],
    "recursiveFunctions": [
      [
        "g{0}(x)=x",
        "g{1}(x)=x^2",
        "g{n}(x)=g{n-1}(x)+g{n-2}(x)"
      ]
    ],
    "expression": "f(x,2)+g{3}(x)"
  }'
```

## 请求字段说明

`normalFunctions`：普通函数定义数组。没有普通函数时传 `[]` 或省略。

```json
["f(x,y)=x+y"]
```

普通函数形参顺序不固定，调用时按位置绑定实参。例如 `f(y,x)=y-x` 调用 `f(2,x)` 会得到 `2-x`。

`recursiveFunctions`：递推函数定义数组。每个递推函数必须恰好包含 3 行：第 0 项、第 1 项、递推项。

```json
[
  [
    "f{0}(x,y)=x+y",
    "f{1}(x,y)=x*y",
    "f{n}(x,y)=f{n-1}(x,y)+f{n-2}(x,y)"
  ]
]
```

递推函数支持多个形参和多个实参，例如 `f{2}(x,2)`。形参顺序不固定，但同一个递推函数三行的形参列表和顺序必须保持一致，例如都写 `f{0}(y,x)`、`f{1}(y,x)`、`f{n}(y,x)`。当前实现只从第 0 行读取形参列表，不会校验第 1 行和第 n 行是否一致，调用方应主动保证一致性。

`expression`：需要化简的目标表达式，不能为空。

## 支持的表达式能力

当前核心语法沿用原项目实现，主要支持：

- 整数常量：`0`、`1`、`123`
- 变量：面向前端产品时目标表达式只开放自由变量 `x`；`y`、`z` 只用于函数定义形参
- 加减乘：`+`、`-`、`*`
- 乘方：`^`
- 括号表达式：`(x+1)^2`
- 三角函数：`sin(x)`、`cos(x)`，可带指数，例如 `sin(x)^2`
- 普通函数调用：`f(x,2)`
- 递推函数调用：`f{3}(x)`、`g{2}(x,2)`
- 求导：`dx(x^2+sin(x))`

注意事项：

- 不支持隐式乘法，必须写 `2*x`，不能写 `2x`。
- 函数名建议使用 `f`、`g`、`h`。
- 函数实参按“因子”解析，复杂表达式建议额外加括号，例如 `f((x+1),2)`。
- 递推函数目前主要支持 `n-1` 和 `n-2` 形式。
- 内部多项式输出统一按 `x` 表示，仍保留原项目的变量处理限制；前端目标表达式不要允许自由 `y`、`z`。

## 错误响应

错误会返回结构化 JSON，而不是 Java 堆栈。

示例：缺少 `expression`。

```json
{
  "code": "INVALID_REQUEST",
  "message": "请求参数错误",
  "detail": "expression must not be blank"
}
```

常见错误码：

- `INVALID_REQUEST`：请求体缺失、字段为空、递推函数不是 3 行等。
- `INVALID_FUNCTION_DEFINITION`：函数定义格式错误。
- `INVALID_EXPRESSION`：表达式格式错误或核心解析失败。
- `INTERNAL_ERROR`：未预期的服务内部错误。

## 命令行兼容模式

仍可以使用原课程作业 stdin 格式运行：

```bash
mvn -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" com.example.javacalc.cli.MainClass
```

也可以通过 Spring Boot Maven 插件构建后运行 jar：

```bash
mvn -q -DskipTests package
java -jar target/java-calc-0.0.1-SNAPSHOT.jar
```

`java -jar` 会启动 Spring Boot 后端服务；如果要运行旧 stdin 入口，请使用前面的 `java -cp ... com.example.javacalc.cli.MainClass`。

stdin 示例：

```text
1
f(x,y)=x+y
0
f(x,2)
```

输出：

```text
x+2
```

## Java 内部调用

如果在 Java 代码中直接调用服务，可以使用：

```java
CalculatorService service = new CalculatorService();
SimplifyRequest request = new SimplifyRequest(
    List.of("f(x,y)=x+y"),
    List.of(),
    "f(x,2)"
);
String result = service.simplify(request);
```

也保留了过渡用的 JSON 字符串入口：

```java
String result = service.simplify("""
    {
      "normalFunctions": ["f(x,y)=x+y"],
      "recursiveFunctions": [],
      "expression": "f(x,2)"
    }
    """);
```

新代码更推荐使用 `SimplifyRequest` DTO。

## 前端联调

默认允许以下本地开发源跨域调用：

- `http://localhost:3000`
- `http://localhost:5173`
- `http://127.0.0.1:3000`
- `http://127.0.0.1:5173`

部署后可以通过 `CORS_ALLOWED_ORIGINS` 配置前端域名，多个域名用英文逗号分隔：

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend.example.com,https://your-preview.example.com
```

当前 Render 环境变量暂时配置为 `CORS_ALLOWED_ORIGINS=*`，用于没有前端时开放测试任意来源。正式接入前端后，建议改为明确的前端域名。

前端请求示例：

```js
const API_BASE_URL = "https://java-formula-simplifier-service.onrender.com";

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

## Render Web Service 部署

详细部署与无前端测试流程见：

```text
docs/RENDER_DEPLOYMENT.md
```

前端调用接口指南见：

```text
docs/FRONTEND_INTEGRATION_GUIDE.md
```

项目已经包含 Docker 部署文件：

- `Dockerfile`：使用 Maven + JDK 17 构建，再用 JRE 17 运行 Spring Boot jar。
- `.dockerignore`：减少 Docker 构建上下文。
- `render.yaml`：可选的 Render Blueprint，声明 Docker Web Service 和健康检查路径。

本地 Docker 验证：

```bash
docker build -t java-calc .
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173 \
  java-calc
```

也可以使用镜像内置的 Render 默认端口：

```bash
docker run --rm -p 8080:10000 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173 \
  java-calc
```

Render Dashboard 手动部署：

1. 将项目推送到 GitHub、GitLab 或 Bitbucket。
2. 在 Render 创建 `New > Web Service`，连接该仓库。
3. Runtime / Language 选择 `Docker`。
4. Dockerfile Path 保持 `./Dockerfile`。
5. Health Check Path 设置为 `/api/v1/health`。
6. Environment Variables 中设置：
   - `CORS_ALLOWED_ORIGINS`：前端线上域名，多个域名用英文逗号分隔。
7. 部署完成后访问：

```bash
curl https://java-formula-simplifier-service.onrender.com/api/v1/health
```

前端线上请求地址改为 Render 分配的 HTTPS 域名：

```js
const API_BASE_URL = "https://java-formula-simplifier-service.onrender.com";
```

当前线上测试已验证：

- `GET /api/v1/health` 返回 `{"status":"UP"}`。
- `POST /api/v1/simplify` 支持普通表达式、普通函数、递推函数和求导。
- 缺少 `expression` 时返回 `400` 和结构化错误 JSON。
- 当前 `CORS_ALLOWED_ORIGINS=*` 时，预检请求返回 `Access-Control-Allow-Origin: *`。

## 当前限制

- 解析器对非法表达式的定位还比较粗，错误 `detail` 可能只包含底层异常信息。
- 递推展开仍可能导致表达式快速膨胀，目前只做了基础请求长度和函数数量限制。
- 输出项顺序沿用核心算法实现，不保证按数学阅读习惯排序。
- 暂未接入数据库、鉴权、历史记录或 OpenAPI 文档。
