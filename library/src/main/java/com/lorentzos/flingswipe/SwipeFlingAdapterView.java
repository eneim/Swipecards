package com.lorentzos.flingswipe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinosaurs might appear!
 */

public class SwipeFlingAdapterView extends BaseFlingAdapterView {

    private int MAX_VISIBLE = 4;
    private int MIN_ADAPTER_STACK = 6;
    private float ROTATION_DEGREES = 15.f;

    private Adapter mAdapter;
    private int LAST_OBJECT_IN_STACK = 0;
    private OnSwipeListener mFlingListener;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean isInLayout = false;
    private View mTopView = null;
    private OnItemClickListener mOnItemClickListener;
    private FlingCardListener flingCardListener;
    private PointF mLastTouchPoint;

    public SwipeFlingAdapterView(Context context) {
        this(context, null);
    }

    public SwipeFlingAdapterView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.SwipeFlingStyle);
    }

    public SwipeFlingAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingAdapterView, defStyle, 0);
        MAX_VISIBLE = a.getInt(R.styleable.SwipeFlingAdapterView_max_visible, MAX_VISIBLE);
        MIN_ADAPTER_STACK = a.getInt(R.styleable.SwipeFlingAdapterView_min_adapter_stack, MIN_ADAPTER_STACK);
        ROTATION_DEGREES = a.getFloat(R.styleable.SwipeFlingAdapterView_rotation_degrees, ROTATION_DEGREES);
        a.recycle();
    }

    /**
     * A shortcut method to set both the listeners and the adapter.
     *
     * @param context  The activity context which extends onFlingListener, OnItemClickListener or both
     * @param mAdapter The adapter you have to set.
     */
    public void init(final Context context, Adapter mAdapter) {
        if (context instanceof OnSwipeListener) {
            mFlingListener = (OnSwipeListener) context;
        } else {
            throw new RuntimeException("Activity does not implement SwipeFlingAdapterView.onFlingListener");
        }
        if (context instanceof OnItemClickListener) {
            mOnItemClickListener = (OnItemClickListener) context;
        }
        setAdapter(mAdapter);
    }

    @Override
    public View getTopView() {
        return mTopView;
    }

    @Override
    public void requestLayout() {
        if (!isInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // if we don't have an adapter, we don't need to do anything
        if (mAdapter == null) {
            return;
        }

        isInLayout = true;
        final int adapterCount = mAdapter.getCount();

        if (adapterCount == 0) {
            removeAllViewsInLayout();
        } else {
            View topCard = getChildAt(LAST_OBJECT_IN_STACK);
            if (mTopView != null && topCard != null && topCard == mTopView) {
                if (this.flingCardListener.isTouching()) {
                    PointF lastPoint = this.flingCardListener.getLastPoint();
                    if (this.mLastTouchPoint == null || !this.mLastTouchPoint.equals(lastPoint)) {
                        this.mLastTouchPoint = lastPoint;
                        removeViewsInLayout(0, LAST_OBJECT_IN_STACK);
                        layoutChildren(1, adapterCount);
                    }
                }
            } else {
                // Reset the UI and set top view listener
                removeAllViewsInLayout();
                layoutChildren(0, adapterCount);
                setTopView();
            }
        }

        isInLayout = false;

        if (adapterCount <= MIN_ADAPTER_STACK) mFlingListener.onAdapterAboutToEmpty(adapterCount);
    }


    private void layoutChildren(int startingIndex, int adapterCount) {
        int maxViewIndex = Math.min(adapterCount, MAX_VISIBLE) - 1;
        while (startingIndex <= maxViewIndex) {
            View newUnderChild = mAdapter.getView(startingIndex, null, this);
            if (newUnderChild.getVisibility() != GONE) {
                makeAndAddView(newUnderChild);

                // custom view scale and transition
                float scaleFactor = startingIndex < maxViewIndex ? 1 - 0.05f * startingIndex : 1
                        - 0.05f * (startingIndex - 1);
                ViewCompat.setScaleX(newUnderChild, scaleFactor);
                ViewCompat.setScaleY(newUnderChild, scaleFactor);

                if (startingIndex < maxViewIndex)
                    ViewCompat.setTranslationY(newUnderChild, -dpToPx(12) * startingIndex);
                else {
                    ViewCompat.setTranslationY(newUnderChild, 0);
                }

                LAST_OBJECT_IN_STACK = startingIndex;
            }
            startingIndex++;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void makeAndAddView(View child) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
        addViewInLayout(child, 0, lp, true);

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }

        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = Gravity.TOP | Gravity.START;
        }

        int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                        lp.leftMargin - lp.rightMargin;
                break;
            case Gravity.END:
                childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
                break;
            case Gravity.START:
            default:
                childLeft = getPaddingLeft() + lp.leftMargin;
                break;
        }
        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
                break;
            case Gravity.TOP:
            default:
                childTop = getPaddingTop() + lp.topMargin;
                break;
        }

        child.layout(childLeft, childTop, childLeft + w, childTop + h);
    }


    /**
     * Set the top view and add the fling listener
     */
    private void setTopView() {
        if (getChildCount() > 0) {
            mTopView = getChildAt(LAST_OBJECT_IN_STACK);
            if (mTopView != null) {
                flingCardListener = new FlingCardListener(this, mTopView, ROTATION_DEGREES) {
                    @Override
                    void onExited() {
                        mTopView = null;
                        mFlingListener.onTopExited();
                    }

                    @Override
                    void onExitToLeft(View view) {
                        mFlingListener.onExitToLeft(view);
                    }

                    @Override
                    void onExitToRight(View view) {
                        mFlingListener.onExitToRight(view);
                    }

                    @Override
                    void onClickTopView(View view) {
                        if (mOnItemClickListener != null)
                            mOnItemClickListener.onItemClick(SwipeFlingAdapterView.this, view, 0,
                                    getItemIdAtPosition(0));
                    }

                    @Override
                    void onFlingTopView(float offset) {
                        mFlingListener.onFlingTopView(offset);
                        onTopViewFling(Math.abs(offset));
                    }
                };

                mTopView.setOnTouchListener(flingCardListener);
            }
        }
    }

    private void onTopViewFling(float scrollProgressPercent) {
        int topIndex = LAST_OBJECT_IN_STACK;
        for (int i = 1; i < topIndex + 1; i++) {
            int viewIndex = topIndex - i;
            View child = getChildAt(viewIndex);
            if (child == null)
                continue;
            float minScaleFactor = 1 - 0.05f * i;
            float maxScaleFactor = 1 - 0.05f * (i - 1);
            float scaleFactor = minScaleFactor * (1 - scrollProgressPercent) + maxScaleFactor *
                    scrollProgressPercent;

            float fromTransY = i < topIndex ? -dpToPx(12) * i : 0.0f;
            float toTransY = -dpToPx(12) * (i - 1);
            float transY = fromTransY * (1 - scrollProgressPercent) + toTransY *
                    scrollProgressPercent;
            if (i < topIndex) {
                ViewCompat.setScaleX(child, scaleFactor);
                ViewCompat.setScaleY(child, scaleFactor);
            }

            ViewCompat.setTranslationY(child, transY);
        }
    }

    public FlingCardListener getTopCardListener() throws NullPointerException {
        if (flingCardListener == null) {
            throw new NullPointerException();
        }
        return flingCardListener;
    }

    public void setMaxVisible(int MAX_VISIBLE) {
        this.MAX_VISIBLE = MAX_VISIBLE;
    }

    public void setMinStackInAdapter(int MIN_ADAPTER_STACK) {
        this.MIN_ADAPTER_STACK = MIN_ADAPTER_STACK;
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    public void setFlingListener(OnSwipeListener onSwipeListener) {
        this.mFlingListener = onSwipeListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }


    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(getContext(), attrs);
    }

    float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }


//    public interface OnItemClickListener {
//        void onItemClicked(int itemPosition, View view);
//    }

    public interface OnSwipeListener {

        void onTopExited();

        void onExitToLeft(View dataObject);

        void onExitToRight(View dataObject);

        void onAdapterAboutToEmpty(int itemsInAdapter);

        void onFlingTopView(float offset);
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            requestLayout();
        }

    }

}
