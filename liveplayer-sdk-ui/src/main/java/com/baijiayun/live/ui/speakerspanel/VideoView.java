package com.baijiayun.live.ui.speakerspanel;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.livecore.wrapper.impl.LPVideoView;

/**
 * Created by Shubo on 2017/6/10.
 */

public class VideoView extends FrameLayout {

    private TextView tvName, awardTv, networkTv;
    private View bottomLayout;
    private LPVideoView lpVideoView;
    private ImageView loadingView;

    private String name;
    private TextView loadingText;
    private boolean isLoading = true;

    @ColorInt
    int color = -1;

    public VideoView(Context context) {
        super(context);
        init();
    }

    public VideoView(Context context, boolean isNeedLoading) {
        super(context);
        this.isLoading = isNeedLoading;
        init();
        if (!isLoading)
            closeLoading();
    }

    private void init() {
        ViewGroup.LayoutParams flLp = new ViewGroup.LayoutParams(DisplayUtils.dip2px(getContext(), 100), DisplayUtils.dip2px(getContext(), 76));
        this.setLayoutParams(flLp);
        if (color == -1) color = ContextCompat.getColor(getContext(), R.color.live_white);
        //视频
        lpVideoView = new LPVideoView(getContext());

        if (lpVideoView.getParent() != null) {
            ((ViewGroup) lpVideoView.getParent()).removeView(lpVideoView);
        }
        this.addView(lpVideoView);

        bottomLayout = LayoutInflater.from(getContext()).inflate(R.layout.video_name_award_layout, null);
        tvName = bottomLayout.findViewById(R.id.live_name_tv);
        awardTv = bottomLayout.findViewById(R.id.live_award_count_tv);
        networkTv = bottomLayout.findViewById(R.id.item_video_network);
        tvName.setText(name);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.gravity = Gravity.BOTTOM | Gravity.START;
        bottomLayout.setLayoutParams(tvLp);
        bottomLayout.setVisibility(View.GONE);
        this.addView(bottomLayout);

        loadingText = new TextView(getContext());
        loadingText.setLines(1);
        loadingText.setText("与对方连接中...");
        loadingText.setTextSize(13);
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setTextColor(color);
        FrameLayout.LayoutParams loadingTextLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        loadingTextLp.setMargins(0, DisplayUtils.dip2px(getContext(), 50), 0, 0);
        loadingTextLp.gravity = Gravity.BOTTOM;
        this.addView(loadingText, loadingTextLp);

        FrameLayout.LayoutParams loadingLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        loadingView = new ImageView(getContext());
        loadingLp.gravity = Gravity.CENTER;
        loadingView.setAdjustViewBounds(true);
        loadingView.setImageResource(R.drawable.ic_live_loading);
        loadingLp.setMargins(DisplayUtils.dip2px(getContext(), 19), DisplayUtils.dip2px(getContext(), 20), DisplayUtils.dip2px(getContext(), 19), DisplayUtils.dip2px(getContext(), 25));
        this.addView(loadingView, loadingLp);

        if (isLoading)
            startRotate();
    }

    public void startRotate() {
        Animation operatingAnim = AnimationUtils.loadAnimation(getContext(), R.anim.live_video_loading);
        operatingAnim.setInterpolator(new LinearInterpolator());
        loadingView.setVisibility(VISIBLE);
        loadingView.startAnimation(operatingAnim);
    }

    public void stopRotate() {
        if (!isLoading) return;
        loadingText.setVisibility(GONE);
        loadingView.setVisibility(GONE);
        loadingView.clearAnimation();
        isLoading = false;
        tvName.setVisibility(VISIBLE);
        bottomLayout.setVisibility(VISIBLE);
    }

    private void closeLoading() {
        loadingText.setVisibility(GONE);
        loadingView.setVisibility(GONE);
        loadingView.clearAnimation();
        isLoading = false;
        tvName.setVisibility(VISIBLE);
        bottomLayout.setVisibility(VISIBLE);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isLoading && oldh > 0 && h > oldh) {
            loadingView.setPadding(w / 4, h / 4, w / 4, h / 4);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            loadingText.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            loadingText.setLayoutParams(layoutParams);
            startRotate();
        } else if (isLoading && h < oldh && oldh > 0) {
            loadingView.setImageResource(R.drawable.ic_live_loading);
            loadingView.setPadding(0, 0, 0, 0);
            startRotate();
        }
    }

    public LPVideoView getLpVideoView() {
        return lpVideoView;
    }

    public void setNameColor(@ColorInt int color) {
        this.color = color;
        if (tvName != null) {
            tvName.setTextColor(color);
        }
    }

    public void setName(String name) {
        this.name = name;
        if (tvName != null)
            tvName.setText(name);
    }

    public void setAwardCount(int awardCount) {
        if (awardCount > 0) {
            awardTv.setVisibility(View.VISIBLE);
            awardTv.setText(String.valueOf(awardCount));
        }
    }

    public void setAwardTvVisibility(int visibility){
        awardTv.setVisibility(visibility);
    }

    public void updateNetworkTip(String content, int textColor){
        networkTv.setText(content);
        networkTv.setTextColor(textColor);
    }

    public TextView getVideoViewNetworkTextView(){
        return networkTv;
    }

}
