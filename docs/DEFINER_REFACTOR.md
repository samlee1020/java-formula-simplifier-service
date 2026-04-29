# Definer 去共享状态改造记录

本文档记录本次围绕 `Definer` 的修改过程。目标是消除函数定义的 JVM 全局共享状态，为后续把命令行作业改造成业务接口做准备。

## 修改背景

原实现中，`Definer` 使用了三个静态可变 `HashMap`：

```java
private static HashMap<String, HashMap<Integer, String>> funcMap = new HashMap<>();
private static HashMap<String, ArrayList<String>> paramMap = new HashMap<>();
private static HashMap<String, String> recMap = new HashMap<>();
```

这在单次命令行运行中可行，但在业务接口场景下有两个明显问题：

- 多个请求共享同一份函数定义，后一个请求可能覆盖前一个请求的 `f/g/h` 定义。
- `HashMap` 不是线程安全的，并发读写有数据竞争风险。

因此本次改造将函数定义从“类级别全局状态”改为“对象实例状态”。每次计算可以创建自己的 `Definer` 实例，函数定义只在本次计算上下文内生效。

## 修改内容

### 1. `Definer` 从静态仓库改为实例对象

修改文件：`src/Definer.java`

主要变化：

- 删除三个 `static` 字段。
- 字段改为实例级 `final Map`。
- `addFunc`、`getFuncDef`、`getParamList`、`getRecExpr` 全部从静态方法改为实例方法。
- `getParamList` 返回新的 `ArrayList`，避免调用方直接修改 `Definer` 内部保存的参数列表。

修改后，每个 `Definer` 实例拥有自己独立的：

```java
funcMap
paramMap
recMap
```

这使得两个请求可以分别持有不同的 `Definer`，互不覆盖函数定义。

### 2. `Input` 持有当前计算的 `Definer`

修改文件：`src/Input.java`

主要变化：

- 新增 `private final Definer definer;`。
- 新增 `Input(Definer definer)` 构造器。
- 保留无参构造器，默认创建一个新的 `Definer`。
- 读取函数定义时从 `Definer.addFunc(...)` 改为 `definer.addFunc(...)`。
- 新增 `getDefiner()`，用于无参构造器场景下取回内部创建的 `Definer`。

这样 `Input` 不再把函数定义写入全局静态区域，而是写入当前计算绑定的 `Definer`。

### 3. `Parser` 携带同一个 `Definer`

修改文件：`src/Parser.java`

主要变化：

- 新增 `private final Definer definer;`。
- 新增 `Parser(Lexer lexer, Definer definer)` 构造器。
- 保留 `Parser(Lexer lexer)` 构造器，内部创建一个空 `Definer`，用于没有自定义函数的内部解析场景。
- 解析函数调用时，从：

```java
return new FactorFunc(funcName, funcIndex, actualParams);
```

改为：

```java
return new FactorFunc(funcName, funcIndex, actualParams, definer);
```

这样 `FactorFunc` 在后续展开函数时，能访问当前请求自己的函数定义。

### 4. `FactorFunc` 不再访问全局 `Definer`

修改文件：`src/FactorFunc.java`

主要变化：

- 新增 `private final Definer definer;`。
- 构造器接收 `Definer`。
- 函数展开时，从 `Definer.getFuncDef(...)`、`Definer.getParamList(...)`、`Definer.getRecExpr(...)` 改为使用实例方法：

```java
definer.getFuncDef(...)
definer.getParamList(...)
definer.getRecExpr(...)
```

- 函数体递归展开时，新建的 `Parser` 继续传入同一个 `definer`。
- `actualParams` 在构造器中复制一份，减少外部列表后续修改对对象内部状态的影响。
- `equals` 中增加 `definer == otherFunc.definer`，避免不同计算上下文中的同名函数调用被误判为完全相同。

### 5. `MainClass` 创建并传递同一个 `Definer`

修改文件：`src/MainClass.java`

主要变化：

- 在主流程开始时创建：

```java
Definer definer = new Definer();
```

- 将它传给 `Input`：

```java
Input input = new Input(definer);
```

- 两次表达式解析都传入同一个 `definer`：

```java
Parser parser = new Parser(lexer, definer);
Parser newParser = new Parser(newLexer, definer);
```

这样一次命令行运行内，函数定义仍能被后续表达式解析和递归展开正常访问。

## 当前状态

本次修改后：

- `Definer` 不再有静态可变字段。
- 普通函数和递推函数定义绑定到具体 `Definer` 实例。
- `Input`、`Parser`、`FactorFunc`、`MainClass` 已经接入新的实例式 `Definer`。
- 原命令行输入格式保持不变。

## 后续改业务接口的建议

后续可以进一步抽出一个服务类，例如：

```java
public class CalculatorService {
    public String simplify(ArrayList<String> normalFuncs,
        ArrayList<ArrayList<String>> recursiveFuncs, String exprString) {
        ...
    }
}
```

这个服务方法内部每次创建新的 `Definer`，然后按当前请求填充函数定义并计算表达式。这样接口层不需要经过 `System.in`，也不会跨请求共享函数定义。

仍需注意：`Poly`、`Mono`、`Expr` 等类内部仍有可变集合和浅拷贝行为，建议不要把这些对象作为跨请求缓存对象或多线程共享对象。业务接口最好只暴露字符串输入和字符串输出。
