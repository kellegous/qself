package kellegous.hud;

import android.text.format.Time;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by knorton on 2/8/15.
 */
public class AgentApi {
    private static final String TAG = AgentApi.class.getSimpleName();

    private static final Time timeZero = emptyTime();

    private static Time emptyTime() {
        Time t = new Time();
        t.set(0);
        return t;
    }

    private static void parseTime(String s, Time time) {
        time.parse3339(s);
        time.switchTimezone("America/New_York");
    }

    private static JsonReader fetchJson(String url) throws IOException {
        HttpResponse res = new DefaultHttpClient().execute(new HttpGet(url));
        return new JsonReader(new InputStreamReader(res.getEntity().getContent(), "UTF-8"));
    }

    public static class Hourly {
        public static class Hrt {
            private Time mTime = new Time(timeZero);
            private double mHr;
            private double mHrv;
            private int mCount;

            public Time time() {
                return mTime;
            }

            public double rate() {
                return mHr;
            }

            public double variability() {
                return mHrv;
            }

            public int count() {
                return mCount;
            }

            private static void parseList(JsonReader r, List<Hrt> hrts) throws IOException {
                r.beginArray();
                while (r.hasNext()) {
                    hrts.add(parse(r, new Hrt()));
                }
                r.endArray();
            }

            private static Hrt parse(JsonReader r, Hrt hrt) throws IOException {
                r.beginObject();
                while (r.hasNext()) {
                    String name = r.nextName();
                    if (name.equals("Time")) {
                        parseTime(r.nextString(), hrt.mTime);
                    } else if (name.equals("Hr")) {
                        hrt.mHr = r.nextDouble();
                    } else if (name.equals("Hrv")) {
                        hrt.mHrv = r.nextDouble();
                    } else if (name.equals("Count")) {
                        hrt.mCount = r.nextInt();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
                return hrt;
            }
        }

        public static class Tmp {
            private Time mTime = new Time(timeZero);
            private double mTmp;
            private int mCount;

            public Time time() {
                return mTime;
            }

            public double temp() {
                return mTmp;
            }

            private static void parseList(JsonReader r, List<Tmp> tmps) throws IOException {
                r.beginArray();
                while (r.hasNext()) {
                    tmps.add(parse(r, new Tmp()));
                }
                r.endArray();
            }

            private static Tmp parse(JsonReader r, Tmp tmp) throws IOException {
                r.beginObject();
                while (r.hasNext()) {
                    String name = r.nextName();
                    if (name.equals("Time")) {
                        parseTime(r.nextString(), tmp.mTime);
                    } else if (name.equals("Temp")) {
                        tmp.mTmp = r.nextDouble();
                    } else if (name.equals("Count")) {
                        tmp.mCount = r.nextInt();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
                return tmp;
            }
        }

        private List<Tmp> mTmp = new ArrayList<>();
        private List<Hrt> mHrt = new ArrayList<>();

        public List<Tmp> tmp() {
            return mTmp;
        }

        public List<Hrt> hrt() {
            return mHrt;
        }

        private static Hourly parse(JsonReader r, Hourly hourly) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Hrt")) {
                    Hrt.parseList(r, hourly.mHrt);
                } else if (name.equals("Tmp")) {
                    Tmp.parseList(r, hourly.mTmp);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return hourly;
        }
    }

    public static Hourly getHourly(String origin, int start, int limit) throws IOException {
        JsonReader r = fetchJson(
                String.format("%s/api/hourly/all?start=%d&limit=%d", origin, start, limit));
        return Hourly.parse(r, new Hourly());
    }

    public static class Status {
        public static class Hrt {
            private boolean mActive;
            private double mRate;
            private double mVariability;

            public boolean active() {
                return mActive;
            }

            public double rate() {
                return mRate;
            }

            public double variability() {
                return mVariability;
            }

            private static void parse(JsonReader r, Status.Hrt hrt) throws IOException {
                r.beginObject();
                while (r.hasNext()) {
                    String name = r.nextName();
                    if (name.equals("Active")) {
                        hrt.mActive = r.nextBoolean();
                    } else if (name.equals("Rate")) {
                        hrt.mRate = r.nextDouble();
                    } else if (name.equals("Variability")) {
                        hrt.mVariability = r.nextDouble();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
            }
        }

        public static class Tmp {
            private boolean mActive;
            private double mTemp;

            public boolean active() {
                return mActive;
            }

            public double temp() {
                return mTemp;
            }

            private static void parse(JsonReader r, Status.Tmp tmp) throws IOException {
                r.beginObject();
                while (r.hasNext()) {
                    String name = r.nextName();
                    if (name.equals("Active")) {
                        tmp.mActive = r.nextBoolean();
                    } else if (name.equals("Temp")) {
                        tmp.mTemp = r.nextDouble();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
            }
        }

        public static class Weather {
            private double mTemp;
            private String mIcon;
            private String mSummary;
            private double mApparentTemp;

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

            private static void parse(JsonReader r, Status.Weather w) throws IOException {
                r.beginObject();
                while (r.hasNext()) {
                    String name = r.nextName();
                    if (name.equals("Temp")) {
                        w.mTemp = r.nextDouble();
                    } else if (name.equals("Icon")) {
                        w.mIcon = r.nextString();
                    } else if (name.equals("Summary")) {
                        w.mSummary = r.nextString();
                    } else if (name.equals("ApparentTemp")) {
                        w.mApparentTemp = r.nextDouble();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
            }
        }

        private final Hrt mHrt = new Hrt();
        private final Tmp mTmp = new Tmp();
        private final Weather mWeather = new Weather();

        public Hrt hrt() {
            return mHrt;
        }

        public Tmp tmp() {
            return mTmp;
        }

        public Weather weather() {
            return mWeather;
        }

        private static Status parse(JsonReader r, Status status) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Hrt")) {
                    Hrt.parse(r, status.mHrt);
                } else if (name.equals("Tmp")) {
                    Tmp.parse(r, status.mTmp);
                } else if (name.equals("Weather")) {
                    Weather.parse(r, status.mWeather);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return status;
        }
    }

    public static Status getStatus(String origin) throws IOException {
        JsonReader r = fetchJson(origin + "/api/status");
        return Status.parse(r, new Status());
    }
}
