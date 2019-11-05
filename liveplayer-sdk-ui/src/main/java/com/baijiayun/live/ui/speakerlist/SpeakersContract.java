package com.baijiayun.live.ui.speakerlist;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.livecore.models.LPInteractionAwardModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;

/**
 * Created by Shubo on 2019-07-25.
 */
public interface SpeakersContract {

    interface View extends BaseView<Presenter> {

        // 通知远端流状态变化
        void notifyRemotePlayableChanged(IMediaModel iMediaModel);

        // 通知远程控制本地推流
        void notifyLocalPlayableChanged(boolean isVideoOn, boolean isAudioOn);

        // 通知主讲人改变
        void notifyPresenterChanged(String userId, IMediaModel defaultMediaModel);

        // 通知主讲人屏幕分享或者播放媒体文件
        void notifyPresenterDesktopShareAndMedia(boolean isDesktopShareAndMedia);

        // 通知有学生举手
        void showSpeakApply(IMediaModel iMediaModel);

        // 学生取消举手
        void removeSpeakApply(String identity);

        // 收到点赞
        void notifyAward(LPInteractionAwardModel awardModel);

        // 收到网络状态回调
        void notifyNetworkStatus(String identity, double lossRate);

        // 用户主动关闭视频
        void notifyUserCloseAction(RemoteItem remoteItem);
    }

    interface Presenter extends BasePresenter {

        // 同意学生举手
        void agreeSpeakApply(String userId);

        // 拒绝学生举手
        void disagreeSpeakApply(String userId);

        // 获得Router
        LiveRoomRouterListener getRouterListener();

        // 奖励点赞
        void requestAward(String number);

        // 获得奖励数
        int getAwardCount(String number);

        // 处理用户手动关闭他人视频
        void handleUserCloseAction(RemoteItem remoteItem);

        // 结束其他人发言
        void closeSpeaking(String userId);

        /**
         * 是否本地显示点赞动画
         */
        void localShowAwardAnimation(String userNumber);
    }

}
