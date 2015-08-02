package kellegous.hud.kellegous.hud.api;

import android.text.format.Time;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tides {
    public static final int STATE_INVALID = 0;
    public static final int STATE_HIGH_TIDE = 1;
    public static final int STATE_LOW_TIDE = 2;
    public static final int STATE_RISING_TIDE = 3;
    public static final int STATE_FALLING_TIDE = 4;

    public static class Prediction {
        private Time mTime = new Time(Api.timeZero);
        private double mValue;
        private int mState;

        public Time time() {
            return mTime;
        }

        public double value() {
            return mValue;
        }

        public int state() {
            return mState;
        }

        private static Prediction parse(JsonReader r, Prediction p) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Time")) {
                    Api.parseTime(r.nextString(), p.mTime);
                } else if (name.equals("Value")) {
                    p.mValue = r.nextDouble();
                } else if (name.equals("State")) {
                    p.mState = r.nextInt();
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return p;
        }
    }

    public static class Report {
        private Time mNextHighTide = new Time(Api.timeZero);
        private Time mNextLowTide = new Time(Api.timeZero);
        private Time mNow = new Time(Api.timeZero);
        private List<Prediction> mPredictions = new ArrayList<>();

        public Time nextHighTide() {
            return mNextHighTide;
        }

        public Time nextLowTide() {
            return mNextLowTide;
        }

        public Time now() {
            return mNow;
        }

        public List<Prediction> predictions() {
            return mPredictions;
        }

        private static void parsePredictions(JsonReader r, List<Prediction> predictions) throws IOException {
            r.beginArray();
            while (r.hasNext()) {
                predictions.add(Prediction.parse(r, new Prediction()));
            }
            r.endArray();
        }

        private static Report parse(JsonReader r, Report report) throws IOException {
            r.beginObject();
            while (r.hasNext()) {
                String name = r.nextName();
                if (name.equals("Predictions")) {
                    parsePredictions(r, report.mPredictions);
                } else if (name.equals("NextHighTide")) {
                    Api.parseTime(r.nextString(), report.mNextHighTide);
                } else if (name.equals("NextLowTide")) {
                    Api.parseTime(r.nextString(), report.mNextLowTide);
                } else if (name.equals("Now")) {
                    Api.parseTime(r.nextString(), report.mNow);
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
            return report;
        }
    }

    public static Report getPredictions(String origin) throws IOException {
        JsonReader r = Api.fetchJson(origin + "/api/tides/predictions");
        try {
            return Report.parse(r, new Report());
        } finally {
            r.close();
        }
    }
}
