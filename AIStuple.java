import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class AIStuple {

    private String MMSI;
    private Double longitude;
    private Double latitude;
    private Date date;
    private Double SOG;
    private Double COG;

    public AIStuple(String mmsi, double lat, double lon, double cog, double sog) {
        this.setMMSI(mmsi);
        this.setLatitude(lat);
        this.setLongitude(lon);
        this.setCOG(cog);
        this.setSOG(sog);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIStuple aiStuple = (AIStuple) o;
        return Objects.equals(MMSI, aiStuple.MMSI) && Objects.equals(longitude, aiStuple.longitude) && Objects.equals(latitude, aiStuple.latitude) && Objects.equals(date, aiStuple.date) && Objects.equals(SOG, aiStuple.SOG) && Objects.equals(COG, aiStuple.COG);
    }

    @Override
    public int hashCode() {
        return Objects.hash(MMSI, longitude, latitude, date, SOG, COG);
    }

    public String toString() {
        SimpleDateFormat sdf ;
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "MMSI: " + this.getMMSI() + "\tlongitude: " + this.longitude + "\tlatitude: " + this.latitude + "\tCOG: " + this.COG + "\tSOG: " + this.SOG+"\tTime: "+sdf.format(this.getDate());
    }

    public AIStuple() {
    }

    public AIStuple(String MMSI, Double latitude, Double longitude, String date, Double COG, Double SOG) throws ParseException {
        this.setMMSI(MMSI);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
        this.setSOG(SOG);
        this.setCOG(COG);
        SimpleDateFormat sdf ;
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        date = date.replace('T', ' ');
        this.date = sdf.parse(date);
    }

    public AIStuple(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public AIStuple(String mmsi, Double latitude, Double longitude, Date date, Double cog, Double sog) throws ParseException {
        this(latitude,longitude,date);
        this.setMMSI(mmsi);
        this.setCOG(cog);
        this.setSOG(sog);
    }

    public AIStuple(Double latitude, Double longitude, Date date){
        this(latitude,longitude);
        this.setDate(date);
    }

    public AIStuple(AIStuple o) throws ParseException {
        this(o.getMMSI(), o.getLatitude(), o.getLongitude(), o.getDate(), o.getCOG(), o.getSOG());
    }

    public void setMMSI(String MMSI) {
        this.MMSI = MMSI;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getSOG() {
        return this.SOG;
    }

    public void setSOG(Double SOG) {
        this.SOG = SOG;
    }

    public Double getCOG() {
        return this.COG;
    }

    public void setCOG(Double COG) {
        this.COG = COG;
    }

    public String getMMSI() {
        return this.MMSI;
    }

    private double[] angle2Radian() {
        return new double[]{Math.PI * this.latitude / 180, Math.PI * this.longitude / 180};
    }

    private double RealDistance(AIStuple anotherTuple) {

        double[] position1 = this.angle2Radian();
        double[] position2 = anotherTuple.angle2Radian();
        double lat1 = position1[0];
        double lon1 = position1[1];
        double lat2 = position2[0];
        double lon2 = position2[1];
        double pow1 = Math.pow(Math.sin((lat2 - lat1) / 2.0), 2.0);
        double pow2 = Math.pow(Math.sin((lon2 - lon1) / 2.0), 2.0);
        return 2.0 * AISUtils.EARTH_RADIUS * Math.asin(Math.sqrt(pow1 + Math.cos(lat1) * Math.cos(lat2) * pow2));
    }

    private double EuclideanDistance(AIStuple anotherTuple) {
        return Math.sqrt(Math.pow(this.latitude - anotherTuple.latitude, 2.0) + Math.pow(this.longitude - anotherTuple.longitude, 2.0));
    }

    public Double distanceFrom(AIStuple anotherTuple, SimilarityMeasureMethod measureMethod) {
        switch (measureMethod) {
            case HAVERSINE_DISTANCE:
                return this.RealDistance(anotherTuple);
            case EUCLIDEAN_DISTANCE:
                return this.EuclideanDistance(anotherTuple);
            default:
                return -1.0;
        }
    }

    public Double distanceFrom(AIStuple anotherTuple, SimilarityMeasure similarityMeasure) {
        return similarityMeasure.distanceFrom(anotherTuple);
    }


    /**
     *
     * @param anotherTuple 另一个tuple
     * @param timeUnit 时间单位
     * @return 时间间隔
     * @throws AIStupleException date字段缺失
     */

    /**
     *
     * @param anotherTuple 另一个元组
     * @param timeUnit 时间单位
     * @return 两元组的以timeUnit为单位的时间间隔
     * @throws AIStupleException
     */
    public double timeInterval(AIStuple anotherTuple, TimeUnit timeUnit) throws AIStupleException {
        double millisecondDiff;
        if(anotherTuple.getDate()==null){
            throw new AIStupleException(anotherTuple);
        }
        if(this.getDate()==null){
            throw new AIStupleException(this);
        }
        millisecondDiff = (double) Math.abs(this.getDate().getTime()-anotherTuple.getDate().getTime());
        return millisecondDiff / AISUtils.millisecondOfTimeUnit(timeUnit);
    }

    public double timeInterval(AIStuple anotherTuple) throws AIStupleException {
        return timeInterval(anotherTuple, TimeUnit.MINUTE);
    }

    public static void main(String[] args) throws IOException {

    }

}
