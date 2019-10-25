package com.baijiayun.live.ui.toolbox.announcement.modelui;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

import io.reactivex.disposables.Disposable;

/**
 *  公告/分组编辑
 *  panzq
 *  20190708
 */
public class EditAnnPresenter implements EditAnnContract.Presenter, IAnnouncementUI{

    private boolean isGroup = true;
    private EditAnnContract.View mView;
    private LiveRoomRouterListener mRouter;

    private Disposable subscriptionOfAnnouncementChange;
    private IAnnouncementModel iAnnModel;

    public EditAnnPresenter(EditAnnContract.View view, boolean isGroup, IAnnouncementModel iAnnouncementModel) {
        this.mView = view;
        this.isGroup = isGroup;
        this.iAnnModel = iAnnouncementModel;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.mRouter = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {

//        subscriptionOfAnnouncementChange = mRouter.getLiveRoom().getObservableOfAnnouncementChange()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<IAnnouncementModel>() {
//                    @Override
//                    public void accept(IAnnouncementModel iAnnouncementModel) {
//
//                        if (iAnnouncementModel == null)
//                            return;
//
//                        if (!String.valueOf(mRouter.getLiveRoom().getGroupId()).equals(iAnnouncementModel.getGroup())) {
//                            return;
//                        }
//
//                        NoticeInfo info = mView.getNoticeInfo();
//
//                        if (info.content != null && info.content.equals(iAnnouncementModel.getContent())) {
//                            //修改成功
//                            mRouter.showMessage(R.string.string_notice_context_suss);
//                        } else {
//                            //
//                            mRouter.showMessage(R.string.string_notice_context_error);
//                        }
//                    }
//                });
        mView.initInfo(iAnnModel);
        if (mRouter.getLiveRoom().isTeacherOrAssistant()) {
            //公告
            mView.setTitle(1);
        } else {
            //通知
            mView.setTitle(2);
        }

    }

    @Override
    public void unSubscribe() {
//        RxUtils.dispose(subscriptionOfAnnouncementChange);
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setNoticeInfo(IAnnouncementModel iAnnouncementModel) {

    }

    @Override
    public NoticeInfo getNotice() {
        return mView.getNoticeInfo();
    }
}
