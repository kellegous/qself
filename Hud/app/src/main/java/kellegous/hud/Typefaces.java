package kellegous.hud;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public class Typefaces {
    public static final String RobotoThin = "Roboto-Thin.ttf";
    public static final String RobotoCondensedLight = "RobotoCondensed-Light.ttf";

    private static Map<String, Typeface> sFonts = new HashMap<>();

    private Typefaces() {
    }

    public static Typeface load(Context context, String name) {
        Typeface face = sFonts.get(name);
        if (face != null) {
            return face;
        }


        face = Typeface.createFromAsset(context.getAssets(), name);
        sFonts.put(name, face);
        return face;
    }
}
