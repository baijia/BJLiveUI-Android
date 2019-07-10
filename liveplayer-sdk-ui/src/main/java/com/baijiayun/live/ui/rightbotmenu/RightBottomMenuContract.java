package com.baijiayun.live.ui.rightbotmenu;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.context.LPConstants;

/**
 * Created by Shubo on 2017/2/15.
 */

interface RightBottomMenuContract {
    interface View extends BaseView<Presenter> {
        void showVideoStatus(boolean isOn);

        void showAudioStatus(boolean isOn);

        void enableSpeakerMode();

        void disableSpeakerMode();

        void clearScreen();

        void unClearScreen();

        void showVolume(LPConstants.VolumeLevel level);

        void showZoomIn();

        void showZoomOut();

        void showZoom();

        void hideZoom();

        void showAudioRoomError();
    }

    interface Presenter extends BasePresenter {
        void changeZoom();

        void changeAudio();

        void changeVideo();

        void more(int anchorX, int anchorY);

        void getSysRotationSetting();

        void setSysRotationSetting();

        void notifySpeakerStatus(boolean isSpeak);
    }
}
