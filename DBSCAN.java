import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:zyz760543032
 */
public class DBSCAN {

    private double distance;
    private int minPoints;
    private SimilarityMeasureMethod measureMethod;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getMinPoints() {
        return this.minPoints;
    }

    public void setMinPoints(int minPoints) {
        this.minPoints = minPoints;
    }

    public DBSCAN(double dist, int minPoints, SimilarityMeasureMethod measureMethod){
        if (!(dist < 0.0) && minPoints >= 1) {
            this.distance = dist;
            this.minPoints = minPoints;
            this.measureMethod = measureMethod;
        } else {
            throw new ErrorParameter();
        }
    }

    public DBSCAN(double dist, int minPoints) throws ErrorParameter {
        this(dist,minPoints, SimilarityMeasureMethod.HAVERSINE_DISTANCE);
    }

    /**
     * @description
     * @param: points 待聚类数据
     * @return: 聚类结果，0号元素为噪声
     */
    public List<List<AIStuple>> fit(List<AIStuple> points) {
        List<List<Integer>> clusters = new ArrayList<>();
        clusters.add(new ArrayList<>());
        Map<Integer, PointStatus> status = new HashMap<>();

        for(int i = 0; i < points.size(); i++) {
            if (status.get(i) != PointStatus.CLUSTERED) {
                this.generateCluster(i, points, status, clusters);
            }
        }
        List<List<AIStuple>> res = new ArrayList<>();
        for (List<Integer> cluster : clusters) {
            List<AIStuple> c = new ArrayList<>();
            for (Integer num : cluster) {
                c.add(points.get(num));
            }
            res.add(c);
        }
        return res;
    }

    private void generateCluster(Integer pointNum, List<AIStuple> points, Map<Integer, PointStatus> visited, List<List<Integer>> clusters) {
        List<Integer> cluster = new ArrayList<>();
        cluster.add(pointNum);
        visited.put(pointNum, PointStatus.CLUSTERED);

        for(int index = 0; index < cluster.size(); index++) {
            AIStuple current = points.get(cluster.get(index));
            List<Integer> currentNeighbors = getNeighbors(current, points);
            if (currentNeighbors.size() + 1 >= minPoints) {
                for (Integer currentNeighbor : currentNeighbors) {
                    if (visited.get(currentNeighbor) != PointStatus.CLUSTERED) {
                        cluster.add(currentNeighbor);
                    }
                    visited.put(currentNeighbor, PointStatus.CLUSTERED);
                }
            }
        }

        if (cluster.size() == 1) {
            visited.put(pointNum, PointStatus.NOISE);
            List<Integer> noises = clusters.get(0);
            noises.add(pointNum);
        } else {
            clusters.add(cluster);
        }

    }

    private List<Integer> getNeighbors(AIStuple point, List<AIStuple> points) {
        List<Integer> neighbors = new ArrayList<>();

        for(int i = 0; i < points.size(); i++) {
            AIStuple neighbor = points.get(i);
            if (point != neighbor && point.distanceFrom(neighbor, measureMethod) <= distance) {
                neighbors.add(i);
            }
        }

        return neighbors;
    }

    private enum PointStatus{
        CLUSTERED,
        NOISE
    }

    private static class ErrorParameter extends RuntimeException{
        private ErrorParameter(){
            System.out.println("Some of parameters are invalid");
        }
    }

    public static void main(String[] args) throws ErrorParameter {
        DBSCAN dbscan = new DBSCAN(3.0, 3);
        dbscan.fit(Arrays.asList(new AIStuple(1.0, 0.0), new AIStuple(0.0, 0.0), new AIStuple(0.0, 1.0), new AIStuple(10.0, 0.0), new AIStuple(11.0, 0.0), new AIStuple(10.0, 1.0), new AIStuple(20.0, 0.0), new AIStuple(0.0, 0.5)));
    }
}

