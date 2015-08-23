package kellegous.hud;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kellegous.hud.kellegous.hud.api.Sensors;
import kellegous.hud.kellegous.hud.api.Tides;
import kellegous.hud.kellegous.hud.api.Weather;

public class Model {

    public interface SensorsEvents {
        void tap(SensorsListener listener);
        void untap(SensorsListener listener);
    }

    public interface WeatherEvents {
        void tap(WeatherListener listener);
        void untap(WeatherListener listener);
    }

    public interface TidalEvents {
        void tap(TidalListener listener);
        void untap(TidalListener listener);
    }

    public interface SensorsListener {
        void sensorsStatusDidUpdate(Sensors.Status status);
        void sensorsMinutelySummaryDidUpdate(Sensors.Summary summary);
        void sensorsHourlySummaryDidUpdate(Sensors.Summary summary);
    }

    public interface WeatherListener {
        void weatherConditionsDidUpdate(Weather.Conditions conditions);
        void weatherForecastDidUpdate(List<Weather.Conditions> forecast);
    }

    public interface TidalListener {
        void tidalPredictionsDidUpdate(Tides.Report report);
    }

    private Activity mActivity;

    private UpdateService mService;

    private Impl mImpl = new Impl();

    private List<SensorsListener> mSensorsListeners = new ArrayList<>();

    private List<WeatherListener> mWeatherListeners = new ArrayList<>();

    private List<TidalListener> mTidalListeners = new ArrayList<>();

    private boolean mFiringEvents;

    private class Impl implements ServiceConnection,
            UpdateService.Delegate,
            SensorsEvents,
            WeatherEvents,
            TidalEvents {
        @Override
        public void sensorsStatusDidUpdate(Sensors.Status status) {
            mFiringEvents = true;
            List<SensorsListener> listeners = mSensorsListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).sensorsStatusDidUpdate(status);
            }
            mFiringEvents = false;
        }

        @Override
        public void sensorsMinutelySummaryDidUpdate(Sensors.Summary summary) {
            mFiringEvents = true;
            List<SensorsListener> listeners = mSensorsListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).sensorsMinutelySummaryDidUpdate(summary);
            }
            mFiringEvents = false;
        }

        @Override
        public void sensorHourlySummaryDidUpdate(Sensors.Summary summary) {
            mFiringEvents = true;
            List<SensorsListener> listeners = mSensorsListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).sensorsHourlySummaryDidUpdate(summary);
            }
            mFiringEvents = false;
        }

        @Override
        public void weatherConditionsDidUpdate(Weather.Conditions conditions) {
            mFiringEvents = true;
            List<WeatherListener> listeners = mWeatherListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).weatherConditionsDidUpdate(conditions);
            }
            mFiringEvents = false;
        }

        @Override
        public void weatherForecastDidUpdate(List<Weather.Conditions> forecast) {
            mFiringEvents = true;
            List<WeatherListener> listeners = mWeatherListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).weatherForecastDidUpdate(forecast);
            }
            mFiringEvents = false;
        }

        @Override
        public void tidalPredictionsDidUpdate(Tides.Report report) {
            Log.d(getClass().getSimpleName(), String.format("tides did update: %s", report.now().time().format("%H:%M")));
            mFiringEvents = true;
            List<TidalListener> listeners = mTidalListeners;
            for (int i = 0, n = listeners.size(); i < n; i++) {
                listeners.get(i).tidalPredictionsDidUpdate(report);
            }
            mFiringEvents = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((UpdateService.Binder) service).getService();
            mService.setDelegate(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        public void destroy() {
            if (mService != null) {
                mService.setDelegate(null);
            }

            mActivity.unbindService(this);

            mService = null;
            mActivity = null;
        }

        private <T> List<T> willMutate(List<T> listeners) {
            if (mFiringEvents) {
                return new ArrayList<T>(listeners);
            }
            return listeners;
        }

        @Override
        public void tap(SensorsListener listener) {
            mSensorsListeners = willMutate(mSensorsListeners);
            mSensorsListeners.add(listener);
        }

        @Override
        public void untap(SensorsListener listener) {
            mSensorsListeners = willMutate(mSensorsListeners);
            mSensorsListeners.remove(listener);
        }

        @Override
        public void tap(TidalListener listener) {
            mTidalListeners = willMutate(mTidalListeners);
            mTidalListeners.add(listener);
        }

        @Override
        public void untap(TidalListener listener) {
            mTidalListeners = willMutate(mTidalListeners);
            mTidalListeners.remove(listener);

        }

        @Override
        public void tap(WeatherListener listener) {
            mWeatherListeners = willMutate(mWeatherListeners);
            mWeatherListeners.add(listener);
        }

        @Override
        public void untap(WeatherListener listener) {
            mWeatherListeners = willMutate(mWeatherListeners);
            mWeatherListeners.remove(listener);
        }
    }

    private Model(Activity activity) {
        mActivity = activity;

        activity.bindService(
                new Intent(activity, UpdateService.class),
                mImpl,
                Context.BIND_AUTO_CREATE);
    }

    public static Model create(Activity activity) {
        return new Model(activity);
    }

    public void destroy() {
        mImpl.destroy();
    }

    public WeatherEvents weather() {
        return mImpl;
    }

    public SensorsEvents sensors() {
        return mImpl;
    }

    public TidalEvents tides() {
        return mImpl;
    }
}
