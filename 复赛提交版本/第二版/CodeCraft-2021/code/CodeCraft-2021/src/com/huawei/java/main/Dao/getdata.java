package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class getdata {

    public static BufferedReader bufferedReader = null;

    public static Data dataloader() {
        Data data=new Data();
        ArrayList<String> Serversinfo = new ArrayList<>();
        ArrayList<String> vminfo = new ArrayList<>();
//        String encoding = "utf-8";

        try {
            bufferedReader =
                    new BufferedReader(new InputStreamReader(System.in));
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
            }
            data.setServersinfo(Serversinfo);
            data.setVminfo(vminfo);
            data.setSequence(sequence);

        } catch (Exception e) {
            System.err.println("Error:" + e.getMessage());
        }
        return data;
    }

    public static ArrayList<String> getOneDayRequest(){
        ArrayList<String>requests= null;
        String line = null;
        try {
            line = bufferedReader.readLine();
            int numofrequest=Integer.parseInt(line);
//                    System.out.println(numofrequest);
            requests = new ArrayList<>();
            for(int j=0;j<numofrequest;j++){
                requests.add(bufferedReader.readLine());
            }
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
            System.out.println(line);
            e.printStackTrace();
            System.exit(0);
        }
        return requests;
    }





//    public static Data dataloader(String filePath) {
//        Data data=new Data();
//        try {
//            ArrayList<String> Serversinfo = new ArrayList<>();
//            ArrayList<String> vminfo = new ArrayList<>();
//            String encoding = "utf-8";
//            File file = new File(filePath);
//            if (file.isFile() && file.exists()) { //判断文件是否存在
//                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
//                BufferedReader bufferedReader = new BufferedReader(read);
//                //读取所有服务器的信息
//                int numofservers=Integer.parseInt(bufferedReader.readLine());
////                System.out.println(numofservers);
//                for (int i = 0; i < numofservers; i++) {
//                    Serversinfo.add(bufferedReader.readLine());
//                }
//                //读取所有虚拟机的信息
//                int numofvm=Integer.parseInt(bufferedReader.readLine());
////                System.out.println(numofvm);
//                for(int i=0;i<numofvm;i++){
//                    vminfo.add(bufferedReader.readLine());
//                }
//                //每天一个arraylist，get所有的天数，创建请求序列
//                //get总共有多少天
//                int numofday=Integer.parseInt(bufferedReader.readLine());
////                System.out.println(numofday);
//                ArrayList<String> sequence[] = new ArrayList[numofday];
//                //遍历后续每天
//                for (int i = 0; i <numofday ; i++) {
//                    int numofrequest=Integer.parseInt(bufferedReader.readLine());
////                    System.out.println(numofrequest);
//                    ArrayList<String>requests=new ArrayList<>();
//                    for(int j=0;j<numofrequest;j++){
//                        requests.add(bufferedReader.readLine());
//                    }
//                    sequence[i]= requests;
//                }
//                data.setServersinfo(Serversinfo);
//                data.setVminfo(vminfo);
//                data.setSequence(sequence);
//                read.close();
//
//            } else {
//                System.out.println("找不到指定的文件");
//                return new Data();
//            }
//        } catch (Exception e) {
//            System.out.println("读取文件内容出错");
//            e.printStackTrace();
//        }
//        return data;
//    }

}

