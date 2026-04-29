package com.example.javacalc.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 函数定义类
public class Definer { 
    // 通过函数名、函数序号获取函数定义字符串
    private final Map<String, Map<Integer, String>> funcMap = new HashMap<>();
    // 通过函数名获取函数参数列表
    private final Map<String, ArrayList<String>> paramMap = new HashMap<>();
    // 通过函数名获取函数递推关系
    private final Map<String, String> recMap = new HashMap<>();

    // 函数添加，传入函数定义字符串列表（含三个字符串）
    public void addFunc(ArrayList<String> funcDefList) {
        // 获取函数名
        String funcName = funcDefList.get(0).substring(0,1); // 第一个字符为函数名
        
        // 获取函数参数列表
        ArrayList<String> paramList = new ArrayList<>();
        String line0 = funcDefList.get(0);
        int start = line0.indexOf('(');
        int end = line0.indexOf(')');
        String paramStr = line0.substring(start + 1, end);
        String[] params = paramStr.split(",");
        for (String param : params) {
            paramList.add(param);
        }
        paramMap.put(funcName, paramList);

        // 获取函数定义字符串和递推关系
        Map<Integer, String> hashMap = new HashMap<>();
        for (int i = 0; i < funcDefList.size(); i++) {
            String line = funcDefList.get(i);
            int equalIndex = line.indexOf('=');
            if (line.charAt(2) == '0') {
                hashMap.put(0, line.substring(equalIndex + 1));
            } else if (line.charAt(2) == '1') {
                hashMap.put(1, line.substring(equalIndex + 1));
            } else if (line.charAt(2) == 'n') { // 递推关系
                recMap.put(funcName, line.substring(equalIndex + 1));
            } else { // 普通函数定义，当作f{0}处理
                hashMap.put(0, line.substring(equalIndex + 1));
            }
        }
        funcMap.put(funcName, hashMap);
        
    }

    // 获取递推函数定义字符串
    public String getFuncDef(String funcName, int funcIndex) {
        return funcMap.get(funcName).get(funcIndex);
    }

    // 获取普通函数定义字符串
    public String getFuncDef(String funcName) {
        return funcMap.get(funcName).get(0);
    }

    // 获取函数参数列表
    public ArrayList<String> getParamList(String funcName) {
        return new ArrayList<>(paramMap.get(funcName));
    }

    // 获取函数递推表达式
    public String getRecExpr(String funcName) {
        return recMap.get(funcName);
    }
}
