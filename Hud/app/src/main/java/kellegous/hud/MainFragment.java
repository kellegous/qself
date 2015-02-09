package kellegous.hud;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainFragment extends Fragment implements ServiceConnection, UpdateService.Listener {
    private UpdateService mService;

    private MetricHistoryView mHrtView;
    private MetricHistoryView mHrvView;

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

        mHrtView = (MetricHistoryView)view.findViewById(R.id.metric_history_hrt);
        mHrvView = (MetricHistoryView)view.findViewById(R.id.metric_history_hrv);

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
    public void dataWasUpdated() {
        mHrtView.setValue(mService.getHrt());
        mHrvView.setValue(mService.getHrv());
    }
}
