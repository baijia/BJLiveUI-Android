package com.baijiayun.live.ui.toolbox.announcement.modelui;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.toolbox.announcement.AnnouncementContract;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

public class DoubleAnnPresenter implements DoubleAnnContract.Presenter, IAnnouncementUI {

    private DoubleAnnContract.View mView;
    private int mType = AnnouncementContract.TYPE_UI_STUDENT;
    private int groupId = 0;

    public DoubleAnnPresenter(DoubleAnnContract.View view, int type, int groupId) {
        this.mView = view;
        this.mType = type;
        this.groupId = groupId;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {

    }

    @Override
    public void subscribe() {
        mView.setType(mType, groupId);
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void setNoticeInfo(IAnnouncementModel iAnnouncementModel) {
        mView.setNoticeInfo(iAnnouncementModel);
    }

    @Override
    public NoticeInfo getNotice() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
