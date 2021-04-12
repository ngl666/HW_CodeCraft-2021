package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.Data;
import com.huawei.java.main.lib.Server;
import com.huawei.java.main.lib.Servernode;
import com.huawei.java.main.lib.VM;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Tools {
    public static ForkJoinPool pool = new ForkJoinPool();
    public static Tools tools = new Tools();
    //保存每一天结束cpu,ram 占有率
    public static ArrayList<Double> cpu = new ArrayList<>();
    public static ArrayList<Double> ram = new ArrayList<>();

    //此时此刻cpu，ram占有率
    public static Double currentCpuRatio = 0.0;
    public static Double currentRamRatio = 0.0;
    //判断是哪种类型的数据、饱和型 1、不饱和型 2
    public static int dataType =0;

    //记录一共有多少天
    public static int dayNumber = 0;

    //记录一次知道多少天的信息
    public static int kownDayNumber = 0;

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
    public static ArrayList<ArrayList<String>> op;
//    public static ArrayList<String>[]op;

    //加载数据
    public static void downLoad(){
        //得到所有数据
        Data data= getdata.dataloader();
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

        //前 k 天所有请求
//        op = (ArrayList)Arrays.asList(data.getSequence());
        op=data.getSequence();
    }
    public static ArrayList<ArrayList<Integer>> move1(int canMoveNumber){
        TreeSet<Servernode> currentAllServerNodesOrder ;//根据服务器编号获取服务器类

        currentAllServerNodesOrder  = new TreeSet(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if(o1 ==o2){
                    return 0;
                }
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
        Set<Integer> movedVm= new HashSet<>();
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
                // double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getNum_cpu()) - vm.getRAM() *1.0/ servernode.getRAM());
                double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                double t = 0.;
//                if (dataType==2)
//                    t = Math.abs((vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                else
//                    t = Math.abs(0.87*(vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
                if(  t > difference){
                    maxDifferenceVm = vm;
                    difference = t;
                }
            }
            //移maxDifferenceVm,移到哪？移到目前所剩下的节点中vmcpu/nodecpu 与vmram/noderam 与它最接近的那个节点，且要可以移，还要考虑单双节点
            if(maxDifferenceVm==null || movedVm.contains(maxDifferenceVm.getId())){
                currentAllServerNodesOrder.pollFirst();
                continue;
            }
            movedVm.add(maxDifferenceVm.getId());
            //目标节点
            ArrayList<Servernode> maxServerNode = isCanMove(maxDifferenceVm);

            if(maxServerNode.get(0) == null){
                //没有可迁移的目的服务器
                currentAllServerNodesOrder.pollFirst();
                continue;
            }else {

                if(maxDifferenceVm.getNodetype() ==0){
                    //单节点,删除一个
                    currentAllServerNodesOrder.pollFirst();
                    Servernode servernode1 =maxServerNode.get(0);
                    currentAllServerNodesOrder.remove(servernode1);

                }else {
                    //双节点，删除两个
//                    currentAllServerNodesOrder.pollFirst();

                    Servernode servernode1 =maxServerNode.get(0);
                    currentAllServerNodesOrder.remove(servernode1);
                    Servernode servernode2 =maxServerNode.get(1);
                    currentAllServerNodesOrder.remove(servernode2);


                }
                //重新部署
                int[]  newServerId =vmMoveOnCurrentServer(maxDifferenceVm,maxServerNode);
                if(newServerId[0] < 0){
//                    currentAllServerNodesOrder.pollFirst();
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

                        currentAllServerNodesOrder.add(servernode); // add source bad server
                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1(); // get target server
//                        currentAllServerNodesOrder.remove(servernode); // delete target server
                        currentAllServerNodesOrder.add(servernode); // add source target server


                    }else if(newServerId[1]==2){
                        //移到节点二
                        ArrayList<Integer> move = new ArrayList<>();
                        move.add(maxDifferenceVm.getId());
                        move.add(newServerId[0]);
                        move.add(2);
                        realMove.add(move);
                        //删除的时候currentAllServers 已经变了

                        currentAllServerNodesOrder.add(servernode); // add source bad server
                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2(); // get target server
//                        currentAllServerNodesOrder.remove(servernode); // delete target server
                        currentAllServerNodesOrder.add(servernode); // add source target server

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
                        Server sourceServer = currentAllServers.get(servernode.getIndex());
                        currentAllServerNodesOrder.add(sourceServer.getNode1()); // add source bad server
                        currentAllServerNodesOrder.add(sourceServer.getNode2());

                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1(); // get target server
//                        currentAllServerNodesOrder.remove(servernode); // delete target server
                        currentAllServerNodesOrder.add(servernode); // add source target server
                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2(); // get target server
//                        currentAllServerNodesOrder.remove(servernode); // delete target server
                        currentAllServerNodesOrder.add(servernode); // add source target server

                    }


                }


            }


        }
        return realMove;
    }
    public static ArrayList<ArrayList<Integer>> move2(int canMoveNumber){
        TreeSet<Server> currentAllServerOrder ;//根据服务器编号获取服务器类

        currentAllServerOrder  = new TreeSet(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if(o1 == o2){
                    return 0;
                }
                if(o1 instanceof Server && o2 instanceof Server){
//                    return (int ) ((((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) *100);
                    Server server1 =(Server) o1;
                    Server server2 =(Server) o2;
                    double server1Ratio=  (server1.getNode1().getCpuRatio() +server1.getNode2().getCpuRatio()) + (server1.getNode1().getRamRatio() +server1.getNode2().getRamRatio());
                    double server2Ratio=  (server2.getNode1().getCpuRatio() +server2.getNode2().getCpuRatio()) + (server2.getNode1().getRamRatio() +server2.getNode2().getRamRatio());
                    int i = (int ) ((server1Ratio/4 -server2Ratio/4)*100);
                    if(i==0){
                        //占比差不多的，比较均衡的放在后面
                        i= (int ) ((    (  Math.abs(server1.getNode1().getCpuRatio()+server1.getNode2().getCpuRatio() -server1.getNode1().getRamRatio()-server1.getNode2().getRamRatio()) ) - (  Math.abs(server2.getNode1().getCpuRatio()+server2.getNode2().getCpuRatio() -server2.getNode1().getRamRatio()-server2.getNode2().getRamRatio()) ))*10000 );
                        if(i==0){
                            return -1;
                        }else {
                            return -i;
                        }
                    }else {
                        return i;
                    }
                }
                return 0;
            }
        });
        for(int k=0;k<currentAllServers.size();k++) {
            Server oneServer = currentAllServers.get(k);
            currentAllServerOrder.add(oneServer);
        }
        Set<Integer> movedVm= new HashSet<>();
        //格式为(虚拟机 ID, 目的服务器 ID)或 (虚拟机 ID, 目的服务器 ID, 目的服务器节点)。

        ArrayList<ArrayList<Integer>> realMove = new ArrayList<>();
        while(canMoveNumber>0){
            //把cpu和ram占用率最小的那些server上的vm移到那些最多
            Server server = currentAllServerOrder.first();
            //获取该节点上的vm列表
//            ArrayList<Integer> vmlist =server.getNode1().getVmlist();
            Set<Integer> vmList = new HashSet<>();
            ArrayList<Integer> vmList1 =server.getNode1().getVmlist();
            ArrayList<Integer> vmList2 =server.getNode2().getVmlist();
            for(Integer vmId : vmList1){
                vmList.add(vmId);
            }
            for(Integer vmId : vmList2){
                vmList.add(vmId);
            }
            //把这个服server从序列中移除
            currentAllServerOrder.pollFirst();
            if(vmList.isEmpty()){
                //如果该服务器上没有vm,遍历下一个server
                continue;
            }
            for(Integer vmId :vmList){
                if(canMoveNumber<=0){
                    return realMove;
                }
                //找vmcpu/nodecpu 与vmram/noderam ,差最大的那个
                VM  maxDifferenceVm  =currentVm.get(vmId);
                if(movedVm.contains(maxDifferenceVm.getId())){
                    //已经移过了
                    continue;
                }

                movedVm.add(maxDifferenceVm.getId());
                //目标节点,要迁移的目的servernode,单节点[目标节点，null],双节点[同一服务器节点1，节点2]
                ArrayList<Servernode> maxServerNode = isCanMove(maxDifferenceVm);

                if(maxServerNode.get(0) == null){
                    //没有目标server 下一个server
                    continue;
                }else {
                    //有目标server，先移除目标server,后面再将改变的server移除
                    Server tagetServer=currentAllServers.get( maxServerNode.get(0).getIndex() );
                    currentAllServerOrder.remove(tagetServer);

//                    Iterator iterator = currentAllServerOrder.iterator();
//                    int targetServerIndex = tagetServer.getIndex();
//                    while(iterator.hasNext()){
//                        int index = ((Server) iterator.next()).getIndex();
//                        if( index ==targetServerIndex){
//                            iterator.remove();
//                        }
//                    }

                    //重新部署
                    int[]  newServerId =vmMoveOnCurrentServer(maxDifferenceVm,maxServerNode);
                    if(newServerId[0] < 0){
//                    currentAllServerNodesOrder.pollFirst();
                        continue;
                    }else {
                        if(newServerId[1] ==1){
                            //移到节点一
                            ArrayList<Integer> move = new ArrayList<>();
                            move.add(maxDifferenceVm.getId());
                            move.add(newServerId[0]);
                            move.add(1);
                            realMove.add(move);

                            // add source target server
                            currentAllServerOrder.add(tagetServer);
                            canMoveNumber--;


                        }else if(newServerId[1]==2){
                            //移到节点二
                            ArrayList<Integer> move = new ArrayList<>();
                            move.add(maxDifferenceVm.getId());
                            move.add(newServerId[0]);
                            move.add(2);
                            realMove.add(move);
                            //删除的时候currentAllServers 已经变了

                            // add source target server
                            currentAllServerOrder.add(tagetServer);
                            canMoveNumber--;

                        }else if(newServerId[1]==3){
                            //双节点
                            ArrayList<Integer> move = new ArrayList<>();
                            move.add(maxDifferenceVm.getId());
                            move.add(newServerId[0]);
                            move.add(3);
                            realMove.add(move);

                            // add source target server
                            currentAllServerOrder.add(tagetServer);
                            canMoveNumber--;
                        }


                    }
                }





            }
        }
        return realMove;
    }



    //进行迁移操作
    public static ArrayList<ArrayList<Integer>> move(int canMoveNumber){
        if(canMoveNumber<=0){
            return null;
        }
        if((0.5*currentCpuRatio +0.5*currentRamRatio) <0.85){

            //cpu,ram比较空的情况，使用move2方法,这种方法一般都会将迁移次数用完
            return move2(canMoveNumber);

        }else{
            //cpu,ram比较满的情况，使用move1方法
            ArrayList<ArrayList<Integer>> realMove1 = move1(canMoveNumber);
            ArrayList<ArrayList<Integer>> realMove2 = null;
            if((canMoveNumber - realMove1.size()) >0){
                realMove2 = move2(canMoveNumber - realMove1.size());
            }
            realMove1.addAll(realMove2);
            return realMove1;
        }
        //按cpu占用率 与ram占用率 之差 从小到大  排好序的节点



    }
    //进行迁移操作
//    public static ArrayList<ArrayList<Integer>> move(int canMoveNumber){
//        if(canMoveNumber<=0){
//            return null;
//        }
//        //按cpu占用率 与ram占用率 之差 从小到大  排好序的节点
//        TreeSet<Servernode> currentAllServerNodesOrder ;//根据服务器编号获取服务器类
//
//        currentAllServerNodesOrder  = new TreeSet(new Comparator() {
//            @Override
//            public int compare(Object o1, Object o2) {
//                if(o1 instanceof Servernode && o2 instanceof Servernode){
////                    return (int ) ((((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) *100);
//
//                    int i= (int ) (((  Math.abs(((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) )  - (Math.abs(((Servernode) o2).getCpuRatio() -((Servernode) o2).getRamRatio()) ))*10000 );
//                    if(i==0){
//                        return -1;
//                    }else {
//                        return -i;
//                    }
//
//
//                }
//                return 0;
//            }
//        });
//        for(int k=0;k<currentAllServers.size();k++) {
//            Server oneServer = currentAllServers.get(k);
//            currentAllServerNodesOrder.add(oneServer.getNode1());
//            currentAllServerNodesOrder.add(oneServer.getNode2());
//        }
//        Set<Integer> movedVm= new HashSet<>();
//        //格式为(虚拟机 ID, 目的服务器 ID)或 (虚拟机 ID, 目的服务器 ID, 目的服务器节点)。
//        ArrayList<ArrayList<Integer>> realMove = new ArrayList<>();
//        for(int i=0;i<canMoveNumber;i++){
//            //得到cup占用率与RAM占用率相差最大的节点，确定移该节点上的vm,再找该节点上，vmcpu/nodecpu 与vmram/noderam ,差最大的那个
//            Servernode servernode = currentAllServerNodesOrder.first();
//            Server server = currentAllServers.get(servernode.getIndex());
//            //获取该节点上的vm列表
//            ArrayList<Integer> vmlist =servernode.getVmlist();
//
//            //找vmcpu/nodecpu 与vmram/noderam ,差最大的那个
//            VM  maxDifferenceVm  =null;
//            double difference =-1;
////            System.out.println(vmlist);
//            for(int j =0;j< vmlist.size();j++){
//                VM vm =currentVm.get(vmlist.get(j));
////                System.out.println(vm.getNum_cpu());
////                System.out.println(servernode.getNum_cpu());
////                System.out.println( vm.getRAM());
////                System.out.println(servernode.getRAM());
//                // double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getNum_cpu()) - vm.getRAM() *1.0/ servernode.getRAM());
//                double t = Math.abs(0.87*(vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
////                double t = 0.;
////                if (dataType==2)
////                    t = Math.abs((vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
////                else
////                    t = Math.abs(0.87*(vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                if(  t > difference){
//                    maxDifferenceVm = vm;
//                    difference = t;
//                }
//            }
//            //移maxDifferenceVm,移到哪？移到目前所剩下的节点中vmcpu/nodecpu 与vmram/noderam 与它最接近的那个节点，且要可以移，还要考虑单双节点
//            if(maxDifferenceVm==null || movedVm.contains(maxDifferenceVm.getId())){
//                currentAllServerNodesOrder.pollFirst();
//                continue;
//            }
//            movedVm.add(maxDifferenceVm.getId());
//            //目标节点
//            ArrayList<Servernode> maxServerNode = isCanMove(maxDifferenceVm);
//
//            if(maxServerNode.get(0) == null){
//                //没有可迁移的目的服务器
//                currentAllServerNodesOrder.pollFirst();
//                continue;
//            }else {
//                //
////                if(maxServerNode.get(0).getIndex() == servernode.getIndex()  && maxServerNode.get(0).getNodename().equals(servernode.getNodename())){
////                    System.out.println("ssss");
////                }
//
//                if(maxDifferenceVm.getNodetype() ==0){
//                    //单节点,删除一个
//                    currentAllServerNodesOrder.pollFirst();
//                    Iterator iterator = currentAllServerNodesOrder.iterator();
//                    int targetServerIndex = maxServerNode.get(0).getIndex();
//                    String targetNodeType = maxServerNode.get(0).getNodename();
//
//                    while(iterator.hasNext()){
//                        Servernode oneServernode =(Servernode) iterator.next();
//                        int index = oneServernode.getIndex();
//                        String nodeType =oneServernode.getNodename();
//                        if(  nodeType == targetNodeType && index ==targetServerIndex){
//                            iterator.remove();
//                        }
//                    }
//
//                }else {
//                    //双节点，删除两个
////                    currentAllServerNodesOrder.pollFirst();
////                    Iterator iterator = currentAllServerNodesOrder.iterator();
////                    int serverIndex = servernode.getIndex();
////                    Server sourceServer = currentAllServers.get(servernode.getIndex());
////                    currentAllServerNodesOrder.remove(sourceServer.getNode1()); // add source bad server
////                    currentAllServerNodesOrder.remove(sourceServer.getNode2());
////
////                    currentAllServerNodesOrder.remove(maxServerNode.get(0));
////                    currentAllServerNodesOrder.remove(maxServerNode.get(1));
//
////                    while(iterator.hasNext()){
////                        if(( (Servernode) iterator.next()).getIndex() == serverIndex){
////                            iterator.remove();
////                        }
////                    }
//                    //双节点，删除两个
//                    currentAllServerNodesOrder.pollFirst();
//                    Iterator iterator = currentAllServerNodesOrder.iterator();
//                    int sourceServerIndex = servernode.getIndex();
//                    int targetServerIndex = maxServerNode.get(0).getIndex();
//
//                    while(iterator.hasNext()){
//                        int index = ((Servernode) iterator.next()).getIndex();
//                        if(  index== sourceServerIndex || index ==targetServerIndex){
//                            iterator.remove();
//                        }
//                    }
//
//                }
//                //重新部署
//                int[]  newServerId =vmMoveOnCurrentServer(maxDifferenceVm,maxServerNode);
//                if(newServerId[0] < 0){
////                    currentAllServerNodesOrder.pollFirst();
//                    continue;
//                }else {
//                    if(newServerId[1] ==1){
//                        //移到节点一
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(1);
//                        realMove.add(move);
//
//                        //删除的时候currentAllServers 已经变了
//
//                        currentAllServerNodesOrder.add(servernode); // add source bad server
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1(); // get target server
////                        currentAllServerNodesOrder.remove(servernode); // delete target server
//                        currentAllServerNodesOrder.add(servernode); // add source target server
//
//
//                    }else if(newServerId[1]==2){
//                        //移到节点二
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(2);
//                        realMove.add(move);
//                        //删除的时候currentAllServers 已经变了
//
//                        currentAllServerNodesOrder.add(servernode); // add source bad server
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2(); // get target server
////                        currentAllServerNodesOrder.remove(servernode); // delete target server
//                        currentAllServerNodesOrder.add(servernode); // add source target server
//
//                    }else if(newServerId[1]==3){
//                        //双节点
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(3);
//                        realMove.add(move);
//                        //移动后要变更currentAllServerNodesOrder
////                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1());
////                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2());
//                        Server sourceServer = currentAllServers.get(servernode.getIndex());
//                        currentAllServerNodesOrder.add(sourceServer.getNode1()); // add source bad server
//                        currentAllServerNodesOrder.add(sourceServer.getNode2());
//
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1(); // get target server
////                        currentAllServerNodesOrder.remove(servernode); // delete target server
//                        currentAllServerNodesOrder.add(servernode); // add source target server
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2(); // get target server
////                        currentAllServerNodesOrder.remove(servernode); // delete target server
//                        currentAllServerNodesOrder.add(servernode); // add source target server
//
//                    }
//
//
//                }
//
//
//            }
//
//
//        }
//        return realMove;
//
//
//    }
//    public static ArrayList<ArrayList<Integer>> move(int canMoveNumber){
//        if(canMoveNumber<=0){
//            return null;
//        }
//        //按cpu占用率 与ram占用率 之差 从小到大  排好序的节点
//        TreeSet<Servernode> currentAllServerNodesOrder ;//根据服务器编号获取服务器类
//
//        currentAllServerNodesOrder  = new TreeSet(new Comparator() {
//            @Override
//            public int compare(Object o1, Object o2) {
//                if(o1 instanceof Servernode && o2 instanceof Servernode){
////                    return (int ) ((((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) *100);
//
//                    int i= (int ) (((  Math.abs(((Servernode) o1).getCpuRatio() -((Servernode) o1).getRamRatio()) )  - (Math.abs(((Servernode) o2).getCpuRatio() -((Servernode) o2).getRamRatio()) ))*10000 );
//                    if(i==0){
//                        return -1;
//                    }else {
//                        return -i;
//                    }
//
//
//                }
//                return 0;
//            }
//        });
//        for(int k=0;k<currentAllServers.size();k++) {
//            Server oneServer = currentAllServers.get(k);
//            currentAllServerNodesOrder.add(oneServer.getNode1());
//            currentAllServerNodesOrder.add(oneServer.getNode2());
//        }
//
//        //格式为(虚拟机 ID, 目的服务器 ID)或 (虚拟机 ID, 目的服务器 ID, 目的服务器节点)。
//        ArrayList<ArrayList<Integer>> realMove = new ArrayList<>();
//        for(int i=0;i<canMoveNumber;i++){
//            //得到cup占用率与RAM占用率相差最大的节点，确定移该节点上的vm,再找该节点上，vmcpu/nodecpu 与vmram/noderam ,差最大的那个
//            Servernode servernode = currentAllServerNodesOrder.first();
//            Server server = currentAllServers.get(servernode.getIndex());
//            //获取该节点上的vm列表
//            ArrayList<Integer> vmlist =servernode.getVmlist();
//
//            //找vmcpu/nodecpu 与vmram/noderam ,差最大的那个
//            VM  maxDifferenceVm  =null;
//            double difference =-1;
////            System.out.println(vmlist);
//            for(int j =0;j< vmlist.size();j++){
//                VM vm =currentVm.get(vmlist.get(j));
////                System.out.println(vm.getNum_cpu());
////                System.out.println(servernode.getNum_cpu());
////                System.out.println( vm.getRAM());
////                System.out.println(servernode.getRAM());
//                // double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getNum_cpu()) - vm.getRAM() *1.0/ servernode.getRAM());
////                double t =Math.abs((vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                double t = 0.;
//                if (dataType==2)
//                    t = Math.abs((vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                else
//                    t = Math.abs(0.87*(vm.getNum_cpu() *1.0/servernode.getCpuisoccupied()) - vm.getRAM() *1.0/ servernode.getRAMisoccupied());
//                if(  t > difference){
//                    maxDifferenceVm = vm;
//                    difference = t;
//                }
//            }
//            //移maxDifferenceVm,移到哪？移到目前所剩下的节点中vmcpu/nodecpu 与vmram/noderam 与它最接近的那个节点，且要可以移，还要考虑单双节点
//            if(maxDifferenceVm==null){
//                currentAllServerNodesOrder.pollFirst();
//                continue;
//            }
//            //目标节点
//            ArrayList<Servernode> maxServerNode = isCanMove(maxDifferenceVm);
//
//            if(maxServerNode.get(0) == null){
//                //没有可迁移的目的服务器
//                currentAllServerNodesOrder.pollFirst();
//                continue;
//            }else {
//                //
////                if(maxServerNode.get(0).getIndex() == servernode.getIndex()  && maxServerNode.get(0).getNodename().equals(servernode.getNodename())){
////                    System.out.println("ssss");
////                }
//
//                if(maxDifferenceVm.getNodetype() ==0){
//                    //单节点,删除一个
//                    currentAllServerNodesOrder.pollFirst();
//
//                }else {
//                    //双节点，删除两个
//                    currentAllServerNodesOrder.pollFirst();
//                    Iterator iterator = currentAllServerNodesOrder.iterator();
//                    int serverIndex = servernode.getIndex();
//                    while(iterator.hasNext()){
//                        if(( (Servernode) iterator.next()).getIndex() == serverIndex){
//                            iterator.remove();
//                        }
//                    }
//
//                }
//                //重新部署
//                int[]  newServerId =vmMoveOnCurrentServer(maxDifferenceVm,maxServerNode);
//                if(newServerId[0] < 0){
//                    currentAllServerNodesOrder.pollFirst();
//                    continue;
//                }else {
//                    if(newServerId[1] ==1){
//                        //移到节点一
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(1);
//                        realMove.add(move);
//
//                        //删除的时候currentAllServers 已经变了
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1();
////                        currentAllServerNodesOrder.add(servernode);
//
//
//                    }else if(newServerId[1]==2){
//                        //移到节点二
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(2);
//                        realMove.add(move);
//                        //删除的时候currentAllServers 已经变了
//                        servernode = currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2();
////                        currentAllServerNodesOrder.add(servernode);
//
//                    }else if(newServerId[1]==3){
//                        //双节点
//                        ArrayList<Integer> move = new ArrayList<>();
//                        move.add(maxDifferenceVm.getId());
//                        move.add(newServerId[0]);
//                        move.add(3);
//                        realMove.add(move);
//                        //移动后要变更currentAllServerNodesOrder
////                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode1());
////                        currentAllServerNodesOrder.add(currentAllServers.get(maxServerNode.get(0).getIndex()).getNode2());
//
//
//                    }
//
//
//                }
//
//
//            }
//
//
//        }
//        return realMove;
//
//
//    }
    class IsCanMoveMaxTaskResult {
        double maxServerRatio = 0.;
        ArrayList<Servernode> maxServerNode = new ArrayList<>();
        IsCanMoveMaxTaskResult() {
            maxServerNode.add(null);
            maxServerNode.add(null);
        }
    }

    class IsCanMoveMaxTask extends RecursiveTask<IsCanMoveMaxTaskResult> {
        int thresh = 1500;
        int start;
        int end;

        VM addVm;
        double roitcpu;
        double roitram;

        IsCanMoveMaxTask(int start, int end, VM addVm, double roitcpu, double roitram) {
            this.start = start;
            this.end = end;

            this.addVm = addVm;
            this.roitcpu = roitcpu;
            this.roitram = roitram;
        }

        @Override
        protected IsCanMoveMaxTaskResult compute() {
            if (end - start <= thresh) {
                IsCanMoveMaxTaskResult result = new IsCanMoveMaxTaskResult();
                for (int k=start; k<end; k++) {

                    Server oneServer = currentAllServers.get(k);
                    int[] isArray = Tools.isCanArrange(oneServer, addVm);
                    if(addVm.getNodetype() ==0){

                        //判断该服务器是否可以满足部署条件
                        if(isArray[0] == 1){

                            //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                            Servernode servernode1 =oneServer.getNode1();
                            Servernode servernode2 =oneServer.getNode2();
                            //已经在该节点上部署，不能往该节点上迁移
                            if(!servernode1.getVmlist().contains(addVm.getId())){
                                double cpuRatio = 0.5 *(servernode1.getCpuisoccupied()+addVm.getNum_cpu())*1.0 / servernode1.getNum_cpu()  +0.5 *servernode2.getCpuisoccupied()  *1.0 / servernode2.getNum_cpu();
                                double ramRatio = 0.5 *(servernode1.getRAMisoccupied() +addVm.getRAM())*1.0 / servernode1.getRAM()  +0.5 *servernode2.getRAMisoccupied() *1.0 / servernode2.getRAM();

                                double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
                                if(serverRatio>result.maxServerRatio){
                                    result.maxServerNode.set(0,servernode1);
                                    result.maxServerRatio = serverRatio;
                                }
                            }


                        }
                        if(isArray[1] == 1){

                            //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                            Servernode servernode1 =oneServer.getNode1();
                            Servernode servernode2 =oneServer.getNode2();
                            //已经在该节点上部署，不能往该节点上迁移
                            if(!servernode2.getVmlist().contains(addVm.getId())){
                                double cpuRatio = 0.5 *servernode1.getCpuisoccupied()*1.0 / servernode1.getNum_cpu()  +0.5 *(servernode2.getCpuisoccupied() + addVm.getNum_cpu()) *1.0 / servernode2.getNum_cpu();
                                double ramRatio = 0.5 *servernode1.getRAMisoccupied() *1.0 / servernode1.getRAM()  +0.5 *(servernode2.getRAMisoccupied() +addVm.getRAM())*1.0 / servernode2.getRAM();

                                double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
                                if(serverRatio>result.maxServerRatio){
                                    result.maxServerNode.set(0,servernode2);
                                    result.maxServerRatio = serverRatio;
                                }
                            }



                        }

                    }else {
                        if(isArray[2] == 1){
                            //可以在一节点上部署计算假设部署后的cpu、ram，占用率
                            Servernode servernode1 =oneServer.getNode1();
                            Servernode servernode2 =oneServer.getNode2();
                            //已经在该节点上部署，不能往该节点上迁移
                            if(!servernode1.getVmlist().contains(addVm.getId())){
                                double cpuRatio = 0.5 *(servernode1.getCpuisoccupied()+addVm.getNum_cpu()*1.0/2)*1.0 / servernode1.getNum_cpu()  +0.5 *(servernode2.getCpuisoccupied() + addVm.getNum_cpu()*1.0/2) *1.0 / servernode2.getNum_cpu();
                                double ramRatio = 0.5 *(servernode1.getRAMisoccupied() +addVm.getRAM()*1.0/2)*1.0 / servernode1.getRAM()  +0.5 *(servernode2.getRAMisoccupied() +addVm.getRAM()*1.0/2)*1.0 / servernode2.getRAM();

                                double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
                                if(serverRatio > result.maxServerRatio){
                                    result.maxServerNode.set(0,servernode1);
                                    result.maxServerNode.set(1,servernode2);
                                    result.maxServerRatio = serverRatio;
                                }
                            }


                        }

                    }
                }
                return result;
            }
            else {
                int middle = (start + end) / 2;
                IsCanMoveMaxTask left = new IsCanMoveMaxTask(start, middle, addVm, roitcpu, roitram);
                IsCanMoveMaxTask right = new IsCanMoveMaxTask(middle, end, addVm, roitcpu, roitram);
                invokeAll(left, right);

                IsCanMoveMaxTaskResult leftResult = left.join();
                IsCanMoveMaxTaskResult rightResult = right.join();
                if (leftResult.maxServerRatio > rightResult.maxServerRatio)
                    return leftResult;
                else
                    return rightResult;
            }
        }

    }

    //判断是否可以迁移 可以返回要迁移的目的servernode,单节点[目标节点，null],双节点[同一服务器节点1，节点2]
    public static ArrayList<Servernode> isCanMove(VM addVm){
        //倒着往上找 找到第一个可以放的就放进去
        // currentAllServerNodesOrder
        //找addVm加入后该节点两个占用率最好的那个服务器
        double roitcpu=0.6;
        double roitram=0.4;
        if (false) {
            IsCanMoveMaxTaskResult result = null;
            try {
                result = pool.submit(tools.new IsCanMoveMaxTask(0, currentAllServers.size(), addVm, roitcpu, roitram)).get();
            } catch (Exception e) {
                System.out.println("IsCanMoveMaxTask Error");
            }
            double maxServerRatio = result.maxServerRatio;
            ArrayList<Servernode> maxServerNode = result.maxServerNode;
            return maxServerNode;
        }
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

                        double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
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

                        double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
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

                        double serverRatio = roitcpu*cpuRatio + roitram*ramRatio;
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
        //部署失败
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
     * @param i 第几天的请求
     * @return 是否部署成功
     */
    public static boolean vmDeploynOnCurrentServer(VM addVm,int i){
        //获取所部署的服务器
        ArrayList<Servernode> oneServerNode = isCanMove(addVm);
        int addVmNodetype =addVm.getNodetype();
        if(oneServerNode.get(0) == null){
            //没有部署的目的服务器
            return false;
        }else {

            //不用判断该服务器是否可以满足部署条件，前面已经判断，现在可以直接填写
            Server oneServer = currentAllServers.get(oneServerNode.get(0).getIndex());
            if (addVmNodetype == 0) {//单节点

                if (oneServerNode.get(0).getNodename().equals("A")) {
                    //在已有服务器节点1上部署
                    oneServer = Tools.arrangeVm(addVm, oneServer, 1,i);
                    currentAllServers.set(oneServer.getIndex(), oneServer);
                    return true;
                } else {
                    //在已有服务器节点2上部署
                    oneServer = Tools.arrangeVm(addVm, oneServer, 2,i);
                    currentAllServers.set(oneServer.getIndex(), oneServer);

                    return true;
                }
            } else if (addVmNodetype == 1) {//双节点
                //在已有服务器两节点上上部署
                oneServer = Tools.arrangeVm(addVm, oneServer, 3,i);
                currentAllServers.set(oneServer.getIndex(), oneServer);
                return true;

            }
            return false;

        }

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
//                        double den1 = (server1Price/ server1Gpu * 0.62 + 0.38 * server1Price/ server1Ram)+(Math.abs((server1Gpu/server1Ram)-(vmGpu/vmRam)))*30;
//                        double den2 = (server2Price/ server2Gpu * 0.62 + 0.38 * server2Price/ server2Ram)+(Math.abs((server2Gpu/server2Ram)-(vmGpu/vmRam)))*30;
                        double den1 = (server1Price * (0.62*(vmGpu/server1Gpu  ) + 0.38*(vmRam/server1Ram)))/2;
                        double den2 = (server2Price * (0.62*(vmGpu/server2Gpu  ) + 0.38*(vmRam/server2Ram)))/2;
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



    //计算每一天
    public static void processRequests(){
        //保存迁移输出信息
        ArrayList<String> moveInfo = new ArrayList<>();
        moveInfo.add("(migration, 0)");

        //判断是哪种类型的数据、饱和型 1、不饱和型 2
        //数据变化时，该部分也要变
//        double addNumber =0;
//        double delNumber =0;
//        int len = op.size();
//        for (int i = 0; i < (len/2); i++) {
//            //第i天的所有请求
//            ArrayList<String> today = op[i];
//            for (int j = 0; j < today.size(); j++) {
//
//                //当前这条请求String
//                String oneRequest = today.get(j);
//                oneRequest = oneRequest.substring(1, oneRequest.length() - 1);
//                String[] oneRequestInfo = oneRequest.split(", ");
//                //请求类型
//                String oneRequestType = oneRequestInfo[0];
//                if (oneRequestType.equals("add")) {
//                    //添加请求
//                    addNumber++;
//                } else if (oneRequestType.equals("del")) {
//                    //删除请求
//                    delNumber++;
//
//                }
//            }
//        }
//        if((delNumber/addNumber ) <0.75){
//            Tools.dataType =2;
//        }else {
//            Tools.dataType =1;
//        }

        for (int i = 0; i < op.size(); i++) {

            //第i天的所有请求
            ArrayList<String> today = op.get(i);
            //初始化存每天买的服务器、每天部署的虚拟机的ArrayList
            ArrayList<Integer> indexNumber = new ArrayList<>();
            dailyBuyServers.add(indexNumber);
            ArrayList<Integer> vmId = new ArrayList<>();
            dailyArrangeVM.add(vmId);

            //遍历每条请求
            for (int j = 0; j < today.size(); j++) {

                //当前这条请求String
                String oneRequest = today.get(j);
                oneRequest = oneRequest.substring(1, oneRequest.length() - 1);
                String[] oneRequestInfo = oneRequest.split(", ");
                //请求类型
                String oneRequestType = oneRequestInfo[0];
                if (oneRequestType.equals("add")) {
//                    System.out.println("添加请求");
                    //获取请求添加的VM的信息
                    String addVmModel = oneRequestInfo[1];
                    int addVmId = Integer.parseInt(oneRequestInfo[2]);
                    int addVmum_cpu = allVmObject.get(addVmModel).getNum_cpu();
                    int addVmRAM = allVmObject.get(addVmModel).getRAM();
                    int addVmNodetype = allVmObject.get(addVmModel).getNodetype();

                    VM addVm = new VM(addVmModel, addVmum_cpu, addVmRAM, addVmNodetype, addVmId);

//                    System.out.println(addVmId+"  "+addVmModel+"  "+addVmum_cpu+"  "+addVmRAM+"  "+addVmNodetype);
                    //遍历当前已购买服务器，是否有可部署的服务器
                    Boolean isArrange = false;//记录原来已经买的服务器是否可以完成此虚拟机的部署

                    //在当前已买服务器上部署虚拟机，如果存在可部署服务器，则部署返回true,如果不存在可部署服务器返回false 下面购买
                    isArrange = vmDeploynOnCurrentServer(addVm, i);

                    if (isArrange == false) {
                        //已有服务器不可以部署该虚拟机，选择服务器 部署虚拟机,第 i天部署
                        Tools.buyServer(addVm, i);

                    }

                } else if (oneRequestType.equals("del")) {
//                    System.out.println("删除请求");
                    int deleteVmId = Integer.parseInt(oneRequestInfo[1]);
                    deleteVM(deleteVmId);


                } else {
                    System.out.println("请求错误！！！");


                }
            }


            //输出

            //统计第i天需要买什么
//            System.out.println(dailyBuyServers.get(i));
            ArrayList<Integer> serverIdNumber = dailyBuyServers.get(i);
            //<型号，这一天该类服务器的所有类>
            HashMap<String, ArrayList<Server>> serverNumberObject = new HashMap<>();
            for (int e = 0; e < serverIdNumber.size(); e++) {//遍历买什么
                Integer serverId = serverIdNumber.get(e);
                Server server = currentAllServers.get(serverId);


                if (serverNumberObject.containsKey(server.getModel())) {
                    serverNumberObject.get(server.getModel()).add(server);
                } else {
                    ArrayList<Server> oneTypeServer = new ArrayList<>();
                    oneTypeServer.add(server);
                    serverNumberObject.put(server.getModel(), oneTypeServer);
                }
            }
            //输出每天买多少服务器
            System.out.println("(purchase, " + serverNumberObject.size() + ")");
            for (String serverType : serverNumberObject.keySet()) {
                System.out.println("(" + serverType + ", " + serverNumberObject.get(serverType).size() + ")");

                //构建编号映射hashMap <申请时服务器的编号, 输出时服务器的编号>
                for (Server oneserver : serverNumberObject.get(serverType)) {

                    severNumberMapping.put(oneserver.getIndex(), numberCount++);

                }
            }
            //计算一共可迁移几个虚拟机 5/1000 *所有虚拟机个数


            int canMoveNumber = (int) Math.floor(currentVm.size() * 30.0 / 1000);

//            int canMoveNumber =0;

            //输出迁移方案
            for (int z = 0; z < moveInfo.size(); z++) {
                System.out.println(moveInfo.get(z));
            }
            //输出节点怎么部署
//            System.out.println(dailyArrangeVM.get(i));
            ArrayList<Integer> vmIdNumber = dailyArrangeVM.get(i);
            for (int r = 0; r < vmIdNumber.size(); r++) {

                ArrayList<Integer> serveridList = VMidInServer.get(vmIdNumber.get(r));//部署的服务器
                int serverid = serveridList.get(0);//服务器编号
                int nodetype = serveridList.get(1);//服务器节点1，2，表示节点，3表示双节点
                if (nodetype == 1) {
                    System.out.println("(" + severNumberMapping.get(serverid) + ", A)");
                } else if (nodetype == 2) {
                    System.out.println("(" + severNumberMapping.get(serverid) + ", B)");

                } else if (nodetype == 3) {
                    System.out.println("(" + severNumberMapping.get(serverid) + ")");
                }
            }


            //清空 moveInfo
            moveInfo.clear();

            //计算下一天的迁移方案
            //迁移
            //格式为(虚拟机 ID, 目的服务器 ID)或 (虚拟机 ID, 目的服务器 ID, 目的服务器节点)。
            ArrayList<ArrayList<Integer>> realMove = move(canMoveNumber);
            if (realMove == null) {
                moveInfo.add("(migration, 0)");
//                System.out.println("(migration, 0)");
            } else {
                int realMoveNumber = realMove.size();
                moveInfo.add("(migration, " + realMoveNumber + ")");
//                System.out.println("(migration, " +realMoveNumber+")");
                for (int x = 0; x < realMoveNumber; x++) {
                    ArrayList<Integer> realMoveData = realMove.get(x);
                    if (realMoveData.get(2) == 1) {
                        moveInfo.add("(" + realMoveData.get(0) + ", " + severNumberMapping.get(realMoveData.get(1)) + ", " + "A)");
//                        System.out.println("("+realMoveData.get(0)+", "+severNumberMapping.get(realMoveData.get(1)) +", "+"A)");

                    } else if (realMoveData.get(2) == 2) {
                        moveInfo.add("(" + realMoveData.get(0) + ", " + severNumberMapping.get(realMoveData.get(1)) + ", " + "B)");
//                        System.out.println("("+realMoveData.get(0)+", "+severNumberMapping.get(realMoveData.get(1)) +", "+"B)");

                    } else if (realMoveData.get(2) == 3) {
                        moveInfo.add("(" + realMoveData.get(0) + ", " + severNumberMapping.get(realMoveData.get(1)) + ")");
//                        System.out.println("("+realMoveData.get(0)+", "+severNumberMapping.get(realMoveData.get(1)) +")");

                    }
                }

            }
            if( op.size() <dayNumber){
                //当i输出第i天后，获取第K+i天的信息
                ArrayList<String>requests = getdata.getOneDayRequest();
                op.add(requests);
            }

            //计算gpu占用率和ram占用率
            double allCpuRatio = 0;
            double allRamRatio = 0;
            int size = currentAllServers.size();
//            System.out.println("一共需要"+size+"个服务器");
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
            double onecpu =allCpuRatio/size;
            double oneram = allRamRatio/size;
            currentCpuRatio = onecpu;
            currentRamRatio = oneram;
            cpu.add(onecpu);
            ram.add(oneram);

        }
        try {
            if(getdata.bufferedReader!=null)
                getdata.bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


//        //输出gpu占用率和ram占用率
//        double allCpuRatio = 0;
//        double allRamRatio = 0;
//        int size = currentAllServers.size();
//        System.out.println("一共需要"+size+"个服务器");
//        for(int m =0 ;m< size;m++){
//            Server server = currentAllServers.get(m);
//            Servernode servernode1 =server.getNode1();
//            Servernode servernode2 =server.getNode2();
//
//            double cpuRatio = 0.5 *servernode1.getCpuisoccupied() *1.0 / servernode1.getNum_cpu()  +0.5 *servernode2.getCpuisoccupied() *1.0 / servernode2.getNum_cpu();
//
//            double ramRatio = 0.5 *servernode1.getRAMisoccupied() *1.0 / servernode1.getRAM()  +0.5 *servernode2.getRAMisoccupied() *1.0 / servernode2.getRAM();
//
////            System.out.println("cpuRatio : "+cpuRatio +"ramRatio : "+ramRatio);
//            allCpuRatio += cpuRatio;
//            allRamRatio += ramRatio;
//        }
//        System.out.println("allCpuRatio : "+ allCpuRatio/size + "allRamRatio : "+allRamRatio/size);
//
//
    }





}
