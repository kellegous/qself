package kellegous.hud;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by knorton on 2/8/15.
 */
public class UpdateService extends Service {
    private static final String TAG = UpdateService.class.getSimpleName();

    private static final int POLLING_DELAY = 10000;

    private AgentApi.Status mStatus = new AgentApi.Status();

    private String mOrigin = "http://turtle.kellego.us:8077";

    private Handler mHandler;

    private List<Listener> mListeners = new ArrayList<>();

    private final Binder mBinder = new Binder();

    public interface Listener {
        void dataWasUpdated();
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

    private final Runnable mNeedsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateTask().execute();
        }
    };

    private class UpdateTask extends AsyncTask<Void, Void, AgentApi.Status> {
        @Override
        protected AgentApi.Status doInBackground(Void... params) {
            try {
                return AgentApi.getStatus(mOrigin);
            } catch (Exception e) {
                Log.e(TAG, "agent api call failed", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(AgentApi.Status status) {
            mHandler.postDelayed(mNeedsUpdateRunnable, POLLING_DELAY);

            if (status == null) {
                return;
            }

            mStatus = status;
            fireModelDidUpdate();
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

    private void fireModelDidUpdate() {
        for (int i = 0, n = mListeners.size(); i < n; i++) {
            mListeners.get(i).dataWasUpdated();
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
}
