package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class NamedStatusView extends View {
    private static final int VALUE_COLOR = 0xff666666;
    private static final float VALUE_TEXT_SIZE = 96f;

    private static final int LABEL_COLOR = 0xff999999;
    private static final float LABEL_TEXT_SIZE = 24f;

    private static final int VIEW_WIDTH = 168;
    private static final int VIEW_HEIGHT = 168;

    private static final int VIEW_PADDING = 16;
    private static final int LABEL_MARGIN_TOP = 8;

    private String mLabel = "";
    private String mValue = "";

    private Paint mLabelPaint;
    private Paint mValuePaint;

    private int mBaseline;

    private Rect mValueBounds = new Rect();
    private float mValueBase;

    private Rect mLabelBounds = new Rect();
    private float mLabelBase;

    private float mLeftOffset;

    public NamedStatusView(Context context) {
        super(context);
        init(context);
    }

    public NamedStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyAttrs(context, attrs);
        init(context);
    }

    public NamedStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyAttrs(context, attrs);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NamedStatusView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        applyAttrs(context, attrs);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = widthSize;
        if (widthMode != MeasureSpec.EXACTLY) {
            width = VIEW_WIDTH;
        }

        int height = heightSize;
        if (heightMode != MeasureSpec.EXACTLY) {
            height = VIEW_HEIGHT;
        }

        setMeasuredDimension(width, height);
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.NamedStatusView,
                0, 0);

        String label = a.getString(R.styleable.NamedStatusView_statusLabel);
        setLabel(label == null ? "" : label);

        String value = a.getString(R.styleable.NamedStatusView_statusValue);
        setValue(value == null ? "" : value);
    }

    private void init(Context context) {
        Typeface roboThin = Typefaces.load(context, Typefaces.RobotoThin);
        mValuePaint = new Paint(0);
        mValuePaint.setColor(VALUE_COLOR);
        mValuePaint.setTypeface(roboThin);
        mValuePaint.setTextSize(VALUE_TEXT_SIZE);

        Typeface roboCondLight = Typefaces.load(context, Typefaces.RobotoCondensedLight);
        mLabelPaint = new Paint(0);
        mLabelPaint.setColor(LABEL_COLOR);
        mLabelPaint.setTypeface(roboCondLight);
        mLabelPaint.setTextSize(LABEL_TEXT_SIZE);
    }

    public void setLabel(String label) {
        mLabel = label.toUpperCase();
        requestLayout();
    }

    public void setValue(String value) {
        mValue = value.toUpperCase();
        requestLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Resources resources = getResources();
        float padding = Dimens.dpToPx(resources, VIEW_PADDING);

        Paint.FontMetrics valueMetrics = mValuePaint.getFontMetrics();
        Paint.FontMetrics labelMetrics = mLabelPaint.getFontMetrics();

        mValuePaint.getTextBounds(mValue, 0, mValue.length(), mValueBounds);
        mLabelPaint.getTextBounds(mLabel, 0, mLabel.length(), mLabelBounds);

        float valueBase = (bottom - top) / 2f + valueMetrics.bottom;
        float labelBase = valueBase - labelMetrics.top + Dimens.dpToPx(resources, LABEL_MARGIN_TOP);

        mValueBounds.offset((int) padding, (int) valueBase);
        mLabelBounds.offset((int)padding, (int)labelBase);

        mLeftOffset = padding;
        mValueBase = valueBase;
        mLabelBase = labelBase;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw value
        canvas.drawText(mValue, mLeftOffset, mValueBase, mValuePaint);

        // draw value
        canvas.drawText(mLabel, mLeftOffset, mLabelBase, mLabelPaint);
    }

    /**
     * The internal bounds of the value text.
     */
    public Rect getValueBounds() {
        return mValueBounds;
    }

    /**
     * The internal bounds of the label text.
     */
    public Rect getLabelBounds() {
        return mLabelBounds;
    }
}
