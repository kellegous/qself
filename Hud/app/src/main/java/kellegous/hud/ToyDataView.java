package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by knorton on 7/22/15.
 */
public class ToyDataView extends DataView {
    private static final String TAG = ToyDataView.class.getSimpleName();

    public ToyDataView(Context context) {
        super(context);
    }

    public ToyDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToyDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ToyDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.cloud_stroke;
    }

    private static class ToyView extends View {

        private Paint mValuePaint;
        private Paint mLabelPaint;

        private Paint mHilitePaint;

        private Paint mDebugBgPaint;

        public ToyView(Context context) {
            super(context);
            init(context);
        }

        public ToyView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }

        public ToyView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init(context);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public ToyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init(context);
        }

        private void init(Context context) {
            Typeface roboThin = Typefaces.load(context, Typefaces.RobotoThin);
            Typeface roboCondLight = Typefaces.load(context, Typefaces.RobotoCondensedLight);

            mValuePaint = new Paint(0);
            mValuePaint.setColor(0xff666666);
            mValuePaint.setTypeface(roboThin);
            mValuePaint.setTextSize(96f);

            mLabelPaint = new Paint(0);
            mLabelPaint.setColor(0xff999999);
            mLabelPaint.setTypeface(roboCondLight);
            mLabelPaint.setTextSize(24f);

            mHilitePaint = new Paint(0);
            mHilitePaint.setColor(0xffff9900);
            mHilitePaint.setStyle(Paint.Style.STROKE);

            mDebugBgPaint = new Paint(0);
            mDebugBgPaint.setColor(0x33ffff00);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float padding = Dimens.dpToPx(getResources(), 16);

            String value = "87\u00b0";
            String label = "INDOOR";

            Rect valueRect = new Rect();
            mValuePaint.getTextBounds(value, 0, value.length(), valueRect);
            Paint.FontMetrics valueMetrics = mValuePaint.getFontMetrics();

            Rect labelRect = new Rect();
            mLabelPaint.getTextBounds(label, 0, label.length(), labelRect);
            Paint.FontMetrics labelMetrics = mLabelPaint.getFontMetrics();

            canvas.drawRect(0, 0, valueRect.width() + 2 * padding, getHeight(), mDebugBgPaint);

            float w = valueRect.width() + 2*padding;
            Log.d(TAG, String.format("width = %f, %f", w, Dimens.pxToDp(getResources(), w)));

            canvas.drawText(value, padding, getHeight() / 2f + valueMetrics.bottom, mValuePaint);

            canvas.drawText(label, padding, getHeight()/2f + valueMetrics.bottom - labelMetrics.top + 8, mLabelPaint);
//            canvas.drawLine(0, 100, rect.right, 100, mHilitePaint);
//            canvas.drawLine(0, 100 + metrics.ascent, rect.right, 100 + metrics.ascent, mHilitePaint);
//            canvas.drawLine(0, 100 + metrics.top, rect.right, 100 + metrics.top, mHilitePaint);
//            canvas.drawLine(0, 100 + metrics.descent, rect.right, 100 + metrics.descent, mHilitePaint);
//            canvas.drawLine(0, 100 + metrics.bottom, rect.right, 100 + metrics.bottom, mHilitePaint);
            // canvas.drawRect(rect, mHilitePaint);
        }
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.toy_data_view, parent, false);
    }
}
