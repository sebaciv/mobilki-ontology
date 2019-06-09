package pl.edu.agh.ki.mob.onto;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class WeatherUtils {
    private static final String API_KEY = "28e5a2f5acc38bf3cb70798ec4902514";
    private static final String OPENWEATHER_WEATHER_QUERY = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&mode=json&units=metric&appid=" + API_KEY;

    public static Optional<Double> getTemperature(double lat, double lon) {
        try {
            String json = getContentFromUrl(String.format(OPENWEATHER_WEATHER_QUERY, lat, lon));
            JSONObject reader = new JSONObject(json);
            return Optional.of(reader.getJSONObject("main").getDouble("temp"));
        } catch (JSONException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static String getContentFromUrl(String addr) {
        String content = null;

        Log.v("[GEO WEATHER ACTIVITY]", addr);
        HttpURLConnection urlConnection = null;
        URL url = null;
        try {
            url = new URL(addr);
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            content = stringBuilder.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return content;
    }
}
