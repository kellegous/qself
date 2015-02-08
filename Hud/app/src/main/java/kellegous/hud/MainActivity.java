package kellegous.hud;

import android.app.Activity;
import android.app.Fragment;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentUtils.bind(this, new FragmentUtils.Creator() {
            @Override
            public Fragment create() {
                return new MainFragment();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBar.tryToHide();
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatusBar.tryToShow();
    }
}
