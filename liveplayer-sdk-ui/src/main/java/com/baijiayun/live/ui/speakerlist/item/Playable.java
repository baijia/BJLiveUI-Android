package com.baijiayun.live.ui.speakerlist.item;

import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2019-07-25.
 */
public interface Playable extends SpeakItem {

    boolean hasVideo();

    boolean hasAudio();

    boolean isVideoStreaming();

    boolean isAudioStreaming();

    boolean isStreaming();

    void stopStreaming();

    void refreshPlayable();

    IUserModel getUser();

    void notifyAwardChange(int count);

}
