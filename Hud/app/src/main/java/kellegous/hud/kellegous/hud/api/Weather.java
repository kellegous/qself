package kellegous.hud.kellegous.hud.api;

import android.text.format.Time;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Weather {

    public static final int ICON_UNKNOWN = 0;
    public static final int ICON_CLEAR_DAY = 1;
    public static final int ICON_CLEAR_NIGHT = 2;
    public static final int ICON_RAIN = 3;
    public static final int ICON_SNOW = 4;
    public static final int ICON_SLEET = 5;
    public static final int ICON_WIND = 6;
    public static final int ICON_FOG = 7;
    public static final int ICON_CLOUDY = 8;
    public static final int ICON_PARTLY_CLOUDY_DAY = 9;
    public static final int ICON_PARTLY_CLOUDY_NIGHT = 10;
    public static final int ICON_THUNDERSTORM = 11;

    public static final int ICON_LAST_INDEX = ICON_THUNDERSTORM;

    private Weather() {
    }

    private static int iconFromApiString(String name) {
        if ("clear-day".equals(name)) {
            return ICON_CLEAR_DAY;
        } else if ("clear-night".equals(name)) {
            return ICON_CLEAR_NIGHT;
        } else if ("rain".equals(name)) {
            return ICON_RAIN;
        } else if ("snow".equals(name)) {
            return ICON_SNOW;
        } else if ("sleet".equals(name)) {
            return ICON_SLEET;
        } else if ("wind".equals(name)) {
            return ICON_WIND;
        } else if ("fog".equals(name)) {
            return ICON_FOG;
        } else if ("cloudy".equals(name)) {
            return ICON_CLOUDY;
        } else if ("partly-cloudy-day".equals(name)) {
            return ICON_PARTLY_CLOUDY_DAY;
        } else if ("partly-cloudy-night".equals(name)) {
            return ICON_PARTLY_CLOUDY_NIGHT;
        } else if ("thunderstorm".equals(name)) {
            return ICON_THUNDERSTORM;
        }
        return ICON_UNKNOWN;
    }
    /**
     *
     */
    public static class Conditions {
        private Time mTime = new Time(Api.timeZero);
        private double mTemp;
        private int mIcon;
        private String mSummary;
        private double mApparentTemp;
        private double mPrecipProb;

        public Time time() {
            return mTime;
        }

        public double temp() {
            return mTemp;
        }

        public int icon() {
            return mIcon;
        }

        public String summary() {
            return mSummary;
        }

        public double apparentTemp() {
            return mApparentTemp;
        }

        public double probabilityOfPrecipitation() {
            return mPrecipProb;
        }

        private static Conditions parse(JsonReader r, Conditions c) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Temp")) {
                    c.mTemp = r.nextDouble();
                } else if (name.equals("Icon")) {
                    c.mIcon = iconFromApiString(r.nextString());
                } else if (name.equals("Summary")) {
                    c.mSummary = r.nextString();
                } else if (name.equals("ApparentTemp")) {
                    c.mApparentTemp = r.nextDouble();
                } else if (name.equals("Time")) {
                    Api.parseTime(r.nextString(), c.mTime);
                } else if (name.equals("PrecipProbability")) {
                    c.mPrecipProb = r.nextDouble();
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
