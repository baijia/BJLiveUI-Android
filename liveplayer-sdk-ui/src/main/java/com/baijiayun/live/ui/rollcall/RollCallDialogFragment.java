package com.baijiayun.live.ui.rollcall;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.QueryPlus;

/**
 * Created by wangkangfei on 17/5/31.
 */

public class RollCallDialogFragment extends BaseDialogFragment implements RollCallDialogContract.View {
    private QueryPlus $;
    private RollCallDialogContract.Presenter presenter;

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_roll_call;
    }

    @Override
    public void onStart() {
        contentBackgroundColor(android.R.color.transparent);
        super.onStart();
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        hideTitleBar();
        $ = QueryPlus.with(contentView);
        $.id(R.id.tv_roll_call_answer).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.rollCallConfirm();
                dismissAllowingStateLoss();
            }
        });
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        int screenWidth = DisplayUtils.getScreenWidthPixels(getContext());
        int screenHeight = DisplayUtils.getScreenHeightPixels(getContext());
        windowParams.width = Math.min(screenHeight, screenWidth);
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.CENTER;
        windowParams.x = 0;
        windowParams.y = 0;

        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
    }

    @Override
    public void setPresenter(RollCallDialogContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void timerDown(int time) {
        if ($ != null)
        ((TextView) $.id(R.id.tv_roll_call_tip).view()).setText(getString(R.string.live_roll_call_count_down, String.valueOf(time)));
    }

    @Override
    public void timeOutSoDismiss() {
        if (getFragmentManager() != null)
            this.dismissAllowingStateLoss();
    }

}
