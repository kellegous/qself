package kellegous.hud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WeatherDataView extends DataView {
    private NamedStatusView mIndoorView;
    private NamedStatusView mOutdoorView;

    public WeatherDataView(Context context) {
        super(context);
    }

    public WeatherDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeatherDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WeatherDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.cloud_stroke;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_weather_data, parent, false);

        mIndoorView = (NamedStatusView) view.findViewById(R.id.weather_current_temp_int_view);
        mOutdoorView = (NamedStatusView) view.findViewById(R.id.weather_current_temp_ext_view);

        return view;
    }

    private static String toDegreeString(int tmp) {
        return String.format("%d\u00b0", tmp);
    }

    public void setCurrentTemperature(int indoor, int outdoor) {
        mIndoorView.setValue(toDegreeString(indoor));
        mOutdoorView.setValue(toDegreeString(outdoor));
    }
}
