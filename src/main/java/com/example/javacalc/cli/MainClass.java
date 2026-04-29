package com.example.javacalc.cli;

import com.example.javacalc.service.CalculatorApi;
import com.example.javacalc.service.CalculatorService;

public class MainClass {

    public static void main(String[] args) {

        // 读取原始作业格式输入，包装成请求对象后调用计算接口
        Input input = new Input();
        input.getInput();

        CalculatorApi calculator = new CalculatorService();
        System.out.println(calculator.simplify(input.toRequest()));

    }
}
