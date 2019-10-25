package com.baijiayun.live.ui.toolbox.announcement.modelui;

import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

/**
 * 当前公告子View设置公告/通知信息
 * panzq
 * 20190708
 */
public interface IAnnouncementUI {

    /**
     * 设置当前的公告/通知信息
     * @param iAnnouncementModel
     */
    void setNoticeInfo(IAnnouncementModel iAnnouncementModel);

    NoticeInfo getNotice();
}
