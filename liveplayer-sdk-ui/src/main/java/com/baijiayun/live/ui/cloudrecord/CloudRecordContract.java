package com.baijiayun.live.ui.cloudrecord;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by wangkangfei on 17/5/8.
 */

public interface CloudRecordContract {

    interface View extends BaseView<Presenter> {

    }

    interface Presenter extends BasePresenter {
        void cancelCloudRecord();

        boolean canOperateCloudRecord();
    }
}
