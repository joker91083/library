package com.joker.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sp on 2023/5/24.
 */

public class WheelView extends ScrollView {

    public static final String TAG = WheelView.class.getSimpleName();

    public static class OnWheelViewListener {
        public void onSelected(int selectedIndex, String item) {
        }
    }

    private Context context;

    private LinearLayout views;

    private String normalColor = "";
    private String selectedColor = "";
    private String lineColor = "";
    private int normalTextSize = 0;
    private int selectedTextSize = 0;
    private int normalPadding = 0;
    private int selectedPadding = 0;

    public WheelView(Context context) {
        super(context);
        init(context);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    List<String> items;

    private List<String> getItems() {
        return items;
    }

    public void setItems(List<String> list) {
        if (null == items) {
            items = new ArrayList<String>();
        }
        items.clear();
        items.addAll(list);

        // 前面和后面补全
        for (int i = 0; i < offset; i++) {
            items.add(0, "");
            items.add("");
        }
        initData();
    }

    public static final int OFF_SET_DEFAULT = 1;
    int offset = OFF_SET_DEFAULT; // 偏移量（需要在最前面和最后面补全）

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    int displayItemCount; // 每页显示的数量

    int selectedIndex = 1;


    private void init(Context context) {
        this.context = context;
        Log.d(TAG, "parent: " + this.getParent());
        this.setVerticalScrollBarEnabled(false);

        views = new LinearLayout(context);
        views.setOrientation(LinearLayout.VERTICAL);
        this.addView(views);

        scrollerTask = new Runnable() {

            public void run() {
                int newY = getScrollY();
                if (initialY - newY == 0) { // stopped
                    final int remainder = initialY % itemHeight;
                    final int divided = initialY / itemHeight;
                    if (remainder == 0) {
                        selectedIndex = divided + offset;

                        onSelectedCallBack();
                    } else {
                        if (remainder > itemHeight / 2) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    smoothScrollTo(0, initialY - remainder + itemHeight);
                                    selectedIndex = divided + offset + 1;
                                    onSelectedCallBack();
                                }
                            });
                        } else {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    smoothScrollTo(0, initialY - remainder);
                                    selectedIndex = divided + offset;
                                    onSelectedCallBack();
                                }
                            });
                        }
                    }
                } else {
                    initialY = getScrollY();
                    postDelayed(scrollerTask, newCheck);
                }
            }
        };
        // 默认初始不滑动时执行一次回调
        if (null != onWheelViewListener) {
            onWheelViewListener.onSelected(selectedIndex, items.get(selectedIndex));
        }
    }

    int initialY;

    Runnable scrollerTask;
    int newCheck = 50;

    public void startScrollerTask() {

        initialY = getScrollY();
        this.postDelayed(scrollerTask, newCheck);
    }

    private void initData() {
        displayItemCount = offset * 2 + 1;

        views.removeAllViews();
        for (String item : items) {
            views.addView(createView(item));
        }

        refreshItemView(0);
    }

    int itemHeight = 0;

    private TextView createView(String item) {
        TextView tv = new TextView(context);
        tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setSingleLine(true);
        if (selectedTextSize == 0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        } else {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedTextSize);
        }
        tv.setText(item);
        tv.setGravity(Gravity.CENTER);
        int padding = 0;
        if (selectedPadding == 0) {
            padding = dip2px(6);
        } else {
            padding = dip2px(selectedPadding);
        }
        tv.setPadding(padding, padding, padding, padding);
        if (0 == itemHeight) {
            itemHeight = getViewMeasuredHeight(tv);
            Log.d(TAG, "itemHeight: " + itemHeight);
            views.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, itemHeight * displayItemCount, Gravity.CENTER_HORIZONTAL));
            views.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.getLayoutParams();
            this.setLayoutParams(new LinearLayout.LayoutParams(lp.width, itemHeight * displayItemCount));
        }
        return tv;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        refreshItemView(t);

        if (t > oldt) {
            //            Log.d(TAG, "向下滚动");
            scrollDirection = SCROLL_DIRECTION_DOWN;
        } else {
            //            Log.d(TAG, "向上滚动");
            scrollDirection = SCROLL_DIRECTION_UP;
        }
    }

    private void refreshItemView(int y) {
        int position = y / itemHeight + offset;
        int remainder = y % itemHeight;
        int divided = y / itemHeight;

        if (remainder == 0) {
            position = divided + offset;
        } else {
            if (remainder > itemHeight / 2) {
                position = divided + offset + 1;
            }
        }

        int childSize = views.getChildCount();
        for (int i = 0; i < childSize; i++) {
            TextView itemView = (TextView) views.getChildAt(i);
            if (null == itemView) {
                return;
            }
            if (position == i) {
//                itemView.setTextColor(context.getResources().getColor(R.color.black));
                if (selectedColor == null || selectedColor.isEmpty()) {
                    itemView.setTextColor(Color.parseColor("#000000"));
                } else {
                    itemView.setTextColor(Color.parseColor(selectedColor));
                }

                if (selectedTextSize == 0) {
                    if (normalTextSize == 0) {
                        itemView.setTextSize(20);
                    } else {
                        itemView.setTextSize(normalTextSize);
                    }
                } else {
                    itemView.setTextSize(selectedTextSize);
                }

                if (selectedPadding == 0) {
                    if (normalPadding == 0) {
                        itemView.setPadding(dip2px(6), dip2px(6), dip2px(6), dip2px(6));
                    } else {
                        itemView.setPadding(dip2px(normalPadding), dip2px(normalPadding), dip2px(normalPadding), dip2px(normalPadding));
                    }
                } else {
                    itemView.setPadding(dip2px(selectedPadding), dip2px(selectedPadding), dip2px(selectedPadding), dip2px(selectedPadding));
                }
            } else {
//                itemView.setTextColor(context.getResources().getColor(R.color.text_default_color));
                if (normalColor == null || normalColor.isEmpty()) {
                    itemView.setTextColor(Color.parseColor("#4B5B76"));
                } else {
                    itemView.setTextColor(Color.parseColor(normalColor));
                }

                if (normalTextSize == 0) {
                    itemView.setTextSize(20);
                } else {
                    itemView.setTextSize(normalTextSize);
                }

                if (normalPadding == 0) {
                    itemView.setPadding(dip2px(6), dip2px(6), dip2px(6), dip2px(6));
                } else {
                    itemView.setPadding(dip2px(normalPadding), dip2px(normalPadding), dip2px(normalPadding), dip2px(normalPadding));
                }
            }
        }
    }

    /**
     * 获取选中区域的边界
     */
    int[] selectedAreaBorder;

    private int[] obtainSelectedAreaBorder() {
        if (null == selectedAreaBorder) {
            selectedAreaBorder = new int[2];
            selectedAreaBorder[0] = itemHeight * offset;
            selectedAreaBorder[1] = itemHeight * (offset + 1);
        }
        return selectedAreaBorder;
    }


    private int scrollDirection = -1;
    private static final int SCROLL_DIRECTION_UP = 0;
    private static final int SCROLL_DIRECTION_DOWN = 1;

    Paint paint;
    int viewWidth;

    @Override
    public void setBackgroundDrawable(Drawable background) {

        if (viewWidth == 0) {
            //            viewWidth = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
            viewWidth = views.getWidth();
            Log.d(TAG, "viewWidth: " + viewWidth);
        }

        if (null == paint) {
            paint = new Paint();
            if (lineColor == null || lineColor.isEmpty()) {
                paint.setColor(Color.parseColor("#4B5B76"));
            } else {
                paint.setColor(Color.parseColor(lineColor));
            }
            paint.setStrokeWidth(dip2px(1f));
        }

        background = new Drawable() {
            @Override
            public void draw(Canvas canvas) {
//                canvas.drawLine(viewWidth * 1 / 6, obtainSelectedAreaBorder()[0], viewWidth * 5 / 6, obtainSelectedAreaBorder()[0], paint);
                canvas.drawLine(0, obtainSelectedAreaBorder()[0], viewWidth, obtainSelectedAreaBorder()[0], paint);
//                canvas.drawLine(viewWidth * 1 / 6, obtainSelectedAreaBorder()[1], viewWidth * 5 / 6, obtainSelectedAreaBorder()[1], paint);
                canvas.drawLine(0, obtainSelectedAreaBorder()[1], viewWidth, obtainSelectedAreaBorder()[1], paint);
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(ColorFilter cf) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.UNKNOWN;
            }
        };
        super.setBackgroundDrawable(background);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "w: " + w + ", h: " + h + ", oldw: " + oldw + ", oldh: " + oldh);
        viewWidth = w;
        setBackgroundDrawable(null);
    }

    /**
     * 选中回调
     */
    private void onSelectedCallBack() {
        if (null != onWheelViewListener) {
            onWheelViewListener.onSelected(selectedIndex, items.get(selectedIndex));
        }

    }

    public void setSeletion(int position) {
        final int p = position;
        selectedIndex = p + offset;
        this.post(new Runnable() {
            @Override
            public void run() {
                smoothScrollTo(0, p * itemHeight);
            }
        });

    }

    public String getSeletedItem() {
        return items.get(selectedIndex);
    }

    public int getSeletedIndex() {
        return selectedIndex - offset;
    }

    public void setItemColor(String normalColor, String selectedColor) {
        this.normalColor = normalColor;
        this.selectedColor = selectedColor;
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    public void setNormalTextSize(int normalTextSize) {
        this.normalTextSize = normalTextSize;
    }

    public void setSelectedTextSize(int selectedTextSize) {
        this.selectedTextSize = selectedTextSize;
    }

    public void setNormalPadding(int normalPadding) {
        this.normalPadding = normalPadding;
    }

    public void setSelectedPadding(int selectedPadding) {
        this.selectedPadding = selectedPadding;
    }


    @Override
    public void fling(int velocityY) {
        super.fling(velocityY / 3);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {

            startScrollerTask();
        }
        return super.onTouchEvent(ev);
    }

    private OnWheelViewListener onWheelViewListener;

    public OnWheelViewListener getOnWheelViewListener() {
        return onWheelViewListener;
    }

    public void setOnWheelViewListener(OnWheelViewListener onWheelViewListener) {
        this.onWheelViewListener = onWheelViewListener;
        // 默认初始不滑动时执行一次回调
        onWheelViewListener.onSelected(selectedIndex, items.get(selectedIndex));
    }

    private int dip2px(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int getViewMeasuredHeight(View view) {
        int width = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        view.measure(width, expandSpec);
        return view.getMeasuredHeight();
    }
}
