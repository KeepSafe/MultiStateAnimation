package com.getkeepsafe.android.drawableanimationseries;

import android.graphics.drawable.AnimationDrawable;

/**
 * Extends AnimationDrawable to signal an event when the animation finishes.
 * This class behaves identically to a normal AnimationDrawable, but contains a method for
 * registering a callback that is called whenever the final frame of the animation is played.
 * If the animation is continuous, the callback will be called repeatedly while the animation
 * is running.
 *
 * @author AJ Alt
 */
public class NotifyingAnimationDrawable extends AnimationDrawable {
    public interface OnAnimationFinishedListener {
        void onAnimationFinished();
    }

    private boolean mFinished = false;
    private OnAnimationFinishedListener mListener;

    /**
     * @param drawable The frames data from animation will be copied into this instance. The animation object will be unchanged.
     */
    public NotifyingAnimationDrawable(AnimationDrawable drawable) {
        for (int i = 0; i < drawable.getNumberOfFrames(); i++) {
            addFrame(drawable.getFrame(i), drawable.getDuration(i));
        }
        setOneShot(drawable.isOneShot());
    }

    public NotifyingAnimationDrawable() {
        super();
    }

    /**
     * @return The registered animation listener.
     */
    public OnAnimationFinishedListener getAnimationFinishedListener() {
        return mListener;
    }

    /**
     * Sets a listener that will be called when the last frame of the animation is rendered.
     * If the animation is continuous, the listener will be called repeatedly while the animation
     * is running.
     *
     * @param listener The listener to register.
     */
    public void setAnimationFinishedListener(OnAnimationFinishedListener listener) {
        this.mListener = listener;
    }

    /**
     * Indicates whether the animation has ever finished.
     */
    public boolean isFinished() {
        return mFinished;
    }

    @Override
    public boolean selectDrawable(int idx) {
        boolean result = super.selectDrawable(idx);

        if (idx != 0 && idx == getNumberOfFrames() - 1) {
            if (!mFinished || !isOneShot()) {
                mFinished = true;
                if (mListener != null) {
                    mListener.onAnimationFinished();
                }
            }
        }

        return result;
    }
}