package com.baijiayun.live.ui.loading;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.listener.LPLaunchListener;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/2/14.
 */

public class LoadingPresenter implements LoadingContract.Presenter {

    private LPLaunchListener launchListener;
    private LiveRoomRouterListener routerListener;
    private LoadingContract.View view;
    private String code, name, sign, avatar;
    private long roomId;
    private IUserModel userModel;
    private boolean isJoinCode;

    public LoadingPresenter(LoadingContract.View view, String code, String name, String avatar) {
        this.view = view;
        this.code = code;
        this.name = name;
        this.avatar = avatar;
        isJoinCode = true;
        initListener();
    }

    public LoadingPresenter(LoadingContract.View view, long roomId, String sign, IUserModel model) {
        this.view = view;
        this.roomId = roomId;
        this.sign = sign;
        this.userModel = model;
        isJoinCode = false;
        initListener();
    }

    private void initListener() {
        launchListener = new LPLaunchListener() {
            @Override
            public void onLaunchSteps(int i, int i1) {
                if (LoadingPresenter.this.view != null)
                    LoadingPresenter.this.view.showLoadingSteps(i, i1);
            }

            @Override
            public void onLaunchError(LPError lpError) {
                // 如果出错显示ErrorFragment，LoadingFragment被移除了 routerListener为空了
                if (routerListener != null)
                    routerListener.showError(lpError);
            }

            @Override
            public void onLaunchSuccess(LiveRoom liveRoom) {
                if (routerListener == null) return;
                if (liveRoom.isUseWebRTC()) {
                    if (liveRoom.isTeacher()) {
                        if (routerListener.checkTeacherCameraPermission(liveRoom)) {
                            routerListener.setLiveRoom(liveRoom);
                            routerListener.navigateToMain();
                        }
                    } else {
                        LPConstants.LPRoomType roomType = liveRoom.getRoomType();
                        if (roomType != LPConstants.LPRoomType.Multi) {
                            if (routerListener.checkTeacherCameraPermission(liveRoom)) {
                                routerListener.setLiveRoom(liveRoom);
                                routerListener.navigateToMain();
                            }
                        } else {
                            routerListener.setLiveRoom(liveRoom);
                            routerListener.navigateToMain();
                        }
                    }
                } else {
                    routerListener.setLiveRoom(liveRoom);
                    routerListener.navigateToMain();
                }
            }
        };
    }
    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public LPLaunchListener getLaunchListener() {
        return launchListener;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    @Override
    public String getSign() {
        return sign;
    }

    @Override
    public IUserModel getUser() {
        return userModel;
    }

    @Override
    public long getRoomId() {
        return roomId;
    }

    @Override
    public void setLiveRoom(LiveRoom liveRoom) {
        routerListener.setLiveRoom(liveRoom);
    }


    @Override
    public boolean isJoinCode() {
        return isJoinCode;
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }
}
