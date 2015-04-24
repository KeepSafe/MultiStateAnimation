package com.getkeepsafe.android.multistateanimation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Animates a series of separate AnimationDrawables on the background of a single View.
 * The view to animate is passed in the constructor. Animations are added with addSection, or they
 * can be defined in a JSON resource and passed to fromJsonResource. To start an animation,
 * call transitionNow or queueTransition.
 *
 * @author AJ Alt
 */
public class MultiStateAnimation implements NotifyingAnimationDrawable.OnAnimationFinishedListener {
    private final static String TAG = "MultiStateAnimation";
    public static final int DEFAULT_FRAME_DURATION = 33;
    public static final boolean DEFAULT_ONESHOT_STATUS = true;

    /**
     * A class that creates an AnimationDrawable from a list of frames.
     */
    private static class AnimationDrawableLoader {
        private int mFrameDuration;
        private boolean mIsOneShot;
        private int[] mFrameIds;
        private Context mContext;

        public AnimationDrawableLoader(Context context) {
            mContext = context;
        }

        public AnimationDrawableLoader(Context context, int frameDuration, boolean isOneShot, String[] frameNames) {
            mContext = context;
            mFrameDuration = frameDuration;
            mIsOneShot = isOneShot;

            mFrameIds = new int[frameNames.length];

            for (int i = 0; i < frameNames.length; i++) {
                mFrameIds[i] = mContext.getResources().getIdentifier(frameNames[i], "drawable", mContext.getPackageName());
            }
        }

        public NotifyingAnimationDrawable load() {
            NotifyingAnimationDrawable d = new NotifyingAnimationDrawable();
            d.setOneShot(mIsOneShot);

            // XXX: AnimationDrawable has a bug that causes it to be unresponsive
            // if exactly one frame is added. A workaround is to add the same frame
            // twice if there's only one.
            if (mFrameIds.length == 1) {
                for (int i = 0; i < 2; i++) {
                    d.addFrame(mContext.getResources().getDrawable(mFrameIds[0]), mFrameDuration);
                }
                d.setOneShot(true);
            } else {
                for (int resid : mFrameIds) {
                    d.addFrame(mContext.getResources().getDrawable(resid), mFrameDuration);
                }
            }
            return d;
        }

        /**
         * Returns the duration of this animation.
         *
         * @return int number of milliseconds that the animation will play.
         */
        public int totalDuration() {
            return mFrameDuration * mFrameIds.length;
        }
    }

    /**
     * A class that holds loaders for a single animation section and transitions to that section.
     */
    private static class AnimationSection {
        private String mId;
        private AnimationDrawableLoader mLoader;
        private Map<String, AnimationDrawableLoader> mTransitions;

        public AnimationSection(String id, AnimationDrawableLoader loader, Map<String, AnimationDrawableLoader> transitions) {
            mId = id;
            mLoader = loader;
            mTransitions = transitions;
        }

        /**
         * @param id     The id of this section.
         * @param loader A loader for this section's primary animation.
         */
        public AnimationSection(String id, AnimationDrawableLoader loader) {
            this(id, loader, new HashMap<String, AnimationDrawableLoader>());
        }

        public String getId() {
            return mId;
        }

        /**
         * Creates the primary animation drawable for this section.
         */
        public NotifyingAnimationDrawable loadDrawable() {
            return mLoader.load();
        }

        /**
         * @param fromId The id of the section to transition from.
         * @return The transition animation for fromId if one has been added, or null.
         */
        public NotifyingAnimationDrawable getTransition(String fromId) {
            if (mTransitions.containsKey(fromId)) {
                return mTransitions.get(fromId).load();
            }
            return null;
        }

        /**
         * @param fromId The Id of the section that will be transitioned from.
         * @param loader The loader for this transition animation.
         */
        public void addTransition(String fromId, AnimationDrawableLoader loader) {
            mTransitions.put(fromId, loader);
        }

        /**
         * Calculates the total duration if the animation, including the transition.
         *
         * @param fromId If a transition exists from this id, the duration will include the transition duration.
         * @return int number of milliseconds.
         */
        public int getDuration(String fromId) {
            int total = mLoader.totalDuration();
            AnimationDrawableLoader loader = mTransitions.get(fromId);
            if (loader != null) {
                total += loader.totalDuration();
            }
            return total;
        }

        /**
         * Calculates the duration of the animation, excluding any transition.
         *
         * @return int number of milliseconds.
         */
        public int getDuration() {
            return getDuration(null);
        }
    }


    public interface AnimationSeriesListener {
        /**
         * Called when a playing animation finishes and before the drawable is replaced.
         */
        void onAnimationFinished();

        /**
         * Called after a new animation has been created, but before the animation has started.
         * The new animation can be accessed through getCurrentDrawable.
         */
        void onAnimationStarting();
    }

    private WeakReference<AnimationSeriesListener> mListener = new WeakReference<AnimationSeriesListener>(null);
    private AnimationSection mCurrentSection;

    /**
     * The id of the animation that will be started as soon as the current animation
     * finishes, or null if no animation is queued.
     */
    private String mQueuedSectionId;
    private NotifyingAnimationDrawable mCurrentDrawable;

    /**
     * The id of the previous section if a transition is currently playing, or null
     * id no transition is playing.
     */
    private String mTransitioningFromId;
    private View mView;

    /**
     * An array of resource IDs corresponding to animations that can be played.
     */
    private Map<String, AnimationSection> mSectionsById;

    /**
     * Create a new instance and automatically set animations as the background of the given view.
     *
     * @param view    If not null, animations will be set as the background of this view.
     */
    public MultiStateAnimation(View view) {
        mSectionsById = new HashMap<String, AnimationSection>();
        mView = view;
    }

    /**
     * Create an instance without giving a view to hold the animations.
     * <p/>
     * Note that due to a limitation in AnimationDrawable, you must set
     * created animations as the image or background of a View in an
     * onAnimationStarting listener, otherwise the animation will not
     * advance.
     *
     * @see com.getkeepsafe.android.multistateanimation.MultiStateAnimation.AnimationSeriesListener#onAnimationStarting()
     */
    public MultiStateAnimation() {
        this(null);
    }

    /**
     * Convert a JSONArray containing only strings to a String[].
     *
     * @param jsonArray the array to convert.
     * @return a String[] with the contents of the JSONArray.
     * @throws org.json.JSONException
     */
    private static String[] jsonArrayToArray(JSONArray jsonArray) throws JSONException {
        String[] array = new String[jsonArray.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = jsonArray.getString(i);
        }
        return array;
    }

    /**
     * Creates a new MultiStateAnimation object from a json string.
     * <p/>
     * The document must have the following structure:
     * <pre>
     *  {
     *      "first_section": {
     *          "oneshot": false,
     *          "frame_duration": 33,
     *          "frames": [
     *              "frame_01",
     *              "frame_02"
     *          ],
     *
     *      }
     *      "second_section": {
     *          "oneshot": true,
     *          "frames": [
     *              "other_frame_01"
     *          ],
     *          "transitions_from": {
     *              "first_section": {
     *                  "frame_duration": 33,
     *                  "frames": [
     *                          "first_to_second_transition_001",
     *                          "first_to_second_transition_002"
     *                  ]
     *              }
     *              "": {
     *                  "frames": [
     *                      "nothing_to_second_001",
     *                      "nothing_to_second_002"
     *                  ]
     *              }
     *          }
     *      }
     *  }
     * </pre>
     * The key for each entry is the ID of the state.
     * <dl>
     *     <dt>"oneshot"</dt>
     *     <dd>If false, the animation will play in a loop instead of stopping at the last
     *      frame.</dd>
     *     <dt>"frame_duration"</dt><dd>The number of milliseconds that each frame in the "frame"
     *     list will play. It defaults to 33 (30fps) if not given.</dd>
     *     <dt>"frames"</dt><dd>A list of string resource ID names that must correspond to a
     *     drawable resource.</dd>
     *     <dt>"transitions_from"</dt><dd>Optional, and is a set of animations that play when transitioning to
     *      the current state from another given state. A transition will play when the ID of the
     *      current state matches the transition's key and the state is transitioning to the state
     *      in which the transition is defined.</dd>
     * </dl>
     *
     * @param context The application Context.
     * @param view    If not null, animations will be set as the background of this view.
     * @param resid   The resource ID the the raw json document.
     * @return A new MultiStateAnimation.
     * @throws JSONException
     */
    public static MultiStateAnimation fromJsonResource(Context context, View view, int resid) throws JSONException, IOException {
        // Read the resource into a string
        BufferedReader r = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resid)));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            builder.append(line);
        }

        // Parse
        MultiStateAnimation drawableSeries = new MultiStateAnimation(view);
        JSONObject root = new JSONObject(builder.toString());

        // The root is a an object with keys that are sequence IDs
        for (Iterator<String> iter = root.keys(); iter.hasNext();) {
            String id = iter.next();
            JSONObject obj = root.getJSONObject(id);
            int frameDuration = obj.optInt("frame_duration", DEFAULT_FRAME_DURATION);
            boolean isOneShot = obj.optBoolean("oneshot", DEFAULT_ONESHOT_STATUS);
            JSONArray frames = obj.getJSONArray("frames");
            AnimationDrawableLoader loader = new AnimationDrawableLoader(context, frameDuration, isOneShot, jsonArrayToArray(frames));
            AnimationSection section = new AnimationSection(id, loader);

            JSONObject transitions_from;
            if (obj.has("transitions_from")) {
                transitions_from = obj.getJSONObject("transitions_from");
            } else {
                transitions_from = new JSONObject();
            }

            // The optional "transitions" entry is another list of objects
            for (Iterator<String> transition_iter = transitions_from.keys(); transition_iter.hasNext();) {
                String from = transition_iter.next();

                JSONObject t_obj = transitions_from.getJSONObject(from);
                frameDuration = t_obj.optInt("frame_duration", DEFAULT_FRAME_DURATION);
                frames = t_obj.getJSONArray("frames");
                loader = new AnimationDrawableLoader(context, frameDuration, true, jsonArrayToArray(frames));
                section.addTransition(from, loader);
            }
            drawableSeries.addSection(section);
        }

        return drawableSeries;
    }

    /**
     * Create a MultiStateAnimation from a JSON resource without a connected View.
     *
     * @param context the Application context.
     * @param resid   The resource ID the the raw json document.
     * @return A new MultiStateAnimation instance.
     * @throws JSONException
     * @throws IOException
     */
    public static MultiStateAnimation fromJsonResource(Context context, int resid) throws JSONException, IOException {
        return MultiStateAnimation.fromJsonResource(context, null, resid);
    }

    /**
     * Add an animation section to this series.
     *
     * @param section the section to add.
     */
    private void addSection(AnimationSection section) {
        mSectionsById.put(section.getId(), section);
    }

    /**
     * Returns the registered listener, if one exists.
     */
    public AnimationSeriesListener getSeriesAnimationFinishedListener() {
        return mListener.get();
    }

    /**
     * Registers a listener that will be called when a running animation finishes. If the
     * animation is continuous, the listener will be called every time the last frame of the
     * animation is played.
     *
     * @param listener The listener to register.
     */
    public void setSeriesAnimationFinishedListener(AnimationSeriesListener listener) {
        this.mListener = new WeakReference<AnimationSeriesListener>(listener);
    }

    /**
     * Calculates the total duration of the current animation section, including the transition
     * if applicable. If the the animation is not a oneshot, the total will be for a single loop.
     *
     * @return The total animation duration, or 0 if no animation is playing.
     */
    public int currentSectionDuration() {
        if (mCurrentSection == null) return 0;
        return mCurrentSection.getDuration(mTransitioningFromId);
    }

    /**
     * Returns the currently playing animation. If no animation has played since this object was
     * created or since a call to {@link #clearAnimation()}, null is returned.
     */
    public AnimationDrawable getCurrentDrawable() {
        return mCurrentDrawable;
    }

    /**
     * Return the ID of the current section if one is playing, or null otherwise.
     */
    public String getCurrentSectionId() {
        return mCurrentSection == null ? null : mCurrentSection.getId();
    }

    /**
     * If the currently playing animation is a transition, return the ID of the
     * section that is being transitioned from. Otherwise return null.
     */
    public String getTransitioningFromId() {
        return mTransitioningFromId;
    }

    /**
     * Play an animation drawable.
     *
     * @param drawable The drawable to play.
     */
    @SuppressLint("NewApi")
    private void playDrawable(NotifyingAnimationDrawable drawable) {
        mCurrentDrawable = drawable;
        mCurrentDrawable.setAnimationFinishedListener(this);

        if (mListener.get() != null) {
            mListener.get().onAnimationStarting();
        }

        if (mView != null) {
            if (Build.VERSION.SDK_INT >= 16) {
                mView.setBackground(mCurrentDrawable);
            } else {
                mView.setBackgroundDrawable(mCurrentDrawable);
            }
        }
        mCurrentDrawable.start();
    }

    /**
     * Queues a section to start as soon as the current animation finishes.
     * If no animation is playing, the queued animation will be started immediately.
     * Queueing a transition to the currently playing section has no effect.
     *
     * @param id The name of the section that will be queued.
     */
    public void queueTransition(String id) {
        if (id.equals(getCurrentSectionId())) return;
        if (mCurrentSection == null ||
                mCurrentDrawable != null &&
                mCurrentDrawable.isOneShot() &&
                mCurrentDrawable.isFinished()) {
            transitionNow(id);
        } else {
            mQueuedSectionId = id;
        }
    }

    /**
     * Starts a specific section without waiting for the current animation to finish.
     * If there is a defined transition from the current section to the new one, the
     * transition will be played, followed immediately by the regular section animation.
     * Transitioning to the currently playing section will restart the animation.
     *
     * @param id The name of the section that will be played.
     */
    public void transitionNow(String id) {
        AnimationSection newSection = mSectionsById.get(id);
        if (newSection == null) {
            throw new RuntimeException("transitionNow called with invalid id: " + id);
        }

        // If the section has a transition from the old section, play the
        // transition before the main animation.
        NotifyingAnimationDrawable transition = mCurrentSection == null ?
                newSection.getTransition(""):
                newSection.getTransition(mCurrentSection.getId());
        if (transition != null) {
            mCurrentDrawable = transition;
            mTransitioningFromId = mCurrentSection == null ? "" : mCurrentSection.getId();
        } else {
            mCurrentDrawable = newSection.loadDrawable();
            mTransitioningFromId = null;
        }
        mCurrentSection = newSection;
        mQueuedSectionId = null;

        playDrawable(mCurrentDrawable);
    }

    /**
     * Clear any currently playing animation. This will cause a "" transition to
     * be played before the next queued section, if one was defined for .
     */
    public void clearAnimation() {
        if (mCurrentDrawable != null) {
            mCurrentDrawable.stop();
        }
        if (mView != null) {
            mView.setBackgroundResource(0);
        }
        mCurrentDrawable = null;
        mCurrentSection = null;
        mQueuedSectionId = null;
        mTransitioningFromId = null;
    }

    /**
     * Callback that is run when a playing animation finishes.
     */
    @Override
    public void onAnimationFinished() {
        if (mListener.get() != null) {
            mListener.get().onAnimationFinished();
        }
        if (mTransitioningFromId != null) {
            mTransitioningFromId = null;
            playDrawable(mCurrentSection.loadDrawable());
        } else if (mQueuedSectionId != null) {
            transitionNow(mQueuedSectionId);
        }
    }
}
