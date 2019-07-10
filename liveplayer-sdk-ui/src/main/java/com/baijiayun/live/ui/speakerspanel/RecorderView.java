package com.baijiayun.live.ui.speakerspanel;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.livecore.wrapper.impl.LPCameraView;

import io.reactivex.disposables.Disposable;


/**
 * Created by Shubo on 2017/6/10.
 */

public class RecorderView extends FrameLayout {

    private View bottomLayout;
    private TextView tvName, awardTv, networkTv;
    private LPCameraView cameraView;
    private String userName;

    public RecorderView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        cameraView = new LPCameraView(context);
        cameraView.setZOrderMediaOverlay(true);
        addView(cameraView);

        bottomLayout = LayoutInflater.from(getContext()).inflate(R.layout.video_name_award_layout, null);
        tvName = bottomLayout.findViewById(R.id.live_name_tv);
        awardTv = bottomLayout.findViewById(R.id.live_award_count_tv);
        networkTv = bottomLayout.findViewById(R.id.item_video_network);
        tvName.setText(userName);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.gravity = Gravity.BOTTOM | Gravity.START;
        bottomLayout.setLayoutParams(tvLp);
        this.addView(bottomLayout);
    }

    public LPCameraView getCameraView(){
        return cameraView;
    }

    public void setZOrderMediaOverlay(boolean isZOrder){
        cameraView.setZOrderMediaOverlay(isZOrder);
    }

    public void setName(String name) {
        this.userName = name;
        if (tvName != null)
            tvName.setText(name);
    }

    public void setAwardCount(int awardCount) {
        if (awardCount > 0) {
            awardTv.setVisibility(View.VISIBLE);
            awardTv.setText(String.valueOf(awardCount));
        } else {
            awardTv.setVisibility(View.GONE);
        }
    }

    public void setAwardTvVisibility(int visibility){
        awardTv.setVisibility(visibility);
    }

    public TextView getNetworkTextView(){
        return networkTv;
    }
}
