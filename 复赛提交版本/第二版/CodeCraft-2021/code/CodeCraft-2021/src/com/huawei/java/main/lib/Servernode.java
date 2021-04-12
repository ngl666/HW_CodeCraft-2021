package com.huawei.java.main.lib;

import java.util.ArrayList;

public class Servernode implements Comparable{
    private int num_cpu;
    private int RAM;
    private String nodename;
    //被占用多少
    private int cpuisoccupied;
    private int RAMisoccupied;
    //编号，与它的server一致
    private int index;

    //目前部署的VM 编号列表
    private ArrayList<Integer> vmlist ;

    public Servernode(int num_cpu, int RAM, String nodename, int cpuisoccupied , int RAMisoccupied,int index) {
        this.num_cpu = num_cpu;
        this.RAM = RAM;
        this.nodename = nodename;
        this.cpuisoccupied = cpuisoccupied;
        this.RAMisoccupied = RAMisoccupied;
        this.vmlist = new ArrayList<>();
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Servernode(int num_cpu, int RAM, String nodename, int index) {
        this.num_cpu=num_cpu;
        this.RAM=RAM;
        this.nodename=nodename;
        this.cpuisoccupied=0;
        this.RAMisoccupied=0;
        this.vmlist = new ArrayList<>();
        this.index = index;

    }

    public String getNodename() {
        return nodename;
    }


    public void setNodename(String nodename) {
        this.nodename = nodename;
    }

    public int getNum_cpu() {
        return num_cpu;
    }

    public void setNum_cpu(int num_cpu) {
        this.num_cpu = num_cpu;
    }

    public int getRAM() {
        return RAM;
    }

    public void setRAM(int RAM) {
        this.RAM = RAM;
    }

    public int getCpuisoccupied() {
        return cpuisoccupied;
    }

    public void setCpuisoccupied(int cpuisoccupied) {
        this.cpuisoccupied = cpuisoccupied;
    }

    public int getRAMisoccupied() {
        return RAMisoccupied;
    }

    public void setRAMisoccupied(int RAMisoccupied) {
        this.RAMisoccupied = RAMisoccupied;
    }

    public ArrayList<Integer> getVmlist() {
        return vmlist;
    }

    public void setVmlist(ArrayList<Integer> vmlist) {
        this.vmlist = vmlist;
    }


    /**
     * 往该节点添加vm
     * @param id 所添加的vm id
     */
    public void addVm(int id ){
        this.vmlist.add(id);

    }

    public double getCpuRatio(){
        return  this.cpuisoccupied*1.0/this.num_cpu;
    }
    public double getRamRatio(){
        return  this.RAMisoccupied*1.0/this.RAM;
    }





    @Override
    public String toString() {
        return "Servernode{" +
                "num_cpu=" + num_cpu +
                ", RAM=" + RAM +
                ", nodename='" + nodename + '\'' +
                ", cpuisoccupied=" + cpuisoccupied +
                ", RAMisoccupied=" + RAMisoccupied +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Servernode that = (Servernode) o;

        if (num_cpu != that.num_cpu) return false;
        if (RAM != that.RAM) return false;
        if (cpuisoccupied != that.cpuisoccupied) return false;
        if (RAMisoccupied != that.RAMisoccupied) return false;
        if (index != that.index) return false;
        if (nodename != null ? !nodename.equals(that.nodename) : that.nodename != null) return false;
        return vmlist != null ? vmlist.equals(that.vmlist) : that.vmlist == null;
    }

    @Override
    public int hashCode() {
        int result = num_cpu;
        result = 31 * result + RAM;
        result = 31 * result + (nodename != null ? nodename.hashCode() : 0);
        result = 31 * result + cpuisoccupied;
        result = 31 * result + RAMisoccupied;
        result = 31 * result + index;
        result = 31 * result + (vmlist != null ? vmlist.hashCode() : 0);
        return result;
    }
}
