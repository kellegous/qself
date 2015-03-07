package kellegous.hud;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by knorton on 2/8/15.
 */
public class UpdateService extends Service {
    private static final String TAG = UpdateService.class.getSimpleName();

    private static final int POLLING_DELAY = 10000;
    private static final int HOURLY_METRICS_INTERVAL = 5*1000*60;
    private static final int HOURS_IN_HOURLY = 24;

    private AgentApi.Status mStatus = new AgentApi.Status();
    private AgentApi.Hourly mHourly = new AgentApi.Hourly();

    private String mOrigin = "http://flint.kellego.us:8077";

    private Handler mHandler;

    private List<Listener> mListeners = new ArrayList<>();

    private final Binder mBinder = new Binder();

    private long mNextHistoryUpdateAt;

    private final Runnable mNeedsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateTask().execute();
        }
    };

    public interface Listener {
        void statusDidUpdate();
        void hourlyDidUpdate();
    }

    public class Binder extends android.os.Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    private static class Result {
        double mHrt;
        double mHrv;
        double mTmp;

        Result(double hrt, double hrv, double tmp) {
            mHrt = hrt;
            mHrv = hrv;
            mTmp = tmp;
        }
    }

    private class GetHourlyMetricsTask extends AsyncTask<Void, Void, AgentApi.Hourly> {

        @Override
        protected AgentApi.Hourly doInBackground(Void... params) {
            try {
                return AgentApi.getHourly(mOrigin, 0, HOURS_IN_HOURLY);
            } catch (IOException e) {
                Log.e(TAG, "getHourly api call failed", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(AgentApi.Hourly hourly) {
            if (hourly == null) {
                return;
            }

            mHourly = hourly;
            fireStatusDidUpdate();
        }
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {

        private AgentApi.Status mStatus;
        private AgentApi.Hourly mHourly;

        @Override
        protected void onPreExecute() {
            mStatus = null;
            mHourly = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long time = System.currentTimeMillis();

            try {
                mStatus = AgentApi.getStatus(mOrigin);
            } catch (Exception e) {
                Log.e(TAG, "agent api call failed", e);
            }

            try {
                if (mNextHistoryUpdateAt < time || mNextHistoryUpdateAt == 0) {
                    mHourly = AgentApi.getHourly(mOrigin, 0, HOURS_IN_HOURLY);
                    mNextHistoryUpdateAt = time + HOURLY_METRICS_INTERVAL;
                }
            } catch (IOException e) {
                Log.e(TAG, "hourly update failed", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            mHandler.postDelayed(mNeedsUpdateRunnable, POLLING_DELAY);

            if (mStatus != null) {
                UpdateService.this.mStatus = mStatus;
                fireStatusDidUpdate();
            }

            if (mHourly != null) {
                UpdateService.this.mHourly = mHourly;
                fireHourlyDidUpdate();
            }
        }
    }

    public UpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler(Looper.getMainLooper());
        mNeedsUpdateRunnable.run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void fireStatusDidUpdate() {
        for (int i = 0, n = mListeners.size(); i < n; i++) {
            mListeners.get(i).statusDidUpdate();
        }
    }

    private void fireHourlyDidUpdate() {
        for (int i = 0, n = mListeners.size(); i < n; i++) {
            mListeners.get(i).hourlyDidUpdate();
        }
    }

    public void addListener(Listener listener) {
        List<Listener> listeners = new ArrayList<>(mListeners.size() + 1);
        listeners.addAll(mListeners);
        listeners.add(listener);
        mListeners = listeners;
    }

    public void removeListener(Listener listener) {
        List<Listener> listeners = new ArrayList<>(mListeners.size()-1);
        for (int i = 0, n = mListeners.size(); i < n; i++) {
            Listener l = mListeners.get(i);
            if (l != listener) {
                listeners.add(l);
            }
        }
    }

    public int getHrt() {
        return (int)mStatus.hrt().rate();
    }

    public int getHrv() {
        return (int)mStatus.hrt().variability();
    }

    public int getTmp() {
        return (int)mStatus.tmp().temp();
    }

    public AgentApi.Hourly getHourly() {
        return mHourly;
    }
}
