package com.huawei.java.main.Dao;

import java.util.ArrayList;
import java.util.Collections;

public class Test {

    public static void main(String[] args) {
        int []v={47,143};
        int []w={25,69};
        int []b={22,74};
        int c=325;
        int d= 476;

//        int []v={10,20};
//        int []w={1,2};
//        int []b={2,1};
//        int c=8;
//        int d= 9;

        System.out.println(dynamicProgram(v,w,b,c,d));
    }

    //动态规划解决二维背包问题
    public static ArrayList<Integer> dynamicProgram(int[] v, int[] w, int[] b, int c, int d){



        //定义三个背包的重量
        int[] weight = w;
        //第二维的重量限制
        int[] weight2 = b;

        //定义三个背包的价值
        int[] value = v;

        //定义背包所能够承载的最大重量
        int m = c;
        //第二维的重量限制
        int m2 = d;

        int[][] dp = new int[m + 1][m2 + 1];



        for (int i = 1; i <weight.length+1; i++) {//开始遍历物品

            for (int j = m; j >= weight[i - 1]; j--) { //从大到小遍历重量
                for (int k = m2; k >= weight2[i - 1]; k--) {//对每一种重量从大到小遍历体积

                    dp[j][k] = Math.max(dp[j][k], dp[j - weight[i - 1]][k - weight2[i - 1]] + value[i - 1]);////二维状态转移方程，找到当前体积当前重量下的最大价值
                }

            }


        }

//        for(int i=0;i<dp.length;i++){
//            for(int j=0;j<dp[0].length;j++){
//                System.out.print(dp[i][j]+" ");
//            }
//            System.out.println();
//        }
        int j=0;
        int k=0;
        for(int i=0;i<dp.length;i++){
            for(int j1=0;j1<dp[0].length;j1++){
                if(dp[i][j1] ==dp[c][d]){
                   j=i;
                   k=j1;
                   break;
                }
            }
            if(j!=0 ||k!=0){
                break;
            }
        }
        //打印方案
//        int[] x = new int[weight.length];
//        int o=0;
        ArrayList<Integer> x = new ArrayList<>();


        for(int i = weight.length-1;i>=0;i--){
            if(dp[j][k] ==0){
                break;
            }
            if((j - weight[i])>=0 && (k - weight2[i]>=0) ){
                if(dp[j][k] == (dp[ j - weight[i] ][k - weight2[i]] + value[i]) ){
                    x.add(i);
                    j=j-weight[i];
                    k=k-weight2[i];
                }
            }
        }
        return x;



    }
}
