package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.util.List;

import kellegous.hud.kellegous.hud.api.Tides;

public class TidalDataView extends DataView {
    private static class ImplView extends View implements Model.TidalListener {
        private static final boolean DEBUG = false;

        private static final int DEPTH_GRAPH_COLOR = 0xff0099ff;
        private static final float GRAPH_VALUE_WIDTH = 2f;
        private static final float DATA_LABEL_TEXT_SIZE = 12f;

        private static final int GRAPH_MARKER_COLOR = 0xffeeeeee;
        private static final float GRAPH_MARKER_TEXT_SIZE = 18f;
        private static final float GRAPH_MARKER_WIDTH = 1f;

        private static final int GRAPH_LABEL_COLOR = 0xff999999;
        private static final float GRAPH_LABEL_TEXT_SIZE = 12f;
        private static final float GRAPH_LABEL_WIDTH = 2f;

        private static final int VIEW_PADDING = 32;

        private static final DecimalFormat DEPTH_FORMAT = new DecimalFormat("0.0");

        private Tides.Report mReport = new Tides.Report();

        private RectF mViewRect = new RectF();
        private float mMinValue;
        private float mMaxValue;
        private float mLabelBaseline;

        private Paint mGraphDataPaint;
        private Paint mGraphNowFillPaint;
        private Paint mGraphNowStrokePaint;
        private Paint mGraphNowBackFillPaint;
        private Paint mGraphNowLabelPaint;
        private Paint mGraphMarkerPaint;
        private Paint mGraphLabelPaint;

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
            Typeface roboCondReg = Typefaces.load(context, Typefaces.RobotoCondensedRegular);

            mGraphDataPaint = new Paint(0);
            mGraphDataPaint.setColor(DEPTH_GRAPH_COLOR);
            mGraphDataPaint.setStyle(Paint.Style.STROKE);
            mGraphDataPaint.setStrokeWidth(GRAPH_VALUE_WIDTH);
            mGraphDataPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

            mGraphNowFillPaint = new Paint(0);
            mGraphNowFillPaint.setColor(DEPTH_GRAPH_COLOR);
            mGraphNowFillPaint.setStyle(Paint.Style.FILL);
            mGraphNowFillPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

            mGraphNowStrokePaint = new Paint(0);
            mGraphNowStrokePaint.setColor(DEPTH_GRAPH_COLOR);
            mGraphNowStrokePaint.setStyle(Paint.Style.STROKE);
            mGraphNowStrokePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mGraphNowStrokePaint.setStrokeWidth(1f);

            mGraphNowBackFillPaint = new Paint(0);
            mGraphNowBackFillPaint.setColor(Color.WHITE);
            mGraphNowBackFillPaint.setStyle(Paint.Style.FILL);

            mGraphNowLabelPaint = new Paint(0);
            mGraphNowLabelPaint.setColor(DEPTH_GRAPH_COLOR);
            mGraphNowLabelPaint.setTextSize(DATA_LABEL_TEXT_SIZE);
            mGraphNowLabelPaint.setTypeface(roboCondReg);

            mGraphMarkerPaint = new Paint(0);
            mGraphMarkerPaint.setColor(GRAPH_MARKER_COLOR);
            mGraphMarkerPaint.setStyle(Paint.Style.STROKE);
            mGraphMarkerPaint.setStrokeWidth(GRAPH_MARKER_WIDTH);
            mGraphMarkerPaint.setTextSize(GRAPH_MARKER_TEXT_SIZE);

            mGraphLabelPaint = new Paint(0);
            mGraphLabelPaint.setColor(GRAPH_LABEL_COLOR);
            mGraphLabelPaint.setTextSize(GRAPH_LABEL_TEXT_SIZE);
            mGraphLabelPaint.setTypeface(roboCondReg);
            mGraphLabelPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mGraphLabelPaint.setStrokeWidth(GRAPH_LABEL_WIDTH);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);

            float padding = Dimens.dpToPx(getResources(), VIEW_PADDING);

            Paint.FontMetrics labelMetrics = mGraphLabelPaint.getFontMetrics();

            // determine the rect that encloses the actual graph
            mViewRect.set(0, padding, getWidth(), getHeight() - padding + labelMetrics.top);

            mLabelBaseline = getHeight() - padding;

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
            Rect rect = new Rect();

            float padding = Dimens.dpToPx(resources, VIEW_PADDING);

            List<Tides.Prediction> predictions = mReport.predictions();
            if (predictions.isEmpty()) {
                return;
            }

            float dy = mViewRect.height() / (mMaxValue - mMinValue);
            float dx = (float)getWidth() / predictions.size();

            for (int i = 0, n = predictions.size(); i < n; i++) {
                Tides.Prediction prediction = predictions.get(i);
                if (prediction.time().minute != 0) {
                    continue;
                }

                float x = dx*i + dx / 2f;

                canvas.drawLine(x, padding, x, mViewRect.bottom, mGraphMarkerPaint);
                String text = prediction.time().format("%H");
                mGraphMarkerPaint.getTextBounds(text, 0, text.length(), rect);
                float tx = x - rect.width() / 2f;
                if (tx > 0) {
                    canvas.drawText(text, tx, mLabelBaseline, mGraphLabelPaint);
                }
            }

            Path path = new Path();
            path.moveTo(0,
                    padding + + mViewRect.height() - (predictions.get(0).value() - mMinValue) * dy);
            for (int i = 1, n = predictions.size(); i < n; i++) {
                Tides.Prediction prediction = predictions.get(i);
                path.lineTo(
                        dx * i + dx / 2f,
                        padding + mViewRect.height() - (prediction.value() - mMinValue) * dy);
            }
            canvas.drawPath(path, mGraphDataPaint);

            drawNowHighlight(canvas, resources, padding, dx, dy, rect);

            if (DEBUG) {
                Debug.hline(canvas, 0, getWidth(), mViewRect.top);
                Debug.hline(canvas, 0, getWidth(), mViewRect.bottom);
                Debug.hline(canvas, 0, getWidth(), mLabelBaseline);
            }
        }

        private void drawNowHighlight(Canvas canvas,
                                      Resources resources,
                                      float offsetY,
                                      float dx,
                                      float dy,
                                      Rect rect) {
            int indexOfNow = mReport.indexOfNow();
            if (indexOfNow == -1) {
                return;
            }

            List<Tides.Prediction> predictions = mReport.predictions();
            Tides.Prediction now = predictions.get(indexOfNow);
            float cx = dx * indexOfNow + dx / 2f;
            float cy = offsetY + mViewRect.height() - dy * (now.value() - mMinValue);

            canvas.drawCircle(cx, cy, Dimens.dpToPx(resources, 8), mGraphNowBackFillPaint);
            canvas.drawCircle(cx, cy, Dimens.dpToPx(resources, 3), mGraphNowFillPaint);
            canvas.drawCircle(cx, cy, Dimens.dpToPx(resources, 8), mGraphDataPaint);

            String text = String.format("%s ft", DEPTH_FORMAT.format(now.value()));
            mGraphLabelPaint.getTextBounds(text, 0, text.length(), rect);

            float py = (now.value() - mMinValue) / (mMaxValue - mMinValue);
            float tx, ty;
            if (py > 0.85) {
                tx = cx - rect.width() / 2f;
                ty = cy + Dimens.dpToPx(resources, 16) + rect.height() / 2f;
            } else if (py < 0.15) {
                tx = cx - rect.width() / 2f;
                ty = cy - Dimens.dpToPx(resources, 16);
            } else {
                tx = cx + Dimens.dpToPx(resources, 16);
                ty = cy;
            }
            canvas.drawText(text, tx, ty, mGraphNowLabelPaint);
        }

        @Override
        public void tidalPredictionsDidUpdate(Tides.Report report) {
            mReport = report;
            requestLayout();
            invalidate();
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
