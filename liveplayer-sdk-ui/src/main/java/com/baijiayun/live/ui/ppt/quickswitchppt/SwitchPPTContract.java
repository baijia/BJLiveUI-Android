package com.baijiayun.live.ui.ppt.quickswitchppt;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.viewmodels.impl.LPDocListViewModel;

import java.util.List;

/**
 * Created by bjhl on 17/7/4.
 */
class SwitchPPTContract {
    interface View extends BaseView<Presenter> {
        void setIndex();

        void setMaxIndex(int maxIndex);

        void setType(boolean isStudent, boolean enableMultiWhiteboard);

        void docListChanged(List<LPDocListViewModel.DocModel> docModelList);
    }

    interface Presenter extends BasePresenter {
        void setSwitchPosition(int position);

        /**
         * 添加白板
         */
        void addPage();

        /**
         * 删除白板
         */
        void delPage(int pageId);

        LiveRoomRouterListener getRoute();

        void changePage(int page);

        boolean canOperateDocumentControl();
    }
}
