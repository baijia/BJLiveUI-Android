package com.baijiayun.live.ui.ppt;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.baijiayun.livecore.ppt.PPTView;
import com.baijiayun.livecore.ppt.photoview.OnViewTapListener;
import com.baijiayun.livecore.ppt.whiteboard.Whiteboard;

/**
 * Created by Shubo on 2017/2/18.
 */

public class MyPPTView extends PPTView implements PPTContract.View {

    public MyPPTView(@NonNull Context context) {
        super(context);
    }

    private PPTContract.Presenter presenter;

    private boolean mRemarksEnable = true;

    private TextView mTextView;

    @Override
    public void setPresenter(PPTContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void onStart() {
        super.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                try {
                    // Fragment MyPPTFragment not attached to Activity
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                            && presenter != null) {
                        presenter.clearScreen();
                    }
                } catch (IllegalStateException ignore) {
                }
            }
        });

        super.setPPTErrorListener(new OnPPTErrorListener() {
            @Override
            public void onAnimPPTLoadError(int errorCode, String description) {
                presenter.showPPTLoadError(errorCode, description);
            }
        });

        mPageTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!presenter.isPPTInSpeakerList()) {
                    presenter.showQuickSwitchPPTView(getCurrentPageIndex(), getMaxPage());
                } else {
                    presenter.showOptionDialog();
                }
            }
        });

        super.setOnPageSelectedListener(new Whiteboard.OnPageSelectedListener() {
            @Override
            public void onPageSelected(int position, String remarksInfo) {
                //大班课在initDocList之后设置animPPTAuth为true，使得学生可翻页
                MyPPTView.super.setAnimPPTAuth(true);
            }
        });

    }

    @Override
    public void setMaxPage(int maxIndex) {
        super.setMaxPage(maxIndex);
        presenter.updateQuickSwitchPPTView(maxIndex);
    }

    //    @Override
    public void onDestroy() {
        super.destroy();
        if (presenter != null)
            presenter.destroy();
        presenter = null;
    }

    public void onSizeChange() {
        super.onSizeChange();
    }
}
