package com.baijiahulian.live.ui.speakerspanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baijia.baijiashilian.liveplayer.ViESurfaceViewRenderer;
import com.baijiahulian.live.ui.R;
import com.baijiahulian.live.ui.utils.DisplayUtils;
import com.baijiahulian.live.ui.utils.RxUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

/**
 * Created by Shubo on 2017/6/10.
 */

public class VideoView extends FrameLayout {

    private TextView tvName, awardTv;
    private LinearLayout awardAndNameLayout;
    private SurfaceView surfaceView;
    private ImageView ivWaterMark;
    private ImageView loadingView;
    private FrameLayout loadingLayout;
    private Bitmap waterMark;
    private WaterMarkTarget target;

    private String name;
    private String waterMarkUrl;
    private TextView loadingText;
    private int waterMarkPosition = 1;
    private boolean isLoading = true;

    @ColorInt
    int color = -1;

    public VideoView(Context context, String name, String waterMarkUrl, int waterMarkPosition, View view) {
        super(context);
        this.name = name;
        this.waterMarkPosition = waterMarkPosition;
        this.waterMarkUrl = waterMarkUrl;
        if (view instanceof SurfaceView)
            this.surfaceView = (SurfaceView) view;
        init();
    }

    public VideoView(Context context, String name, View view) {
        super(context);
        this.name = name;
        if (view instanceof SurfaceView)
            this.surfaceView = (SurfaceView) view;
        init();
    }

    private void init() {
        ViewGroup.LayoutParams flLp = new ViewGroup.LayoutParams(DisplayUtils.dip2px(getContext(), 100), DisplayUtils.dip2px(getContext(), 76));
        this.setLayoutParams(flLp);
        if (color == -1) color = ContextCompat.getColor(getContext(), R.color.live_white);
        //视频
        if (surfaceView == null) {
            surfaceView = ViESurfaceViewRenderer.CreateRenderer(getContext(), true);
            surfaceView.setZOrderMediaOverlay(true);
        }
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }
        this.addView(surfaceView);

        awardAndNameLayout = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.video_name_award_layout, null);
        tvName = awardAndNameLayout.findViewById(R.id.live_name_tv);
        awardTv = awardAndNameLayout.findViewById(R.id.live_award_count_tv);
        tvName.setText(name);
        LayoutParams tvLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.gravity = Gravity.BOTTOM | Gravity.START;
        awardAndNameLayout.setLayoutParams(tvLp);
        this.addView(awardAndNameLayout);
        awardAndNameLayout.setVisibility(View.GONE);

        loadingText = new TextView(getContext());
        loadingText.setLines(1);
        loadingText.setText("与对方连接中...");
        loadingText.setTextSize(13);
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setTextColor(color);
        LayoutParams loadingTextLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        loadingTextLp.setMargins(0, DisplayUtils.dip2px(getContext(), 50), 0, 0);
        loadingTextLp.gravity = Gravity.BOTTOM;
        this.addView(loadingText, loadingTextLp);

        LayoutParams loadingLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
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
        awardAndNameLayout.setVisibility(VISIBLE);
        if (ivWaterMark != null)
            ivWaterMark.setVisibility(VISIBLE);
    }


    private static class WaterMarkTarget implements Target {

        private WeakReference<VideoView> wrVideoView;

        WaterMarkTarget(VideoView videoView) {
            wrVideoView = new WeakReference<>(videoView);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            VideoView videoView = wrVideoView.get();
            if (videoView == null) return;
            videoView.waterMark = bitmap;
            int height = Math.min(videoView.waterMark.getHeight(), videoView.getMeasuredHeight() / 9);
            int width = Math.min(videoView.waterMark.getWidth(), videoView.getMeasuredWidth() / 9);
            LayoutParams ivLp = new LayoutParams(width, height);
            switch (videoView.waterMarkPosition) {
                case 1:
                    ivLp.gravity = GravityCompat.START | Gravity.TOP;
                    break;
                case 2:
                    ivLp.gravity = GravityCompat.END | Gravity.TOP;
                    break;
                case 3:
                    ivLp.gravity = GravityCompat.END | Gravity.BOTTOM;
                    break;
                case 4:
                    ivLp.gravity = GravityCompat.START | Gravity.BOTTOM;
                    break;
            }
            videoView.ivWaterMark.setImageBitmap(videoView.waterMark);
            videoView.ivWaterMark.setScaleType(ImageView.ScaleType.FIT_START);
            videoView.addView(videoView.ivWaterMark, ivLp);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isLoading && oldh > 0 && h > oldh) {
            loadingView.setPadding(w / 4, h / 4, w / 4, h / 4);
//
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            loadingText.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            loadingText.setLayoutParams(layoutParams);

            startRotate();
        } else if (isLoading && h < oldh && oldh > 0) {
            loadingView.setImageResource(R.drawable.ic_live_loading);
            loadingView.setPadding(0, 0, 0, 0);
            startRotate();
        }
        if (TextUtils.isEmpty(waterMarkUrl)) {
            return;
        }
        if (target == null) {
            target = new WaterMarkTarget(this);
            ivWaterMark = new ImageView(getContext());
            ivWaterMark.setVisibility(GONE);
            Picasso.with(getContext()).load(waterMarkUrl).into(target);
        } else {
            this.post(new Runnable() {
                @Override
                public void run() {
                    resizeWaterMark(videoHeight, videoWidth, h, w);
                }
            });

        }
    }

    private int videoHeight, videoWidth;

    public void resizeWaterMark(int videoHeight, int videoWidth) {
        resizeWaterMark(videoHeight, videoWidth, getMeasuredHeight(), getMeasuredWidth());
    }

    public void resizeWaterMark(int videoHeight, int videoWidth, int viewHeight, int viewWidth) {
        if (ivWaterMark == null || waterMark == null) return;
        if (videoHeight == 0 || videoWidth == 0) return;
        this.videoHeight = videoHeight;
        this.videoWidth = videoWidth;
        int height = Math.min(waterMark.getHeight(), viewHeight / 9);
        int width = Math.min(waterMark.getWidth(), viewWidth / 9);
        LayoutParams ivLp = (LayoutParams) ivWaterMark.getLayoutParams();
        ivLp.width = width;
        ivLp.height = height;

        float videoRatio = (float) videoWidth / videoHeight;
        float viewRation = (float) viewWidth / viewHeight;

        int transverseMargin;
        int verticalMargin;

        if (videoRatio < viewRation) {
            verticalMargin = 0;
            transverseMargin = Math.abs((viewWidth - viewHeight * videoWidth / videoHeight) / 2);
        } else {
            transverseMargin = 0;
            verticalMargin = Math.abs((viewHeight - videoHeight * viewWidth / videoWidth) / 2);
        }

        switch (waterMarkPosition) {
            case 1:
                ivLp.leftMargin = transverseMargin + 20;
                ivLp.topMargin = verticalMargin + 15;
                ivLp.rightMargin = 0;
                ivLp.bottomMargin = 0;
                ivLp.gravity = GravityCompat.START | Gravity.TOP;
                break;
            case 2:
                ivLp.leftMargin = 0;
                ivLp.topMargin = verticalMargin + 10;
                ivLp.rightMargin = transverseMargin + 15;
                ivLp.bottomMargin = 0;
                ivLp.gravity = GravityCompat.END | Gravity.TOP;
                break;
            case 3:
                ivLp.leftMargin = 0;
                ivLp.topMargin = 0;
                ivLp.rightMargin = transverseMargin + 15;
                ivLp.bottomMargin = verticalMargin + 10;
                ivLp.gravity = GravityCompat.END | Gravity.BOTTOM;
                break;
            case 4:
                ivLp.leftMargin = transverseMargin + 15;
                ivLp.topMargin = 0;
                ivLp.rightMargin = 0;
                ivLp.bottomMargin = verticalMargin + 10;
                ivLp.gravity = GravityCompat.START | Gravity.BOTTOM;
                break;
        }
        ivWaterMark.setLayoutParams(ivLp);
    }

    public View getSurfaceView() {
        return surfaceView;
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

    public void setAwardLayoutVisibility(int visibility){
        awardAndNameLayout.setVisibility(visibility);
    }
}
