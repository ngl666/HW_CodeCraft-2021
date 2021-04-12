package com.huawei.java.main;

import com.huawei.java.main.Dao.Tools;

public class Main {



    public static void main(String[] args) {
        Tools.downLoad();
        //统计所有请求
        Tools.statisticsRequest();
        //Tools.sortServerForVM();
        Tools.sortVmForServer();
//        Tools.findBestVmforServer();
        Tools.processRequests2();
    }
}
