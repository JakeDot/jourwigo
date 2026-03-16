package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.platform.LocationService;

/**
 * Simple console-based implementation of {@link LocationService}.
 * Starts at a fixed location; position can be updated programmatically
 * (e.g. from command-line input or a test driver).
 */
public class ConsoleLocationService implements LocationService {

    private double latitude;
    private double longitude;
    private double altitude;
    private double heading;
    private double precision;
    private int state;

    public ConsoleLocationService(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.heading = 0.0;
        this.precision = 5.0;
        this.state = ONLINE;
    }

    /** Update the current position. */
    public void setPosition(double lat, double lon, double alt) {
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = alt;
    }

    @Override
    public double getLatitude() { return latitude; }

    @Override
    public double getLongitude() { return longitude; }

    @Override
    public double getAltitude() { return altitude; }

    @Override
    public double getHeading() { return heading; }

    @Override
    public double getPrecision() { return precision; }

    @Override
    public int getState() { return state; }

    @Override
    public void connect() { state = ONLINE; }

    @Override
    public void disconnect() { state = OFFLINE; }
}
