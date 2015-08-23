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

import kellegous.hud.kellegous.hud.api.Sensors;
import kellegous.hud.kellegous.hud.api.Weather;

public class MainFragment extends Fragment  {
    private static final String TAG = MainFragment.class.getSimpleName();

    private Model mModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mModel = Model.create(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mModel != null) {
            mModel.destroy();
        }

        mModel = null;
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

        ((HeartDataView) view.findViewById(R.id.heart_data_view)).setModel(mModel);
        ((WeatherDataView) view.findViewById(R.id.weather_data_view)).setModel(mModel);
        ((TidalDataView) view.findViewById(R.id.tidal_data_view)).setModel(mModel);
        ((NewHeartDataView) view.findViewById(R.id.new_heart_data_view)).setModel(mModel);

        return view;
    }
}
