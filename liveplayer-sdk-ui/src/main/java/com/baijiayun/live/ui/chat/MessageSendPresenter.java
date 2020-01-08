package com.baijiayun.live.ui.chat;

import android.text.TextUtils;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.Precondition;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;

import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/3/4.
 */

public class MessageSendPresenter implements MessageSendContract.Presenter {

    private MessageSendContract.View view;
    private LiveRoomRouterListener routerListener;

    public MessageSendPresenter(MessageSendContract.View view) {
        this.view = view;
    }

    public void setView(MessageSendContract.View view) {
        this.view = view;
    }

    @Override
    public void sendMessage(String message) {
        Precondition.checkNotNull(routerListener);
        if (!TextUtils.isEmpty(message)) {
            if (message.startsWith("/dev")) {
                routerListener.showDebugBtn();
                return;
            }
        }
        routerListener.getLiveRoom().getChatVM().sendMessage(message);
        view.showMessageSuccess();
    }

    @Override
    public void sendEmoji(String emoji) {
        Precondition.checkNotNull(routerListener);
        routerListener.getLiveRoom().getChatVM().sendEmojiMessage(emoji);
        view.showMessageSuccess();
    }

    @Override
    public void sendPicture(String path) {
        routerListener.sendImageMessage(path);
        view.onPictureSend();
    }

    @Override
    public void sendMessageToUser(String message) {
        Precondition.checkNotNull(routerListener);
        if (!TextUtils.isEmpty(message)) {
            if (message.startsWith("/dev")) {
                routerListener.showDebugBtn();
                return;
            }
        }
        IUserModel toUser = routerListener.getPrivateChatUser();
        routerListener.getLiveRoom().getChatVM().sendMessageToUser(toUser, message);
        view.showMessageSuccess();
    }



    @Override
    public void sendEmojiToUser(String emoji) {
        Precondition.checkNotNull(routerListener);
        IUserModel toUser = routerListener.getPrivateChatUser();
        routerListener.getLiveRoom().getChatVM().sendEmojiMessageToUser(toUser, emoji);
        view.showMessageSuccess();
    }


    @Override
    public void chooseEmoji() {
        view.showEmojiPanel();
    }


    public void choosePrivateChatUser() {
        view.showPrivateChatUserPanel();
    }

    @Override
    public IUserModel getPrivateChatUser() {
        return routerListener.getPrivateChatUser();
    }

    @Override
    public boolean isPrivateChat() {
        return routerListener.isPrivateChat();
    }

    @Override
    public boolean isLiveCanWhisper() {
        return routerListener.getLiveRoom().getChatVM().isLiveCanWhisper();
    }

    @Override
    public boolean canSendPicture() {
        // 大班课只有老师和助教能发图片，一对一、小班课都能发
        return (routerListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi
                || routerListener.isTeacherOrAssistant() || routerListener.isGroupTeacherOrAssistant());
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }


    public void onPrivateChatUserChange() {
        if (view != null) {
            view.showPrivateChatChange();
        }
    }
}
