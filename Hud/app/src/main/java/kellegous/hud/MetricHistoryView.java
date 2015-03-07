package kellegous.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetricHistoryView extends LinearLayout{
    private TextView mMetricValue;
    private TextView mMetricLabel;
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
        LayoutInflater.from(context).inflate(R.layout.view_metric_history, this, true);

        mMetricValue = (TextView)findViewById(R.id.metric_value);
        mMetricLabel = (TextView)findViewById(R.id.metric_label);
        mMetricGraph = (MetricHistoryGraph)findViewById(R.id.metric_graph);
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
        mMetricValue.setText(Integer.toString(value));
    }

    public void setLabel(String text) {
        mMetricLabel.setText(text);
    }

    public void setLabel(int refid) {
        mMetricLabel.setText(refid);
    }

    public void setData(double[] data) {
        mMetricGraph.setData(data);
    }
}
