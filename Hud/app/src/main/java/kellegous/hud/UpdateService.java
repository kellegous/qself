package kellegous.hud;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import kellegous.hud.kellegous.hud.api.Api;
import kellegous.hud.kellegous.hud.api.Sensors;
import kellegous.hud.kellegous.hud.api.Tides;
import kellegous.hud.kellegous.hud.api.Weather;

public class UpdateService extends Service {
    private static final String TAG = UpdateService.class.getSimpleName();

    private static final int BASE_INTERVAL = 10*1000;
    private static final int WEATHER_CONDITIONS_INTERVAL = 30*1000;
    private static final int WEATHER_FORECAST_INTERVAL = 5*1000*60;
    private static final int SENSOR_HOURLY_SUMMARY_INTERVAL = 5*1000*60;
    private static final int TIDES_PREDICTIONS_INTERVAL = 6*1000*60;

    private static final int HOURS_IN_HOURLY = 24;

    private String mOrigin = "http://flint.kellego.us:8077";

    private Handler mHandler;

    private Delegate mDelegate;

    private final Binder mBinder = new Binder();

    private final Runnable mNeedsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateTask().execute();
        }
    };

    // Used in UpdateTask
    private Api.Client mClient = Api.create(mOrigin);
    private long mNextWeatherConditionsUpdate;
    private long mNextSensorHourlySummaryUpdate;
    private long mNextWeatherForecastUpdate;
    private long mNextTidalPredictionsUpdate;

    public interface Delegate {
        void sensorsStatusDidUpdate(Sensors.Status status);
        void sensorHourlySummaryDidUpdate(Sensors.HourlySummary summary);

        void weatherConditionsDidUpdate(Weather.Conditions conditions);
        void weatherForecastDidUpdate(List<Weather.Conditions> forecast);

        void tidalPredictionsDidUpdate(Tides.Report report);
    }

    public class Binder extends android.os.Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {

        private Sensors.Status mSensorStatus;
        private Sensors.HourlySummary mSensorsHourlySummary;
        private Weather.Conditions mWeatherConditions;
        private List<Weather.Conditions> mWeatherHourlyForecast;
        private Tides.Report mTidalPredictions;

        @Override
        protected void onPreExecute() {
            mSensorStatus = null;
            mSensorsHourlySummary = null;
            mWeatherConditions = null;
            mTidalPredictions = null;
        }

        private void fetchSensorsStatus(long time) {
            try {
                mSensorStatus = mClient.sensors().getStatus();
            } catch (Exception e) {
                Log.e(TAG, "agent api call failed", e);
            }
        }

        private void fetchSensorsHourlySummary(long time) {
            try {
                if (mNextSensorHourlySummaryUpdate < time || mNextSensorHourlySummaryUpdate == 0) {
                    mSensorsHourlySummary = mClient.sensors().getHourlySummary(0, HOURS_IN_HOURLY);
                    mNextSensorHourlySummaryUpdate = time + SENSOR_HOURLY_SUMMARY_INTERVAL;
                }
            } catch (IOException e) {
                Log.e(TAG, "hourly update failed", e);
            }
        }

        private void fetchWeatherConditions(long time) {
            try {
                if (mNextWeatherConditionsUpdate < time || mNextWeatherConditionsUpdate == 0) {
                    mWeatherConditions = mClient.weather().getCurrentConditions();
                    mNextWeatherConditionsUpdate = time + WEATHER_CONDITIONS_INTERVAL;
                }
            } catch (IOException e) {
                Log.e(TAG, "weather conditions update failed", e);
            }
        }

        private void fetchTidalPredictions(long time) {
            try {
                if (mNextTidalPredictionsUpdate < time || mNextTidalPredictionsUpdate == 0) {
                    mTidalPredictions = mClient.tides().getPredictions();
                    mNextTidalPredictionsUpdate = time + TIDES_PREDICTIONS_INTERVAL;
                }
            } catch (IOException e) {
                Log.e(TAG, "tidal predictions update failed", e);
            }
        }

        private void fetchWeatherForecast(long time) {
            try {
                if (mNextWeatherForecastUpdate < time || mNextWeatherForecastUpdate == 0) {
                    mWeatherHourlyForecast = mClient.weather().getHourlyForecast();
                    mNextWeatherForecastUpdate = time + WEATHER_FORECAST_INTERVAL;
                }
            } catch (IOException e) {
                Log.e(TAG, "weather forecast update failed", e);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            long time = System.currentTimeMillis();


            fetchSensorsStatus(time);
            fetchSensorsHourlySummary(time);
            fetchWeatherConditions(time);
            fetchWeatherForecast(time);
            fetchTidalPredictions(time);

            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            mHandler.postDelayed(mNeedsUpdateRunnable, BASE_INTERVAL);

            if (mSensorStatus != null) {
                mDelegate.sensorsStatusDidUpdate(mSensorStatus);
            }

            if (mWeatherConditions != null) {
                mDelegate.weatherConditionsDidUpdate(mWeatherConditions);
            }

            if (mSensorsHourlySummary != null) {
                mDelegate.sensorHourlySummaryDidUpdate(mSensorsHourlySummary);
            }

            if (mWeatherHourlyForecast != null) {
                mDelegate.weatherForecastDidUpdate(mWeatherHourlyForecast);
            }

            if (mTidalPredictions != null) {
                mDelegate.tidalPredictionsDidUpdate(mTidalPredictions);
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

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }
}
