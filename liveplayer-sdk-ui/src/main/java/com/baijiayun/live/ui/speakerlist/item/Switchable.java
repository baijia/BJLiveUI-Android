package com.baijiayun.live.ui.speakerlist.item;


/**
 * Created by Shubo on 2019-07-25.
 */
public interface Switchable extends SpeakItem {

    /**
     * 获取所在父容器的位置
     *
     * @return position
     */
    int getPositionInParent();

    /**
     * 是否在大屏
     *
     * @return 是否在全屏位置
     */
    boolean isInFullScreen();

    /**
     * 切换到大屏幕
     */
    void switchToFullScreen();

    /**
     * 切换到发言列表
     */
    void switchBackToList();
}
