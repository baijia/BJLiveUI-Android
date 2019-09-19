package com.baijiayun.live.ui.cloudrecord;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by wangkangfei on 17/5/8.
 */

public class CloudRecordPresenter implements CloudRecordContract.Presenter {
    private LiveRoomRouterListener liveRoomRouterListener;
    private Disposable subscriptionOfCloudRecord;

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.liveRoomRouterListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        subscriptionOfCloudRecord = liveRoomRouterListener.getLiveRoom().getObservableOfCloudRecordStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        if (liveRoomRouterListener.isTeacherOrAssistant()) {
                            liveRoomRouterListener.navigateToCloudRecord(true);
                        } else {
                            liveRoomRouterListener.navigateToCloudRecord(false);
                        }
                    } else {
                        liveRoomRouterListener.navigateToCloudRecord(false);
                    }
                });
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfCloudRecord);
    }

    @Override
    public void destroy() {
        liveRoomRouterListener = null;
    }

    @Override
    public void cancelCloudRecord() {
        liveRoomRouterListener.getLiveRoom().requestCloudRecord(false);
    }
    @Override
    public boolean canOperateCloudRecord() {
        return !(liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Assistant &&
                liveRoomRouterListener.getLiveRoom().getAdminAuth()!=null && !liveRoomRouterListener.getLiveRoom().getAdminAuth().cloudRecord);
    }
}
