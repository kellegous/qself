package kellegous.hud;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

public class FragmentUtils {
    public interface Creator {
        Fragment create();
    }

    public static void bind(Activity activity, Creator creator) {
        activity.setContentView(R.layout.activity_fragment);
        FragmentManager fm = activity.getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_root);
        if (fragment == null) {
            fm.beginTransaction()
                    .add(R.id.fragment_root, creator.create())
                    .commit();
        }
    }
}
