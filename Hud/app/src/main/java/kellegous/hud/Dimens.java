package kellegous.hud;

import android.content.Context;
import android.content.res.Resources;

public class Dimens {
    private Dimens() {
    }

    public static float dpToPx(Resources resouces, float dp) {
        return dp * (resouces.getDisplayMetrics().densityDpi / 160f);
    }

    public static float pxToDp(Resources resources, float px) {
        return px / (resources.getDisplayMetrics().densityDpi / 160f);
    }
}
