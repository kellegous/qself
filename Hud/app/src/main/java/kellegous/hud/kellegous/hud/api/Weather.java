package kellegous.hud.kellegous.hud.api;

import android.text.format.Time;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Weather {
    private Weather() {
    }

    /**
     *
     */
    public static class Conditions {
        private Time mTime = new Time(Api.timeZero);
        private double mTemp;
        private String mIcon;
        private String mSummary;
        private double mApparentTemp;

        public Time time() {
            return mTime;
        }

        public double temp() {
            return mTemp;
        }

        public String icon() {
            return mIcon;
        }

        public String summary() {
            return mSummary;
        }

        public double apparentTemp() {
            return mApparentTemp;
        }

        private static Conditions parse(JsonReader r, Conditions c) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Temp")) {
                    c.mTemp = r.nextDouble();
                } else if (name.equals("Icon")) {
                    c.mIcon = r.nextString();
                } else if (name.equals("Summary")) {
                    c.mSummary = r.nextString();
                } else if (name.equals("ApparentTemp")) {
                    c.mApparentTemp = r.nextDouble();
                } else if (name.equals("Time")) {
                    Api.parseTime(r.nextString(), c.mTime);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return c;
        }
    }

    public static Conditions getCurrentConditions(String origin) throws IOException {
        JsonReader r = Api.fetchJson(origin + "/api/weather/current");
        try {
            return Conditions.parse(r, new Conditions());
        } finally {
            r.close();
        }
    }

    private static List<Conditions> parseForecast(JsonReader r, List<Conditions> forecast) throws IOException {
        r.beginArray();
        while (r.hasNext()) {
            forecast.add(Conditions.parse(r, new Conditions()));
        }
        r.endArray();
        return forecast;
    }

    public static List<Conditions> getHourlyForecast(String origin) throws IOException {
        JsonReader r = Api.fetchJson(origin + "/api/weather/hourly");
        try {
            return parseForecast(r, new ArrayList<Conditions>());
        } finally {
            r.close();
        }
    }
}
