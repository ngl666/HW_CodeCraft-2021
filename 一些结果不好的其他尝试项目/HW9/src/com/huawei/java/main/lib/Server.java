package com.huawei.java.main.lib;

public class Server implements Comparable{
    private int num_cpu;
    private int RAM;
    private String model;
    private int Hardware_cost;
    private int perday_cost;
    private boolean isopen;
    private Servernode node1;
    private Servernode node2;
    private int index;
    public Server(int num_cpu, int RAM, String model, int hardware_cost, int perday_cost) {
        this.num_cpu = num_cpu;
        this.RAM = RAM;
        this.model = model;
        Hardware_cost = hardware_cost;
        this.perday_cost = perday_cost;
        this.node1=new Servernode(num_cpu/2,RAM/2,"A",this.index);
        this.node2=new Servernode(num_cpu/2,RAM/2,"B",this.index);
        this.isopen=false;
    }


    public int getNum_cpu() {
        return num_cpu;
    }

    public void setNum_cpu(int num_cpu) {
        this.num_cpu = num_cpu;
    }

    public Servernode getNode1() {
        return node1;
    }

    public void setNode1(Servernode node1) {
        this.node1 = node1;
    }

    public Servernode getNode2() {
        return node2;
    }

    public void setNode2(Servernode node2) {
        this.node2 = node2;
    }

    public int getRAM() {
        return RAM;
    }

    public void setRAM(int RAM) {
        this.RAM = RAM;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getHardware_cost() {
        return Hardware_cost;
    }

    public void setHardware_cost(int hardware_cost) {
        Hardware_cost = hardware_cost;
    }

    public int getPerday_cost() {
        return perday_cost;
    }

    public void setPerday_cost(int perday_cost) {
        this.perday_cost = perday_cost;
    }


    public boolean isIsopen() {
        return isopen;
    }

    public void setIsopen(boolean isopen) {
        this.isopen = isopen;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        this.getNode1().setIndex(index);
        this.getNode2().setIndex(index);
    }

    @Override
    public String toString() {
        return "Server{" +
                "num_cpu=" + num_cpu +
                ", RAM=" + RAM +
                ", model='" + model + '\'' +
                ", Hardware_cost=" + Hardware_cost +
                ", perday_cost=" + perday_cost +
                ", node1=" + node1 +
                ", node2=" + node2 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (num_cpu != server.num_cpu) return false;
        if (RAM != server.RAM) return false;
        if (Hardware_cost != server.Hardware_cost) return false;
        if (perday_cost != server.perday_cost) return false;
        if (isopen != server.isopen) return false;
        if (index != server.index) return false;
        if (model != null ? !model.equals(server.model) : server.model != null) return false;
        if (node1 != null ? !node1.equals(server.node1) : server.node1 != null) return false;
        return node2 != null ? node2.equals(server.node2) : server.node2 == null;
    }

    @Override
    public int hashCode() {
        int result = num_cpu;
        result = 31 * result + RAM;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + Hardware_cost;
        result = 31 * result + perday_cost;
        result = 31 * result + (isopen ? 1 : 0);
        result = 31 * result + (node1 != null ? node1.hashCode() : 0);
        result = 31 * result + (node2 != null ? node2.hashCode() : 0);
        result = 31 * result + index;
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
