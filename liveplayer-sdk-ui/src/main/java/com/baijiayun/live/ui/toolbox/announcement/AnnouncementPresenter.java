package com.baijiayun.live.ui.toolbox.announcement;

import android.text.TextUtils;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.activity.GlobalPresenter;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by Shubo on 2017/4/19.
 */

public class AnnouncementPresenter implements AnnouncementContract.Presenter {

    private final String TAG = AnnouncementPresenter.class.getName();

    private AnnouncementContract.View view;

    private LiveRoomRouterListener routerListener;

    private Disposable subscriptionOfAnnouncementChange;

    private Pattern pattern;

    private GlobalPresenter globalPresenter;
    private int mType;

    public AnnouncementPresenter(AnnouncementContract.View view, GlobalPresenter globalPresenter) {
        String mode = "(http|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
        pattern = Pattern.compile(mode);
        this.view = view;
        this.globalPresenter = globalPresenter;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public LiveRoomRouterListener getRouter() {
        return routerListener;
    }

    @Override
    public boolean canOperateNoite() {
        return !(routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Assistant &&
                routerListener.getLiveRoom().getAdminAuth() != null && !routerListener.getLiveRoom().getAdminAuth().notice);
    }

    @Override
    public void subscribe() {
        subscriptionOfAnnouncementChange = routerListener.getLiveRoom().getObservableOfAnnouncementChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<IAnnouncementModel>() {
                    @Override
                    public void accept(IAnnouncementModel iAnnouncementModel) {

                        if (iAnnouncementModel == null)
                            return;

                        if (routerListener.isTeacherOrAssistant() && TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                            //空公告，跳转空页面
                            view.showBlankTips();
                            return;
                        }

                        view.setNoticeInfo(iAnnouncementModel);

//                        content = iAnnouncementModel.getContent();
//                        link = iAnnouncementModel.getLink();
//                        checkInput(content, link);
                    }
                });

        switchUI();
    }

    @Override
    public void switchUI() {

        int groupId = routerListener.getLiveRoom().getCurrentUser().getGroup();
        if (routerListener.isTeacherOrAssistant()) {
            view.editButtonEnable(true, R.string.live_edit);
            view.showCurrUI(AnnouncementContract.TYPE_UI_TEACHERORASSISTANT, groupId);
        } else if (routerListener.isGroupTeacherOrAssistant()) {
            view.editButtonEnable(true, R.string.string_notice_group);
            view.showCurrUI(AnnouncementContract.TYPE_UI_GROUPTEACHERORASSISTANT, groupId);
        } else {
            view.editButtonEnable(false, 0);
            view.showCurrUI(AnnouncementContract.TYPE_UI_STUDENT, groupId);
        }

        if (routerListener.isTeacherOrAssistant()) {
            //获取
            routerListener.getLiveRoom().requestAnnouncement(0);
        } else {
            routerListener.getLiveRoom().requestAnnouncement(0);
            routerListener.getLiveRoom().requestAnnouncement(routerListener.getLiveRoom().getGroupId());
        }
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfAnnouncementChange);
    }

    @Override
    public void destroy() {
        globalPresenter.observeAnnouncementChange();
        globalPresenter = null;
        routerListener = null;
        view = null;
    }

    @Override
    public void saveAnnouncement(String text, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (!TextUtils.isEmpty(url))
                url = "http://" + url;
        }

        int group = 0;
        if (routerListener.isTeacherOrAssistant()) {
            //获取
            group = 0;
        } else {
            group = routerListener.getLiveRoom().getGroupId();
        }

        routerListener.getLiveRoom().changeRoomAnnouncement(group, text, url);
    }

    @Override
    public boolean isGrouping() {

        if (routerListener.isTeacherOrAssistant()) {
            return false;
        }
        return true;
    }

    @Override
    public void checkInput(String text, String url) {
//        if (text.equals(content) && link.equals(url)) {
//            view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_SAVED);
//            return;
//        }
//        if (TextUtils.isEmpty(url)) {
//            view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CAN_SAVE);
//        } else {
//            if (!url.startsWith("http://") && !url.startsWith("https://")) {
//                url = "http://" + url;
//            }
//            if (pattern.matcher(url).find()) {
//                view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CAN_SAVE);
//            } else {
//                view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CANNOT_SAVE);
//            }
//        }
    }
}
