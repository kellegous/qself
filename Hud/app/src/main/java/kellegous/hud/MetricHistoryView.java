package kellegous.hud;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetricHistoryView extends LinearLayout{
    private TextView mMetricValue;

    public MetricHistoryView(Context context) {
        super(context);
        init(context);
    }

    public MetricHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MetricHistoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_metric_history, this, true);

        mMetricValue = (TextView)findViewById(R.id.metric_value);

        // Typeface tf = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
        // mMetricValue.setTypeface(tf);
    }
}
