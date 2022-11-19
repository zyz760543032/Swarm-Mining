import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class AISUtils {
    protected final static int EARTH_RADIUS = 6371000;
    static int newMMSI = 0;
    static final int gridNumLat = 100, kd = 8, kc = 5;
    static final double minLat = 21.48, maxLat = 30.4837, minLon = -97.5778, maxLon = -75.49, gridSizeLat
            = (maxLat - minLat) / gridNumLat;
    static final int gridNumLon = (int) Math.round(gridNumLat * (maxLon - minLon) / (maxLat - minLat) *
            Math.cos((minLat + maxLat) / 2 * Math.PI / 180));
    static final double gridSizeLon = (maxLon - minLon) / gridNumLon, lenPerLat = 111.31955 * 1000;
    static final double gridLatLen = gridSizeLat * lenPerLat; //一纬度约为111公里
    static String path = "D:\\AISPlotDataload\\swarm.csv";
    static String resamplePath = "D:\\AISPlotDataload\\swarmResample.csv";

    static List<AISTrajectory> readAmerica(int m1, int m2, int d) throws IOException, ParseException {
        final int[] days = new int[]{0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        Map<String, List<AIStuple>> map = new HashMap<>();
        List<AIStuple> AIStuples = new ArrayList<>();
        List<List<AIStuple>> tras = new ArrayList<>();
        final String root = "E:\\AIS_DATA\\AIS_2019";
        boolean last = false;
        for (int month = m1; month <= m2; month++) {
            String mstr = month < 10 ? "_0" + month : "_" + month;
            for (int day = 1; day <= Math.min(days[month], d); day++) {
                if (month == m2 && day == d) {
                    last = true;
                }
                String dstr = day < 10 ? "_0" + day : "_" + day;
                String path = root + mstr + dstr + ".csv";
                AIStuples.addAll(locSelect(path));
                tras.addAll(preprocess(AIStuples, map, last));
                AIStuples.clear();
                System.out.println(mstr + "月" + dstr + "日已处理完毕");
            }
        }
        List<AISTrajectory> trajectories = new ArrayList<>();
        for (List<AIStuple> tra : tras) {
            if (tra.get(0).getMMSI().equals("2706")) {
                int a = 1;
            }
            trajectories.add(new AISTrajectory(tra));
        }
        return trajectories;
    }

    static List<AISTrajectory> groupByMMSI(List<AIStuple> tuples) {
        Map<String, List<AIStuple>> map = new HashMap<>();
        for (AIStuple t : tuples) {
            List<AIStuple> tra;
            if (map.containsKey(t.getMMSI())) {
                tra = map.get(t.getMMSI());
            } else {
                tra = new ArrayList<>();
            }
            tra.add(t);
            map.put(t.getMMSI(), tra);
        }
        List<AISTrajectory> trajectories = new ArrayList<>();
        map.forEach((mmsi, tra) -> {
            trajectories.add(new AISTrajectory(tra));
        });
        return trajectories;
    }

    static List<AISTrajectory> readMulMon(int m1, int m2) throws IOException, ParseException {
        return readAmerica(m1, m2, 31);
    }


    static List<AISTrajectory> readSinMon(int m, int d) throws IOException, ParseException {
        return readAmerica(m, m, d);
    }


    /**
     * @param tuples
     * @param map    键为mmsi，值为按时间顺序排列的元组
     * @param last   是最后一份文件吗
     * @return 预处理轨迹集
     * @throws ParseException
     */
    public static List<List<AIStuple>> preprocess(List<AIStuple> tuples, Map<String, List<AIStuple>> map, boolean last)
            throws ParseException {
        final double maxInterval = lenPerLat * 0.25; // 轨迹点之间最大间隔
        final double seLen = lenPerLat * 0.3; // 首末点最小距离
//        final double shortestLen = lenPerLat * 2;
        final double shortestLen = lenPerLat * 0.7;  // 轨迹最短长度
        final int maxTimeInterval = 10;//小时
        List<List<AIStuple>> newTras = new ArrayList<>();
        if (tuples.size() > 0) {
            /*
                String month = tuples.get(0).getDate().toString().split(" ")[1];
                int day = Integer.parseInt(tuples.get(0).getDate().toString().split(" ")[2]);
            */
            // 按mmsi分类
            for (AIStuple t : tuples) {
                List<AIStuple> tra = map.getOrDefault(t.getMMSI(), new ArrayList<>());
                tra.add(t);
                map.put(t.getMMSI(), tra);
            }
            tuples.clear();
            Iterator<Map.Entry<String, List<AIStuple>>> iterator = map.entrySet().iterator();
            List<AIStuple> traConcurrentModification ;
            while (iterator.hasNext()) {
                double len = 0;
                Map.Entry<String, List<AIStuple>> kv = iterator.next();
                List<AIStuple> tra = kv.getValue();
                tra.sort(Comparator.comparing(AIStuple::getDate));
                traConcurrentModification = tra;
                int k0 = 0;
                double distance;
                for (int k = 1; k < tra.size(); k++) {
                    distance = tra.get(k).distanceFrom(tra.get(k - 1), SimilarityMeasureMethod.HAVERSINE_DISTANCE);
                    long timeDis = tra.get(k).getDate().getTime() - tra.get(k - 1).getDate().getTime();
                    //分割距离过远或时间间隔过大或为最后一个点且该last为真的轨迹
                    if (distance > maxInterval || timeDis / 1000 / 60 / 60 > maxTimeInterval || k == tra.size() - 1 && last) {
                        // 轨迹的长度len要大于shortestLen，否则忽略
                        if (tra.get(k).distanceFrom(tra.get(k0), SimilarityMeasureMethod.HAVERSINE_DISTANCE) > seLen && len > shortestLen) {
                            List<AIStuple> tempTra = new ArrayList<>(tra.subList(k0, k));
                            tempTra.forEach(tuple -> tuple.setMMSI(newMMSI + ""));
                            newTras.add(tempTra);
                            newMMSI++;
                        }
                        traConcurrentModification = tra.subList(k, tra.size());
                        len = 0;
                        k0 = k;
                    } else {
                        len += distance;
                    }
                }
                map.put(kv.getKey(),traConcurrentModification);
            }
        }
        System.out.println(newMMSI);
        return newTras;
    }


    static List<AIStuple> locSelect(String path) {
        // 2 纬度 3 经度 4 速度 5 方向 10 船舶类型
        List<AIStuple> AIStuples = new ArrayList<>();
        try (BufferedReader raw = new BufferedReader(new FileReader(path))) {
            raw.readLine();
            int i = 1;
            String tuple = raw.readLine();
            while (tuple != null) {
                if (i % 10e5 == 0) {
                    System.out.println("第" + (int) (i / 10e5) + "十万条AIS记录");
                }
                i++;
                // 筛选位置
                String[] tupleSplited = tuple.split(",");
                double lat = Double.parseDouble(tupleSplited[2]);
                double lon = Double.parseDouble(tupleSplited[3]);
                String mmsi = tupleSplited[0];
                double cog = Double.parseDouble(tupleSplited[5]);
                double sog = Double.parseDouble(tupleSplited[4]);
                if (lat > minLat && lat < maxLat && lon < maxLon && lon > minLon) {
                    AIStuples.add(new AIStuple(mmsi, lat, lon, tupleSplited[1], cog, sog));
                }
                tuple = raw.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return AIStuples;
    }

    static void writeTras(String path, List<AISTrajectory> tras) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        for (int i = 0; i < tras.size(); i++) {
            System.out.println("正在写入第 " + i + " 条轨迹");
            AISTrajectory tra = tras.get(i);
            for (int j = 0; j < tra.getLength(); j++) {
                AIStuple t = tra.getTupleByIndex(j);
                if (j != 0) {
                    bw.write("*");
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                bw.write(t.getMMSI() + "," + t.getLatitude() + "," + t.getLongitude() + "," + t.getCOG() + "," + t.getSOG() + "," + sdf.format(t.getDate()));
            }
            if (i < tras.size() - 1) bw.newLine();
        }
        bw.close();
    }

    static List<AISTrajectory> readTras(String path, int num1, int num2) throws IOException, ParseException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String traLine;
        List<AISTrajectory> tras = new ArrayList<>();
        int i = 1;
        while ((traLine = br.readLine()) != null) {
            System.out.println("正在读取第 " + i++ + " 条轨迹");
            if (i >= num1 && i < num2) {
                List<AIStuple> tra = new ArrayList<>();
                String[] tuples = traLine.split("\\*");
                for (String s : tuples) {
                    String[] tupleStr = s.split(",");
                    AIStuple tuple = new AIStuple(tupleStr[0], Double.parseDouble(tupleStr[1]), Double.parseDouble(tupleStr[2]), tupleStr[5], Double.parseDouble(tupleStr[3]), Double.parseDouble(tupleStr[4]));
                    tra.add(tuple);
                }
                tras.add(new AISTrajectory(tra));
            }
        }
        br.close();
        return tras;
    }

    protected static long millisecondOfTimeUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MILLISECOND:
                return 1;
            case SECOND:
                return 1000;
            case MINUTE:
                return 1000 * 60;
            case HOUR:
                return 1000 * 60 * 60;
            case DAY:
                return 1000 * 60 * 60 * 24;
            default:
                return -1;
        }
    }

    public static List<AISTrajectory> readTras(String path) throws IOException, ParseException {
        return readTras(path, 0, Integer.MAX_VALUE);
    }

}
