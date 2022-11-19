import java.util.*;

public class ObjectTime {
    private TreeSet<Integer> objects;
    private List<Date> times;

    public TreeSet<Integer> getObjects() {
        TreeSet<Integer> tempObjects = new TreeSet<>(objects);
        tempObjects.remove(-1);
        return objects;
    }

    @Override
    public String toString() {
        TreeSet<Integer> tempObjects = new TreeSet<>(objects);
        tempObjects.remove(-1);
        return "ObjectTime{" +
                "objectsNum=" + tempObjects +
                ", times=" + times +
                '}';
    }

    public Integer getObjectsNum(){
        return objects.size()-1;
    }
    public Integer getTimesNum(){
        return times.size();
    }

    public ObjectTime(List<Date> times){
        this.times = times;
        this.objects = new TreeSet<>();
        objects.add(-1);
    }
    public ObjectTime(TreeSet<Integer> objects, List<Date> times) {
        objects.add(-1);
        this.objects = objects;
        this.times = times;
    }


    public void setObjects(TreeSet<Integer> objects) {
        objects.add(-1);
        this.objects = objects;
    }

    public List<Date> getTimes() {
        return times;
    }

    public void setTimes(List<Date> times) {
        this.times = times;
    }
}
