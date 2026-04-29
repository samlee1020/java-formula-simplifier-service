# 主体业务接口改造记录

本文档记录本次把命令行主流程包装成 JSON 业务接口的改造过程。目标是让核心计算逻辑不再散落在 `MainClass` 中，而是通过一个明确的服务入口调用。

## JSON 请求格式

服务入口接收一个 JSON 字符串，结构如下：

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

字段含义：

- `normalFunctions`：普通函数定义数组，每个普通函数定义占一个字符串。
- `recursiveFunctions`：递推函数定义数组。每个递推函数由 3 个字符串组成，分别是第 0 项、第 1 项和递推项。
- `expression`：目标表达式字符串。

返回值是化简后的表达式字符串，例如：

```text
2*x^2+2*x+2
```

## 新增文件

### 1. `src/CalculatorApi.java`

新增业务接口：

```java
public interface CalculatorApi {
    String simplify(String requestJson);
}
```

这是后续对接 Web Controller、RPC、命令行适配器时最稳定的入口。外层只需要准备 JSON 字符串，调用 `simplify`，拿到化简结果字符串。

### 2. `src/CalculatorService.java`

新增业务实现类：

```java
public class CalculatorService implements CalculatorApi
```

主要职责：

- 解析 JSON 为 `CalculatorRequest`。
- 为每次调用创建新的 `Definer`。
- 将普通函数和递推函数写入本次调用的 `Definer`。
- 执行原有的两轮解析和三角化简流程。
- 返回最终化简字符串。

核心入口：

```java
public String simplify(String requestJson)
```

同时提供：

```java
public String simplify(CalculatorRequest request)
```

方便 Java 内部调用时绕过 JSON 字符串拼装。

### 3. `src/CalculatorRequest.java`

新增请求对象，保存：

- `normalFunctions`
- `recursiveFunctions`
- `expression`

该类负责在构造和 getter 中复制集合，减少调用方后续修改集合对请求对象内部状态的影响。

### 4. `src/CalculatorJson.java`

新增轻量 JSON 解析和生成工具。

项目当前没有 Maven/Gradle，也没有第三方 JSON 依赖，因此这里实现了一个专用于本请求结构的小解析器。它支持：

- JSON 对象。
- 字符串。
- 字符串数组。
- 二维字符串数组。
- 字符串中的 `\"`、`\\`、`\n`、`\r`、`\t` 转义。

它不是通用 JSON 库，只服务于当前计算请求格式。

## 修改文件

### 1. `src/Input.java`

原先 `Input` 会在读取 stdin 时直接把函数定义写入 `Definer`。现在改为：

- 读取原作业输入格式。
- 保存普通函数定义列表。
- 保存递推函数定义列表。
- 保存目标表达式。
- 通过 `toRequest()` 生成 `CalculatorRequest`。
- 通过 `toJson()` 生成业务接口需要的 JSON 字符串。

这样 `Input` 变成命令行适配层，不再参与核心计算。

### 2. `src/MainClass.java`

原先 `MainClass` 自己完成：

- 创建 `Definer`。
- 词法分析。
- 解析。
- 二次解析。
- 三角化简。

现在改为：

```java
Input input = new Input();
input.getInput();

CalculatorApi calculator = new CalculatorService();
System.out.println(calculator.simplify(input.toJson()));
```

也就是说，`MainClass` 只负责兼容原来的 stdin 输入格式，然后包装成 JSON 调用业务接口。

## 当前调用链

命令行模式：

```text
stdin 原作业格式
-> Input.getInput()
-> Input.toJson()
-> CalculatorService.simplify(json)
-> CalculatorRequest.fromJson(json)
-> 每次调用新建 Definer
-> Lexer / Parser / Expr / Poly
-> 输出化简结果
```

业务接口模式：

```text
JSON 请求字符串
-> CalculatorService.simplify(json)
-> 输出化简结果
```

## 设计收益

- 核心计算流程从 `MainClass` 中抽出，后续可以直接被 HTTP 接口或其它业务层调用。
- 每次 `CalculatorService.simplify(...)` 都创建新的 `Definer`，避免函数定义跨请求共享。
- 旧的课程作业 stdin 输入格式仍然可用。
- JSON 格式明确表达了普通函数、递推函数和目标表达式之间的关系。

## 注意事项

- `CalculatorJson` 是专用解析器，不是完整 JSON 库。后续如果项目引入 Maven/Gradle，建议替换为 Jackson、Gson 等成熟库。
- 递推函数仍要求每个定义块恰好 3 行。
- 表达式语法、函数名限制、递推展开限制仍沿用原有解析器行为。
- `Poly`、`Mono`、`Expr` 等内部对象仍是可变对象，不建议跨请求缓存这些对象。业务接口层应以字符串输入和字符串输出为主。
