package com.baijiayun.live.ui.chat.preview;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by wangkangfei on 17/5/13.
 */

public interface ChatPictureViewContract {

    interface View extends BaseView<Presenter> {

    }

    interface Presenter extends BasePresenter {
        void showSaveDialog(byte[] bmpArray);
    }
}
