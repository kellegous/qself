package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TidalDataView extends DataView {
    private class ImplView extends View {

        public ImplView(Context context) {
            super(context);
        }

        public ImplView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ImplView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public ImplView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
    }

    public TidalDataView(Context context) {
        super(context);
    }

    public TidalDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TidalDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TidalDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return R.drawable.tidal_stroke;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        return new ImplView(getContext());
    }
}
