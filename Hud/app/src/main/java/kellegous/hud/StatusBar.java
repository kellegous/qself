package kellegous.hud;

import android.util.Log;

public class StatusBar {
    private static final String TAG = StatusBar.class.getSimpleName();

    private static boolean sIsShowing = true;

    public static void hide() throws Exception {
        Runtime.getRuntime().exec(new String[]{
                        "su",
                        "-c",
                        "service call activity 42 s16 com.android.systemui"}
        ).waitFor();
        sIsShowing = false;
    }

    public static void show() throws Exception {
        Runtime.getRuntime().exec(new String[]{
                "am",
                "startservice",
                "-n",
                "com.android.systemui/.SystemUIService"}).waitFor();
        sIsShowing = true;
    }

    public static void toggle() throws Exception {
        if (sIsShowing) {
            hide();
        } else {
            show();
        }
    }

    public static void tryToToggle() {
        if (sIsShowing) {
            tryToHide();
        } else {
            tryToShow();
        }
    }

    public static void tryToHide() {
        try {
            hide();
        } catch (Exception e) {
            Log.e(TAG, "unable to hide status bar", e);
        }
    }

    public static void tryToShow() {
        try {
            show();
        } catch (Exception e) {
            Log.e(TAG, "unable to show status bar", e);
        }
    }
}
