import java.util.*;

public class ModeMining {
    private int minT,minO;

    public int getMinT() {
        return minT;
    }

    public void setMinT(int minT) {
        this.minT = minT;
    }

    public int getMinO() {
        return minO;
    }

    public void setMinO(int minO) {
        this.minO = minO;
    }



    ModeMining(int minO, int minT){
        this.minT=minT;
        this.minO = minO;
    }


    Map<Integer,TreeMap<Date, Integer>> clusters = new HashMap<>();
    // objectsNum为对象数量，对象序号从0到objectsNum-1
    Integer objectsNum;
    List<Date> times;

    /**
     * 蜂群挖掘
     * @param originalMap dbscan的输出
     * @param beginTime 蜂群挖掘起始时间
     * @param endTime 蜂群挖掘终止时间
     * @param interval 重采样间隔
     * @param timeUnit 时间单位
     * @param objectsNum 物体数量
     * @return 闭蜂群集合
     */
    public List<ObjectTime> initate(Map<Date, List<List<AIStuple>>> originalMap, Date beginTime, Date endTime, int interval, TimeUnit timeUnit, Integer objectsNum){
        // 生成以时间为上层键，物体序号为二层键，簇号为值，簇号只在一个时段内有效
        for (Map.Entry<Date, List<List<AIStuple>>> kv : originalMap.entrySet()) {
            Date time = kv.getKey();
            List<List<AIStuple>> clustersEachTime = kv.getValue();
            for (int clusterNum = 1; clusterNum < clustersEachTime.size(); clusterNum++) {
                List<AIStuple> cluster = clustersEachTime.get(clusterNum);
                for (AIStuple tuple : cluster) {
                    TreeMap<Date, Integer> mapOfObject =  clusters.getOrDefault(Integer.parseInt(tuple.getMMSI()), new TreeMap<>());
                    mapOfObject.put(time, clusterNum);
                    clusters.put(Integer.parseInt(tuple.getMMSI()), mapOfObject);
                }
            }
        }
        this.objectsNum = objectsNum;
        times = new ArrayList<>();
        for(long time = beginTime.getTime();time<=endTime.getTime();time+=AISUtils.millisecondOfTimeUnit(timeUnit)*interval){
            this.times.add(new Date(time));
        }
        TreeSet<Integer> O = new TreeSet<>();
        O.add(-1);
        return ObjectGrowth(new ObjectTime(O,this.times));
    }

    private boolean aprioriPruning(ObjectTime objectTime){
        return objectTime.getTimes().size() < minT;
    }

    private boolean backwardPruning(ObjectTime objectTime){
        for(int object=0;object<objectTime.getObjects().last();object++) {
            boolean flag = true;
            if (!objectTime.getObjects().contains(object)) {
                Map<Date, Integer> map = clusters.get(object);
                if(map!=null) {
                    for (Date time : objectTime.getTimes()) {
                        if (!map.containsKey(time) || !Objects.equals(map.get(time), clusters.get(objectTime.getObjects().last()).get(time))) {
                            flag = false;
                        }
                    }
                    if(flag){
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private List<Date> forwardClosureChecking(ObjectTime objectTime,Integer object, Boolean CONVEY){
        Map<Date, Integer> map = clusters.get(object);
        List<Date> newTimes = new ArrayList<>(),currentTimes = new ArrayList<>();
        if(map==null){
            return new ArrayList<>();
        }
        for (Date time : objectTime.getTimes()) {
            if (map.containsKey(time) && (objectTime.getObjectsNum()==0 || Objects.equals(map.get(time), clusters.get(objectTime.getObjects().last()).get(time)))) {
                if(CONVEY) {
                    currentTimes.add(time);
                }else {
                    newTimes.add(time);
                }
            }else if(CONVEY){
                if(newTimes.size()<currentTimes.size()){
                    newTimes = new ArrayList<>(currentTimes);
                }
                currentTimes.clear();
            }
        }
        return newTimes;
    }

    private List<ObjectTime> ObjectGrowth(ObjectTime objectTime){

        List<ObjectTime> res = new ArrayList<>();
        if(aprioriPruning(objectTime)){
            return new ArrayList<>();
        }
        if(backwardPruning(objectTime)){
            return new ArrayList<>();
        }
        boolean isClosed = true;
        final boolean CONVEY = true;
        for(int object = objectTime.getObjects().last()+1; object<this.objectsNum; object++){
            List<Date> newTimes = forwardClosureChecking(objectTime,object,CONVEY);
            if(newTimes.size()==objectTime.getTimes().size()){
                isClosed = false;
            }
            TreeSet<Integer> objectsAdded= objectTime.getObjects();
            objectsAdded.add(object);
            ObjectTime newObjectTime = new ObjectTime(objectsAdded,newTimes);
            res.addAll(ObjectGrowth(newObjectTime));
            newObjectTime.getObjects().remove(newObjectTime.getObjects().last());
        }
        if(isClosed&&objectTime.getObjectsNum()>=minO){
            res.add(objectTime);
            System.out.println("a new closed convey is found: "+objectTime);
        }
        return res;
    }

}
