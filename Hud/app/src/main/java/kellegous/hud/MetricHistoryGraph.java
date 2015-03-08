package kellegous.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by knorton on 2/8/15.
 */
public class MetricHistoryGraph extends View{
    private static final int PADDING_BETWEEN_GRAPH_AND_LABELS = 5;

    private int mNumberOfSamples;
    private double[] mData;
    private boolean[] mInterpolated;
    private String[] mLabels;

    private int mGraphHeight;

    private Paint mRealBarPaint;
    private Paint mFakeBarPaint;

    private Paint mBgPaint;

    private Paint mLabelPaint;

    public MetricHistoryGraph(Context context) {
        super(context);
        init(context);
    }

    public MetricHistoryGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        applyAttrs(context, attrs);
    }

    public MetricHistoryGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        applyAttrs(context, attrs);
    }

    private void init(Context context) {
        mRealBarPaint = new Paint(0);

        mFakeBarPaint = new Paint(0);

        mBgPaint = new Paint(0);
        mBgPaint.setColor(0xffffffff);

        mLabelPaint = new Paint(0);
        mLabelPaint.setColor(0xff999999);
        mLabelPaint.setTextSize(10);
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MetricHistoryGraph,
                0, 0);

        setRealBarColor(a.getColor(R.styleable.MetricHistoryGraph_realBarColor, 0xff000000));
        setFakeBarColor(a.getColor(R.styleable.MetricHistoryGraph_fakeBarColor, 0xff666666));

        setNumberOfSamples(a.getInteger(R.styleable.MetricHistoryGraph_numberOfSamples, 24));

        a.recycle();
    }

    public void setRealBarColor(int color) {
        mRealBarPaint.setColor(color);
    }

    public void setFakeBarColor(int color) {
        mFakeBarPaint.setColor(color);
    }

    public void setNumberOfSamples(int n) {
        mNumberOfSamples = n;
    }

    /**
     * Interpolates missing data points.
     */
    private void smoothAndAssign(double[] data) {
        int n = data.length;
        double[] smoothed = new double[n];
        boolean[] interpolated = new boolean[n];

        int i = 0;
        while (i < n) {
            // if this element is non-zero, just keep it
            if (data[i] > 0.001) {
                smoothed[i] = data[i];
                i++;
                continue;
            }

            // otherwise, find the next non-zero item
            int j = i + 1;
            for (; j < n; j++) {
                if (data[j] > 0.001) {
                    break;
                }
            }

            // now average the bookends to fill in the missing values
            double s = 0.0;
            int k = 0;
            if (i > 0) {
                s += data[i - 1];
                k++;
            }

            if (j < n) {
                s += data[j];
                k++;
            }

            if (k == 0) {
                return;
            }

            double v = s/k;
            while (i != j) {
                smoothed[i] = v;
                interpolated[i] = true;
                i++;
            }
        }

        mData = smoothed;
        mInterpolated = interpolated;
    }

    public void setData(String[] labels, double[] data) {
        mLabels = labels;
        smoothAndAssign(data);
        invalidate();
    }

    void setGraphHeight(int height) {
        mGraphHeight = height;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mData == null || mData.length == 0) {
            return;
        }

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();

        int w = getWidth() - pl - pr;
        int h = mGraphHeight;

        float dw = (float) w / (float) mNumberOfSamples;

        double max = mData[0];
        for (int i = 1, n = mData.length; i < n; i++) {
            max = Math.max(max, mData[i]);
        }

        int n = Math.min(mData.length, mNumberOfSamples);
        for (int i = 0; i < n; i++) {
            float bx = i*dw + 1;
            float by = h - Math.max((float)((mData[i]/max) * h), 1.0f);
            canvas.drawRect(pl + bx, pt + by, pl + bx + dw - 2, pt + h,
                    mInterpolated[i] ? mFakeBarPaint : mRealBarPaint);
        }

        Rect rect = new Rect();
        mLabelPaint.getTextBounds(mLabels[0], 0, mLabels[0].length(), rect);
        int labelHeight = rect.height();

        canvas.drawText(
                mLabels[0],
                pl + dw/2 - rect.width()/2,
                pt + mGraphHeight + labelHeight + PADDING_BETWEEN_GRAPH_AND_LABELS,
                mLabelPaint);

        for (int i = 1; i < n; i++) {
            String label = mLabels[i];
            mLabelPaint.getTextBounds(label, 0, label.length(), rect);
            canvas.drawText(
                    label,
                    pl + i*dw + dw/2 - rect.width()/2,
                    pt + mGraphHeight + labelHeight + PADDING_BETWEEN_GRAPH_AND_LABELS,
                    mLabelPaint);
        }
    }
}
