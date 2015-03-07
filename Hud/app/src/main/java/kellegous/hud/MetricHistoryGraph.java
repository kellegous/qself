package kellegous.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.Random;

/**
 * Created by knorton on 2/8/15.
 */
public class MetricHistoryGraph extends View{

    private Paint mBarPaint;
    private int mNumberOfSamples;
    private double[] mData;
    private int mBottomOffset = 50;

    private Paint mBgPaint;

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
        mBgPaint.setColor(0xff999999);
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

    public void setBottomOffset(int offset) {
        mBottomOffset = offset;
    }

    public void setData(double[] data) {
        mData = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // canvas.drawRect(0, 0, getWidth(), getHeight(), mBgPaint);

        if (mData == null || mData.length == 0) {
            return;
        }

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        int w = getWidth() - pl - pr;
        int h = (getHeight() - mBottomOffset - pb - pt);

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
    }
}
