package kellegous.beats;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;


public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FragmentUtils.bind(this, new FragmentUtils.Creator() {
            @Override
            public Fragment create() {
                return new MainFragment();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
