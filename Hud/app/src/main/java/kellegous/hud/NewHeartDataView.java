package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kellegous.hud.kellegous.hud.api.Sensors;

public class NewHeartDataView extends DataView {
    private static class ImplView extends View implements Model.SensorsListener {
        private static final boolean DEBUG = true;

        private static final int PADDING = 8;

        private static final int VALUE_COLOR = 0xff999999;
        private static final float VALUE_TEXT_SIZE = 64f;

        private static final int LABEL_COLOR = 0xff999999;
        private static final float LABEL_TEXT_SIZE = 18f;

        private String mHrtRate = "";
        private String mHrtVariability = "";

        private Paint mValuePaint;
        private Paint mLabelPaint;

        private float mCenterBaseline;
        private float mTopValueBaseline;
        private float mTopLabelBaseline;
        private float mTopValueXHeight;
        private float mBottomValueBaseline;
        private float mBottomLabelBaseline;
        private float mBottomValueXHeight;
        private float mValueWidth;

        public ImplView(Context context) {
            super(context);
            init(context);
        }

        public ImplView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }

        public ImplView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init(context);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public ImplView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init(context);
        }

        private void init(Context context) {
            mValuePaint = new Paint(0);
            mValuePaint.setColor(VALUE_COLOR);
            mValuePaint.setTypeface(Typefaces.load(context, Typefaces.RobotoThin));
            mValuePaint.setTextSize(VALUE_TEXT_SIZE);

            mLabelPaint = new Paint(0);
            mLabelPaint.setColor(LABEL_COLOR);
            mLabelPaint.setTypeface(Typefaces.load(context, Typefaces.RobotoCondensedLight));
            mLabelPaint.setTextSize(LABEL_TEXT_SIZE);
        }

        @Override
        public void sensorsStatusDidUpdate(Sensors.Status status) {
            int rate = (int)Math.round(status.hrt().rate());
            int variability = (int)Math.round(status.hrt().variability());

            mHrtRate = Integer.toString(rate);
            mHrtVariability = Integer.toString(variability);
            invalidate();
        }

        @Override
        public void sensorsMinutelySummaryDidUpdate(Sensors.Summary summary) {

        }

        @Override
        public void sensorsHourlySummaryDidUpdate(Sensors.Summary summary) {
        }

        private float computeValueWidth() {
            Rect rect = new Rect();
            String v = "100";
            mValuePaint.getTextBounds(v, 0, v.length(), rect);
            return rect.width();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);

            float padding = Dimens.dpToPx(getResources(), PADDING);

            mCenterBaseline = (bottom - top) / 2f;

            Paint.FontMetrics lm = mLabelPaint.getFontMetrics();
            Paint.FontMetrics vm = mValuePaint.getFontMetrics();
            mTopLabelBaseline = mCenterBaseline - padding;
            mTopValueBaseline = mTopLabelBaseline - padding + lm.top + lm.bottom;
            mTopValueXHeight = mTopValueBaseline + vm.top + vm.bottom;

            mBottomValueXHeight = mCenterBaseline + padding;
            mBottomValueBaseline = mBottomValueXHeight - vm.top - vm.bottom;
            mBottomLabelBaseline = mBottomValueBaseline + padding - lm.top - lm.bottom;

            mValueWidth = computeValueWidth();
        }

        private void drawGraph(Canvas canvas, Rect rect) {
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Resources resources = getResources();
            Rect rect = new Rect();

            // Draw rate value
            mValuePaint.getTextBounds(mHrtRate, 0, mHrtRate.length(), rect);
            canvas.drawText(
                    mHrtRate,
                    mValueWidth - rect.width(),
                    mTopValueBaseline,
                    mValuePaint);

            // Draw rate label
            String rateLabel = resources.getString(R.string.heart_data_view_rate_label);
            mLabelPaint.getTextBounds(rateLabel, 0, rateLabel.length(), rect);
            canvas.drawText(
                    rateLabel,
                    mValueWidth - rect.width(),
                    mTopLabelBaseline,
                    mLabelPaint);

            // Draw hrv value
            mValuePaint.getTextBounds(mHrtVariability, 0, mHrtVariability.length(), rect);
            canvas.drawText(
                    mHrtVariability,
                    mValueWidth - rect.width(),
                    mBottomValueBaseline,
                    mValuePaint);

            // Draw hrv label
            String hrvLabel = resources.getString(R.string.heart_data_view_vary_label);
            mLabelPaint.getTextBounds(hrvLabel, 0, hrvLabel.length(), rect);
            canvas.drawText(
                    hrvLabel,
                    mValueWidth - rect.width(),
                    mBottomLabelBaseline,
                    mLabelPaint);

            if (DEBUG) {
                Debug.hline(canvas, 0, getWidth(), mCenterBaseline);

                Debug.hline(canvas, 0, getWidth(), mTopLabelBaseline);
                Debug.hline(canvas, 0, getWidth(), mTopValueBaseline);
                Debug.hline(canvas, 0, getWidth(), mTopValueXHeight);

                Debug.hline(canvas, 0, getWidth(), mBottomValueXHeight);
                Debug.hline(canvas, 0, getWidth(), mBottomValueBaseline);
                Debug.hline(canvas, 0, getWidth(), mBottomLabelBaseline);

                Debug.vline(canvas, mValueWidth, 0, getHeight());
                Debug.vline(canvas, mValueWidth + Dimens.dpToPx(resources, PADDING), 0, getHeight());
            }
        }
    }

    private ImplView mView;

    public NewHeartDataView(Context context) {
        super(context);
    }

    public NewHeartDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewHeartDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NewHeartDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.heart_stroke;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        mView = new ImplView(getContext());
        return mView;
    }

    public void setModel(Model model) {
        model.sensors().tap(mView);
    }
}
