package kellegous.beats;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements ServiceConnection,
        HeartDataService.Listener, View.OnClickListener {
    private static final String TAG = MainFragment.class.getSimpleName();

    private final int REQUEST_ENABLE_BT = 1;

    private TextView mRateView;
    private TextView mIntervalView;
    private TextView mBatteryView;

    private HeartDataService mService;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().bindService(
                new Intent(getActivity(), HeartDataService.class),
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mRateView = (TextView)view.findViewById(R.id.textview_hr);
        mIntervalView = (TextView)view.findViewById(R.id.textview_rr);
        mBatteryView = (TextView)view.findViewById(R.id.textview_battery);

        mBatteryView.setOnClickListener(this);

        return view;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((HeartDataService.Binder)service).getService();
        mService.addListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void readingReceived(HeartDataNotifier.Reading reading) {
        mRateView.setText(String.format("%d bpm", reading.heartRate()));
        mRateView.setVisibility(View.VISIBLE);
        if (reading.hasInterval()) {
            mIntervalView.setText(String.format("%d ms", reading.interval()));
            mIntervalView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void batteryLevelWasReceived(float pct) {
        int level = (int)(pct * 100.0);
        mBatteryView.setText(String.format("%d%%", level));
        mBatteryView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.textview_battery) {
            mService.requestBatteryLevel();
        }
    }
}
