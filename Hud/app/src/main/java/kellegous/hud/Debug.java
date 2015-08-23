package kellegous.hud;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Debug {
    private static Paint sStrokePaint;
    private static Paint sFillPaint;

    private Debug() {
    }

    private static void init() {
        if (sStrokePaint != null) {
            return;
        }

        sFillPaint = new Paint(0);
        sFillPaint.setColor(0x11ffff00);

        sStrokePaint = new Paint(0);
        sStrokePaint.setColor(0x66ff0000);
        sStrokePaint.setStyle(Paint.Style.STROKE);

    }

    public static void rect(Canvas canvas, float left, float top, float right, float bottom) {
        init();
        canvas.drawRect(left, top, right, bottom, sFillPaint);
    }

    public static void line(Canvas canvas, float xa, float ya, float xb, float yb) {
        init();
        canvas.drawLine(xa, ya, xb, yb, sStrokePaint);
    }

    public static void hline(Canvas canvas, float xa, float xb, float y) {
        line(canvas, xa, y, xb, y);
    }

    public static void vline(Canvas canvas, float x, float ya, float yb) {
        line(canvas, x, ya, x, yb);
    }
}
