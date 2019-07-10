package com.baijiayun.live.ui.chat.preview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.QueryPlus;

/**
 * Created by wangkangfei on 17/5/13.
 */

public class ChatSavePicDialogFragment extends BaseDialogFragment implements ChatSavePicDialogContract.View {
    private QueryPlus $;
    private ChatSavePicDialogContract.Presenter presenter;

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_chat_save_pic;
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
        $.id(R.id.dialog_save_pic).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.realSavePic();
                dismissAllowingStateLoss();
            }
        });
        $.id(R.id.dialog_save_pic_cancel).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
    }

    @Override
    public void setPresenter(ChatSavePicDialogContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        int screenWidth = DisplayUtils.getScreenWidthPixels(getContext());
        int screenHeight = DisplayUtils.getScreenHeightPixels(getContext());
        windowParams.width = Math.min(screenHeight, screenWidth);
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
        windowParams.x = 0;
        windowParams.y = 0;

        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        $ = null;
        presenter = null;
    }
}
