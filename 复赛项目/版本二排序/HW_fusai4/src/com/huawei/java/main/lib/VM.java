package com.huawei.java.main.lib;

public class VM {
    private String model;
    private int num_cpu;
    private int RAM;
    private int nodetype;
    private int id=0;

    public VM(String model, int num_cpu, int RAM, int nodetype,int id)  {
        setModel(model);
        setNodetype(nodetype);
        setNum_cpu(num_cpu);
        setRAM(RAM);
        setId(id);
    }

    @Override
    public String toString() {
        return "VM{" +
                "model='" + model + '\'' +
                ", num_cpu=" + num_cpu +
                ", RAM=" + RAM +
                ", nodetype=" + nodetype +
                '}';
    }

//    public VM() throws Exception {
//        throw new Exception("传入参数为空，无法创建");
//    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public int getNodetype() {
        return nodetype;
    }

    public void setNodetype(int nodetype) {
        this.nodetype = nodetype;
    }
    //    public void setNodetype(int nodetype) throws Exception{
//        if(nodetype==1||nodetype==2){
//            this.nodetype = nodetype;
//        }
//        else{
//            System.out.println("请输入1/2节点");
//            throw new Exception("请输入1/2节点");
//        }
//    }


    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VM vm = (VM) o;

        if (num_cpu != vm.num_cpu) return false;
        if (RAM != vm.RAM) return false;
        if (nodetype != vm.nodetype) return false;
        return model != null ? model.equals(vm.model) : vm.model == null;
    }

    @Override
    public int hashCode() {
        int result = model != null ? model.hashCode() : 0;
        result = 31 * result + num_cpu;
        result = 31 * result + RAM;
        result = 31 * result + nodetype;
        return result;
    }

}
