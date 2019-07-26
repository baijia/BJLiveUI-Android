package com.baijiahulian.live.ui.ppt;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.baijiahulian.livecore.context.LiveRoom;
import com.baijiahulian.livecore.ppt.LPPPTFragment;
import com.baijiahulian.livecore.ppt.whiteboard.LPWhiteBoardView;

/**
 * Created by Shubo on 2017/2/18.
 */

public class MyPPTFragment extends LPPPTFragment implements PPTContract.View {

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        showPPTPageView();
    }

    public static MyPPTFragment newInstance(LiveRoom liveRoom) {

        Bundle args = new Bundle();

        MyPPTFragment fragment = new MyPPTFragment();
        fragment.setLiveRoom(liveRoom);
        boolean sdkValid = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        fragment.setAnimPPTEnable(liveRoom.getPartnerConfig().PPTAnimationDisable == 0 && sdkValid);
        fragment.setArguments(args);
        return fragment;
    }

    private PPTContract.Presenter presenter;

    @Override
    public void setPresenter(PPTContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onStart() {
        super.onStart();

        super.setOnSingleTapListener(new LPWhiteBoardView.OnSingleTapListener() {
            @Override
            public void onSingleTap(LPWhiteBoardView whiteBoardView) {
                if (isDetached()) return;
                try {
                    // Fragment MyPPTFragment not attached to Activity
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        presenter.clearScreen();
                    }
                } catch (IllegalStateException ignore) {
                }
            }
        });

        mPageTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.showQuickSwitchPPTView(currentPageIndex, maxIndex);
            }
        });
    }

    public LPWhiteBoardView getLPWhiteBoardView() {
        return mWhiteBoardView;
    }

    @Override
    public void setMaxPage(int maxIndex) {
        super.setMaxPage(maxIndex);
        presenter.updateQuickSwitchPPTView(maxIndex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
