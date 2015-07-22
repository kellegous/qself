package kellegous.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetricHistoryView extends LinearLayout {
    private static final String TAG = MetricHistoryView.class.getSimpleName();

    private NamedStatusView mStatusView;
    private MetricHistoryGraph mMetricGraph;

    public MetricHistoryView(Context context) {
        super(context);
        init(context);
    }

    public MetricHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        applyAttrs(context, attrs);
    }

    public MetricHistoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        applyAttrs(context, attrs);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_metric_history, this, true);
        mStatusView = (NamedStatusView)findViewById(R.id.metric_status_view);
        mMetricGraph = (MetricHistoryGraph)findViewById(R.id.metric_graph);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        Rect valueRect = mStatusView.getValueBounds();

        mMetricGraph.setPadding(
                mMetricGraph.getPaddingLeft(),
                valueRect.top,
                mMetricGraph.getPaddingRight(),
                0);

        mMetricGraph.setGraphHeight(valueRect.height());
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MetricHistoryView,
                0, 0);

        setLabel(a.getString(R.styleable.MetricHistoryView_label));

        a.recycle();
    }

    public void setValue(int value) {
        mStatusView.setValue(Integer.toString(value));
        requestLayout();
    }

    public void setLabel(String text) {
        mStatusView.setLabel(text);
        requestLayout();
    }

    public void setLabel(int refid) {
        setLabel(getResources().getString(refid));
        requestLayout();
    }

    public void setData(String[] labels, double[] values) {
        mMetricGraph.setData(labels, values);
    }
}
