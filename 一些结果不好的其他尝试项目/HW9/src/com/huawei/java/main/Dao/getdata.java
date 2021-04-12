package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class getdata {
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
                //get总共有多少天
                int numofday=Integer.parseInt(bufferedReader.readLine());
//                System.out.println(numofday);
                Tools.dayNumber = numofday;
                ArrayList<String> sequence[] = new ArrayList[numofday];
                //遍历后续每天
                for (int i = 0; i <numofday ; i++) {
                    int numofrequest=Integer.parseInt(bufferedReader.readLine());
//                    System.out.println(numofrequest);
                    ArrayList<String>requests=new ArrayList<>();
                    for(int j=0;j<numofrequest;j++){
                        requests.add(bufferedReader.readLine());
                    }
                    sequence[i]= requests;
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

}

