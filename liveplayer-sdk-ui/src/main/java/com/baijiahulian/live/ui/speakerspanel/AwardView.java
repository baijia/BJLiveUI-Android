package com.baijiahulian.live.ui.speakerspanel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.baijiahulian.live.ui.R;
import com.baijiahulian.live.ui.utils.DisplayUtils;
import com.baijiahulian.live.ui.utils.FrameAnimation;

import java.util.logging.Handler;

/**
 * Created by yongjiaming on 2018/10/23
 */
public class AwardView extends FrameLayout {

    private FrameAnimation frameAnimation;
    private ImageView starIv, goldenShineIv, medalIv;
    private Paint paint;
    private String userName;
    private SoundPool soundPool;
    private float volumeCurrent = 1;

    public AwardView(@NonNull Context context) {
        this(context, null);
    }

    public AwardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AwardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_award, this);
        starIv = findViewById(R.id.award_start_iv);
        goldenShineIv = findViewById(R.id.golden_light_iv);
        medalIv = findViewById(R.id.award_medal_iv);
        initPaint();
        setWillNotDraw(false);
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(DisplayUtils.sp2px(getContext(), 18));
    }

    public void setUserName(String userName) {
        this.userName = userName;
        invalidate();
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        Configuration configuration = this.getResources().getConfiguration();
        int widgetWidth = configuration.orientation == Configuration.ORIENTATION_PORTRAIT ?  getMeasuredWidth() : getMeasuredHeight();
        int offsetY = (int) (medalIv.getMeasuredHeight() * 0.694f);
        while (paint.measureText(userName) > widgetWidth){
            userName = userName.substring(0, userName.length() / 3 * 2);
        }
        int offsetX = (int) ((widgetWidth - paint.measureText(userName)) /2);
        canvas.drawText(userName, offsetX, offsetY, paint);
    }

    public void startAnim() {
        if(frameAnimation != null){
            return;
        }
        frameAnimation = new FrameAnimation(starIv, getRes(), 33, true);
        frameAnimation.start();
        ObjectAnimator ra = ObjectAnimator.ofFloat(goldenShineIv,"rotation", 0f, 360f);
        ra.setDuration(10000);
        ra.setRepeatCount(ValueAnimator.INFINITE);
        ra.setRepeatMode(ValueAnimator.REVERSE);
        ra.start();

        AnimatorSet scaleInAnimatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0, 1f);
        scaleInAnimatorSet.setDuration(500);
        scaleInAnimatorSet.setInterpolator(new DecelerateInterpolator());
        scaleInAnimatorSet.playTogether(scaleX, scaleY);
        scaleInAnimatorSet.start();

        postDelayed(new Runnable() {
            @Override
            public void run() {
                AnimatorSet scaleOutAnimatorSet = new AnimatorSet();
                ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(AwardView.this, "scaleX", 1f, 0f);
                ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(AwardView.this, "scaleY", 1f, 0f);
                scaleOutAnimatorSet.setDuration(500);
                scaleOutAnimatorSet.setInterpolator(new DecelerateInterpolator());
                scaleOutAnimatorSet.play(scaleOutX).with(scaleOutY);
                scaleOutAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if(frameAnimation != null){
                            frameAnimation.stop();
                            frameAnimation = null;
                        }
                        if(soundPool != null){
                            soundPool.release();
                            soundPool = null;
                        }
                    }
                });
                scaleOutAnimatorSet.start();
            }
        }, 3000);
        playAudio();
    }

    private int[] getRes() {
        TypedArray typedArray = getResources().obtainTypedArray(R.array.star);
        int len = typedArray.length();
        int[] resId = new int[len];
        for (int i = 0; i < len; i++) {
            resId[i] = typedArray.getResourceId(i, -1);
        }
        typedArray.recycle();
        return resId;
    }



    public void playAudio(){
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            volumeCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        final int soundId = soundPool.load(getContext(), R.raw.award, 1);
        // soundId for reuse later on
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(soundId, volumeCurrent, volumeCurrent, 0, 0, 1);
            }
        });
    }


}
