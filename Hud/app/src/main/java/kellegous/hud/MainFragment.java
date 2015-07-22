package kellegous.hud;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class MainFragment extends Fragment implements ServiceConnection, UpdateService.Listener {
    private static final String TAG = MainFragment.class.getSimpleName();

    private UpdateService mService;

    private HeartDataView mHeartDataView;

    private WeatherDataView mWeatherDataView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().bindService(
                new Intent(getActivity(), UpdateService.class),
                this,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            mService.removeListener(this);
        }

        getActivity().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                StatusBar.tryToToggle();
                return true;
            }
        });

        mHeartDataView = (HeartDataView) view.findViewById(R.id.heart_data_view);
        mWeatherDataView = (WeatherDataView) view.findViewById(R.id.weather_data_view);

        return view;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((UpdateService.Binder)service).getService();
        mService.addListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void statusDidUpdate() {
        mHeartDataView.updateCurrentData(mService.getHrt(), mService.getHrv());
        mWeatherDataView.setCurrentTemperature(
                mService.getTmp(),
                (int)Math.round(mService.getWeather().temp()));
    }

    private  void updateTmpData(List<AgentApi.Hourly.Tmp> vals) {
        int n = vals.size();
        double[] tmp = new double[n];
        for (int i = 0; i < n; i++) {
            tmp[n - i - 1] = vals.get(i).temp();
        }

        // TODO(knorton): Update the temperature view
    }

    @Override
    public void hourlyDidUpdate() {
        AgentApi.Hourly hourly = mService.getHourly();
        mHeartDataView.updateHourlyData(hourly.hrt());
        updateTmpData(hourly.tmp());
    }
}
