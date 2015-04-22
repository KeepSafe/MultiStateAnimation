package com.getkeepsafe.android.drawableanimationseries;

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
public class DrawableAnimationSeries implements NotifyingAnimationDrawable.OnAnimationFinishedListener {
    private final static String TAG = "DrawableAnimationSeries";
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

    private AnimationSeriesListener mListener;
    private AnimationSection mCurrentSection;
    /**
     * The id of the animation that will be started as soon as the current animation
     * finishes, or null if no animation is queued.
     */
    private String mQueuedSectionId;
    private Context mContext;
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
     * @param context the Application context.
     * @param view    If not null, animations will be set as the background of this view.
     */
    public DrawableAnimationSeries(Context context, View view) {
        mContext = context;
        mSectionsById = new HashMap<String, AnimationSection>();
        mView = view;
    }

    /**
     * @param context The application Context.
     */
    public DrawableAnimationSeries(Context context) {
        this(context, null);
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
     * Creates a new DrawableAnimationSeries object from a json string.
     * The document must have the following structure:
     * <pre>
     * {
     *    "section_name": {
     *        "oneshot": false,
     *        "frame_duration": 33,
     *        "frames": [
     *            "frame_01",
     *            "frame_02"
     *        ],
     *        "transitions_from": {
     *            "other_section_id": {
     *                "frame_duration": 33,
     *                "frames": [
     *                    "spinner_intro_001",
     *                    "spinner_intro_002"
     *                ]
     *            }
     *        }
     *    }
     *    "other_section_id": {
     *        "oneshot": true,
     *        "frames": [
     *          "other_frame_01"
     *        ]
     *    }
     * }
     * </pre>
     * If "oneshot" is false, the animation will play in a loop instead of stopping at the last
     * frame.
     * "frame_duration" is the number of milliseconds that each frame in the "frame" list will play.
     * It defaults to 33 if not given.
     * "frames" is a list of string resource ID names that must correspond to a drawable resource.
     * "transitions_from" is optional, and is a list of animations that play when transitioning to the
     * current state from another given state.
     *
     * @param context The application Context.
     * @param view    If not null, animations will be set as the background of this view.
     * @param resid   The resource ID the the raw json document.
     * @return A new DrawableAnimationSeries.
     * @throws JSONException
     */
    public static DrawableAnimationSeries fromJsonResource(Context context, View view, int resid) throws JSONException, IOException {
        // Read the resource into a string
        BufferedReader r = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resid)));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            builder.append(line);
        }

        // Parse
        DrawableAnimationSeries drawableSeries = new DrawableAnimationSeries(context, view);
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
     * Create a DrawableAnimationSeries from a JSON resource without a connected View.
     *
     * @param context the Application context.
     * @param resid   The resource ID the the raw json document.
     * @return A new DrawableAnimationSeries instance.
     * @throws JSONException
     * @throws IOException
     */
    public static DrawableAnimationSeries fromJsonResource(Context context, int resid) throws JSONException, IOException {
        return DrawableAnimationSeries.fromJsonResource(context, null, resid);
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
     * Returns the registered listener.
     */
    public AnimationSeriesListener getSeriesAnimationFinishedListener() {
        return mListener;
    }

    /**
     * Registers a listener that will be called when a running animation finishes. If the
     * animation is continuous, the listener will be called every time the last frame of the
     * animation is played.
     *
     * @param listener The listener to register.
     */
    public void setSeriesAnimationFinishedListener(AnimationSeriesListener listener) {
        this.mListener = listener;
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
     * Returns the currently playing animation, or null if no animation has ever played.
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
     * If the currently playing animation is a transition, return the ID if the
     * section that this is transitioning from. Otherwise return null.
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

        if (mListener != null) {
            mListener.onAnimationStarting();
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
     * If no animation is playing, the queued animation will be started immediately
     * if it is not the current animation.
     */
    public void queueTransition(String id) {
        if (mCurrentSection == null ||
                !getCurrentSectionId().equals(id) &&
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
     * If the last registered animation is currently playing, or no animations have been
     * registered, no action is taken.
     */
    public void transitionNow(String id) {
        AnimationSection newSection = mSectionsById.get(id);
        if (newSection == null) {
            throw new RuntimeException("transitionNow called with invalid id: " + id);
        }

        // If the section has a transition from the old section, play the
        // transition before the main animation.
        NotifyingAnimationDrawable transition = mCurrentSection == null ?
                null : newSection.getTransition(mCurrentSection.getId());
        if (transition != null) {
            mCurrentDrawable = transition;
            mTransitioningFromId = mCurrentSection.getId();
        } else {
            mCurrentDrawable = newSection.loadDrawable();
            mTransitioningFromId = null;
        }
        mCurrentSection = newSection;
        mQueuedSectionId = null;

        playDrawable(mCurrentDrawable);
    }

    /**
     * Calls the listener callback if one was registered and transitions to the next state.
     */
    @Override
    public void onAnimationFinished() {
        if (mListener != null) {
            mListener.onAnimationFinished();
        }
        if (mTransitioningFromId != null) {
            mTransitioningFromId = null;
            playDrawable(mCurrentSection.loadDrawable());
        } else if (mQueuedSectionId != null) {
            transitionNow(mQueuedSectionId);
        }
    }
}
