package com.uav.weather.model;

import java.util.HashMap;
import java.util.Map;

public class WeatherData {
    private String source;
    private String droneId;
    private String stationId;
    private long timestamp;
    private double latitude, longitude, altitude;
    private double temperature, humidity, windSpeed, windDirection, windGust;
    private double pressure, visibility, turbulence, precipitation;
    private double cloudCover;
    private String countryCode;
    private String parameters;
    private double dataQuality = 1.0;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("source", source);
        map.put("drone_id", droneId);
        map.put("station_id", stationId);
        map.put("timestamp", timestamp);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("altitude", altitude);
        map.put("temperature", temperature);
        map.put("humidity", humidity);
        map.put("wind_speed", windSpeed);
        map.put("wind_direction", windDirection);
        map.put("wind_gust", windGust);
        map.put("pressure", pressure);
        map.put("visibility", visibility);
        map.put("turbulence", turbulence);
        map.put("precipitation", precipitation);
        map.put("cloud_cover", cloudCover);
        map.put("country_code", countryCode);
        map.put("parameters", parameters);
        map.put("data_quality", dataQuality);
        return map;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public double getWindDirection() { return windDirection; }
    public void setWindDirection(double windDirection) { this.windDirection = windDirection; }
    public double getWindGust() { return windGust; }
    public void setWindGust(double windGust) { this.windGust = windGust; }
    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }
    public double getVisibility() { return visibility; }
    public void setVisibility(double visibility) { this.visibility = visibility; }
    public double getTurbulence() { return turbulence; }
    public void setTurbulence(double turbulence) { this.turbulence = turbulence; }
    public double getPrecipitation() { return precipitation; }
    public void setPrecipitation(double precipitation) { this.precipitation = precipitation; }
    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public double getCloudCover() { return cloudCover; }
    public void setCloudCover(double cloudCover) { this.cloudCover = cloudCover; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public double getDataQuality() { return dataQuality; }
    public void setDataQuality(double dataQuality) { this.dataQuality = dataQuality; }
}
