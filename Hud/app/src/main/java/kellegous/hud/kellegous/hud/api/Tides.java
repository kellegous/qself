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
        private float mValue;
        private int mState;

        public Time time() {
            return mTime;
        }

        public float value() {
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
                    p.mValue = (float)r.nextDouble();
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
        private int mNextHighTide = -1;
        private int mNextLowTide = -1;
        private int mNow = -1;
        private List<Prediction> mPredictions = new ArrayList<>();

        public int indexOfNextHighTide() {
            return mNextHighTide;
        }

        public Prediction nextHighTide() {
            return (mNextHighTide != -1) ? mPredictions.get(mNextHighTide) : null;
        }

        public int indexOfNextLowTide() {
            return mNextLowTide;
        }

        public Prediction nextLowTide() {
            return (mNextLowTide != -1) ? mPredictions.get(mNextLowTide) : null;
        }

        public int indexOfNow() {
            return mNow;
        }

        public Prediction now() {
            return (mNow != -1) ? mPredictions.get(mNow) : null;
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
                    report.mNextHighTide = r.nextInt();
                } else if (name.equals("NextLowTide")) {
                    report.mNextLowTide = r.nextInt();
                } else if (name.equals("Now")) {
                    report.mNow = r.nextInt();
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
