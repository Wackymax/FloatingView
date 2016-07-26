/**
 * Copyright 2015 RECRUIT LIFESTYLE CO., LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.recruit_lifestyle.android.floatingview;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.ArrayList;

public class FloatingViewManager implements ScreenChangedListener, View.OnTouchListener, TrashViewListener {

    public static final int DISPLAY_MODE_SHOW_ALWAYS = 1;
    public static final int DISPLAY_MODE_HIDE_ALWAYS = 2;
    public static final int DISPLAY_MODE_HIDE_FULLSCREEN = 3;
    public static final int MOVE_DIRECTION_DEFAULT = 0;
    public static final int MOVE_DIRECTION_LEFT = 1;
    public static final int MOVE_DIRECTION_RIGHT = 2;
    public static final int MOVE_DIRECTION_NONE = 3;
    private static final long VIBRATE_INTERSECTS_MILLIS = 15;
    public static final float SHAPE_CIRCLE = 1.0f;
    public static final float SHAPE_RECTANGLE = 1.4142f;
    private final Context mContext;
    private final WindowManager mWindowManager;
    private FloatingView mTargetFloatingView;
    private final FullscreenObserverView mFullscreenObserverView;
    private final TrashView mTrashView;
    private final FloatingViewListener mFloatingViewListener;
    private final Rect mFloatingViewRect;
    private final Rect mTrashViewRect;
    private final Vibrator mVibrator;

    private boolean mIsMoveAccept;
    private int mDisplayMode;
    private final ArrayList<FloatingView> mFloatingViewList;

    public FloatingViewManager(Context context, FloatingViewListener listener) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mFloatingViewListener = listener;
        mFloatingViewRect = new Rect();
        mTrashViewRect = new Rect();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mIsMoveAccept = false;
        mDisplayMode = DISPLAY_MODE_HIDE_FULLSCREEN;

        mFloatingViewList = new ArrayList<>();
        mFullscreenObserverView = new FullscreenObserverView(context, this);
        mTrashView = new TrashView(context);
    }

    private boolean isIntersectWithTrash() {

        if (!mTrashView.isTrashEnabled()) {
            return false;
        }

        mTrashView.getWindowDrawingRect(mTrashViewRect);
        mTargetFloatingView.getWindowDrawingRect(mFloatingViewRect);
        return Rect.intersects(mTrashViewRect, mFloatingViewRect);
    }

    @Override
    public void onScreenChanged(boolean isFullscreen) {

        if (mDisplayMode != DISPLAY_MODE_HIDE_FULLSCREEN) {
            return;
        }

        mIsMoveAccept = false;
        final int state = mTargetFloatingView.getState();

        if (state == FloatingView.STATE_NORMAL) {
            final int size = mFloatingViewList.size();
            for (int i = 0; i < size; i++) {
                final FloatingView floatingView = mFloatingViewList.get(i);
                floatingView.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
            }
            mTrashView.dismiss();
        }
        else if (state == FloatingView.STATE_INTERSECTING) {
            mTargetFloatingView.setFinishing();
            mTrashView.dismiss();
        }
    }

    @Override
    public void onTrashAnimationStarted(int animationCode) {

        if (animationCode == TrashView.ANIMATION_CLOSE || animationCode == TrashView.ANIMATION_FORCE_CLOSE) {
            final int size = mFloatingViewList.size();
            for (int i = 0; i < size; i++) {
                final FloatingView floatingView = mFloatingViewList.get(i);
                floatingView.setDraggable(false);
            }
        }
    }

    @Override
    public void onTrashAnimationEnd(int animationCode) {

        final int state = mTargetFloatingView.getState();

        if (state == FloatingView.STATE_FINISHING) {
            removeViewToWindow(mTargetFloatingView);
        }

        final int size = mFloatingViewList.size();
        for (int i = 0; i < size; i++) {
            final FloatingView floatingView = mFloatingViewList.get(i);
            floatingView.setDraggable(true);
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();

        if (action != MotionEvent.ACTION_DOWN && !mIsMoveAccept) {
            return false;
        }

        final int state = mTargetFloatingView.getState();
        mTargetFloatingView = (FloatingView) v;

        if (action == MotionEvent.ACTION_DOWN) {
            mIsMoveAccept = true;
        }
        else if (action == MotionEvent.ACTION_MOVE) {
            final boolean isIntersecting = isIntersectWithTrash();
            final boolean isIntersect = state == FloatingView.STATE_INTERSECTING;

            if (isIntersecting) {
                mTargetFloatingView.setIntersecting((int) mTrashView.getTrashIconCenterX(), (int) mTrashView.getTrashIconCenterY());
            }

            if (isIntersecting && !isIntersect) {
                mVibrator.vibrate(VIBRATE_INTERSECTS_MILLIS);
                mTrashView.setScaleTrashIcon(true);
            }
            else if (!isIntersecting && isIntersect) {
                mTargetFloatingView.setNormal();
                mTrashView.setScaleTrashIcon(false);
            }

        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (state == FloatingView.STATE_INTERSECTING) {

                mTargetFloatingView.setFinishing();
                mTrashView.setScaleTrashIcon(false);
            }
            mIsMoveAccept = false;
        }

        if (state == FloatingView.STATE_INTERSECTING) {
            mTrashView.onTouchFloatingView(event, mFloatingViewRect.left, mFloatingViewRect.top);
        } else {
            final WindowManager.LayoutParams params = mTargetFloatingView.getWindowLayoutParams();
            mTrashView.onTouchFloatingView(event, params.x, params.y);
        }

        return false;
    }

    public void setFixedTrashIconImage(int resId) {
        mTrashView.setFixedTrashIconImage(resId);
    }

    public void setActionTrashIconImage(int resId) {
        mTrashView.setActionTrashIconImage(resId);
    }

    public void setFixedTrashIconImage(Drawable drawable) {
        mTrashView.setFixedTrashIconImage(drawable);
    }

    public void setActionTrashIconImage(Drawable drawable) {
        mTrashView.setActionTrashIconImage(drawable);
    }

    public void setDisplayMode(int displayMode) {
        mDisplayMode = displayMode;

        if (mDisplayMode == DISPLAY_MODE_SHOW_ALWAYS || mDisplayMode == DISPLAY_MODE_HIDE_FULLSCREEN) {
            for (FloatingView floatingView : mFloatingViewList) {
                floatingView.setVisibility(View.VISIBLE);
            }
        }
        else if (mDisplayMode == DISPLAY_MODE_HIDE_ALWAYS) {
            for (FloatingView floatingView : mFloatingViewList) {
                floatingView.setVisibility(View.GONE);
            }
            mTrashView.dismiss();
        }
    }
    public void setTrashViewEnabled(boolean enabled) {
        mTrashView.setTrashEnabled(enabled);
    }

    public boolean isTrashViewEnabled() {
        return mTrashView.isTrashEnabled();
    }

    @Deprecated
    public void addViewToWindow(View view, float shape, int overMargin, String tag) {
        final Options options = new Options();
        options.shape = shape;
        options.overMargin = overMargin;
        addViewToWindow(view, options, tag);
    }

    public void addViewToWindow(View view, Options options, String tag) {

        final boolean isFirstAttach = mFloatingViewList.isEmpty();

        final FloatingView floatingView = new FloatingView(mContext);
        floatingView.setInitCoords(options.floatingViewX, options.floatingViewY);
        floatingView.setOnTouchListener(this);
        floatingView.setShape(options.shape);
        floatingView.setOverMargin(options.overMargin);
        floatingView.setMoveDirection(options.moveDirection);
        floatingView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                floatingView.getViewTreeObserver().removeOnPreDrawListener(this);
                mTrashView.calcActionTrashIconPadding(floatingView.getMeasuredWidth(), floatingView.getMeasuredHeight(), floatingView.getShape());
                return false;
            }
        });
        floatingView.addView(view);

        if (mDisplayMode == DISPLAY_MODE_HIDE_ALWAYS) {
            floatingView.setVisibility(View.GONE);
        }
        mFloatingViewList.add(floatingView);
        mTrashView.setTrashViewListener(this);

        mWindowManager.addView(floatingView, floatingView.getWindowLayoutParams());
        if (isFirstAttach) {
            mWindowManager.addView(mFullscreenObserverView, mFullscreenObserverView.getWindowLayoutParams());
            mTargetFloatingView = floatingView;
        } else {
            mWindowManager.removeViewImmediate(mTrashView);
        }
        mWindowManager.addView(mTrashView, mTrashView.getWindowLayoutParams());
    }

    private void removeViewToWindow(FloatingView floatingView) {
        final int matchIndex = mFloatingViewList.indexOf(floatingView);

        if (matchIndex != -1) {
            mFloatingViewListener.onFinishFloatingView(floatingView.getChildAt(0));
            mWindowManager.removeViewImmediate(floatingView);
            mFloatingViewList.remove(matchIndex);
        }

        if (mFloatingViewList.isEmpty()) {

            if (mFloatingViewListener != null) {
                mFloatingViewListener.onFinishFloatingView();
            }
        }
    }

    public void removeAllViewToWindow() {
        mWindowManager.removeViewImmediate(mFullscreenObserverView);
        mWindowManager.removeViewImmediate(mTrashView);

        final int size = mFloatingViewList.size();
        for (int i = 0; i < size; i++) {
            final FloatingView floatingView = mFloatingViewList.get(i);
            mWindowManager.removeViewImmediate(floatingView);
        }
        mFloatingViewList.clear();
    }

    public static class Options {

        public float shape;
        public int overMargin;
        public int floatingViewX;
        public int floatingViewY;
        public int moveDirection;

        public Options() {
            shape = SHAPE_CIRCLE;
            overMargin = 0;
            floatingViewX = FloatingView.DEFAULT_X;
            floatingViewY = FloatingView.DEFAULT_Y;
            moveDirection = MOVE_DIRECTION_DEFAULT;
        }

    }

}
