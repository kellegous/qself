package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import kellegous.hud.kellegous.hud.api.Sensors;
import kellegous.hud.kellegous.hud.api.Weather;

public class WeatherDataView extends DataView {
    private static class ImplView extends View implements Model.SensorsListener, Model.WeatherListener {
        private static final int VALUE_COLOR = 0xff666666;
        private static final float VALUE_TEXT_SIZE = 96f;

        private static final int LABEL_COLOR = 0xff999999;
        private static final float LABEL_TEXT_SIZE = 18f;
        private static final int LABEL_MARGIN_TOP = 12;

        private static final int TITLE_COLOR = 0xff999999;
        private static final float TITLE_TEXT_SIZE = 18f;

        private static final int ICON_COLOR = 0xff999999;
        private static final float ICON_LARGE_TEXT_SIZE = 96f;
        private static final float ICON_SMALL_TEXT_SIZE = 48f;

        private static final int FORECAST_TEMP_COLOR = 0xff999999;
        private static final float FORECAST_TEMP_TEXT_SIZE = 24f;
        private static final int FORECAST_PADDING = 8;

        private static final int FORECAST_PRECIP_COLOR = 0xff999999;
        private static final float FORECAST_PRECIP_TEXT_SIZE = 12f;

        private static final int VIEW_PADDING = 16;
        private static final int VALUE_WIDTH = 168;
        private static final int ICON_WIDTH = VALUE_WIDTH - 32;

        private static final int FORECAST_NUMBER_OF_HOURS = 8;

        private static final float FORECAST_PRECIP_MIN = 0.1f;

        private static final boolean DEBUG = false;

        private String mIndoorTemp = "";
        private String mOutdoorTemp = "";
        private String mOutdoorFeels = "";
        private String mOutdoorIcon = toIconString(Weather.ICON_CLEAR_DAY);
        private String mOutdoorSummary = "";
        private List<Weather.Conditions> mHourlyForecast = new ArrayList<>();

        private Paint mValuePaint;
        private Paint mLabelPaint;
        private Paint mTitlePaint;
        private Paint mLargeIconPaint;
        private Paint mSmallIconPaint;
        private Paint mForecastTempPaint;
        private Paint mForecastPrecipPaint;

        private Paint mDebugFillPaint;
        private Paint mDebugStrokePaint;

        private float mTitleBaseline;
        private float mValueBaseline;
        private float mLabelBaseline;
        private float mIconBaseline;
        private float mForecastTempBaseline;

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

            mTitlePaint = new Paint(0);
            mTitlePaint.setColor(TITLE_COLOR);
            mTitlePaint.setTypeface(roboCondLight);
            mTitlePaint.setTextSize(TITLE_TEXT_SIZE);

            Typeface weather  = Typefaces.load(context, Typefaces.Weather);
            mLargeIconPaint = new Paint(0);
            mLargeIconPaint.setColor(ICON_COLOR);
            mLargeIconPaint.setTypeface(weather);
            mLargeIconPaint.setTextSize(ICON_LARGE_TEXT_SIZE);

            mSmallIconPaint = new Paint(0);
            mSmallIconPaint.setColor(ICON_COLOR);
            mSmallIconPaint.setTypeface(weather);
            mSmallIconPaint.setTextSize(ICON_SMALL_TEXT_SIZE);

            mForecastTempPaint = new Paint(0);
            mForecastTempPaint.setColor(FORECAST_TEMP_COLOR);
            mForecastTempPaint.setTypeface(roboCondLight);
            mForecastTempPaint.setTextSize(FORECAST_TEMP_TEXT_SIZE);

            Typeface roboCondRegular = Typefaces.load(context, Typefaces.RobotoCondensedRegular);
            mForecastPrecipPaint = new Paint(0);
            mForecastPrecipPaint.setColor(FORECAST_PRECIP_COLOR);
            mForecastPrecipPaint.setTypeface(roboCondRegular);
            mForecastPrecipPaint.setTextSize(FORECAST_PRECIP_TEXT_SIZE);

            mDebugFillPaint = new Paint(0);
            mDebugFillPaint.setColor(0x11ffff00);

            mDebugStrokePaint = new Paint(0);
            mDebugStrokePaint.setColor(0x66ff0000);
            mDebugStrokePaint.setStyle(Paint.Style.STROKE);
        }

        private void drawMetric(Canvas canvas, String label, String value, float x) {
            canvas.drawText(value, x, mValueBaseline, mValuePaint);
            canvas.drawText(label, x, mLabelBaseline, mLabelPaint);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);

            Resources resources = getResources();
            float padding = Dimens.dpToPx(resources, VIEW_PADDING);

            Rect rect = new Rect();
            String title = resources.getString(R.string.weather_data_view_indoor_title);
            mTitlePaint.getTextBounds(title, 0, title.length(), rect);
            mTitleBaseline = padding + rect.height();

            Paint.FontMetrics valueMetrics = mValuePaint.getFontMetrics();
            mValueBaseline = (bottom - top)/2f + valueMetrics.bottom;

            Paint.FontMetrics precipMetrics = mForecastPrecipPaint.getFontMetrics();
            Paint.FontMetrics iconMetrics = mLargeIconPaint.getFontMetrics();
            mIconBaseline = mValueBaseline - iconMetrics.descent/2f;

            Paint.FontMetrics labelMetrics = mLabelPaint.getFontMetrics();
            mLabelBaseline = mValueBaseline - labelMetrics.top + Dimens.dpToPx(resources, LABEL_MARGIN_TOP);

            mValuePaint.getTextBounds("7", 0, 1, rect);
            int valueHeight = rect.height();
            mForecastTempPaint.getTextBounds("7", 0, 1, rect);
            mForecastTempBaseline = mValueBaseline - valueHeight + rect.height();
        }

        private static float getIconBaselineOffset(Resources resources, int icon) {
            switch (icon) {
            case Weather.ICON_PARTLY_CLOUDY_DAY:
                return Dimens.dpToPx(resources, 8);
            }
            return Dimens.dpToPx(resources, 4);
        }

        private void drawForecast(Canvas canvas, float x) {
            Resources resources = getResources();

            float padding = Dimens.dpToPx(resources, FORECAST_PADDING);

            float width = getWidth() - x - Dimens.dpToPx(resources, VIEW_PADDING);
            float dx = width / FORECAST_NUMBER_OF_HOURS;

            int n = Math.min(FORECAST_NUMBER_OF_HOURS, mHourlyForecast.size());

            Rect rect = new Rect();

            for (int i = 0; i < n; i++) {
                Weather.Conditions conditions = mHourlyForecast.get(i);

                float xa = x + dx * i + padding;
                float xb = x + dx * (i+1);

                if (DEBUG) {
                    canvas.drawRect(xa, 0, xb, getHeight(), mDebugFillPaint);
                }

                String time = conditions.time().format("%H:%M");
                mLabelPaint.getTextBounds(time, 0, time.length(), rect);
                canvas.drawText(
                        time,
                        xa + (dx - padding)/2f - rect.width()/2f,
                        mLabelBaseline,
                        mLabelPaint);
                float rightOfTemp = xa + (dx - padding)/2f + rect.width()/2f;

                String icon = toIconString(conditions.icon());
                mSmallIconPaint.getTextBounds(icon, 0, icon.length(), rect);
                canvas.drawText(
                        icon,
                        xa + (dx - padding)/2f - rect.width()/2f,
                        mIconBaseline + getIconBaselineOffset(resources, conditions.icon()),
                        mSmallIconPaint);

                String temp = toDegreeString(conditions.temp());
                mLabelPaint.getTextBounds(temp, 0, temp.length(), rect);
                canvas.drawText(
                        temp,
                        xa + (dx - padding)/2f - rect.width()/2f,
                        mForecastTempBaseline,
                        mForecastTempPaint);

                if (conditions.probabilityOfPrecipitation() >= FORECAST_PRECIP_MIN) {
                    String text = toPrecipPercentString(conditions.probabilityOfPrecipitation());
                    mForecastPrecipPaint.getTextBounds(text, 0, text.length(), rect);
                    canvas.drawText(text,
                            rightOfTemp - rect.width(),
                            mValueBaseline + rect.height()/2f + Dimens.dpToPx(resources, 2),
                            mForecastPrecipPaint);
                }

            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (DEBUG) {
                canvas.drawLine(0, mTitleBaseline, getWidth(), mTitleBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mValueBaseline, getWidth(), mValueBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mLabelBaseline, getWidth(), mLabelBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mIconBaseline, getWidth(), mIconBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mForecastTempBaseline, getWidth(), mForecastTempBaseline, mDebugStrokePaint);
            }

            Resources resources = getResources();
            Rect rect = new Rect();
            float padding = Dimens.dpToPx(resources, VIEW_PADDING);
            float valueWidth = Dimens.dpToPx(resources, VALUE_WIDTH);
            float iconWidth = Dimens.dpToPx(resources, ICON_WIDTH);

            float indoorEndsAt = Dimens.dpToPx(resources, VALUE_WIDTH) + padding;

            String indoorTitle = resources.getString(R.string.weather_data_view_indoor_title);
            String outdoorTitle = resources.getString(R.string.weather_data_view_outdoor_title);

            // Titles
            canvas.drawText(indoorTitle, padding, mTitleBaseline, mTitlePaint);
            canvas.drawText(outdoorTitle, indoorEndsAt + padding, mTitleBaseline, mTitlePaint);

            // Indoor temperature
            String tempLabel = resources.getString(R.string.weather_data_view_temp_label);
            drawMetric(canvas, tempLabel, mIndoorTemp, padding);

            // Divider
            canvas.drawLine(
                    indoorEndsAt - padding,
                    padding,
                    indoorEndsAt - padding,
                    getHeight() - 2 * padding,
                    mLabelPaint);

            // Outdoor icon
            mLargeIconPaint.getTextBounds(mOutdoorIcon, 0, mOutdoorIcon.length(), rect);
            canvas.drawText(
                    mOutdoorIcon,
                    indoorEndsAt + ICON_WIDTH/2f - rect.width()/2f,
                    mIconBaseline,
                    mLargeIconPaint);
            canvas.drawText(mOutdoorSummary, indoorEndsAt + padding, mLabelBaseline, mLabelPaint);

            // Outdoor Temperature
            drawMetric(canvas, tempLabel, mOutdoorTemp, indoorEndsAt + iconWidth + padding);

            // Outdoor Feels Like
            drawMetric(
                    canvas,
                    resources.getString(R.string.weather_data_view_feels_label),
                    mOutdoorFeels,
                    indoorEndsAt + iconWidth + valueWidth + padding);

            drawForecast(canvas, indoorEndsAt + iconWidth + 2*valueWidth);
        }

        @Override
        public void sensorsStatusDidUpdate(Sensors.Status status) {
            mIndoorTemp = toDegreeString(status.tmp().temp());
            invalidate();
        }

        @Override
        public void sensorsHourlySummaryDidUpdate(Sensors.HourlySummary summary) {

        }

        @Override
        public void weatherConditionsDidUpdate(Weather.Conditions conditions) {
            mOutdoorTemp = toDegreeString(conditions.temp());
            mOutdoorFeels = toDegreeString(conditions.apparentTemp());
            mOutdoorIcon = toIconString(conditions.icon());
            mOutdoorSummary = conditions.summary().toUpperCase();
            invalidate();

        }

        @Override
        public void weatherForecastDidUpdate(List<Weather.Conditions> forecast) {
            mHourlyForecast = forecast;
            invalidate();
        }
    }

    private static final String[] sIconToStringMap = buildIconToStringMap();

    private ImplView mView;

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
        mView = new ImplView(getContext());
        return mView;
    }

    private static String toPrecipPercentString(double precip) {
        return Integer.toString((int) Math.round(precip * 100f));
    }

    private static String toDegreeString(int tmp) {
        return String.format("%d\u00b0", tmp);
    }

    private static String toDegreeString(double tmp) {
        return toDegreeString((int) Math.round(tmp));
    }

    private static String[] buildIconToStringMap() {
        String[] map = new String[Weather.ICON_LAST_INDEX+1];
        map[Weather.ICON_UNKNOWN] = "`";
        map[Weather.ICON_CLEAR_DAY] = "v";
        map[Weather.ICON_CLEAR_NIGHT] = "/";
        map[Weather.ICON_RAIN] = "6";
        map[Weather.ICON_SNOW] = "o";
        map[Weather.ICON_SLEET] = "3";
        map[Weather.ICON_WIND] = "k";
        map[Weather.ICON_FOG] = "g";
        map[Weather.ICON_CLOUDY] = "`";
        map[Weather.ICON_PARTLY_CLOUDY_DAY] = "1";
        map[Weather.ICON_PARTLY_CLOUDY_NIGHT] = "2";
        map[Weather.ICON_THUNDERSTORM] = "z";
        return map;
    }

    private static String toIconString(int icon) {
        return sIconToStringMap[icon];
    }

    public void setModel(Model model) {
        model.sensors().tap(mView);
        model.weather().tap(mView);
    }
}
