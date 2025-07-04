package com.longcoding.top.longojchatend;

import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ACM模式核心逻辑示例
        while (scanner.hasNext()) {
            String s = scanner.next();
            String t = scanner.next();

            // 调用解题方法
            int result = solve(s, t);

            // 输出结果
            System.out.println(result);
        }
    }

    private static int solve(String text1, String text2) {
        // TODO
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            char c1 = text1.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                char c2 = text2.charAt(j - 1);
                if (c1 == c2) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }
}