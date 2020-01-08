package com.baijiayun.live.ui.chat;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.imodels.IMessageModel;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/2/15.
 */

interface ChatContract {

    interface View extends BaseView<Presenter> {
        void notifyDataChanged();

        void clearScreen();

        void unClearScreen();

        void notifyItemChange(int position);

        void notifyItemInserted(int position);

        void notifyItemTranslateMessage();

        void showHavingPrivateChat(IUserModel privateChatUser);

        void showNoPrivateChat();

        void showFilterChat(boolean filter);
    }

    interface Presenter extends BasePresenter {
        int getCount();

        IMessageModel getMessage(int position);

        String getTranslateResult(int position);

        void translateMessage(String message, String messageId, String fromLanguage, String toLanguage);

        void showBigPic(int position);

        void reUploadImage(int position);

        void endPrivateChat();

        IUserModel getCurrentUser();

        boolean isPrivateChatMode();

        void showPrivateChat(IUserModel userModel);

        boolean isLiveCanWhisper();

        void changeNewMessageReminder(boolean isNeedShow);

        boolean needScrollToBottom();

        boolean isEnableTranslate();

        void setFilter(boolean filter);

        boolean getFilter();

        boolean isForbiddenByTeacher();
    }
}
