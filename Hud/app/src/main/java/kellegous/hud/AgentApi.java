package kellegous.hud;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;

/**
 * Created by knorton on 2/8/15.
 */
public class AgentApi {
    private static final String TAG = AgentApi.class.getSimpleName();

    private static String fetch(String url) throws IOException {
        HttpResponse res = new DefaultHttpClient().execute(new HttpGet(url));
        return EntityUtils.toString(res.getEntity());
    }

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
    }

    public static class Status {
        private final Hrt mHrt = new Hrt();
        private final Tmp mTmp = new Tmp();

        public Hrt hrt() {
            return mHrt;
        }

        public Tmp tmp() {
            return mTmp;
        }
    }

    public static Status getStatus(String origin) throws IOException, JSONException {
        JSONObject json = new JSONObject(fetch(origin + "/api/status"));

        Status status = new Status();
        JSONObject hrt = json.getJSONObject("Hrt");
        if (hrt != null) {
            status.mHrt.mActive = hrt.getBoolean("Active");
            status.mHrt.mRate = hrt.getDouble("Rate");
            status.mHrt.mVariability = hrt.getDouble("Variability");
        }

        JSONObject tmp = json.getJSONObject("Tmp");
        if (tmp != null) {
            status.mTmp.mActive = tmp.getBoolean("Active");
            status.mTmp.mTemp = tmp.getDouble("Temp");
        }

        return status;
    }
}
