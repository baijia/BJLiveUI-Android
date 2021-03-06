package com.baijiayun.live.ui.chat;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.live.ui.base.RouterListener;
import com.baijiayun.livecore.models.LPExpressionModel;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/3/4.
 */

public interface MessageSendContract {

    interface View extends BaseView<Presenter> {
        void showMessageSuccess();

        void showEmojiPanel();

        void showPrivateChatUserPanel();

        void showPrivateChatChange();
//        void showPicture(int position);

        void onPictureSend();
    }

    interface Presenter extends BasePresenter {

        void sendMessageToUser(String message);

        void sendEmojiToUser(String emoji);

        void sendMessage(String message);

        void sendEmoji(String emoji);

        void sendPicture(String path);

        void chooseEmoji();

        void choosePrivateChatUser();

        boolean canSendPicture();

        boolean isPrivateChat();

        boolean isLiveCanWhisper();

        /**
         * 全体禁言状态
         */
        boolean isAllForbidden();

        boolean canWisperTeacherInForbidAllMode();

        IUserModel getPrivateChatUser();

        LiveRoomRouterListener getLiveRouterListener();

        LPExpressionModel getSingleEmoji(String emojiName);
    }
}
