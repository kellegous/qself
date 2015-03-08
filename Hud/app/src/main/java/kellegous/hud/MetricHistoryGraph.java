package kellegous.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.Random;

/**
 * Created by knorton on 2/8/15.
 */
public class MetricHistoryGraph extends View{
    private static final int PADDING_BETWEEN_GRAPH_AND_LABELS = 5;

    private int mNumberOfSamples;
    private double[] mData;
    private String[] mLabels;

    private int mGraphHeight;

    private Paint mBarPaint;

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
        mBarPaint = new Paint(0);

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

        setBarColor(a.getColor(R.styleable.MetricHistoryGraph_barColor, 0xff000000));

        setNumberOfSamples(a.getInteger(R.styleable.MetricHistoryGraph_numberOfSamples, 24));

        a.recycle();
    }

    public void setBarColor(int color) {
        mBarPaint.setColor(color);
    }

    public void setNumberOfSamples(int n) {
        mNumberOfSamples = n;
    }

    public void setData(String[] labels, double[] data) {
        mLabels = labels;
        mData = data;
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
            canvas.drawRect(pl + bx, pt + by, pl + bx + dw - 2, pt + h, mBarPaint);
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
