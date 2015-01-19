package kellegous.beats;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements ServiceConnection,
        HeartDataService.Listener {
    private static final String TAG = MainFragment.class.getSimpleName();

    private final int REQUEST_ENABLE_BT = 1;

    private TextView mRateView;
    private TextView mIntervalView;

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

        return view;
    }

    private void log(final String message) {
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
}
