import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AISTrajectory {
    private List<AIStuple> trajectory ;

    @Override
    public String toString() {
        SimpleDateFormat sdf ;
        sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        StringBuilder res =new StringBuilder();
        for (AIStuple tuple : trajectory) {
            res.append("MMSI: ").append(this.getTupleByIndex(0).getMMSI()).append("longitude: ").append(tuple.getLongitude()).append("\tlatitude: ").append(tuple.getLatitude()).append("\tCOG: ").append(tuple.getCOG()).append("\tSOG: ").append(tuple.getSOG()).append("\tTime: ").append(sdf.format(tuple.getDate())).append("\n\r");
        }
        return res.toString();
    }
    public String getMMSI(){
        return trajectory.get(0).getMMSI();
    }

    public List<AIStuple> getTrajectory() {
        return trajectory;
    }

    public void setTrajectory(List<AIStuple> trajectory) {
        this.trajectory = trajectory;
    }

    public AIStuple getTupleByIndex(int i){
        return trajectory.get(i);
    }

    AISTrajectory(List<AIStuple> tra){
        trajectory = tra;
    }
    AISTrajectory(){
        trajectory = new ArrayList<>();
    }


    public int getLength() {
        return trajectory.size();
    }



    /**
     * 功能：从p1开始算起，每隔interval时间重采样一个点。这个方法保证了所有轨迹点的时间间隔是相同的。
     * 流程：unixTime为假：从pi开始向后遍历，计算从pi开始的子轨迹时段timeDifference 一旦timeDifference大于等于interval  unixTime为真：计算currentPointIntervalNum是否大于firstPointIntervalNum  找到interval对应的新点newTuple，newTuple的属性由ratio确定
     * @param interval：重采样的时间间隔，重采样后的两两相邻轨迹点时间间隔相同，且其他属性按线性插值处理
     * @param timeUnit: 时间单位
     * @param unixTime: true表示以1970/1/1 00:00:00开始采样，false表示以第一个轨迹点的时间开始采样
     * @return 重采样轨迹
     */
    public AISTrajectory resampleByTime(int interval, TimeUnit timeUnit, boolean unixTime) throws AIStupleException {
        List<AIStuple> newTrajectory = new ArrayList<>();
        int currentPointIdx=1;
        AIStuple firstPoint = this.getTupleByIndex(0),currentPoint;
        if(!unixTime) {
            newTrajectory.add(firstPoint);
        }
        int millisecondOfInterval=(int) (AISUtils.millisecondOfTimeUnit(timeUnit) * interval);
        AIStuple previousCurrentPoint = firstPoint;
        int firstPointIntervalNum=-1,currentPointIntervalNum=-1;
        firstPointIntervalNum = (int)(firstPoint.getDate().getTime() / millisecondOfInterval);

        while (currentPointIdx<this.getLength()){
            currentPoint = this.getTupleByIndex(currentPointIdx);
            double timeDifference = -1;

            if(!unixTime) {
                timeDifference = currentPoint.timeInterval(firstPoint, timeUnit);
            }else {
                currentPointIntervalNum = (int)(currentPoint.getDate().getTime() / millisecondOfInterval);
            }


            if(!unixTime&&timeDifference >=interval||unixTime&&currentPointIntervalNum >firstPointIntervalNum){

                // 分割点位于currentPoint和currentPoint的前一个点之间
                double splitTimeTotalLength = currentPoint.timeInterval(previousCurrentPoint,timeUnit);
                double splitTimePartLength;
                if(!unixTime) {
                    splitTimeTotalLength = currentPoint.timeInterval(previousCurrentPoint,timeUnit);
                    splitTimePartLength = interval - firstPoint.timeInterval(previousCurrentPoint, timeUnit);
                }else {
                    splitTimeTotalLength = currentPoint.timeInterval(previousCurrentPoint, TimeUnit.MILLISECOND);
                    splitTimePartLength = (long) (firstPointIntervalNum + 1) * millisecondOfInterval - previousCurrentPoint.getDate().getTime();
                }
                double ratio = splitTimePartLength / splitTimeTotalLength;

                AIStuple newTuple = new AIStuple();

                newTuple.setMMSI(currentPoint.getMMSI());
                newTuple.setLatitude(previousCurrentPoint.getLatitude()+(currentPoint.getLatitude()-previousCurrentPoint.getLatitude())*ratio);

                newTuple.setLongitude(previousCurrentPoint.getLongitude()+(currentPoint.getLongitude()-previousCurrentPoint.getLongitude())*ratio);

                if(!unixTime) {
                    newTuple.setDate(new Date(previousCurrentPoint.getDate().getTime() + (long) ((currentPoint.getDate().getTime() - previousCurrentPoint.getDate().getTime()) * ratio)));
                }else {
                    newTuple.setDate(new Date((long) (firstPointIntervalNum + 1) *millisecondOfInterval));
                }

                newTuple.setCOG(previousCurrentPoint.getCOG()+(currentPoint.getCOG()-previousCurrentPoint.getCOG())*ratio);

                newTuple.setSOG(previousCurrentPoint.getSOG()+(currentPoint.getSOG()-previousCurrentPoint.getSOG())*ratio);
                newTrajectory.add(newTuple);
                firstPoint = newTuple;
                previousCurrentPoint = newTuple;
                if(unixTime) {
                    firstPointIntervalNum = (int) (firstPoint.getDate().getTime() / millisecondOfInterval);
                }
            }else {
                previousCurrentPoint = trajectory.get(currentPointIdx++);
            }
        }
        if(newTrajectory.get(0).getMMSI().equals("2704")){
            int a = 1;
        }
        return new AISTrajectory(newTrajectory);
    }



    public static void main(String[] args) throws AIStupleException {
        AISTrajectory tra = new AISTrajectory(Arrays.asList(new AIStuple(1.0, 0.0,new Date(0)), new AIStuple(0.0, 0.0,new Date(1)), new AIStuple(0.0, 1.0,new Date(6)), new AIStuple(10.0, 0.0,new Date(12)), new AIStuple(11.0, 0.0,new Date(20))));
        System.out.println(tra);
    }


}
