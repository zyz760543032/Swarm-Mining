import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class Test extends AISUtils{

    private static void testReadFile() throws IOException, ParseException {
        List<AISTrajectory> tras = readSinMon(5, 3);
        writeTras(path,tras);
    }

    /**
     * 测试resampleByTime方法
     *
     * @throws AIStupleException
     * @throws IOException
     * @throws ParseException
     */
    private static void testResampleByTime() throws AIStupleException, IOException, ParseException {
        List<AISTrajectory> tras = readTras(path);
        int b = 0;
        List<AISTrajectory> newTras = new ArrayList<>();
        for (int i = 0; i < tras.size(); i++) {
            System.out.println("重采样第" + i + "条轨迹");
            AISTrajectory tra = tras.get(i);
            tra = tra.resampleByTime(10, TimeUnit.MINUTE, true);
            if (tra.getLength() >= 2) {
                newTras.add(tra);
            }
        }
        int a = 1;
        writeTras(resamplePath,newTras);
    }


    /**
     * 测试对重采样轨迹dbscan
     *
     * @throws IOException
     * @throws ParseException
     * @throws AIStupleException
     */
    private static Map<Date, List<List<AIStuple>>>testDbscanGroupedByTime() throws IOException, ParseException, AIStupleException {
        final int minPoints = 3;
        final double distance = 500;
        List<AISTrajectory> tras = readTras(resamplePath);
        // 按时间将元组分组
        Map<Date, List<AIStuple>> timeBins = new HashMap<>();
        int b = 0;
        objectNum = tras.size();
        for (int i = 0; i < tras.size(); i++) {
            System.out.println("正在对 " + i + " 号轨迹按时间分组");
            AISTrajectory tra = tras.get(i);
            Map<Date, List<AIStuple>> mapTemp = tra.getTrajectory().stream().collect(groupingBy(AIStuple::getDate));
            mapTemp.forEach((date, tuples) -> {
                List<AIStuple> tuplesOriginal = timeBins.getOrDefault(date, new ArrayList<>());
                tuplesOriginal.addAll(tuples);
                timeBins.put(date, tuplesOriginal);
            });
        }
        // 按时间对元组聚类
        Map<Date, List<List<AIStuple>>> clusters = new HashMap<>();
        DBSCAN dbscan = new DBSCAN(distance, minPoints, SimilarityMeasureMethod.HAVERSINE_DISTANCE);
        int i = 1;
        for (Map.Entry<Date, List<AIStuple>> entry : timeBins.entrySet()) {
            System.out.println("正在对第 " + i++ + " 个时刻聚类，共 " + timeBins.entrySet().size() + " 个时刻");
            Date time = entry.getKey();
            List<AIStuple> tuplesWithSameTime = entry.getValue();
            clusters.put(time, dbscan.fit(tuplesWithSameTime));
        }
        return clusters;
    }

    static int objectNum=-1;
    private static void testForObjectGrowth() throws IOException, ParseException, AIStupleException {
        Map<Date, List<List<AIStuple>>> map = testDbscanGroupedByTime();
        String beginDate = "2019-05-01 00:00:00";
        String endDate = "2019-05-03 23:59:59";
        TimeUnit timeUnit = TimeUnit.MINUTE;
        ModeMining objectGrowth = new ModeMining(2,30);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date begin = simpleDateFormat.parse(beginDate);
        Date end = simpleDateFormat.parse(endDate);
        List<ObjectTime> closedSwarms = objectGrowth.initate(map, begin, end, 10, timeUnit, objectNum);
    }

    public static void main(String[] args) throws IOException, ParseException, AIStupleException {
        testForObjectGrowth();

    }
}
