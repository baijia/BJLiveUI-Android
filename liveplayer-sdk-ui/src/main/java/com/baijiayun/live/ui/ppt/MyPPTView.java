package com.baijiayun.live.ui.ppt;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType;
import com.baijiayun.live.ui.speakerlist.item.Switchable;
import com.baijiayun.livecore.ppt.PPTView;
import com.baijiayun.livecore.ppt.photoview.OnDoubleTapListener;
import com.baijiayun.livecore.ppt.whiteboard.Whiteboard;
import com.baijiayun.livecore.ppt.whiteboard.shape.Shape;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shubo on 2017/2/18.
 */

public class MyPPTView extends PPTView implements PPTContract.View, Switchable {

    public MyPPTView(@NonNull Context context) {
        super(context);
    }

    private PPTContract.Presenter presenter;

    private boolean isInFullScreen = true;

    @Override
    public void setPresenter(PPTContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void onStart() {
        super.setOnViewTapListener((view, x, y) -> {
            if (!isInFullScreen) {
                showOptionDialog();
                return;
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                    && presenter != null) {
                presenter.clearScreen();
            }
        });

        super.setOnDoubleTapListener(new OnDoubleTapListener() {
            @Override
            public void onDoubleTapConfirmed() {
                if (isInFullScreen)
                    setDoubleTapScaleEnable(true);
                else {
                    setDoubleTapScaleEnable(false);
                    presenter.getRouter().getFullScreenItem().switchBackToList();
                    switchToFullScreen();
                }
            }

            @Override
            public void onDoubleTapOnShape(Shape shape) {

            }
        });

        super.setPPTErrorListener((errorCode, description) -> presenter.showPPTLoadError(errorCode, description));

        mPageTv.setOnClickListener(v -> {
            if (isInFullScreen) {
                presenter.showQuickSwitchPPTView(getCurrentPageIndex(), getMaxPage());
            }
        });

        super.setOnPageSelectedListener((position, remarksInfo) -> {
            //大班课在initDocList之后设置animPPTAuth为true，使得学生可翻页
            MyPPTView.super.setAnimPPTAuth(true);
        });

    }

    private void showOptionDialog() {
        List<String> options = new ArrayList<>();
        options.add(getContext().getString(R.string.live_full_screen));

        if (getContext() == null) return;
        new MaterialDialog.Builder(getContext())
                .items(options)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    if (getContext() == null) return;
                    if (getContext().getString(R.string.live_full_screen).equals(charSequence.toString())) {
                        presenter.getRouter().getFullScreenItem().switchBackToList();
                        switchToFullScreen();
                    }
                    materialDialog.dismiss();
                })
                .show();
    }

    @Override
    public void setMaxPage(int maxIndex) {
        super.setMaxPage(maxIndex);
        presenter.updateQuickSwitchPPTView(maxIndex);
    }

    public void onDestroy() {
        super.destroy();
        if (presenter != null)
            presenter.destroy();
    }

    public void onSizeChange() {
        super.onSizeChange();
    }

    @Override
    public int getPositionInParent() {
        return 0;
    }

    @Override
    public boolean isInFullScreen() {
        return isInFullScreen;
    }

    @Override
    public void switchToFullScreen() {
        removeSwitchableFromParent(this);
        presenter.getRouter().setFullScreenItem(this);
        isInFullScreen = true;
    }

    @Override
    public void switchBackToList() {
        removeSwitchableFromParent(this);
        presenter.getRouter().switchBackToList(this);
        isInFullScreen = false;
    }

    @Override
    public String getIdentity() {
        return "PPT";
    }

    @Override
    public SpeakItemType getItemType() {
        return SpeakItemType.PPT;
    }

    @Override
    public View getView() {
        return this;
    }

    private void removeSwitchableFromParent(Switchable switchable) {
        View view = switchable.getView();
        if (view == null) return;
        ViewParent viewParent = view.getParent();
        if (viewParent == null) return;
        ((ViewGroup) viewParent).removeView(view);
    }
}
