package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import kellegous.hud.kellegous.hud.api.Tides;

public class TidalDataView extends DataView {
    private static class ImplView extends View implements Model.TidalListener {
        private static final boolean DEBUG = false;

        private static final int DEPTH_GRAPH_COLOR = 0xff0099ff;
        private static final float GRAPH_VALUE_WIDTH = 2f;

        private static final int GRAPH_MARKER_COLOR = 0xffeeeeee;
        private static final float GRAPH_MARKER_WIDTH = 1f;

        private static final int GRAPH_NOW_COLOR = 0xff999999;

        private static final int VIEW_PADDING = 32;

        private Tides.Report mReport = new Tides.Report();

        private RectF mViewRect = new RectF();
        private float mMinValue;
        private float mMaxValue;

        private Paint mDepthGraphPaint;
        private Paint mGraphMarkerPaint;
        private Paint mGraphNowPaint;

        private Paint mDebugFillPaint;
        private Paint mDebugStrokePaint;

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
            mDepthGraphPaint = new Paint(0);
            mDepthGraphPaint.setColor(DEPTH_GRAPH_COLOR);
            mDepthGraphPaint.setStyle(Paint.Style.STROKE);
            mDepthGraphPaint.setStrokeWidth(GRAPH_VALUE_WIDTH);
            mDepthGraphPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

            mGraphMarkerPaint = new Paint(0);
            mGraphMarkerPaint.setColor(GRAPH_MARKER_COLOR);
            mGraphMarkerPaint.setStyle(Paint.Style.STROKE);
            mGraphMarkerPaint.setStrokeWidth(GRAPH_MARKER_WIDTH);

            mGraphNowPaint = new Paint(0);
            mGraphNowPaint.setColor(GRAPH_NOW_COLOR);

            mDebugFillPaint = new Paint(0);
            mDebugFillPaint.setColor(0x11ffff00);

            mDebugStrokePaint = new Paint(0);
            mDebugStrokePaint.setColor(0x66ff0000);
            mDebugStrokePaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);

            float padding = Dimens.dpToPx(getResources(), VIEW_PADDING);

            mViewRect.set(0, padding, getWidth(), getHeight() - padding);

            float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
            List<Tides.Prediction> predictions = mReport.predictions();
            for (int i = 0, n = predictions.size(); i < n; i++) {
                Tides.Prediction prediction = predictions.get(i);
                float value = (float)prediction.value();
                if (value > max) {
                    max = value;
                } else if (value < min) {
                    min = value;
                }
            }

            mMinValue = min;
            mMaxValue = max;
        }

        private float secondsSpanning(List<Tides.Prediction> predictions) {
            return secondsBetween(
                    predictions.get(0).time(),
                    predictions.get(predictions.size() - 1).time());
        }

        private float secondsBetween(Time a, Time b) {
            return (b.toMillis(true) - a.toMillis(true)) / 1000f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Resources resources = getResources();

            float padding = Dimens.dpToPx(resources, VIEW_PADDING);

            List<Tides.Prediction> predictions = mReport.predictions();
            if (predictions.isEmpty()) {
                return;
            }

            float rangeInSeconds = secondsSpanning(predictions);

            float dy = mViewRect.height() / (mMaxValue - mMinValue);
            float dx = (float)getWidth() / predictions.size();
            float dt = getWidth() / rangeInSeconds;

            canvas.drawRect(
                    dt * secondsBetween(predictions.get(0).time(), mReport.now()),
                    padding,
                    dt * secondsBetween(predictions.get(0).time(), mReport.now()) + 6*60*dt,
                    getHeight() - padding,
                    mGraphNowPaint);

            for (int i = 0, n = predictions.size(); i < n; i++) {
                Tides.Prediction prediction = predictions.get(i);
                if (prediction.time().minute != 0) {
                    continue;
                }

                canvas.drawLine(
                        dx*i + dx/2f,
                        padding,
                        dx*i + dx/2f,
                        getHeight() - padding,
                        mGraphMarkerPaint);
            }

            Path path = new Path();
            path.moveTo(0,
                    padding + + mViewRect.height() - ((float) predictions.get(0).value() - mMinValue) * dy);
            for (int i = 1, n = predictions.size(); i < n; i++) {
                Tides.Prediction prediction = predictions.get(i);
                path.lineTo(
                        dx * i + dx / 2f,
                        padding + mViewRect.height() - ((float) prediction.value() - mMinValue) * dy);
            }
            canvas.drawPath(path, mDepthGraphPaint);

            if (DEBUG) {
                canvas.drawLine(0, mViewRect.top, getWidth(), mViewRect.top, mDebugStrokePaint);
                canvas.drawLine(0, mViewRect.bottom, getWidth(), mViewRect.bottom, mDebugStrokePaint);
            }
        }

        @Override
        public void tidalPredictionsDidUpdate(Tides.Report report) {
            List<Tides.Prediction> predictions = report.predictions();

            mReport = report;
            requestLayout();
        }
    }

    private ImplView mView;

    public TidalDataView(Context context) {
        super(context);
    }

    public TidalDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TidalDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TidalDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.tidal_stroke;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        mView = new ImplView(getContext());
        return mView;
    }

    public void setModel(Model model) {
        model.tides().tap(mView);
    }
}
