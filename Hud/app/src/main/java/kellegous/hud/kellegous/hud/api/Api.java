package kellegous.hud.kellegous.hud.api;

import android.text.format.Time;
import android.util.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * A simple API client that provides access to all accumulated dashboard data.
 */
public class Api {
    private static class ClientImpl implements ForSensors, ForWeather, ForTides, Client {

        private final String mOrigin;

        private ClientImpl(String origin) {
            mOrigin = origin;
        }

        @Override
        public Sensors.Status getStatus() throws IOException{
            return Sensors.getStatus(mOrigin);
        }

        @Override
        public Weather.Conditions getCurrentConditions() throws IOException {
            return Weather.getCurrentConditions(mOrigin);
        }

        @Override
        public List<Weather.Conditions> getHourlyForecast() throws IOException{
            return Weather.getHourlyForecast(mOrigin);
        }

        @Override
        public Sensors.Summary getHourlySummary(int start, int limit) throws IOException{
            return Sensors.getHourlySummary(mOrigin, start, limit);
        }

        @Override
        public Sensors.Summary getMinutelySummary(int start, int limit) throws IOException {
            return Sensors.getMinutelySummary(mOrigin, start, limit);
        }

        @Override
        public Tides.Report getPredictions() throws IOException {
            return Tides.getPredictions(mOrigin);
        }

        @Override
        public ForSensors sensors() {
            return this;
        }

        @Override
        public ForWeather weather() {
            return this;
        }

        @Override
        public ForTides tides() {
            return this;
        }
    }

    /**
     * The primary access interface for data APIs.
     */
    public interface Client {
        ForSensors sensors();
        ForWeather weather();
        ForTides tides();
    }

    public interface ForSensors {
        Sensors.Status getStatus() throws IOException;
        Sensors.Summary getHourlySummary(int start, int limit) throws IOException;
        Sensors.Summary getMinutelySummary(int start, int limit) throws IOException;
    }

    public interface ForWeather {
        Weather.Conditions getCurrentConditions() throws IOException;
        List<Weather.Conditions> getHourlyForecast() throws IOException;
    }

    public interface ForTides {
        Tides.Report getPredictions() throws IOException;
    }

    private Api() {
    }

    public static Client create(String origin) {
        return new ClientImpl(origin);
    }

    private static HttpClient clientWithTimeout() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 3600);
        HttpConnectionParams.setSoTimeout(params, 3600);
        return new DefaultHttpClient(params);
    }

    static JsonReader fetchJson(String url) throws IOException {
        HttpResponse res = clientWithTimeout().execute(new HttpGet(url));
        return new JsonReader(new InputStreamReader(res.getEntity().getContent(), "UTF-8"));
    }

    static final Time timeZero = emptyTime();

    private static Time emptyTime() {
        Time t = new Time();
        t.set(0);
        return t;
    }

    static void parseTime(String s, Time time) {
        time.parse3339(s);
        time.switchTimezone("America/New_York");
    }

}
