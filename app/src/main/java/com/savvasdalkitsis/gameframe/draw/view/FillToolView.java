package com.savvasdalkitsis.gameframe.draw.view;

import android.content.Context;
import android.util.AttributeSet;

import com.savvasdalkitsis.gameframe.R;
import com.savvasdalkitsis.gameframe.draw.model.DrawingTool;
import com.savvasdalkitsis.gameframe.draw.model.FillTool;

public class FillToolView extends ToolView {

    private final FillTool fillTool = new FillTool();

    public FillToolView(Context context) {
        super(context);
    }

    public FillToolView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FillToolView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setImageResource(R.drawable.ic_format_color_fill_black_48px);
    }

    @Override
    protected DrawingTool getDrawingTool() {
        return fillTool;
    }
}