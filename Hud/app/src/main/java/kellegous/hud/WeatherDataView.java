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

import kellegous.hud.kellegous.hud.api.Weather;

public class WeatherDataView extends DataView {
    private static class ImplView extends View {
        private static final int VALUE_COLOR = 0xff666666;
        private static final float VALUE_TEXT_SIZE = 96f;

        private static final int LABEL_COLOR = 0xff999999;
        private static final float LABEL_TEXT_SIZE = 18f;
        private static final int LABEL_MARGIN_TOP = 8;

        private static final int TITLE_COLOR = 0xff999999;
        private static final float TITLE_TEXT_SIZE = 18f;

        private static final int ICON_COLOR = 0xff999999;
        private static final float ICON_LARGE_TEXT_SIZE = 96f;
        private static final float ICON_SMALL_TEXT_SIZE = 48f;

        private static final int FORECAST_TEMP_COLOR = 0xff999999;
        private static final float FORECAST_TEMP_TEXT_SIZE = 24f;
        private static final int FORECAST_PADDING = 8;

        private static final int VIEW_PADDING = 16;
        private static final int VALUE_WIDTH = 168;
        private static final int ICON_WIDTH = VALUE_WIDTH;

        private static final int FORECAST_NUMBER_OF_HOURS = 8;

        private static final boolean DEBUG = false;

        private String mIndoorTemp = "";
        private String mOutdoorTemp = "";
        private String mOutdoorFeels = "";
        private String mOutdoorIcon = toIconString("clear-day");
        private String mOutdoorSummary = "";
        private List<Weather.Conditions> mHourlyForecast = new ArrayList<>();

        private Paint mValuePaint;
        private Paint mLabelPaint;
        private Paint mTitlePaint;
        private Paint mLargeIconPaint;
        private Paint mSmallIconPaint;
        private Paint mForecastTempPaint;

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

            Paint.FontMetrics iconMetrics = mLargeIconPaint.getFontMetrics();
            mIconBaseline = mValueBaseline - iconMetrics.descent/2;

            Paint.FontMetrics labelMetrics = mLabelPaint.getFontMetrics();
            mLabelBaseline = mValueBaseline - labelMetrics.top + Dimens.dpToPx(resources, LABEL_MARGIN_TOP);

            mValuePaint.getTextBounds("7", 0, 1, rect);
            int valueHeight = rect.height();
            mForecastTempPaint.getTextBounds("7", 0, 1, rect);
            mForecastTempBaseline = mValueBaseline - valueHeight + rect.height();
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

                String icon = toIconString(conditions.icon());
                mSmallIconPaint.getTextBounds(icon, 0, icon.length(), rect);
                canvas.drawText(
                        icon,
                        xa + (dx - padding)/2f - rect.width()/2f,
                        mIconBaseline + Dimens.dpToPx(resources, 4),
                        mSmallIconPaint);

                String temp = toDegreeString(conditions.temp());
                mLabelPaint.getTextBounds(temp, 0, temp.length(), rect);
                canvas.drawText(
                        temp,
                        xa + (dx - padding)/2f - rect.width()/2f,
                        mForecastTempBaseline,
                        mForecastTempPaint);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (DEBUG) {
                canvas.drawLine(0, mTitleBaseline, getWidth(), mTitleBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mValueBaseline, getWidth(), mValueBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mLabelBaseline, getWidth(), mLabelBaseline, mDebugStrokePaint);
                canvas.drawLine(0, mForecastTempBaseline, getWidth(), mForecastTempBaseline, mDebugStrokePaint);
            }

            Resources resources = getResources();
            Rect rect = new Rect();
            float padding = Dimens.dpToPx(resources, VIEW_PADDING);

            float indoorEndsAt = 2*padding + Dimens.dpToPx(resources, VALUE_WIDTH);

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
                    indoorEndsAt,
                    padding,
                    indoorEndsAt,
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
            drawMetric(canvas, tempLabel, mOutdoorTemp, indoorEndsAt + ICON_WIDTH + padding);

            // Outdoor Feels Like
            drawMetric(
                    canvas,
                    resources.getString(R.string.weather_data_view_feels_label),
                    mOutdoorFeels,
                    indoorEndsAt + ICON_WIDTH + VALUE_WIDTH + padding);

            drawForecast(canvas, indoorEndsAt + ICON_WIDTH + 2*VALUE_WIDTH);
        }
    }

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

    private static String toDegreeString(int tmp) {
        return String.format("%d\u00b0", tmp);
    }

    private static String toDegreeString(double tmp) {
        return toDegreeString((int) Math.round(tmp));
    }

    private static String toIconString(String name) {
        if ("clear-day".equals(name)) {
            return "v";
        } else if ("clear-night".equals(name)) {
            return "/";
        } else if ("rain".equals(name)) {
            return "6";
        } else if ("snow".equals(name)) {
            return "o";
        } else if ("sleet".equals(name)) {
            return "3";
        } else if ("wind".equals(name)) {
            return "k";
        } else if ("fog".equals(name)) {
            return "g";
        } else if ("cloudy".equals(name)) {
            return "`";
        } else if ("partly-cloudy-day".equals(name)) {
            return "1";
        } else if ("partly-cloudy-night".equals(name)) {
            return "2";
        } else if ("thunderstorm".equals(name)) {
            return "z";
        } else {
            return "`";
        }
    }

    public void setIndoorTemperature(double temp) {
        mView.mIndoorTemp = toDegreeString(temp);
        mView.invalidate();
    }

    public void setCurrentOutdoorConditions(Weather.Conditions conditions) {
        mView.mOutdoorTemp = toDegreeString(conditions.temp());
        mView.mOutdoorFeels = toDegreeString(conditions.apparentTemp());
        mView.mOutdoorIcon = toIconString(conditions.icon());
        mView.mOutdoorSummary = conditions.summary().toUpperCase();
        mView.invalidate();
    }

    public void setHourlyForecast(List<Weather.Conditions> forecast) {
        mView.mHourlyForecast = forecast;
        mView.invalidate();
    }
}
