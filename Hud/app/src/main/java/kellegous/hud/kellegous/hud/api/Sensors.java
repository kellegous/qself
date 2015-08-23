package kellegous.hud.kellegous.hud.api;

import android.text.format.Time;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Sensors {

    public static class Summary {
        public static final int TYPE_HOURLY = 0;
        public static final int TYPE_MINUTELY = 1;

        public static class Hrt {
            private Time mTime = new Time(Api.timeZero);
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
                        Api.parseTime(r.nextString(), hrt.mTime);
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
            private Time mTime = new Time(Api.timeZero);
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
                        Api.parseTime(r.nextString(), tmp.mTime);
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

        private int mSummaryType;
        private List<Tmp> mTmp = new ArrayList<>();
        private List<Hrt> mHrt = new ArrayList<>();

        private Summary(int summaryType) {
            mSummaryType = summaryType;
        }

        public List<Tmp> tmp() {
            return mTmp;
        }

        public List<Hrt> hrt() {
            return mHrt;
        }

        public int summaryType() {
            return mSummaryType;
        }

        private static Summary parse(JsonReader r, Summary summary) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Hrt")) {
                    Hrt.parseList(r, summary.mHrt);
                } else if (name.equals("Tmp")) {
                    Tmp.parseList(r, summary.mTmp);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return summary;
        }
    }

    /**
     *
     */
    public static class Status {

        /**
         *
         */
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

        /**
         *
         */
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

        private final Hrt mHrt = new Hrt();
        private final Tmp mTmp = new Tmp();

        public Hrt hrt() {
            return mHrt;
        }

        public Tmp tmp() {
            return mTmp;
        }

        private static Status parse(JsonReader r, Status status) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Hrt")) {
                    Hrt.parse(r, status.mHrt);
                } else if (name.equals("Tmp")) {
                    Tmp.parse(r, status.mTmp);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return status;
        }
    }

    public static Status getStatus(String origin) throws IOException {
        JsonReader r = Api.fetchJson(origin + "/api/sensors/status");
        try {
            return Status.parse(r, new Status());
        } finally {
            r.close();
        }
    }

    public static Summary getMinutelySummary(String origin, int start, int limit) throws IOException {
        JsonReader r = Api.fetchJson(
                String.format("%s/api/sensors/minutely/all?start=%d&limit=%d", origin, start, limit));
        try {
            return Summary.parse(r, new Summary(Summary.TYPE_MINUTELY));
        } finally {
            r.close();
        }
    }

    public static Summary getHourlySummary(String origin, int start, int limit) throws IOException {
        JsonReader r = Api.fetchJson(
                String.format("%s/api/sensors/hourly/all?start=%d&limit=%d", origin, start, limit));
        try {
            return Summary.parse(r, new Summary(Summary.TYPE_HOURLY));
        } finally {
            r.close();
        }


    }
}
