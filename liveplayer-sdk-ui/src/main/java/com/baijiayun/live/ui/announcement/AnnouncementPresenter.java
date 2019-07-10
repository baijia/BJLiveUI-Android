package com.baijiayun.live.ui.announcement;

import android.text.TextUtils;

import com.baijiayun.live.ui.activity.GlobalPresenter;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by Shubo on 2017/4/19.
 */

public class AnnouncementPresenter implements AnnouncementContract.Presenter {

    private AnnouncementContract.View view;

    private LiveRoomRouterListener routerListener;

    private String content;

    private String link;

    private Disposable subscriptionOfAnnouncementChange;

    private Pattern pattern;

    private GlobalPresenter globalPresenter;

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
    public void subscribe() {
        subscriptionOfAnnouncementChange = routerListener.getLiveRoom().getObservableOfAnnouncementChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<IAnnouncementModel>() {
                    @Override
                    public void accept(IAnnouncementModel iAnnouncementModel) {
                        content = iAnnouncementModel.getContent();
                        link = iAnnouncementModel.getLink();
                        view.showAnnouncementText(content);
                        view.showAnnouncementUrl(link);
                        checkInput(content, link);
                    }
                });

        if (routerListener.isTeacherOrAssistant()) {
            view.showTeacherView();
        } else {
            view.showStudentView();
        }

        routerListener.getLiveRoom().requestAnnouncement();
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
        routerListener.getLiveRoom().changeRoomAnnouncement(text, url);
    }

    @Override
    public void checkInput(String text, String url) {
        if (text.equals(content) && link.equals(url)) {
            view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_SAVED);
            return;
        }
        if (TextUtils.isEmpty(url)) {
            view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CAN_SAVE);
        } else {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            if (pattern.matcher(url).find()) {
                view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CAN_SAVE);
            } else {
                view.showCheckStatus(AnnouncementContract.STATUS_CHECKED_CANNOT_SAVE);
            }
        }
    }
}
