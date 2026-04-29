# Spring Boot 后端改造计划

本文档基于当前 `java-calc` 项目现状，规划如何将数学表达式化简程序改造成 Spring Boot 后端服务，向前端提供 HTTP API。本文只给计划，不进行实现。

## 当前项目状态

当前项目仍是普通 Java 源码结构：

```text
src/
  MainClass.java
  Input.java
  CalculatorApi.java
  CalculatorService.java
  CalculatorRequest.java
  CalculatorJson.java
  Definer.java
  Lexer.java
  Parser.java
  Expr.java
  Term.java
  Poly.java
  Mono.java
  Factor*.java
  DxTool.java
  Token.java
```

已经完成的关键改造：

- `Definer` 已从静态全局状态改为实例状态。
- 核心计算流程已经抽到 `CalculatorService`。
- 当前业务入口是：

```java
CalculatorApi calculator = new CalculatorService();
String result = calculator.simplify(requestJson);
```

当前 JSON 请求结构：

```json
{
  "normalFunctions": ["f(x,y)=x+y"],
  "recursiveFunctions": [
    ["g{0}(x)=x", "g{1}(x)=x^2", "g{n}(x)=g{n-1}(x)+g{n-2}(x)"]
  ],
  "expression": "f(x,2)+g{3}(x)"
}
```

当前命令行入口 `MainClass` 仍然兼容课程作业原始 stdin 格式，并在内部包装成 JSON 调用 `CalculatorService`。

## 改造目标

目标是形成一个标准 Spring Boot 后端项目：

- 前端通过 HTTP POST 调用表达式化简 API。
- 后端接收结构化 JSON 请求，返回结构化 JSON 响应。
- 核心计算逻辑保持无状态：每次请求独立创建计算上下文，不共享函数定义。
- 保留命令行入口作为可选调试工具，而不是主要业务入口。
- 引入标准构建工具、测试框架和 API 错误处理。

建议技术栈：

- Java 17 或团队统一 JDK 版本。
- Spring Boot 3.x 或团队统一 Spring Boot 版本。
- Maven 或 Gradle，建议优先 Maven，课程项目迁移成本较低。
- Jackson：替代当前手写 `CalculatorJson`。
- JUnit 5：单元测试和接口测试。

## 目标项目结构

建议迁移为 Maven 标准结构：

```text
java-calc/
  pom.xml
  src/
    main/
      java/
        com/example/javacalc/
          JavaCalcApplication.java
          api/
            CalculatorController.java
            dto/
              SimplifyRequest.java
              SimplifyResponse.java
              ErrorResponse.java
          service/
            CalculatorService.java
            CalculatorApi.java
          core/
            Definer.java
            Lexer.java
            Parser.java
            Expr.java
            Term.java
            Poly.java
            Mono.java
            Factor.java
            FactorNum.java
            FactorVar.java
            FactorSubExpr.java
            FactorSin.java
            FactorCos.java
            FactorFunc.java
            FactorDx.java
            DxTool.java
            Token.java
          cli/
            MainClass.java
            Input.java
      resources/
        application.yml
    test/
      java/
        com/example/javacalc/
          api/
            CalculatorControllerTest.java
          service/
            CalculatorServiceTest.java
          core/
            ParserRegressionTest.java
```

分层建议：

- `api`：HTTP Controller 和接口 DTO。
- `service`：业务服务，负责组织请求、调用 core、返回结果。
- `core`：当前数学表达式解析和化简核心。
- `cli`：兼容旧 stdin 输入的命令行工具，可选保留。

## HTTP API 设计

### 化简接口

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

错误响应：

```json
{
  "code": "INVALID_EXPRESSION",
  "message": "表达式格式错误",
  "detail": "expected ')' at token index 12"
}
```

### 可选健康检查接口

```http
GET /api/v1/health
```

响应：

```json
{
  "status": "UP"
}
```

如果引入 Spring Boot Actuator，也可以直接使用 `/actuator/health`。

## DTO 设计

建议用 Spring/Jackson 直接绑定 JSON，不再让业务接口接收 JSON 字符串。

请求 DTO：

```java
public class SimplifyRequest {
    private List<String> normalFunctions;
    private List<List<String>> recursiveFunctions;
    private String expression;
}
```

响应 DTO：

```java
public class SimplifyResponse {
    private String result;
}
```

错误 DTO：

```java
public class ErrorResponse {
    private String code;
    private String message;
    private String detail;
}
```

保留当前 `CalculatorService.simplify(String requestJson)` 作为过渡可以，但 Spring Boot Controller 最终更适合调用：

```java
String simplify(SimplifyRequest request)
```

或：

```java
SimplifyResponse simplify(SimplifyRequest request)
```

## 服务层改造计划

当前 `CalculatorService` 同时做了 JSON 解析和计算编排。Spring Boot 化后建议拆开：

1. Controller 接收 JSON 并转换为 `SimplifyRequest`。
2. Service 接收 Java 对象，不再接收 JSON 字符串。
3. Service 内部每次请求创建新的 `Definer`。
4. Service 调用现有 `Lexer`、`Parser`、`Expr`、`Poly` 流程。
5. Service 返回化简结果字符串或响应 DTO。

目标服务方法：

```java
public String simplify(SimplifyRequest request)
```

当前 `CalculatorJson` 可以逐步删除，因为 Spring Boot 默认会通过 Jackson 处理 JSON 序列化和反序列化。

## Controller 计划

新增 `CalculatorController`：

```java
@RestController
@RequestMapping("/api/v1")
public class CalculatorController {
    private final CalculatorService calculatorService;

    @PostMapping("/simplify")
    public SimplifyResponse simplify(@RequestBody SimplifyRequest request) {
        String result = calculatorService.simplify(request);
        return new SimplifyResponse(result);
    }
}
```

注意：

- Controller 不直接创建 `Definer`。
- Controller 不处理核心解析逻辑。
- Controller 只做 HTTP 入参出参转换。

## 错误处理计划

当前解析器错误处理较弱，很多非法输入可能触发 `NullPointerException`、`IndexOutOfBoundsException` 或 `NumberFormatException`。

Spring Boot 化时建议新增全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    ...
}
```

建议错误码：

- `INVALID_REQUEST`：JSON 字段缺失、类型错误、递推函数不是 3 行。
- `INVALID_EXPRESSION`：表达式语法错误。
- `INVALID_FUNCTION_DEFINITION`：函数定义格式错误。
- `CALCULATION_LIMIT_EXCEEDED`：递推展开过深、表达式过大、计算超时。
- `INTERNAL_ERROR`：未知错误。

短期可以先把核心异常统一转成 `400 Bad Request`，再逐步细化异常类型。

## 输入校验计划

建议在进入核心计算前做显式校验：

- `expression` 不能为空。
- `normalFunctions` 默认为空数组，不允许为 `null`。
- `recursiveFunctions` 默认为空数组，不允许为 `null`。
- 每个递推函数定义必须恰好 3 行。
- 单次请求的函数定义数量设置上限。
- 单个表达式字符串长度设置上限。
- 递推函数调用下标设置上限，避免递归爆炸。
- 禁止无法识别的函数名或变量名时，返回明确错误。

这些校验不一定第一阶段全部实现，但需要作为 API 稳定化目标。

## 并发与状态计划

当前最重要的并发前提已经具备：`Definer` 是实例状态。

Spring Boot 中 `CalculatorService` 默认会作为单例 Bean，因此必须确保：

- `CalculatorService` 内不保存某次请求的 `Definer`。
- `CalculatorService` 内不保存某次请求的 `Lexer`、`Parser`、`Expr`、`Poly`、`Mono`。
- 每次请求局部创建所有计算对象。
- 不缓存可变核心对象。

如果后续需要缓存，只缓存最终字符串结果，并且缓存 key 必须包含完整请求内容。

## 测试计划

### 单元测试

针对 `CalculatorService`：

- 无函数：`dx(x^2+sin(x)) -> 2*x+cos(x)`。
- 普通函数：`f(x,y)=x+y`，表达式 `f(x,2) -> x+2`。
- 递推函数：`g{3}(x) -> 2*x^2+x`。
- 普通函数和递推函数混合调用。
- 空函数数组。
- 非法递推函数定义行数。

### 接口测试

针对 `CalculatorController`：

- POST `/api/v1/simplify` 成功返回 `200` 和 `{ "result": "..." }`。
- 请求体缺少 `expression` 返回 `400`。
- 递推函数不是 3 行返回 `400`。
- 非法表达式返回结构化错误响应。

### 回归测试

把 `INPUT_FORMAT.md` 和 `BUSINESS_INTERFACE_REFACTOR.md` 中的示例固化为测试用例，确保迁移过程中输出不变。

## 迁移步骤

### 阶段 1：建立 Spring Boot 骨架

- 新建 `pom.xml`。
- 新建 `JavaCalcApplication`。
- 引入 `spring-boot-starter-web` 和 `spring-boot-starter-test`。
- 保持当前代码暂时可编译，不急着改核心算法。

### 阶段 2：迁移包结构

- 给所有类增加 package。
- 将核心算法类移入 `core` 包。
- 将 `CalculatorService` 和 `CalculatorApi` 移入 `service` 包。
- 将 `Input` 和 `MainClass` 移入 `cli` 包或暂时移除。

### 阶段 3：替换 JSON 处理方式

- 新增 `SimplifyRequest`、`SimplifyResponse`、`ErrorResponse`。
- Controller 直接使用 `@RequestBody SimplifyRequest`。
- Service 接收 DTO，不再接收 JSON 字符串。
- 删除或弃用 `CalculatorJson`。

### 阶段 4：提供 HTTP API

- 新增 `CalculatorController`。
- 暴露 `POST /api/v1/simplify`。
- 配置 CORS，允许前端开发环境调用。
- 增加全局异常处理。

### 阶段 5：补充测试

- 添加 Service 单元测试。
- 添加 Controller 接口测试。
- 添加旧样例回归测试。

### 阶段 6：增强稳定性

- 增加输入长度、函数数量、递推下标限制。
- 给解析错误设计自定义异常。
- 梳理 `Poly`、`Mono` 的可变对象行为，避免未来缓存或并发误用。
- 添加日志和请求耗时记录。

## 可暂缓事项

以下内容不是第一版 API 必须完成：

- 用户登录鉴权。
- 数据库存储历史记录。
- 表达式计算结果缓存。
- OpenAPI/Swagger 页面。
- Docker 镜像和部署流水线。
- 前端页面联调。

这些可以等核心 API 稳定后再做。

## 验收标准

第一版 Spring Boot 后端完成后，应满足：

- 项目可以通过 Maven 或 Gradle 一键编译、测试、启动。
- `POST /api/v1/simplify` 可以被前端用 JSON 调用。
- 普通函数、递推函数、求导示例输出与当前命令行版本一致。
- 多次请求之间函数定义互不影响。
- 错误请求返回结构化 JSON 错误，而不是直接暴露 Java 堆栈。
- 原核心算法类不依赖 Spring，仍可独立单元测试。

## 主要风险

- 当前 Parser 缺少严格语法错误处理，接口化后需要避免把内部异常直接暴露给前端。
- 当前 `Poly`、`Mono`、`Expr` 等对象是可变对象，不能跨请求复用。
- 递推函数展开可能导致表达式快速膨胀，需要设置上限。
- 当前表达式语法和变量支持范围有限，API 文档必须明确说明限制。
- 当前输出项顺序不一定符合数学阅读习惯，前端展示时不要假设固定排序，测试也应以当前实现输出为准。
