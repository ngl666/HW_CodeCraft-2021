package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.Data;
import com.huawei.java.main.lib.Server;


import com.huawei.java.main.lib.Servernode;
import com.huawei.java.main.lib.VM;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Array;
import java.util.*;


public class Tools {
    public static int maxCpu;
    public static int maxRam;
    //server根据cpu、ram排序，ArrayList<String>  [型号，cpu,ram]
    public static TreeSet<ArrayList<String>> cpuTreeSet ;
    public static TreeSet<ArrayList<String>> ramTreeSet ;



    public  static HashMap<Integer, ArrayList<String>> allOutData = new HashMap<>();

    //按cpu+ram/价格，对服务器型号排序
    public static ArrayList<String>  serverCostPerformanceOrder =new ArrayList<>();

    //所有add的VM信息<vmid,[第几天，型号，存在多少天]>
    public static HashMap<Integer,ArrayList<String>> allAddVm = new HashMap<>();
    //<第几天，[这一天所有add的VM]>
    public static HashMap<Integer,ArrayList<Integer>> dailyAddVm = new HashMap<>();
    //<第几天，[这一天所有del的VM]>
    public static HashMap<Integer,ArrayList<Integer>> dailyDelVm = new HashMap<>();
    //vm平均活多少天
    public static double vmAverageLiveDay=0;
    //vm存活天数的中位数
    public static int vmMedianLiveDay = 0;
    //<Server类型，<按性价比函数计算的选该类服务器最适配的vm顺序>>
    public static HashMap<String, TreeSet<VM>> mostCostEffectiveVmOrder = new HashMap<>();
    //<Server类型，<该服务器在一个节点放置，可以使占有率最高的放置方式>>
    public static HashMap<String, ArrayList<VM>> FinalMostCostEffectiveVmOrder = new HashMap<>();



    //判断是哪种类型的数据、饱和型 1、不饱和型 2
    public static int dataType =0;

    //记录一共有多少天
    public static int dayNumber = 0;

    //第二版添加<VM类型，<按性价比函数计算的对于该类VM选服务器时的顺序>>
    public static HashMap<String, TreeSet<Server>> mostCostEffectiveServerOrder = new HashMap<>();

    //编号计数,用于编号映射hashMap <申请时服务器的编号, 输出时服务器的编号>
    public static int numberCount = 0;

    //当前所有已购买服务器,有唯一id,购买时生成
//    public static HashMap<Integer, Server> currentAllServers = new HashMap<>();
    public static ArrayList<Server> currentAllServers = new ArrayList<>();//根据服务器编号获取服务器类
    public static ArrayList<ArrayList<Integer>>  dailyBuyServers =new ArrayList<>();//根据第几天获取第几天所购买的服务器id
    public static HashMap<Integer,ArrayList<Integer>> VMidInServer = new HashMap<>(); // <VM编号，[所部署的Server编号,部署的节点]> 1，2，表示节点，3表示双节点

    //当前所有已部署虚拟机，add请求时添加<VM编号,VM类>
    public static HashMap<Integer, VM> currentVm = new HashMap<>();
    public static ArrayList<ArrayList<Integer>> dailyArrangeVM = new ArrayList<>();//根据第几天获取第几天部署，第几天删除 ,可能当天创建后当天删除

//    public static ArrayList<VM> currentVm = new ArrayList<>();

    //编号映射hashMap <申请时服务器的编号, 输出时服务器的编号>
    public static HashMap<Integer,Integer> severNumberMapping = new HashMap<>();

    //所有可选服务器类<服务器类型，服务器类>
    public static HashMap<String, Server> allServerObject = new HashMap<>();
    //所有可选虚拟机类<虚拟机类型，虚拟机类>
    public static HashMap<String, VM> allVmObject = new HashMap<>();
    //所有请求
    public static ArrayList<String>[]op;

    //加载数据
    public static void downLoad(){
        //得到所有数据
//        Data data= getdata.dataloader("D:\\project\\Idea_project\\HW5\\src\\com\\huawei\\java\\main\\Dao\\training-1.txt");
        Data data= getdata.dataloader("D:\\project\\Idea_project\\HW5\\src\\com\\huawei\\java\\main\\Dao\\training-2.txt");
        //所有可选服务器
        ArrayList<String>allserinfo=data.getServersinfo();
        //所有可选服务器类<服务器类型，服务器类>
//        HashMap<String, Server> allServerObject = new HashMap<>();
        for(String server :allserinfo){
            //去括号
            server = server.substring(1,server.length()-1);
            server =server.replaceAll(" ","");
            String[] info = server.split(",");
            String model =info[0];
            int num_cpu = Integer.parseInt(info[1]);
            int RAM = Integer.parseInt(info[2]);
            int hardware_cost =Integer.parseInt(info[3]);
            int perday_cost = Integer.parseInt(info[4]);

            Server serverObject = new Server(num_cpu,RAM, model,hardware_cost,perday_cost);
            allServerObject.put(model,serverObject);
        }
//        System.out.println(allServerObject);


        //所有可选虚拟机
        ArrayList<String>allvminfo=data.getVminfo();

        for(String vm :allvminfo){
            //去括号
            vm = vm.substring(1,vm.length()-1);
            vm = vm.replaceAll(" ","");
            String model = vm.split(",")[0];
            int num_cpu = Integer.parseInt(vm.split(",")[1]);
            int RAM = Integer.parseInt(vm.split(",")[2]);
            int nodetype = Integer.parseInt(vm.split(",")[3]);
            try {
                VM vmObject = new VM( model,num_cpu,RAM,nodetype,0);
                allVmObject.put(model,vmObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        System.out.println(allVmObject);
        //计算服务器性价比队列
        //因为cpu对价格影响比ram大
        TreeSet<String> serverSet = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if(o1!=null &&o2!=null){
                    Server server1= allServerObject.get(o1);
                    Server server2= allServerObject.get(o2);
                    double den1 = 1.0*(server1.getHardware_cost()+server1.getPerday_cost() *vmMedianLiveDay)/(server1.getNum_cpu()*0.7+server1.getRAM()*0.3);//vm存活的中位数
                    double den2 = 1.0*(server2.getHardware_cost()+server2.getPerday_cost() *vmMedianLiveDay)/(server2.getNum_cpu()*0.7+server2.getRAM()*0.3);
                    return  (int)(den1 -den2)==0? 1: (int)(den1-den2);
                }
                return 0;
            }
        });
        for(String serverName :allServerObject.keySet()){
            serverSet.add(allServerObject.get(serverName).getModel());
        }
        Iterator iterator =serverSet.iterator();
        while(iterator.hasNext()){
            serverCostPerformanceOrder.add((String) iterator.next());
        }
        //所有请求
        op=data.getSequence();
    }



    //进行迁移操作
    public static ArrayList<ArrayList<Integer>> move(int canMoveNumber){
        if(canMoveNumber<=0){
            return null;
        }
        //按cpu占用率 与ram占用率 之差 从小到大  排好序的节点
        TreeSet<Servernode> currentAllServerNodesOrder ;//根据服务器编号获取服务器类

        currentAllServerNodesOrder  = new TreeSet(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if(o1 instanceof Servernode && o2 instanceof Servernode){
//                    return (int ) ((((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) *100);
                    int i= (int ) (((  Math.abs(((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) )  - (Math.abs(((Servernode) o2).getCpuRatio() -((Servernode) o2).getRamRatio()) ))*10000 );
                    if(i==0){
                        return -1;
                    }else {
                        return -i;
                    }


                }
                return 0;
            }
        });
        for(int k=0;k<currentAllServers.size();k++) {
            Server oneServer = currentAllServers.get(k);
            currentAllServerNodesOrder.add(oneServer.getNode1());
            currentAllServerNodesOrder.add(oneServer.getNode2());
        }

        //格式为(虚拟机 ID, 目的服务器 ID)或 (虚拟机 ID, 目的服务器 ID, 目的服务器节点)。
        ArrayList<ArrayList<Integer>> realMove = new ArrayList<>();
        for(int i=0;i<canMoveNumber;i++){
            //得到cup占用率与RAM占用率相差最大的节点，确定移该节点上的vm,再找该节点上，vmcpu/nodecpu 与vmram/noderam ,差最大的那个
            Servernode servernode = currentAllServerNodesOrder.first();
            Server server = currentAllServers.get(servernode.getIndex());
            //获取该节点上的vm列表
            ArrayList<Integer> vmlist =servernode.getVmlist();

            //找vmcpu/nodecpu 与vmram/noderam ,差最大的那个
            VM  maxDifferenceVm  =null;
            double difference =-1;
//            System.out.println(vmlist);
            for(int j =0;j< vmlist.size();j++){
                VM vm =currentVm.get(vmlist.get(j));
//                System.out.println(vm.getNum_cpu());
//                System.out.println(servernode.getNum_cpu());
//                System.out.println( vm.getRAM());
//                System.out.println(servernode.getRAM());
                double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getNum_cpu()) - vm.getRAM() *1.0/ servernode.getRAM());
                if(  t > difference){
                    maxDifferenceVm = vm;
                    difference = t;
                }
            }
            //移maxDifferenceVm,移到哪？移到目前所剩下的节点中vmcpu/nodecpu 与vmram/noderam 与它最接近的那个节点，且要可以移，还要考虑单双节点
            if(maxDifferenceVm==null){
                currentAllServerNodesOrder.pollFirst();
                continue;
            }

            //目标节点
            ArrayList<Servernode> maxServerNode = isCanMove(maxDifferenceVm);

            if(maxServerNode.get(0) == null){
                //没有可迁移的目的服务器
                currentAllServerNodesOrder.pollFirst();
                continue;
            }else {
                //
//                if(maxServerNode.get(0).getIndex() == servernode.getIndex()  && maxServerNode.get(0).getNodename().equals(servernode.getNodename())){
//                    System.out.println("ssss");
//                }

                if(maxDifferenceVm.getNodetype() ==0){
                    //单节点,删除一个
                    currentAllServerNodesOrder.pollFirst();

                }else {
                    //双节点，删除两个
                    currentAllServerNodesOrder.pollFirst();
                    Iterator iterator = currentAllServerNodesOrder.iterator();
                    int serverIndex = servernode.getIndex();
                    while(iterator.hasNext()){
                        if(( (Servernode) iterator.next()).getIndex() == serverIndex){
                            iterator.remove();
                        }
                    }

                }
                //重新部署
                int[]  newServerId =vmMoveOnCurrentServer(maxDifferenceVm,maxServerNode);
                if(newServerId[0] < 0){
                    currentAllServerNodesOrder.pollFirst();
                    continue;
                }else {
                    if(newServerId[1] ==1){
                        //移到节点一
                        ArrayList<Integer> move = new ArrayList<>();
                        move.add(maxDifferenceVm.getId());
                        move.add(newServerId[0]);
                        move.add(1);
                        realMove.add(move);

                        //删除的时候currentAllServers 已经变了
                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1();
//                        currentAllServerNodesOrder.add(servernode);


                    }else if(newServerId[1]==2){
                        //移到节点二
                        ArrayList<Integer> move = new ArrayList<>();
                        move.add(maxDifferenceVm.getId());
                        move.add(newServerId[0]);
                        move.add(2);
                        realMove.add(move);
                        //删除的时候currentAllServers 已经变了
                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2();
//                        currentAllServerNodesOrder.add(servernode);

                    }else if(newServerId[1]==3){
                        //双节点
                        ArrayList<Integer> move = new ArrayList<>();
                        move.add(maxDifferenceVm.getId());
                        move.add(newServerId[0]);
                        move.add(3);
                        realMove.add(move);
                        //移动后要变更currentAllServerNodesOrder
//                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1());
//                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2());


                    }


                }


            }


        }
        return realMove;


    }

    //判断是否可以迁移 可以返回要迁移的目的servernode,单节点[目标节点，null],双节点[同一服务器节点1，节点2]
    public static ArrayList<Servernode> isCanMove(VM addVm){
        //倒着往上找 找到第一个可以放的就放进去
        // currentAllServerNodesOrder
        //找addVm加入后该节点两个占用率最好的那个服务器
        double maxServerRatio = 0;
        ArrayList<Servernode> maxServerNode = new ArrayList<>();
        maxServerNode.add(null);
        maxServerNode.add(null);
        for(int k=0;k<currentAllServers.size();k++) {
            Server oneServer = currentAllServers.get(k);
            int[] isArray = Tools.isCanArrange(oneServer, addVm);
            if(addVm.getNodetype() ==0){

                //判断该服务器是否可以满足部署条件
                if(isArray[0] ==1){

                    //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                    Servernode servernode1 =oneServer.getNode1();
                    Servernode servernode2 =oneServer.getNode2();
                    //已经在该节点上部署，不能往该节点上迁移
                    if(!servernode1.getVmlist().contains(addVm.getId())){
                        double cpuRatio = 0.5 *(servernode1.getCpuisoccupied()+addVm.getNum_cpu())*1.0 / servernode1.getNum_cpu()  +0.5 *servernode2.getCpuisoccupied()  *1.0 / servernode2.getNum_cpu();

                        double ramRatio = 0.5 *(servernode1.getRAMisoccupied() +addVm.getRAM())*1.0 / servernode1.getRAM()  +0.5 *servernode2.getRAMisoccupied() *1.0 / servernode2.getRAM();

                        double serverRatio = 0.5*cpuRatio + 0.5*ramRatio;
                        if(serverRatio>maxServerRatio){
                            maxServerNode.set(0,servernode1);

                            maxServerRatio = serverRatio;
                        }
                    }


                }
                if(isArray[1] ==1){

                    //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                    Servernode servernode1 =oneServer.getNode1();
                    Servernode servernode2 =oneServer.getNode2();
                    //已经在该节点上部署，不能往该节点上迁移
                    if(!servernode2.getVmlist().contains(addVm.getId())){
                        double cpuRatio = 0.5 *servernode1.getCpuisoccupied()*1.0 / servernode1.getNum_cpu()  +0.5 *(servernode2.getCpuisoccupied() + addVm.getNum_cpu()) *1.0 / servernode2.getNum_cpu();

                        double ramRatio = 0.5 *servernode1.getRAMisoccupied() *1.0 / servernode1.getRAM()  +0.5 *(servernode2.getRAMisoccupied() +addVm.getRAM())*1.0 / servernode2.getRAM();

                        double serverRatio = 0.5*cpuRatio + 0.5*ramRatio;
                        if(serverRatio>maxServerRatio){
                            maxServerNode.set(0,servernode2);
                            maxServerRatio = serverRatio;
                        }
                    }



                }

            }else {
                if(isArray[2] ==1){
                    //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                    Servernode servernode1 =oneServer.getNode1();
                    Servernode servernode2 =oneServer.getNode2();
                    //已经在该节点上部署，不能往该节点上迁移
                    if(!servernode1.getVmlist().contains(addVm.getId())){
                        double cpuRatio = 0.5 *(servernode1.getCpuisoccupied()+addVm.getNum_cpu()*1.0/2)*1.0 / servernode1.getNum_cpu()  +0.5 *(servernode2.getCpuisoccupied() + addVm.getNum_cpu()*1.0/2) *1.0 / servernode2.getNum_cpu();

                        double ramRatio = 0.5 *(servernode1.getRAMisoccupied() +addVm.getRAM()*1.0/2)*1.0 / servernode1.getRAM()  +0.5 *(servernode2.getRAMisoccupied() +addVm.getRAM()*1.0/2)*1.0 / servernode2.getRAM();

                        double serverRatio = 0.5*cpuRatio + 0.5*ramRatio;
                        if(serverRatio > maxServerRatio){
                            maxServerNode.set(0,servernode1);
                            maxServerNode.set(1,servernode2);
                            maxServerRatio = serverRatio;
                        }
                    }


                }

            }


        }
        return maxServerNode;




//        for(Servernode servernode : currentAllServerNodesOrder){
//            if(servernode.getVmlist().contains(addVm.getId())){
//                continue;
//            }
////            if(addVm.getNodetype() == 0){
////                //该节点是否可以部署
////
////                int[] isArray = Tools.isCanArrange(oneServer, addVm);
////                if(isArray[0] ==1 ||isArray[1] ==1 || isArray[2] ==1){
////                    return oneServer;
////                }
////            }
//            Server oneServer = currentAllServers.get(servernode.getIndex());
//            int[] isArray = Tools.isCanArrange(oneServer, addVm);
//            if(isArray[0] ==1 ||isArray[1] ==1 || isArray[2] ==1){
//                return oneServer;
//            }
//        }
//        return null;

//        double vmGpu =  addVm.getNum_cpu();
//        double vmRam =  addVm.getRAM();
//        TreeSet serverSet = new TreeSet(new Comparator() {
//            @Override
//            public int compare(Object o1, Object o2) {
//                if(o1 instanceof Server && o2 instanceof Server){
//                    Server server1 = (Server) o1;
//                    Server server2 = (Server) o2;
//                    double server1Gpu = server1.getNum_cpu() -server1.getNode1().getCpuisoccupied()-server1.getNode2().getCpuisoccupied();
//                    double server1Ram = server1.getRAM() -server1.getNode1().getRAMisoccupied() -server1.getNode2().getRAMisoccupied();
//                    double server1Price = server1.getHardware_cost() ;
//                    double server2Gpu = server2.getNum_cpu()-server2.getNode1().getCpuisoccupied()-server2.getNode2().getCpuisoccupied();
//                    double server2Ram = server2.getRAM() -server2.getNode1().getRAMisoccupied() -server2.getNode2().getRAMisoccupied();
//                    double server2Price = server2.getHardware_cost() ;
//
//
//                    //服务器对于vm的单位费用，
////                    double den1 = (server1Price * ((vmGpu/server1Gpu  ) + (vmRam/server1Ram)))/2;
////                    double den2 = (server2Price * ((vmGpu/server2Gpu  ) + (vmRam/server2Ram)))/2;
//                    double den1 = (server1Price * Math.max(vmGpu/server1Gpu   , vmRam/server1Ram));
//                    double den2 = (server2Price * Math.max(vmGpu/server2Gpu   , vmRam/server2Ram));
////                    double den1 = server1Price * (Math.abs(vmGpu/server1Gpu -vmRam/server1Ram));
////                    double den2 = server2Price * (Math.abs(vmGpu/server2Gpu -vmRam/server2Ram));
//                    int t =(int ) (den1-den2);
//
//                    return  t== 0 ? 1 :t;
//                }
//
//                return 0;
//            }
//        });
//
//        for(int k=0;k<currentAllServers.size();k++) {
//            Server oneServer = currentAllServers.get(k);
//            //判断该服务器是否可以满足部署条件
//            int[] isArray = Tools.isCanArrange(oneServer, addVm);
//
//            if(isArray[0] ==1 ||isArray[1] ==1 || isArray[2] ==1){
//                serverSet.add(oneServer);
//            }
//        }
//        if(serverSet.size()<1){
//            return null;
//        }else {
//            return (Server) serverSet.first();
//        }
    }






    /**
     * 迁移某个vm
     * @param addVm 要迁移的vm
     * @return 返回move 要的server id,哪个节点，1，2，表示节点，3表示双节点  0,0,0代表当前没有服务器可以移
     */
    public static int[]  vmMoveOnCurrentServer(VM addVm,ArrayList<Servernode> oneServerNode){


        int addVmNodetype =addVm.getNodetype();
        //不用判断该服务器是否可以满足部署条件，前面已经判断，现在可以直接填写
        Server oneServer = currentAllServers.get(oneServerNode.get(0).getIndex());
        if(addVmNodetype == 0){//单节点

            if(oneServerNode.get(0).getNodename().equals("A")){
                //在已有服务器节点1上部署
                oneServer =Tools.moveVm(addVm,oneServer,1);
                currentAllServers.set(oneServer.getIndex(),oneServer);
                return new int[]{oneServer.getIndex(),1};
            }else {
                //在已有服务器节点2上部署
                oneServer =Tools.moveVm(addVm,oneServer,2);
                currentAllServers.set(oneServer.getIndex(),oneServer);

                return  new int[]{oneServer.getIndex(),2};
            }

        }else if(addVmNodetype == 1){//双节点
            //在已有服务器两节点上上部署
            oneServer =Tools.moveVm(addVm,oneServer,3);
            currentAllServers.set(oneServer.getIndex(),oneServer);
            return new int[]{oneServer.getIndex(),3};

        }
        return new int[]{-1,0};

    }
    /**
     *
     * @param vm 需要移动的vm
     * @param server 移动在该服务器上
     * @param nodeNumber 部署在该节点上 1，2，表示节点，3表示双节点
     * @return 已移动的后部署的server
     */
    public static Server moveVm(VM vm,Server server,int nodeNumber){

        if(nodeNumber ==1){
            //先删除
            moveDeleteVM(vm.getId());
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM());
            server.getNode1().addVm(vm.getId());


        }else if(nodeNumber == 2){
            moveDeleteVM(vm.getId());
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() +vm.getRAM());
            server.getNode2().addVm(vm.getId());

        }else if(nodeNumber == 3){
            moveDeleteVM(vm.getId());
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode1().addVm(vm.getId());
            server.getNode2().addVm(vm.getId());

        }else {
            System.out.println("nodeNumber 指定错误！！！");
        }
        ArrayList<Integer> serverIdAndNodeNumber =new ArrayList<>();
        serverIdAndNodeNumber.add(server.getIndex());
        serverIdAndNodeNumber.add(nodeNumber);
        VMidInServer.replace(vm.getId(),serverIdAndNodeNumber);//替换<VM编号，[所部署的Server编号,部署的节点]>

        return server;
    }
    //删除VM
    public static void moveDeleteVM(int deleteVmId){

        ArrayList<Integer> serveridList = VMidInServer.get(deleteVmId);//部署的服务器

        int serverid =serveridList.get(0);//所删除的服务器编号
        int nodetype =serveridList.get(1);//所删除的服务器节点1，2，表示节点，3表示双节点
        Server server =currentAllServers.get(serverid);
        VM deletevm = currentVm.get(deleteVmId);
        if(nodetype ==1){
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied() - deletevm.getNum_cpu());
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied() - deletevm.getRAM());
            server.getNode1().getVmlist().remove(Integer.valueOf(deleteVmId));

        }else if(nodetype == 2){
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied() - deletevm.getNum_cpu());
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() - deletevm.getRAM());
            server.getNode2().getVmlist().remove(Integer.valueOf(deleteVmId));

        }else if(nodetype == 3){
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied() - (deletevm.getNum_cpu()/2));
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied() - (deletevm.getRAM()/2));
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied() - (deletevm.getNum_cpu()/2));
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() - (deletevm.getRAM()/2));
            server.getNode1().getVmlist().remove(Integer.valueOf(deleteVmId));
            server.getNode2().getVmlist().remove(Integer.valueOf(deleteVmId));

        }
        currentAllServers.set(server.getIndex(),server);

    }

    //遍历当前已有服务器是否可以部署当前vm,如果可以部署就部署返回True ,如果不可以部署返回false

    /**
     *
     * @param addVm 需要部署的虚拟机
     * @return 是否部署成功
     */
    public static boolean vmDeploynOnCurrentServer(VM addVm) {
        int addVmNodetype = addVm.getNodetype();
        double vmGpu = addVm.getNum_cpu();
        double vmRam = addVm.getRAM();
        TreeSet serverSet = new TreeSet(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1 instanceof Server && o2 instanceof Server) {
                    Server server1 = (Server) o1;
                    Server server2 = (Server) o2;
                    double server1Gpu = server1.getNum_cpu();
                    double server1Ram = server1.getRAM();
                    double server1Price = server1.getHardware_cost();
                    double server2Gpu = server2.getNum_cpu();
                    double server2Ram = server2.getRAM();
                    double server2Price = server2.getHardware_cost();
                    double server1daycost = server1.getPerday_cost();
                    double server2daycost = server1.getPerday_cost();


                    //服务器对于vm的单位费用，
//                    double den1 = (server1Price * ((vmGpu/server1Gpu  ) + (vmRam/server1Ram)))/2;
//                    double den2 = (server2Price * ((vmGpu/server2Gpu  ) + (vmRam/server2Ram)))/2;
                    double den1 = ((server1Price + server1daycost * 10) * Math.max(vmGpu / server1Gpu, vmRam / server1Ram));
                    double den2 = ((server2Price + server2daycost * 10) * Math.max(vmGpu / server2Gpu, vmRam / server2Ram));
//                    double den1 = server1Price * (Math.abs(vmGpu/server1Gpu -vmRam/server1Ram));
//                    double den2 = server2Price * (Math.abs(vmGpu/server2Gpu -vmRam/server2Ram));


                    int t = (int) (den1 - den2);

                    return t == 0 ? 1 : t;
//                    return t;
                }

                return 0;
            }
        });

        for (int k = 0; k < currentAllServers.size(); k++) {
            Server oneServer = currentAllServers.get(k);
            //判断该服务器是否可以满足部署条件
            int[] isArray = Tools.isCanArrange(oneServer, addVm);
            if (isArray[0] == 1 || isArray[1] == 1 || isArray[2] == 1) {
                //保存所以可以部署次VM的Server
                serverSet.add(oneServer);
            }
        }
        if (serverSet.size() < 1) {
            return false;
        } else {
            //按照性价比部署vm
            Server oneServer = (Server) serverSet.first();
            //判断该服务器是否可以满足部署条件
            int[] isArray = Tools.isCanArrange(oneServer, addVm);

            if (addVmNodetype == 0) {//单节点
                if (isArray[0] == 1) {
                    //在已有服务器节点1上部署
                    oneServer = Tools.arrangeVm(addVm, oneServer, 1);
                    ArrayList<Integer> vmInserver = new ArrayList<>();
                    vmInserver.add(oneServer.getIndex());
                    vmInserver.add(1);
                    VMidInServer.put(addVm.getId(),vmInserver);
                    currentAllServers.set(oneServer.getIndex(), oneServer);
                    return true;
                } else if (isArray[1] == 1) {
                    //在已有服务器节点2上部署
                    oneServer = Tools.arrangeVm(addVm, oneServer, 2);
                    ArrayList<Integer> vmInserver = new ArrayList<>();
                    vmInserver.add(oneServer.getIndex());
                    vmInserver.add(2);
                    VMidInServer.put(addVm.getId(),vmInserver);
                    currentAllServers.set(oneServer.getIndex(), oneServer);
                    return true;
                }

            } else if (addVmNodetype == 1) {//双节点
                if (isArray[2] == 1) {
                    //在已有服务器两节点上上部署
                    oneServer = Tools.arrangeVm(addVm, oneServer, 3);
                    ArrayList<Integer> vmInserver = new ArrayList<>();
                    vmInserver.add(oneServer.getIndex());
                    vmInserver.add(3);
                    VMidInServer.put(addVm.getId(),vmInserver);
                    currentAllServers.set(oneServer.getIndex(), oneServer);
                    return true;
                }

            }
        }
        return false;

    }











    //计算每一个VM对应的性价比服务器队列
    public static void sortServerForVM(){


        for(String vmType:allVmObject.keySet()){
            VM vm =allVmObject.get(vmType);
            double vmGpu =  vm.getNum_cpu();
            double vmRam =  vm.getRAM();
            TreeSet serverSet = new TreeSet(new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    if(o1 instanceof Server && o2 instanceof Server){
                        Server server1 = (Server) o1;
                        Server server2 = (Server) o2;
                        double server1Gpu = server1.getNum_cpu() ;
                        double server1Ram = server1.getRAM() ;
                        double server1Price = server1.getHardware_cost() ;
                        double server2Gpu = server2.getNum_cpu();
                        double server2Ram = server2.getRAM() ;
                        double server2Price = server2.getHardware_cost() ;

                        //服务器对于vm的单位费用，
//                        double den1 = (server1Price * ((vmGpu/server1Gpu  ) + (vmRam/server1Ram)))/2;
//                        double den2 = (server2Price * ((vmGpu/server2Gpu  ) + (vmRam/server2Ram)))/2;
                        double den1 = (server1Price * Math.max(vmGpu/server1Gpu   , vmRam/server1Ram));
                        double den2 = (server2Price * Math.max(vmGpu/server2Gpu   , vmRam/server2Ram));
//                        double den1 = server1Price * (Math.abs(vmGpu/server1Gpu -vmRam/server1Ram));
//                        double den2 = server2Price * (Math.abs(vmGpu/server2Gpu -vmRam/server2Ram));


                        int t =(int ) (den1-den2);

                        return  t== 0 ? 1 : t;
                    }

                    return 0;
                }
            });
            for(String serverType :allServerObject.keySet()){
                serverSet.add(allServerObject.get(serverType));
            }
            mostCostEffectiveServerOrder.put(vmType,serverSet);

        }


    }




    /**
     *
     * @param server 当前服务器
     * @param vm 需要判断是否可以部署的虚拟机
     * @return 返回isArrange[3] 单节点：isArrange[0] isArrange[1]保存服务器节点12是否可以部署VM 1为可以，，isArrange[2]服务器节点是否可以部署VM 1为可以
     */
    //判断当前sever是否可以部署当前VM,
    public static int[]  isCanArrange(Server server,VM vm){
        int needGpu = vm.getNum_cpu();
        int needRam = vm.getRAM();
        int needNodeType = vm.getNodetype();
        Servernode servernode1 =server.getNode1();
        Servernode servernode2 =server.getNode2();
        int []isArrange = new int[3];
        isArrange[0]=0;
        isArrange[1]=0;
        isArrange[2]=0;


        if(needNodeType == 0){
             if((servernode1.getNum_cpu() - servernode1.getCpuisoccupied()) >= needGpu && (servernode1.getRAM() - servernode1.getRAMisoccupied()) >= needRam){
                 isArrange[0] = 1;
             }
             if((servernode2.getNum_cpu() - servernode2.getCpuisoccupied()) >= needGpu && (servernode2.getRAM() - servernode2.getRAMisoccupied()) >= needRam){
                isArrange[1] = 1;
             }

             return isArrange;

        }else {
            if((servernode1.getNum_cpu() - servernode1.getCpuisoccupied()) >= (needGpu/2) && (servernode1.getRAM() - servernode1.getRAMisoccupied()) >= (needRam/2)
                    &&(servernode2.getNum_cpu() - servernode2.getCpuisoccupied()) >= (needGpu/2) && (servernode2.getRAM() - servernode2.getRAMisoccupied()) >= (needRam/2)){
                isArrange[2] = 1;
            }

            return isArrange;
        }

    }


    //根据虚拟机购买服务器，返回购买的服务器
    /**
     *
     *
     * @param addVm  需要部署的虚拟机
     * @param i 第i天买的
     * @return 购买的服务器
     */
    public static Server buyServer(VM addVm,int i){

        Set<String> allCanUseServerType = allServerObject.keySet();
        Server oneServer = null;//一个server类，该类为真正要部署的server
        //遍历每个可以用的服务器

        TreeSet<Server> bestServers =mostCostEffectiveServerOrder.get(addVm.getModel());
        Iterator bestServersInterator = bestServers.iterator();
        while(bestServersInterator.hasNext()){

            Server canUseServerObject = (Server) bestServersInterator.next();
            //new 一个server类，该类为真正要部署的server
            oneServer = new Server(canUseServerObject.getNum_cpu(),canUseServerObject.getRAM(),canUseServerObject.getModel(),canUseServerObject.getHardware_cost(),canUseServerObject.getPerday_cost());
            int[] isArray =Tools.isCanArrange(oneServer,addVm);
            //虚拟机部署只需要单节点
            if(addVm.getNodetype() == 0){
                if(isArray[0] == 1){//当前服务器节点1可以部署
                    //暂时可以部署就选它，以后根据算法

                    int index = currentAllServers.size();//根据当前服务器个数为编号，因为编号从0开始，当前有0台，购买第一台编号为0
                    oneServer.setIndex(index);//设置编号

                    oneServer =Tools.arrangeVm(addVm,oneServer,1,i);
                    Tools.currentAllServers.add(oneServer);
                    if(dailyBuyServers.size() <= i){//这一天还没买，构建第i天买的服务器id数组
                        ArrayList<Integer> indexNumber = new ArrayList<>();
                        indexNumber.add(index);
                        dailyBuyServers.add(indexNumber);
                    }else {
                        dailyBuyServers.get(i).add(index);
                    }
                    return oneServer;

                }else if(isArray[1] == 1){//当前服务器节点2可以部署

                    int index = currentAllServers.size();//根据当前服务器个数为编号，因为编号从0开始，当前有0台，购买第一台编号为0
                    oneServer.setIndex(index);//设置编号

                    oneServer =Tools.arrangeVm(addVm,oneServer,2,i);
                    Tools.currentAllServers.add(oneServer);
                    if(dailyBuyServers.size() <= i){//这一天还没买，构建第i天买的服务器id数组
                        ArrayList<Integer> indexNumber = new ArrayList<>();
                        indexNumber.add(index);
                        dailyBuyServers.add(indexNumber);
                    }else {
                        dailyBuyServers.get(i).add(index);
                    }
                    return oneServer;

                }else{//当前服务器节点不可以部署,继续下一个
                    continue;
                }
            }else if(addVm.getNodetype() == 1){
                if(isArray[2] == 1){
                    //双节点可以部署
                    int index = currentAllServers.size();//根据当前服务器个数为编号，因为编号从0开始，当前有0台，购买第一台编号为0
                    oneServer.setIndex(index);//设置编号

                    oneServer =Tools.arrangeVm(addVm,oneServer,3,i);
                    Tools.currentAllServers.add(oneServer);
                    if(dailyBuyServers.size() <= i){//这一天还没买，构建第i天买的服务器id数组
                        ArrayList<Integer> indexNumber = new ArrayList<>();
                        indexNumber.add(index);
                        dailyBuyServers.add(indexNumber);
                    }else {
                        dailyBuyServers.get(i).add(index);
                    }

                    return oneServer;
                }else {
                    //不可以部署
                    continue;
                }
            }
        }
        return oneServer;
    }
    /**
     *
     * @param vm 需要部署的vm
     * @param server 部署在该服务器上
     * @param nodeNumber 部署在该节点上 1，2，表示节点，3表示双节点
     * @return 已部署的
     */
    public static Server arrangeVm(VM vm,Server server,int nodeNumber){

        if(nodeNumber ==1){

            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM());
            server.getNode1().addVm(vm.getId());

        }else if(nodeNumber == 2){

            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() +vm.getRAM());
            server.getNode2().addVm(vm.getId());

        }else if(nodeNumber == 3){

            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode1().addVm(vm.getId());
            server.getNode2().addVm(vm.getId());

        }else {
            System.out.println("nodeNumber 指定错误！！！");
        }
        ArrayList<Integer> serverIdAndNodeNumber =new ArrayList<>();
        serverIdAndNodeNumber.add(server.getIndex());
        serverIdAndNodeNumber.add(nodeNumber);
        VMidInServer.put(vm.getId(),serverIdAndNodeNumber);//<VM编号，[所部署的Server编号,部署的节点]>
        return server;

    }

    /**
     *
     * @param vm 需要部署的vm
     * @param server 部署在该服务器上
     * @param nodeNumber 部署在该节点上 1，2，表示节点，3表示双节点
     * @return 已部署的
     */
    public static Server arrangeVm(VM vm,Server server,int nodeNumber,int i){

        if(nodeNumber ==1){

            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM());
            server.getNode1().addVm(vm.getId());

        }else if(nodeNumber == 2){

            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu());
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() +vm.getRAM());
            server.getNode2().addVm(vm.getId());

        }else if(nodeNumber == 3){

            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu()/2);
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied()+vm.getRAM()/2);
            server.getNode1().addVm(vm.getId());
            server.getNode2().addVm(vm.getId());

        }else {
            System.out.println("nodeNumber 指定错误！！！");
        }
        ArrayList<Integer> serverIdAndNodeNumber =new ArrayList<>();
        serverIdAndNodeNumber.add(server.getIndex());
        serverIdAndNodeNumber.add(nodeNumber);
        VMidInServer.put(vm.getId(),serverIdAndNodeNumber);//<VM编号，[所部署的Server编号,部署的节点]>
        if(dailyArrangeVM.size() <= i){//这一天还没部署，构建第i天部署的服务器
            ArrayList<Integer>  vmId= new ArrayList<>();
            vmId.add(vm.getId());
            dailyArrangeVM.add(vmId);
        }else {
            dailyArrangeVM.get(i).add(vm.getId());
        }
        currentVm.put(vm.getId(),vm);
        return server;

    }
    //删除VM
    public static void deleteVM(int deleteVmId){

        ArrayList<Integer> serveridList = VMidInServer.get(deleteVmId);//部署的服务器

        int serverid =serveridList.get(0);//所删除的服务器编号
        int nodetype =serveridList.get(1);//所删除的服务器节点1，2，表示节点，3表示双节点
        Server server =currentAllServers.get(serverid);
        VM deletevm = currentVm.get(deleteVmId);
        if(nodetype ==1){
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied() - deletevm.getNum_cpu());
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied() - deletevm.getRAM());
            server.getNode1().getVmlist().remove(Integer.valueOf(deleteVmId));

        }else if(nodetype == 2){
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied() - deletevm.getNum_cpu());
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() - deletevm.getRAM());
            server.getNode2().getVmlist().remove(Integer.valueOf(deleteVmId));

        }else if(nodetype == 3){
            server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied() - (deletevm.getNum_cpu()/2));
            server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied() - (deletevm.getRAM()/2));
            server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied() - (deletevm.getNum_cpu()/2));
            server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied() - (deletevm.getRAM()/2));
            server.getNode1().getVmlist().remove(Integer.valueOf(deleteVmId));
            server.getNode2().getVmlist().remove(Integer.valueOf(deleteVmId));

        }
        currentVm.remove(deleteVmId);
        currentAllServers.set(serverid,server);
    }


    //统计请求 统计所有请求

    /**
     * 保存这两个
     * //所有add的VM信息<vmid,[第几天，型号，存在多少天]>
     *      public static HashMap<Integer,ArrayList<String>> allAddVm = new HashMap<>();
     * //<第几天，[这一天所有add的VM]>
     *      public static HashMap<Integer,ArrayList<Integer>> dailyAddVm = new HashMap<>();
     * //<第几天，[这一天所有del的VM]>
     *     public static HashMap<Integer,ArrayList<Integer>> dailyDelVm = new HashMap<>();
     */
    public static void statisticsRequest(){
        for (int i = 0; i < op.length; i++) {
            //第i天的所有请求
            ArrayList<String> today = op[i];

            ArrayList<Integer> oneDayAddVm = new ArrayList<>();
            ArrayList<Integer> oneDayDelVm = new ArrayList<>();
            for (int j = 0; j < today.size(); j++) {

                //当前这条请求String
                String oneRequest = today.get(j);
                oneRequest = oneRequest.substring(1, oneRequest.length() - 1);
                String[] oneRequestInfo = oneRequest.split(", ");
                //请求类型
                String oneRequestType = oneRequestInfo[0];
                if (oneRequestType.equals("add")) {
                    //添加请求
                    String addVmModel = oneRequestInfo[1];
                    int addVmId = Integer.parseInt(oneRequestInfo[2]);

                    //allAddVm添加
                    ArrayList<String> oneVmData = new ArrayList<>();
//                    [第几天，型号，存在多少天]
                    oneVmData.add(Integer.valueOf(i).toString());
                    oneVmData.add(addVmModel);
                    //初始假设不会删除
                    oneVmData.add(String.valueOf(dayNumber - i-1));
                    allAddVm.put(addVmId,oneVmData);
                    oneDayAddVm.add(addVmId);




                } else if (oneRequestType.equals("del")) {
                    //删除请求
                    int deleteVmId = Integer.parseInt(oneRequestInfo[1]);
                    oneDayDelVm.add(deleteVmId);

                    //变化allAddVm里面的被删除服务器的存在天数
                    allAddVm.get(deleteVmId).set(2,String.valueOf(i-Integer.parseInt(allAddVm.get(deleteVmId).get(0))));
                }
            }
            dailyAddVm.put(i,oneDayAddVm);
            dailyDelVm.put(i,oneDayDelVm);

        }

        //统计这批数据所有服务器活多少天，平均，中位数
        ArrayList<Integer> vmDay = new ArrayList<>();
        int  allDay =0;
        for(Integer vmId: allAddVm.keySet()){
            ArrayList<String> vmData = allAddVm.get(vmId);
            vmDay.add(Integer.parseInt(vmData.get(2)));
            allDay +=Integer.parseInt(vmData.get(2));
        }
        vmDay.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if(o1 instanceof Integer && o2 instanceof Integer){
                    return o1-o2;
                }
                return 0;
            }
        });
        vmMedianLiveDay =vmDay.get(vmDay.size()/2);
        vmAverageLiveDay =allDay*1.0/allAddVm.size();
//        System.out.println("sss");


    }


    //计算每一个服务器最近它cpu/Ram比的vm队列
    public static void sortVmForServer(){
        for(String serverType :allServerObject.keySet()){
            Server server =allServerObject.get(serverType);
            double serverCpu = server.getNum_cpu();
            double serverRam = server.getRAM();
            TreeSet vmSet = new TreeSet(new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    if(o1 instanceof VM && o2 instanceof VM){
                        VM vm1 = (VM) o1;
                        VM vm2 = (VM) o2;
                        double vm1Gpu = vm1.getNum_cpu() ;
                        double vm1Ram = vm1.getRAM() ;
                        double vm2Gpu = vm2.getNum_cpu();
                        double vm2Ram = vm2.getRAM() ;

                        //服务器对于vm的单位费用，
                        double den1 = 100*Math.abs((vm1Gpu/vm1Ram  ) - (serverCpu/serverRam));
                        double den2 = 100*Math.abs((vm2Gpu/vm2Ram  ) - (serverCpu/serverRam));
//                        double den1 = (server1Price * Math.max(vmGpu/server1Gpu   , vmRam/server1Ram));
//                        double den2 = (server2Price * Math.max(vmGpu/server2Gpu   , vmRam/server2Ram));
//                        double den1 = server1Price * (Math.abs(vmGpu/server1Gpu -vmRam/server1Ram));
//                        double den2 = server2Price * (Math.abs(vmGpu/server2Gpu -vmRam/server2Ram));
                        int t =(int ) (den1-den2);

                        return  t== 0 ? 1 : t;
                    }

                    return 0;
                }
            });
            for(String vmType :allVmObject.keySet()){
                vmSet.add(allVmObject.get(vmType));
            }
            mostCostEffectiveVmOrder.put(server.getModel(),vmSet);
        }



    }

    /**
     *
     * @param vmOder 要部署的vm列表0-10个
     * @param server
     * @return 如果可以部署则返回部署方案  部署方案[[vm类型，部署节点] [vm类型，部署节点] [vm类型，部署节点]]，如果不能部署，返回null
     *
     */
    public static ArrayList<ArrayList<String>> isCanArrangeAndOrderVm(ArrayList<String> vmOder,Server server){
        server.getNode1().setRAMisoccupied(0);
        server.getNode1().setCpuisoccupied(0);
        server.getNode2().setCpuisoccupied(0);
        server.getNode2().setRAMisoccupied(0);
        int size = vmOder.size();
        int node1servercpu = server.getNode1().getNum_cpu();
        int node1serverRam = server.getNode1().getRAM();
        int node2servercpu = server.getNode2().getNum_cpu();
        int node2serverRam = server.getNode2().getRAM();

        ArrayList<ArrayList<String>> vmArrange= new ArrayList<>();

        ArrayList<VM> singleVmOder = new ArrayList<>();
        //先把双节点和单节点分开
        for(int i=0;i<vmOder.size();i++){
            VM vm = allVmObject.get(vmOder.get(i));
            if(vm.getNodetype() ==1){
                int[] isArray = Tools.isCanArrange(server, vm);
                if(isArray[2] ==0){
                    return null;
                }
                server.getNode1().setCpuisoccupied(server.getNode1().getCpuisoccupied()+vm.getNum_cpu()/2);
                server.getNode1().setRAMisoccupied(server.getNode1().getRAMisoccupied()+vm.getRAM()/2);
                server.getNode2().setCpuisoccupied(server.getNode2().getCpuisoccupied()+vm.getNum_cpu()/2);
                server.getNode2().setRAMisoccupied(server.getNode2().getRAMisoccupied()+vm.getRAM()/2);
                node1servercpu = node1servercpu-vm.getNum_cpu()/2;
                node1serverRam = node1serverRam-vm.getRAM()/2;
                node2servercpu = node2servercpu-vm.getNum_cpu()/2;
                node2serverRam = node2serverRam-vm.getRAM()/2;
                ArrayList<String> doubleVm = new ArrayList<>();
                doubleVm.add(vm.getModel());
                doubleVm.add(String.valueOf(3));
                vmArrange.add(doubleVm);

            }else {
                singleVmOder.add(vm);
            }
        }

        //计算哪个节点剩余的cpu与ram多，就放在哪个vm
        for(int i=0;i<singleVmOder.size();i++){

            VM vm = allVmObject.get(vmOder.get(i));

            int[] isArray = Tools.isCanArrange(server, vm);
            if(isArray[0] ==0 && isArray[1] ==0){
                return null;
            }
            if(node1servercpu >= node2servercpu && node1serverRam>=node2serverRam){
                //放在A节点
                node1servercpu = node1servercpu-vm.getNum_cpu();
                node1serverRam = node1serverRam-vm.getRAM();
                ArrayList<String> doubleVm = new ArrayList<>();
                doubleVm.add(vm.getModel());
                doubleVm.add(String.valueOf(1));
                vmArrange.add(doubleVm);

            }else if(node1servercpu <= node2servercpu && node1serverRam<=node2serverRam){
                //放在B节点
                node2servercpu = node2servercpu-vm.getNum_cpu();
                node2serverRam = node2serverRam-vm.getRAM();
                ArrayList<String> doubleVm = new ArrayList<>();
                doubleVm.add(vm.getModel());
                doubleVm.add(String.valueOf(2));
                vmArrange.add(doubleVm);

            }else{
                //节点1剩余cpu大于2，但剩余ram小于2
                if(isArray[0] ==1 && isArray[1] ==1){
                    //找剩余比例与vm接近的节点
                    if(Math.abs(vm.getNum_cpu()*1.0/vm.getRAM()  - node1servercpu*1.0/node1serverRam)> Math.abs(vm.getNum_cpu()*1.0/vm.getRAM()  - node2servercpu*1.0/node2serverRam)){
                        //放在A节点
                        node1servercpu = node1servercpu-vm.getNum_cpu();
                        node1serverRam = node1serverRam-vm.getRAM();
                        ArrayList<String> doubleVm = new ArrayList<>();
                        doubleVm.add(vm.getModel());
                        doubleVm.add(String.valueOf(1));
                        vmArrange.add(doubleVm);
                    }else {
                        //放在B节点
                        node2servercpu = node2servercpu-vm.getNum_cpu();
                        node2serverRam = node2serverRam-vm.getRAM();
                        ArrayList<String> doubleVm = new ArrayList<>();
                        doubleVm.add(vm.getModel());
                        doubleVm.add(String.valueOf(2));
                        vmArrange.add(doubleVm);
                    }

                }else if(isArray[0] ==0 && isArray[1] ==1){
                    //放在B节点
                    node2servercpu = node2servercpu-vm.getNum_cpu();
                    node2serverRam = node2serverRam-vm.getRAM();
                    ArrayList<String> doubleVm = new ArrayList<>();
                    doubleVm.add(vm.getModel());
                    doubleVm.add(String.valueOf(2));
                    vmArrange.add(doubleVm);

                }else {
                    //放在A节点
                    node1servercpu = node1servercpu-vm.getNum_cpu();
                    node1serverRam = node1serverRam-vm.getRAM();
                    ArrayList<String> doubleVm = new ArrayList<>();
                    doubleVm.add(vm.getModel());
                    doubleVm.add(String.valueOf(1));
                    vmArrange.add(doubleVm);

                }
            }

        }
        return vmArrange;

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

    //因为这两个数据一般都是不同随机加的，不会某一种加很多。所以我们就按没一种只有一个来算
    //计算每个服务器最好放哪种组合可以得到 cpu，ram,占用最大的。后面选的时候再考虑。只放一种，两种组合、三种组合、、、直到10中组合，
    public static void findBestVmforServer(){

        //每种服务器分别计算
        for(String serverName : allServerObject.keySet()){
            int serverCpu = allServerObject.get(serverName).getNum_cpu();
            int serverRam = allServerObject.get(serverName).getRAM();
            //从一个组合，到10个组合
            int iter=0;
            TreeSet<VM> vmOder=mostCostEffectiveVmOrder.get(serverName);
            //选最好的20个
            ArrayList<VM> vmList = new ArrayList<>();
            Iterator vmOderiterator = vmOder.iterator();
            while (iter<20&& vmOderiterator.hasNext()){
                vmList.add((VM) vmOderiterator.next());
                iter++;
            }
            //        int[] v = {6,3,5,4,6};  //价值
            //        int[] w = {2,2,6,5,4};  //重量
            //        int[] b = {3,2,5,7,6};  //体积
            //        int c = 10;  //背包容量
            //        int d = 12;  //背包容积

            int c =serverCpu/2;
            int d =serverRam/2;
            int[] v = new int[iter];//价值，cpu+ram 双节点的/2
            int[] w = new int[iter];//cpu 双节点的/2
            int[] b = new int[iter];//ram 双节点的/2
            for(int i1=0;i1<iter;i1++){
                VM vm= vmList.get(i1);
                if(vm.getNodetype() ==0){
                    w[i1] = vm.getNum_cpu();
                    b[i1] = vm.getRAM();
                    v[i1] = w[i1]+b[i1];
                }else{
                    w[i1] = vm.getNum_cpu()/2;
                    b[i1] = vm.getRAM()/2;
                    v[i1] = w[i1]+b[i1];
                }
            }


            ArrayList<Integer> bestVmForServerId=dynamicProgram(v,w,b,c,d);
            ArrayList<VM> bestVmForServer = new ArrayList<>();
            for(int i=0;i<bestVmForServerId.size();i++){
                VM vm =vmList.get(bestVmForServerId.get(i));
                if(vm.getNodetype()==0){
                    bestVmForServer.add(vm);
                }else {
                    bestVmForServer.add(vm);
                }


            }

            FinalMostCostEffectiveVmOrder.put(serverName,bestVmForServer);

        }

    }

    public static void sortServer(){
        //                //server根据cpu、ram排序，ArrayList<String>  [型号，cpu,ram]
//                public static TreeSet<ArrayList<String>> cpuTreeSet =new TreeSet<>();
//                public static TreeSet<ArrayList<String>> RamTreeSet =new TreeSet<>();
        cpuTreeSet =new TreeSet<>(new Comparator<ArrayList<String>>() {
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                if(o1!=null &&o2!=null){
                    int cpu1 = Integer.parseInt(o1.get(1));
                    int cpu2 = Integer.parseInt(o2.get(1));
                    return (cpu1 -cpu2)==0 ?1:(cpu1-cpu2);
                }
                return 0;
            }
        });
        ramTreeSet =new TreeSet<>(new Comparator<ArrayList<String>>() {
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                if(o1!=null && o2!=null){
                    int ram1 = Integer.parseInt(o1.get(2));
                    int ram2 = Integer.parseInt(o2.get(2));
                    return (ram1 -ram2)==0 ? 1:(ram1-ram2);
                }
                return 0;
            }
        });
        for(String serverModel:allServerObject.keySet()){
            ArrayList<String> serverData = new ArrayList<>();
            Server server= allServerObject.get(serverModel);
            serverData.add(serverModel);
            serverData.add(String.valueOf(server.getNum_cpu()));
            serverData.add(String.valueOf(server.getRAM()));
            cpuTreeSet.add(serverData);
            ramTreeSet.add(serverData);



        }
        maxCpu=Integer.parseInt(cpuTreeSet.last().get(1));
        maxRam=Integer.parseInt(ramTreeSet.last().get(2));
//        System.out.println("ssss");

    }



    //处理请求，每次读进去足够多的数据，在这段时间里del不能太多  可以由del的个数决定或者del/add的比值确定
    public static void processRequests2(){
        //记录目前跌点到多少天了
        int i=0;
        while(i<dayNumber){
            System.out.println(i);
            //确定迭代天数，根据del个数 start-end
            int start = i;
            int end =i;
            int delnumber= 0;
            int addnumber =0;
            //目前设为20,后期调
            while(delnumber < 5 && addnumber<1000){
                delnumber += dailyDelVm.get(end).size();
                addnumber += dailyAddVm.get(end).size();
                end++;
            }
            //获取start-end的所有的add数据 [vm id]

            //[第几天添加的，型号，存在多少天，vmid]
            ArrayList<ArrayList<String>>   theseDaysAddVm = new ArrayList<>();
            //<型号，[第几天添加到,存在多少天，vmid]>
            HashMap<String,ArrayList<ArrayList<Integer>>>  theseDaysAddVmSaveByType = new HashMap<>();

            for(int j =start; j<end ;j++){

                for(int q = 0;q<dailyAddVm.get(j).size();q++){
                    ArrayList<String> addVm = new ArrayList<>();
                    int vmid = dailyAddVm.get(j).get(q);
                    String vmAddDay = allAddVm.get(vmid).get(0);
                    String vmType = allAddVm.get(vmid).get(1);
                    String vmLiveDays = allAddVm.get(vmid).get(2);
                    addVm.add(vmAddDay);
                    addVm.add(vmType);
                    addVm.add(vmLiveDays);
                    addVm.add(String.valueOf(vmid));
                    theseDaysAddVm.add(addVm);

                    //[第几天添加到,存在多少天，vmid]
                    ArrayList<Integer> thisTypeVm = new ArrayList<>();
                    thisTypeVm.add(Integer.parseInt(vmAddDay));
                    thisTypeVm.add(Integer.parseInt(vmLiveDays));
                    thisTypeVm.add(vmid);
                    if(!theseDaysAddVmSaveByType.keySet().contains(vmType)){
                        ArrayList<ArrayList<Integer>> thisTypeAllVm = new ArrayList<>();
                        thisTypeAllVm.add(thisTypeVm);
                        theseDaysAddVmSaveByType.put(vmType,thisTypeAllVm);
                    }else {
                        theseDaysAddVmSaveByType.get(vmType).add(thisTypeVm);
                    }



                }
            }

            LinkedList<VM> addvms = new LinkedList<>();
            //得到所有的vm序列
            for (ArrayList<String> addvm : theseDaysAddVm) {
                //得到vm的型号
                String model = addvm.get(1);
                //根据型号创建vm
                VM v = allVmObject.get(model);
                VM vm = new VM(v.getModel(), v.getNum_cpu(), v.getRAM(), v.getNodetype(), Integer.parseInt(addvm.get(3)));
                currentVm.put(vm.getId(),vm);
                addvms.add(vm);
            }
            ArrayList<ArrayList<String>>   theseDaysAddVmTemporaryCopy = (ArrayList<ArrayList<String>>)theseDaysAddVm.clone();
            Iterator iterator = addvms.iterator();
            while ((iterator.hasNext())) {
                VM vm = (VM) iterator.next();
                if (vmDeploynOnCurrentServer(vm)) {
                    iterator.remove();
                    for(int e=0;e<theseDaysAddVmTemporaryCopy.size();e++){
                        int vmid =Integer.parseInt(theseDaysAddVmTemporaryCopy.get(e).get(3));
                        if(vmid ==vm.getId()){
                            theseDaysAddVmTemporaryCopy.remove(e);
                        }
                    }

                }
            }

            //        int[] v = {6,3,5,4,6};  //价值
            //        int[] w = {2,2,6,5,4};  //重量
            //        int[] b = {3,2,5,7,6};  //体积
            //        int c = 10;  //背包容量
            //        int d = 12;  //背包容积

            //[第几天添加的，型号，存在多少天，vmid]
//            addvms
            ArrayList<Server> alreadyBuyServers =new ArrayList<>();
            while (!theseDaysAddVmTemporaryCopy.isEmpty()){
                System.out.println(theseDaysAddVmTemporaryCopy.size());
                if(theseDaysAddVmTemporaryCopy.size() ==1){
                    System.out.println("sss");
                }
                //计划
//                ArrayList<ArrayList<String>>   theseDaysAddVmTemporaryCopy1 = (ArrayList<ArrayList<String>>)theseDaysAddVmTemporaryCopy.clone();
//                ArrayList<Server> mayBuyServers = new ArrayList<>();

                double maxDen =Double.MAX_VALUE;
                //<Server型号，[[节点1加的vmid],[节点2加的vmid]]]>
                String bestServerType=null;
                //[[节点1加的vmid],[节点2加的vmid]]]
                ArrayList<ArrayList<Integer>> bestServerVm =new ArrayList<>();
                int p=1;
                while (p<=theseDaysAddVmTemporaryCopy.size()){
                    ArrayList<ArrayList<String>> vmData=new ArrayList<>(theseDaysAddVmTemporaryCopy.subList(0,p));
                    ArrayList<Integer> node1Vm = new ArrayList<>();
                    ArrayList<Integer> node2Vm = new ArrayList<>();
                    //计算这些vm需要多少cpu,vm
                    int node1cpuSum =0;
                    int node1ramSum =0;
                    int node2cpuSum =0;
                    int node2ramSum =0;
                    //[第几天添加到,存在多少天，vmid]
                    for(int u=0;u<vmData.size();u++){
                        VM vm =currentVm.get(Integer.parseInt(vmData.get(u).get(3)));
                        int vmCpu = vm.getNum_cpu();
                        int vmRam = vm.getRAM();
                        if(vm.getNodetype()==0){
                            //找那个空余比较大的往进放
                            if(node1cpuSum>=node2cpuSum && node1ramSum >=node2ramSum){
                                //放在节点2
                                node2cpuSum +=vmCpu;
                                node2ramSum +=vmRam;
                                node2Vm.add(vm.getId());
                            }else if(node1cpuSum<=node2cpuSum && node1ramSum <=node2ramSum){
                                node1cpuSum +=vmCpu;
                                node1ramSum +=vmRam;
                                node1Vm.add(vm.getId());
                            }else if(node1cpuSum > node2cpuSum && node1ramSum <=node2ramSum){
                                if(vmCpu>vmRam){
                                    node1cpuSum +=vmCpu;
                                    node1ramSum +=vmRam;
                                    node1Vm.add(vm.getId());
                                }else {
                                    node2cpuSum +=vmCpu;
                                    node2ramSum +=vmRam;
                                    node2Vm.add(vm.getId());
                                }
                            }else {
                                if(vmCpu<vmRam){
                                    node1cpuSum +=vmCpu;
                                    node1ramSum +=vmRam;
                                    node1Vm.add(vm.getId());
                                }else {
                                    node2cpuSum +=vmCpu;
                                    node2ramSum +=vmRam;
                                    node2Vm.add(vm.getId());
                                }
                            }

                        }else {
                            node1cpuSum +=vmCpu/2;
                            node1ramSum +=vmRam/2;
                            node2cpuSum +=vmCpu/2;
                            node2ramSum +=vmRam/2;
                            node1Vm.add(vm.getId());
                            node2Vm.add(vm.getId());
                        }
                    }
                    //第一个能按的server类型
                    String canUseServer =null;
                    Iterator cpuIterator =cpuTreeSet.iterator();
                    while (cpuIterator.hasNext()){
                        ArrayList<String> serverData = (ArrayList<String>) cpuIterator.next();
//                         [型号，cpu,ram]
                        if(Integer.parseInt(serverData.get(1))/2 > node1cpuSum &&Integer.parseInt(serverData.get(1))/2>node2cpuSum && Integer.parseInt(serverData.get(2))/2 >node1ramSum && Integer.parseInt(serverData.get(2))/2 >node2ramSum){
                            canUseServer =serverData.get(0);
                            break;
                        }
                    }

                    //找不到可以放的server
                    if(canUseServer==null ){
                        break;
                    }
                    //计算性价比
                    double den = allServerObject.get(canUseServer).getHardware_cost()*1.0/((node1cpuSum+node2cpuSum)+(node1ramSum+node2ramSum));
                    if(den < maxDen){
                        bestServerType =canUseServer;
                        bestServerVm.clear();
                        bestServerVm.add(node1Vm);
                        bestServerVm.add(node2Vm);
                        maxDen =den;

                    }
                    p++;
                }

                if(bestServerType ==null){
                    continue;
                }

                //选出了一个最优server
                Server serverObject = allServerObject.get(bestServerType);

                Server oneServer = new Server(serverObject.getNum_cpu(),serverObject.getRAM(),serverObject.getModel(),serverObject.getHardware_cost(),serverObject.getPerday_cost());
                int index = currentAllServers.size();
                oneServer.setIndex(index);
                currentAllServers.add(oneServer);
                //[[节点1加的vmid],[节点2加的vmid]]]
                //节点1
                ArrayList<Integer> node1Vm=bestServerVm.get(0);
                for(int y=0;y<node1Vm.size();y++){
                    Integer vmId = node1Vm.get(y);
                    VM vm = currentVm.get(vmId);
                    if(vm.getNodetype()==0){
                        oneServer =arrangeVm(vm,oneServer,1);
                        //永远移除队列的第一个
                        theseDaysAddVmTemporaryCopy.remove(0);
                    }else {
                        oneServer =arrangeVm(vm,oneServer,3);
                        //永远移除队列的第一个
                        theseDaysAddVmTemporaryCopy.remove(0);
                    }

                }
                //节点二
                ArrayList<Integer> node2Vm=bestServerVm.get(1);
                for(int y=0;y<node2Vm.size();y++){
                    Integer vmId = node2Vm.get(y);
                    VM vm = currentVm.get(vmId);
                    if(vm.getNodetype()==0){
                        oneServer =arrangeVm(vm,oneServer,2);
                        //永远移除队列的第一个
                        theseDaysAddVmTemporaryCopy.remove(0);
                    }
                }
                currentAllServers.set(oneServer.getIndex(),oneServer);
                alreadyBuyServers.add(oneServer);

            }


            //<型号，这一天该类服务器的所有类>
            HashMap<String, ArrayList<Server>> serverNumberObject = new HashMap<>();
            for (int e = 0; e < alreadyBuyServers.size(); e++) {//遍历买什么
                Server server =alreadyBuyServers.get(e);
                if (serverNumberObject.containsKey(server.getModel())) {
                    serverNumberObject.get(server.getModel()).add(server);
                } else {
                    ArrayList<Server> oneTypeServer = new ArrayList<>();
                    oneTypeServer.add(server);
                    serverNumberObject.put(server.getModel(), oneTypeServer);
                }
            }

            //输出每天买多少服务器
            ArrayList<String> thisDayOutData = new ArrayList<>();
            thisDayOutData.add("(purchase, " + serverNumberObject.size() + ")");
            for (String serverType : serverNumberObject.keySet()) {
                thisDayOutData.add("(" + serverType + ", " + serverNumberObject.get(serverType).size() + ")");
                //构建编号映射hashMap <申请时服务器的编号, 输出时服务器的编号>
                for (Server oneserver : serverNumberObject.get(serverType)) {
                    severNumberMapping.put(oneserver.getIndex(), numberCount++);
                }
            }
            thisDayOutData.add("(migration, 0)");
            //输出迁移方案
//            for (int z = 0; z < moveInfo.size(); z++) {
//                System.out.println(moveInfo.get(z));
//            }
            //输出节点怎么部署
//            System.out.println(dailyArrangeVM.get(i));
            ArrayList<Integer> vmIdNumber = dailyAddVm.get(start);
            for (int r = 0; r < vmIdNumber.size(); r++) {
//                System.out.println(vmIdNumber.get(r));
                ArrayList<Integer> serveridList = VMidInServer.get(vmIdNumber.get(r));//部署的服务器

                int serverid = serveridList.get(0);//服务器编号
                int nodetype = serveridList.get(1);//服务器节点1，2，表示节点，3表示双节点
                if (nodetype == 1) {
                    thisDayOutData.add("(" + severNumberMapping.get(serverid) + ", A)");

                } else if (nodetype == 2) {
                    thisDayOutData.add("(" + severNumberMapping.get(serverid) + ", B)");

                } else if (nodetype == 3) {
                    thisDayOutData.add("(" + severNumberMapping.get(serverid) + ")");
                }
            }
            allOutData.put(start,thisDayOutData);
            for(int q=start+1;q<end;q++){
                ArrayList<String> thisDayOutData1 = new ArrayList<>();
                thisDayOutData1.add("(purchase, 0)");
                thisDayOutData1.add("(migration, 0)");
                ArrayList<Integer> vmIdNumber1 = dailyAddVm.get(q);
                for (int r = 0; r < vmIdNumber1.size(); r++) {
                    ArrayList<Integer> serveridList = VMidInServer.get(vmIdNumber1.get(r));//部署的服务器
                    int serverid = serveridList.get(0);//服务器编号
                    int nodetype = serveridList.get(1);//服务器节点1，2，表示节点，3表示双节点
                    if (nodetype == 1) {
                        thisDayOutData1.add("(" + severNumberMapping.get(serverid) + ", A)");

                    } else if (nodetype == 2) {
                        thisDayOutData1.add("(" + severNumberMapping.get(serverid) + ", B)");

                    } else if (nodetype == 3) {
                        thisDayOutData1.add("(" + severNumberMapping.get(serverid) + ")");
                    }
                }
                allOutData.put(q,thisDayOutData1);

            }
            for(int q=start;q<end;q++){
                for(Integer vmId: dailyDelVm.get(q)){
                    deleteVM(vmId);
                    //迁移

                }
            }

            i=end;
        }

        try {
            PrintStream console = System.out;
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("outtest2.txt")));
            System.setOut(out);
            for(int q=0;q<dayNumber;q++){
                for(String outData :allOutData.get(q)){
                    System.out.println(outData);
                }
            }
            out.close();
            System.setOut(console);
        } catch(Exception e) {
            System.out.println("error");
        }

        //输出gpu占用率和ram占用率
        double allCpuRatio = 0;
        double allRamRatio = 0;
        int size = currentAllServers.size();
        System.out.println("一共需要"+size+"个服务器");
        for(int m =0 ;m< size;m++){
            Server server = currentAllServers.get(m);
            Servernode servernode1 =server.getNode1();
            Servernode servernode2 =server.getNode2();

            double cpuRatio = 0.5 *servernode1.getCpuisoccupied() *1.0 / servernode1.getNum_cpu()  +0.5 *servernode2.getCpuisoccupied() *1.0 / servernode2.getNum_cpu();

            double ramRatio = 0.5 *servernode1.getRAMisoccupied() *1.0 / servernode1.getRAM()  +0.5 *servernode2.getRAMisoccupied() *1.0 / servernode2.getRAM();

//            System.out.println("cpuRatio : "+cpuRatio +"ramRatio : "+ramRatio);
            allCpuRatio += cpuRatio;
            allRamRatio += ramRatio;
        }
        System.out.println("allCpuRatio : "+ allCpuRatio/size + "allRamRatio : "+allRamRatio/size);

    }



}
