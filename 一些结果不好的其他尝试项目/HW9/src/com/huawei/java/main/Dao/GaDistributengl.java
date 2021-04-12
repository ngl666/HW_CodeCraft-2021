package com.huawei.java.main.Dao;

import com.huawei.java.main.lib.VM;
import com.huawei.java.main.lib.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ngl 2021/3/15
 * 遗传算法解决虚拟机问题
 */

public class GaDistributengl {
    public String[] resultD;                   //最终输出结果
    public List<VM> vmList;                 //输入虚拟机集合
    public List<Server> serverList;                 //输入服务器集合
    public List<VM> optPerson;             //遗传变异后选出的最优个体
    public List<Server> optServerList;         //最优个体对应的放置结果
    public int[] fDataArray;				   //根据放置结果修改的预测结果





    //获取需要部署的VM
//    public  List<VM> GetInitPerson(){
//
//    }



    /**
     * 获取初始种群
     * @param size        种群规模
     * @return 初始种群
     */
//    public List<List<VM>> GetInitialPopulation(int size){
//        List<List<VM>> initialPopulation = new ArrayList<List<VM>>();
//        List<VM> initialPerson = GetInitPerson();     //初始个体，即需要部署的VM
//        /*1.对初始个体进行排序，加快种群的收敛速度*/
//        int k = initPerson.size();
//        for(int i=0; i<k-1; i++){
//            for(int j=i+1; j<k; j++){
//                if(initPerson.get(i).cpu<initPerson.get(j).cpu){   //按cpu或者mem排序最后结果都是一样的
//                    Flavor flavor = initPerson.get(i);
//                    initPerson.set(i, initPerson.get(j));
//                    initPerson.set(j, flavor);
//                }
//            }
//        }
//        /*2.开始产生初始种群*/
//
//
//        /**
//         * 第三种(适用于种群规模为5的)：从大到小来一个 然后根据这个的变异来一个  大小循环来一个 然后根据这个的变异来一个 再随机来一个
//         */
//        List<Flavor> crossPerson = GetCrossPerson(inputFList, fDataArray);
//
//        initPopulation.add(initPerson);
//        initPopulation.add(GetChild(initPerson));
//        initPopulation.add(GetChild(initPerson));
//        initPopulation.add(GetChaoticPerson(initPerson));
//        initPopulation.add(GetChaoticPerson(initPerson));
//        initPopulation.add(GetChaoticPerson(initPerson));
//        initPopulation.add(crossPerson);
//        initPopulation.add(GetChild(crossPerson));
//        initPopulation.add(GetChild(crossPerson));
//        initPopulation.add(GetChild(crossPerson));
//
//        return initPopulation;
//    }
//
//
//    //构造函数
//    public  GaDistributengl(){
//        //设置服务器集合
//        for(String serverType :Tools.allServerObject.keySet()){
//            this.serverList.add(Tools.allServerObject.get(serverType));
//
//        }
//        //设置虚拟机集合
//        for(String vmType :Tools.allVmObject.keySet()){
//            this.vmList.add(Tools.allVmObject.get(vmType));
//        }
//
//        int size = 10;            //种群规模     这个也可能要改大
//        double maxFitness = 0.98; //最大适应度值      计算服务器集合的总利用率即为个体的适应度值,这个要改
//        int maxEpoch = 50;        //最大遗传代数
//
//
//
//        List<List<VM>> initialPopulation =GetInitialPopulation(size);  //初始种群
//
//
//
//
//
//        this.optPerson = GenAlg(initPopulation,maxEpoch, maxFitness);     //最优个体
//
//        this.optServerList = PutVMtoServer(this.optPerson);     //放置结果
//
//        /*判断最后一个服务器的利用率是否低于给定阈值，若果太小则将对应预测虚拟机删除*/
//        double minRato = 0.5;                //利用率最低阈值
//        double maxRato = 0.5;                //利用率最高阈值
//        int n = optServerList.size();        //放置使用服务器个数
//        double lastRato = 0.5*optServerList.get(n-1).GetCpuUseRate()+0.5*optServerList.get(n-1).GetMemUseRate();   //最后一个服务器的利用率
//        if(lastRato<=minRato){
//            Server server = optServerList.get(n-1);   //获取最后一个服务器
//            ClearLastServer(server);                  //将最后一个服务器的虚拟机从预测结果中删除
//            optServerList.remove(n-1);                //将最后一个服务器从服务器集合中移除
//        }else if(lastRato>maxRato){                   //如果最后一个服务器的利用率超过上限则对预测的服务器进行添加，使得最后的利用率最高
//            Server server = optServerList.get(n-1);   //只修改最后一个服务器
//            fillLastServer(server);                   //将最后一个服务器装满
//        }
//
//        GettotalRato();
//
//        SetresultD(this.optServerList);      //设置输出结果
//
//    }








    /**
     * 测试函数
     * @param args
     */
    public static void main(String[] args) {
        Tools.downLoad();

        GaDistributengl distribute=new GaDistributengl();
        //String[] resultS = distribute.resultD;
        //distribute.GettotalRato();
        System.out.println(Arrays.toString(distribute.fDataArray));
		/*for(int i=0; i<resultS.length; i++){
			System.out.println(resultS[i]);
		}*/

    }


}
