package pl.edu.agh.ki.mob.onto;

public class SimpleLocation {
    private final double lat;
    private final double lon;

    public SimpleLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
