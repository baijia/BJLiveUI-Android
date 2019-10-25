package com.baijiayun.live.ui.ppt.pptmanage;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

import java.util.List;

/**
 * Created by Shubo on 2017/4/26.
 */

interface PPTManageContract {

    interface View extends BaseView<Presenter> {
        void showPPTEmpty();

        void showPPTNotEmpty();

        void notifyDataChange();

        void notifyItemRemoved(int position);

        void notifyItemChanged(int position);

        void notifyItemInserted(int position);

        void showRemoveBtnEnable();

        void showRemoveBtnDisable();
    }

    interface Presenter extends BasePresenter {
        int getCount();

        IDocumentModel getItem(int position);

        void uploadNewPics(List<String> picsPath);

        void selectItem(int position);

        void deselectItem(int position);

        boolean isItemSelected(int position);

        void removeSelectedItems();

        boolean isDocumentAdded(int position);

        void attachView(View view);

        boolean canOperateDocument();

        void detachView();
    }
}
