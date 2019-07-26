package com.baijiahulian.live.ui.ppt;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;

import com.baijiayun.photoview.OnDoubleTapListener;
import com.baijiayun.photoview.OnViewTapListener;
import com.baijiayun.ppt.PPTView;

/**
 * Created by Shubo on 2017/2/18.
 */

public class MyPPTView extends PPTView implements PPTContract.View {

    public MyPPTView(@NonNull Context context) {
        super(context);
    }

    private PPTContract.Presenter presenter;

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
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
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

        mPageTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!presenter.isPPTInSpeakerList()){
                    presenter.showQuickSwitchPPTView(getCurrentPageIndex(), getMaxPage());
                } else{
                    presenter.showOptionDialog();
                }
            }
        });

    }

    @Override
    public void setMaxPage(int maxIndex) {
        super.setMaxPage(maxIndex);
        presenter.updateQuickSwitchPPTView(maxIndex);
    }

    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();
        presenter = null;
    }

    public void onSizeChange() {
        super.onSizeChange();
    }
}
