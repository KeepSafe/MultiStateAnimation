package com.getkeepsafe.android.multistateanimation.samples;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.getkeepsafe.android.multistateanimation.MultiStateAnimation;


public class ThreeStateSampleActivity extends Activity implements MultiStateAnimation.AnimationSeriesListener {
    private MultiStateAnimation mAnimation1;
    private MultiStateAnimation mAnimation2;
    private TextView mCurrentStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_three_state_sample);
        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        mCurrentStateTextView.setText("Not started");

        ImageView animationView1 = (ImageView) findViewById(R.id.animationImageView1);
        mAnimation1 = MultiStateAnimation.fromJsonResource(this, animationView1, R.raw.sample_animation);
        mAnimation1.setSeriesAnimationFinishedListener(this);

        ImageView animationView2 = (ImageView) findViewById(R.id.animationImageView2);
        mAnimation2 = makeAnimation2(animationView2);
        mAnimation2.setSeriesAnimationFinishedListener(this);

        mAnimation1.transitionNow("pending");
        mAnimation2.transitionNow("pending");
    }

    public void onNextStateBtnClick(View view) {
        if (mAnimation1.getCurrentSectionId() == null) {
            mAnimation1.transitionNow("pending");
            mAnimation2.transitionNow("pending");
            return;
        }
        switch (mAnimation1.getCurrentSectionId()) {
            case "pending":
                mAnimation1.queueTransition("loading");
                mAnimation2.queueTransition("loading");
                break;
            case "loading":
                mAnimation1.queueTransition("finished");
                mAnimation2.queueTransition("finished");
                break;
            case "finished":
                mAnimation1.queueTransition("pending");
                mAnimation2.queueTransition("pending");
                break;
        }
    }

    @Override
    public void onAnimationFinished() {
        if (mAnimation1.getCurrentDrawable().isOneShot()) {
            mCurrentStateTextView.setText("Showing: " + mAnimation1.getCurrentSectionId());
        }
    }

    @Override
    public void onAnimationStarting() {
        if (mAnimation1.getTransitioningFromId() != null) {
            mCurrentStateTextView.setText("Transitioning to: " + mAnimation1.getCurrentSectionId());
        } else if (!mAnimation1.getCurrentDrawable().isOneShot()) {
            mCurrentStateTextView.setText("Current state: " + mAnimation1.getCurrentSectionId());
        }
    }

    private MultiStateAnimation makeAnimation2(View view) {
        MultiStateAnimation.SectionBuilder startSection = new MultiStateAnimation.SectionBuilder("pending")
                .setOneshot(true)
                .addFrame(R.drawable.pending_animation_000);

        MultiStateAnimation.TransitionBuilder endTransition = new MultiStateAnimation.TransitionBuilder()
                .setFrameDuration(33)
                .addFrame(R.drawable.pending_animation_091)
                .addFrame(R.drawable.pending_animation_092)
                .addFrame(R.drawable.pending_animation_093)
                .addFrame(R.drawable.pending_animation_094)
                .addFrame(R.drawable.pending_animation_095)
                .addFrame(R.drawable.pending_animation_096)
                .addFrame(R.drawable.pending_animation_097)
                .addFrame(R.drawable.pending_animation_098);

        MultiStateAnimation.SectionBuilder endSection = new MultiStateAnimation.SectionBuilder("finished")
                .setOneshot(true)
                .addTransition("working", endTransition)
                .addFrame(R.drawable.pending_animation_099);

        MultiStateAnimation.SectionBuilder loadingSection = new MultiStateAnimation.SectionBuilder("loading")
                .setOneshot(false)
                .setFrameDuration(33)
                .addFrame(R.drawable.pending_animation_001)
                .addFrame(R.drawable.pending_animation_002)
                .addFrame(R.drawable.pending_animation_003)
                .addFrame(R.drawable.pending_animation_004)
                .addFrame(R.drawable.pending_animation_005)
                .addFrame(R.drawable.pending_animation_006)
                .addFrame(R.drawable.pending_animation_007)
                .addFrame(R.drawable.pending_animation_008)
                .addFrame(R.drawable.pending_animation_009)
                .addFrame(R.drawable.pending_animation_010)
                .addFrame(R.drawable.pending_animation_011)
                .addFrame(R.drawable.pending_animation_012)
                .addFrame(R.drawable.pending_animation_013)
                .addFrame(R.drawable.pending_animation_014)
                .addFrame(R.drawable.pending_animation_015)
                .addFrame(R.drawable.pending_animation_016)
                .addFrame(R.drawable.pending_animation_017)
                .addFrame(R.drawable.pending_animation_018)
                .addFrame(R.drawable.pending_animation_019)
                .addFrame(R.drawable.pending_animation_020)
                .addFrame(R.drawable.pending_animation_021)
                .addFrame(R.drawable.pending_animation_022)
                .addFrame(R.drawable.pending_animation_023)
                .addFrame(R.drawable.pending_animation_024)
                .addFrame(R.drawable.pending_animation_025)
                .addFrame(R.drawable.pending_animation_026)
                .addFrame(R.drawable.pending_animation_027)
                .addFrame(R.drawable.pending_animation_028)
                .addFrame(R.drawable.pending_animation_029)
                .addFrame(R.drawable.pending_animation_030)
                .addFrame(R.drawable.pending_animation_031)
                .addFrame(R.drawable.pending_animation_032)
                .addFrame(R.drawable.pending_animation_033)
                .addFrame(R.drawable.pending_animation_034)
                .addFrame(R.drawable.pending_animation_035)
                .addFrame(R.drawable.pending_animation_036)
                .addFrame(R.drawable.pending_animation_037)
                .addFrame(R.drawable.pending_animation_038)
                .addFrame(R.drawable.pending_animation_039)
                .addFrame(R.drawable.pending_animation_040)
                .addFrame(R.drawable.pending_animation_041)
                .addFrame(R.drawable.pending_animation_042)
                .addFrame(R.drawable.pending_animation_043)
                .addFrame(R.drawable.pending_animation_044)
                .addFrame(R.drawable.pending_animation_045)
                .addFrame(R.drawable.pending_animation_046)
                .addFrame(R.drawable.pending_animation_047)
                .addFrame(R.drawable.pending_animation_048)
                .addFrame(R.drawable.pending_animation_049)
                .addFrame(R.drawable.pending_animation_050)
                .addFrame(R.drawable.pending_animation_051)
                .addFrame(R.drawable.pending_animation_052)
                .addFrame(R.drawable.pending_animation_053)
                .addFrame(R.drawable.pending_animation_054)
                .addFrame(R.drawable.pending_animation_055)
                .addFrame(R.drawable.pending_animation_056)
                .addFrame(R.drawable.pending_animation_057)
                .addFrame(R.drawable.pending_animation_058)
                .addFrame(R.drawable.pending_animation_059)
                .addFrame(R.drawable.pending_animation_060)
                .addFrame(R.drawable.pending_animation_061)
                .addFrame(R.drawable.pending_animation_062)
                .addFrame(R.drawable.pending_animation_063)
                .addFrame(R.drawable.pending_animation_064)
                .addFrame(R.drawable.pending_animation_065)
                .addFrame(R.drawable.pending_animation_066)
                .addFrame(R.drawable.pending_animation_067)
                .addFrame(R.drawable.pending_animation_068)
                .addFrame(R.drawable.pending_animation_069)
                .addFrame(R.drawable.pending_animation_070)
                .addFrame(R.drawable.pending_animation_071)
                .addFrame(R.drawable.pending_animation_072)
                .addFrame(R.drawable.pending_animation_073)
                .addFrame(R.drawable.pending_animation_074)
                .addFrame(R.drawable.pending_animation_075)
                .addFrame(R.drawable.pending_animation_076)
                .addFrame(R.drawable.pending_animation_077)
                .addFrame(R.drawable.pending_animation_078)
                .addFrame(R.drawable.pending_animation_079)
                .addFrame(R.drawable.pending_animation_080)
                .addFrame(R.drawable.pending_animation_081)
                .addFrame(R.drawable.pending_animation_082)
                .addFrame(R.drawable.pending_animation_083)
                .addFrame(R.drawable.pending_animation_084)
                .addFrame(R.drawable.pending_animation_085)
                .addFrame(R.drawable.pending_animation_086)
                .addFrame(R.drawable.pending_animation_087)
                .addFrame(R.drawable.pending_animation_088)
                .addFrame(R.drawable.pending_animation_089)
                .addFrame(R.drawable.pending_animation_090);

        return new MultiStateAnimation.Builder(view)
                .addSection(startSection)
                .addSection(loadingSection)
                .addSection(endSection)
                .build(this);
    }
}
