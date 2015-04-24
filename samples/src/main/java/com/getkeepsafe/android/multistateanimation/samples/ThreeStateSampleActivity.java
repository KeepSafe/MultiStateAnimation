package com.getkeepsafe.android.multistateanimation.samples;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.getkeepsafe.android.multistateanimation.MultiStateAnimation;

import org.json.JSONException;

import java.io.IOException;


public class ThreeStateSampleActivity extends Activity implements MultiStateAnimation.AnimationSeriesListener {
    private MultiStateAnimation mAnimationSeries;
    private TextView mCurrentStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_three_state_sample);

        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        ImageView animationView = (ImageView) findViewById(R.id.animationImageView);
        try {
            mAnimationSeries = MultiStateAnimation.fromJsonResource(animationView.getContext(), animationView, R.raw.sample_animation);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid sync animation JSON file format.");
        } catch (IOException e) {
            throw new RuntimeException("Cannot Read JSON sync animation Resource");
        }

        mAnimationSeries.setSeriesAnimationFinishedListener(this);
        mCurrentStateTextView.setText("Not started");
    }

    public void onNextStateBtnClick(View view) {
        if (mAnimationSeries.getCurrentSectionId() == null) {
            mAnimationSeries.transitionNow("pending");
            return;
        }
        switch (mAnimationSeries.getCurrentSectionId()) {
            case "pending":
                mAnimationSeries.queueTransition("loading");
                break;
            case "loading":
                mAnimationSeries.queueTransition("finished");
                break;
            case "finished":
                mAnimationSeries.queueTransition("pending");
                break;
        }
    }

    @Override
    public void onAnimationFinished() {
        if (mAnimationSeries.getCurrentDrawable().isOneShot()) {
            mCurrentStateTextView.setText("Finished playing: " + mAnimationSeries.getCurrentSectionId());
        }
    }

    @Override
    public void onAnimationStarting() {
        if (mAnimationSeries.getTransitioningFromId() != null) {
            mCurrentStateTextView.setText("Transitioning to: " + mAnimationSeries.getCurrentSectionId());
        } else if (!mAnimationSeries.getCurrentDrawable().isOneShot()) {
            mCurrentStateTextView.setText("Current state: " + mAnimationSeries.getCurrentSectionId());
        }
    }
}
