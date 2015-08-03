package com.lorentzos.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinausaurs might appear!
 */


public abstract class FlingCardListener implements View.OnTouchListener {
    private static final String TAG = FlingCardListener.class.getSimpleName();
    private static final Object LOCK = new Object();

    private final int INVALID_POINTER_ID = -1;
    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private final int SELECT_ITEM_DURATION = 150;

    private final float frameX;
    private final float frameY;
    private final int frameHeight;
    private final int frameWidth;
    private final int parentWidth;
    private final float frameHalfWidth;

    private float BASE_ROTATION_DEGREES;
    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;
    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private View mFrame = null;
    private int touchPosition;
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));

    public FlingCardListener(ViewGroup parent, View view) {
        this(parent, view, 15.f);
    }

    public FlingCardListener(ViewGroup parent, View frame, float rotationDegree) {
        this.mFrame = frame;
        this.frameX = frame.getX();
        this.frameY = frame.getY();
        this.frameHeight = frame.getHeight();
        this.frameWidth = frame.getWidth();
        this.frameHalfWidth = frameWidth / 2f;
        this.parentWidth = parent.getWidth();
        this.BASE_ROTATION_DEGREES = rotationDegree;
    }

    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer

                mActivePointerId = event.getPointerId(0);
                float x = 0;
                float y = 0;
                boolean success = false;
                try {
                    x = event.getX(mActivePointerId);
                    y = event.getY(mActivePointerId);
                    success = true;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Exception in onTouch(view, event) : " + mActivePointerId, e);
                }
                if (success) {
                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;
                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier mFrame
                    if (aPosX == 0) {
                        aPosX = mFrame.getX();
                    }
                    if (aPosY == 0) {
                        aPosY = mFrame.getY();
                    }

                    if (y < frameHeight / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }
                }

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                resetCardViewOnStack();
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                // Find the index of the active pointer and fetch its position
                final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;

                // Move the mFrame
                aPosX += dx;
                aPosY += dy;

                // calculate the rotation degrees
                float distobjectX = aPosX - frameX;
                float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                //in this area would be code for doing something with the view as the mFrame moves.
                mFrame.setX(aPosX);
                mFrame.setY(aPosY);
                mFrame.setRotation(rotation);
                onFlingTopView(getScrollProgressPercent());
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }
        }

        return true;
    }

    private float getScrollProgressPercent() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosX + frameHalfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private boolean resetCardViewOnStack() {
        if (movedBeyondLeftBorder()) {
            // Left Swipe
            onSelected(true, getExitPoint(-frameWidth), SELECT_ITEM_DURATION);
            onFlingTopView(-1.0f);
        } else if (movedBeyondRightBorder()) {
            // Right Swipe
            onSelected(false, getExitPoint(parentWidth), SELECT_ITEM_DURATION);
            onFlingTopView(1.0f);
        } else {
            float absMoveDistance = Math.abs(aPosX - frameX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            mFrame.animate()
                    .setDuration(SELECT_ITEM_DURATION)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(frameX)
                    .y(frameY)
                    .rotation(0);
            onFlingTopView(0.0f);
            if (absMoveDistance < 4.0) {
                onClickTopView(mFrame);
            }
        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return aPosX + frameHalfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + frameHalfWidth > rightBorder();
    }

    public float leftBorder() {
        return parentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * parentWidth / 4.f;
    }


    public void onSelected(final boolean isLeft, float exitY, long duration) {
        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -frameWidth - getRotationWidthOffset();
        } else {
            exitX = parentWidth + getRotationWidthOffset();
        }

        this.mFrame.animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isLeft) {
                            onExited();
                            onExitToLeft(mFrame);
                        } else {
                            onExited();
                            onExitToRight(mFrame);
                        }
                        isAnimationRunning = false;
                    }
                })
                .rotation(getExitRotation(isLeft));
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning) {
            onSelected(true, frameY, SELECT_ITEM_DURATION);
        }
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight() {
        if (!isAnimationRunning) {
            onSelected(false, frameY, SELECT_ITEM_DURATION);
        }
    }


    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = frameX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = frameY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical y = ax+b linear regression
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (parentWidth - frameX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }


    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p/>
     * The below method calculates the width offset of the rotation.
     */
    private float getRotationWidthOffset() {
        return frameWidth / MAX_COS - frameWidth;
    }


    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.aPosX, this.aPosY);
    }

    /**
     *
     */
    abstract void onExited();

    /**
     *
     * @param view
     */
    abstract void onExitToLeft(View view);

    /**
     *
     * @param view
     */
    abstract void onExitToRight(View view);

    /**
     *
     * @param view
     */
    abstract void onClickTopView(View view);

    /**
     *
     * @param offset
     */
    abstract void onFlingTopView(float offset);

}





