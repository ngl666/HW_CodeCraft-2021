package com.huawei.java.main.lib;

import java.util.ArrayList;
import java.util.Arrays;

public class Data {
    private ArrayList Serversinfo;
    private ArrayList vminfo;
    private ArrayList<ArrayList<String> >sequence;

    public ArrayList getServersinfo() {
        return Serversinfo;
    }

    public void setServersinfo(ArrayList serversinfo) {
        Serversinfo = serversinfo;
    }

    public ArrayList getVminfo() {
        return vminfo;
    }

    public void setVminfo(ArrayList vminfo) {
        this.vminfo = vminfo;
    }

    public ArrayList<ArrayList<String>> getSequence() {
        return sequence;
    }

    public void setSequence(ArrayList<ArrayList<String> >sequence) {
        this.sequence = sequence;
    }
}
