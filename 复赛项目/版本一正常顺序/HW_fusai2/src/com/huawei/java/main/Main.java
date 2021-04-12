package com.huawei.java.main;

import com.huawei.java.main.Dao.Tools;

public class Main {



    public static void main(String[] args) {
        Tools.downLoad();
        Tools.sortServerForVM();
        Tools.processRequests();
    }
}
