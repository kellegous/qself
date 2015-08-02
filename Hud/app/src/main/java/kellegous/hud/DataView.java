package kellegous.hud;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public abstract class DataView extends LinearLayout {
    public DataView(Context context) {
        super(context);
        init(context, context.getResources());
    }

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, context.getResources());
    }

    public DataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, context.getResources());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, context.getResources());
    }

    private void init(Context context, Resources resources) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.data_view_icon_width),
                ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = resources.getDimensionPixelSize(R.dimen.data_view_icon_margin);
        params.setMargins(margin, 0, margin, 0);


        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(params);
        int imgId = getIconId();
        if (imgId != 0) {
            imageView.setImageResource(getIconId());
        }
        addView(imageView);

        View contentView = createView(this, LayoutInflater.from(context));
        contentView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addView(contentView);
    }

    protected abstract int getIconId();

    protected abstract View createView(ViewGroup parent, LayoutInflater inflater);
}
