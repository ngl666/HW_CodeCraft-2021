package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.Data;

import java.io.*;
import java.util.ArrayList;

public class getdata {
    //保存全部请求
    public static ArrayList<ArrayList<String>> allRequest = new ArrayList<>();
    public static Data dataloader(String filePath) {
        Data data=new Data();
        try {
            ArrayList<String> Serversinfo = new ArrayList<>();
            ArrayList<String> vminfo = new ArrayList<>();
            String encoding = "utf-8";
            File file = new File(filePath);
            if (file.isFile() && file.exists()) { //判断文件是否存在
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                //读取所有服务器的信息
                int numofservers=Integer.parseInt(bufferedReader.readLine());
//                System.out.println(numofservers);
                for (int i = 0; i < numofservers; i++) {
                    Serversinfo.add(bufferedReader.readLine());
                }
                //读取所有虚拟机的信息
                int numofvm=Integer.parseInt(bufferedReader.readLine());
//                System.out.println(numofvm);
                for(int i=0;i<numofvm;i++){
                    vminfo.add(bufferedReader.readLine());
                }
                //每天一个arraylist，get所有的天数，创建请求序列
                //get总共有多少天T,和一次给多少天的信息 K
                String[] day = bufferedReader.readLine().split(" ");
                int numofday=Integer.parseInt(day[0]);
//                System.out.println(numofday);
                Tools.dayNumber = numofday;

                int numOfKownDay = Integer.parseInt(day[1]);

                Tools.kownDayNumber = numOfKownDay;
                //保存前k天的请求
                ArrayList<ArrayList<String>> sequence = new ArrayList<>();
                //遍历前k天的请求
                for (int i = 0; i <numOfKownDay ; i++) {
                    int numofrequest=Integer.parseInt(bufferedReader.readLine());
//                    System.out.println(numofrequest);
                    ArrayList<String>requests=new ArrayList<>();
                    for(int j=0;j<numofrequest;j++){
                        requests.add(bufferedReader.readLine());
                    }
                    sequence.add(requests);
                    allRequest.add(requests);
                }
                for (int i = numOfKownDay; i <numofday ; i++) {
                    int numofrequest=Integer.parseInt(bufferedReader.readLine());
//                    System.out.println(numofrequest);
                    ArrayList<String>requests=new ArrayList<>();
                    for(int j=0;j<numofrequest;j++){
                        requests.add(bufferedReader.readLine());
                    }
                    allRequest.add(requests);
                }
                data.setServersinfo(Serversinfo);
                data.setVminfo(vminfo);
                data.setSequence(sequence);
                read.close();

            } else {
                System.out.println("找不到指定的文件");
                return new Data();
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }
        return data;
    }


    //获取第i天的请求
    public static ArrayList<String> getOneDayRequest(int i){
        return allRequest.get(i);
    }

}

