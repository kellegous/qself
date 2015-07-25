package kellegous.hud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import kellegous.hud.kellegous.hud.api.Sensors;

public class HeartDataView extends DataView {
    private MetricHistoryView mHrtView;
    private MetricHistoryView mHrvView;

    public HeartDataView(Context context) {
        super(context);
    }

    public HeartDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HeartDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HeartDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.heart_stroke;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_heart_data, parent, false);

        mHrtView = (MetricHistoryView)view.findViewById(R.id.metric_history_hrt);
        mHrvView = (MetricHistoryView)view.findViewById(R.id.metric_history_hrv);

        return view;
    }

    public void updateCurrentData(double hrt, double hrv) {
        mHrtView.setValue((int)Math.round(hrt));
        mHrvView.setValue((int)Math.round(hrv));
    }

    public void updateHourlyData(List<Sensors.HourlySummary.Hrt> vals) {
        int n = vals.size();
        String[] labels = new String[n];
        double[] hrt = new double[n];
        double[] hrv = new double[n];

        for (int i = 0; i < n; i++) {
            Sensors.HourlySummary.Hrt h = vals.get(i);
            hrt[n - i - 1] = h.rate();
            hrv[n - i - 1] = h.variability();
            labels[n - i - 1] = h.time().format("%H");
        }

        mHrtView.setData(labels, hrt);
        mHrvView.setData(labels, hrv);
    }
}
