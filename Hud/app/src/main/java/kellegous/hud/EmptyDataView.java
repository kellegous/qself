package kellegous.hud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class EmptyDataView extends DataView {
    public EmptyDataView(Context context) {
        super(context);
    }

    public EmptyDataView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EmptyDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getIconId() {
        return 0;
    }

    @Override
    protected View createView(ViewGroup parent, LayoutInflater inflater) {
        return new View(getContext());
    }
}
