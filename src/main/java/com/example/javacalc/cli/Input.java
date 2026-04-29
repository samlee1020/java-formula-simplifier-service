package com.example.javacalc.cli;

import com.example.javacalc.api.dto.SimplifyRequest;
import java.util.Scanner;
import java.util.ArrayList;

public class Input {
    private String exprString;
    private final ArrayList<String> normalFunctions = new ArrayList<>();
    private final ArrayList<ArrayList<String>> recursiveFunctions = new ArrayList<>();

    public void getInput() {
        Scanner scanner = new Scanner(System.in);
        int funcNum;
        // 普通函数，当作只有{0}的递推函数处理
        funcNum = Integer.parseInt(scanner.nextLine());
        for (int i = 0; i < funcNum; i++) {
            normalFunctions.add(scanner.nextLine().replaceAll("\\s+", ""));
        }
        // 递推函数
        funcNum = Integer.parseInt(scanner.nextLine());
        for (int i = 0; i < funcNum; i++) {
            ArrayList<String> lines = new ArrayList<String>();
            for (int j = 0; j < 3; j++) {
                lines.add(scanner.nextLine().replaceAll("\\s+", ""));
            }
            recursiveFunctions.add(lines);
        }
        this.exprString = scanner.nextLine().replaceAll("\\s+", "");
        scanner.close();
    }

    public String getExprString() {
        return this.exprString;
    }

    public SimplifyRequest toRequest() {
        return new SimplifyRequest(normalFunctions, new ArrayList<>(recursiveFunctions),
            exprString);
    }

}
