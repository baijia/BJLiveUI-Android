package com.baijiayun.live.ui.menu.rightbotmenu;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * Created by Shubo on 2017/2/16.
 */

public class RightBottomMenuPresenter implements RightBottomMenuContract.Presenter {

    private RightBottomMenuContract.View view;

    private LiveRoomRouterListener liveRoomRouterListener;

    private Disposable subscriptionOfCamera, subscriptionOfMic, subscriptionOfVolume, subscriptionOfClassStart, subscriptionOfForbidAllAudio;

    private boolean isSpeaking;//是否是发言状态


    public RightBottomMenuPresenter(RightBottomMenuContract.View view) {
        this.view = view;
    }

    @Override
    public void changeZoom() {
        if (liveRoomRouterListener.getCurrentScreenOrientation() == 2) {
            view.showZoomIn();
        } else if (liveRoomRouterListener.getCurrentScreenOrientation() == 1) {
            view.showZoomOut();
        }
        liveRoomRouterListener.changeScreenOrientation();
    }

    @Override
    public void changeAudio() {
        if (liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()) {
            liveRoomRouterListener.getLiveRoom().getRecorder().detachAudio();
            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()) {
                liveRoomRouterListener.getLiveRoom().getRecorder().stopPublishing();
            }
        } else {
            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isPublishing()) {
                liveRoomRouterListener.getLiveRoom().getRecorder().publish();
            }
            liveRoomRouterListener.attachLocalAudio();
        }
    }

    @Override
    public void changeVideo() {
        if (liveRoomRouterListener.getLiveRoom().getRoomMediaType() == LPConstants.LPMediaType.Audio) {
            view.showAudioRoomError();
            return;
        }
        if (liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()) {
            liveRoomRouterListener.detachLocalVideo();
            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()) {
                liveRoomRouterListener.getLiveRoom().getRecorder().stopPublishing();
            }
        } else {
            liveRoomRouterListener.attachLocalVideo();
            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isPublishing()) {
                liveRoomRouterListener.getLiveRoom().getRecorder().publish();
            }
        }
    }

    @Override
    public void more(int anchorX, int anchorY) {
        liveRoomRouterListener.showMorePanel(anchorX, anchorY);
    }

    @Override
    public void getSysRotationSetting() {
        /*避免引入sdk代码，这里直接用了数字*/
        if (liveRoomRouterListener.getSysRotationSetting() == 1) {
            //可自由转屏
            view.hideZoom();
        } else if (liveRoomRouterListener.getSysRotationSetting() == 0) {
            //不可自由转屏
            view.showZoom();
            //横屏
            if (liveRoomRouterListener.getCurrentScreenOrientation() == 2) {
                view.showZoomOut();
            } else if (liveRoomRouterListener.getCurrentScreenOrientation() == 1) {
                //竖屏
                view.showZoomIn();
            }
        }
    }

    @Override
    public void setSysRotationSetting() {
        if (liveRoomRouterListener.getSysRotationSetting() == 1) {
            //可自由转屏
            view.hideZoom();
            liveRoomRouterListener.letScreenRotateItself();
        } else if (liveRoomRouterListener.getSysRotationSetting() == 0) {
            //不可自由转屏
            view.showZoom();
            liveRoomRouterListener.forbidScreenRotateItself();
            //横屏
            if (liveRoomRouterListener.getCurrentScreenOrientation() == 2) {
                view.showZoomOut();
            } else if (liveRoomRouterListener.getCurrentScreenOrientation() == 1) {
                //竖屏
                view.showZoomIn();
            }
        }
    }

    @Override
    public void notifySpeakerStatus(boolean isSpeak) {
        isSpeaking = isSpeak;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.liveRoomRouterListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        subscriptionOfCamera = liveRoomRouterListener.getLiveRoom().getRecorder().getObservableOfCameraOn()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> view.showVideoStatus(aBoolean));
        subscriptionOfMic = liveRoomRouterListener.getLiveRoom().getRecorder().getObservableOfMicOn()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> view.showAudioStatus(aBoolean));

        subscriptionOfVolume = liveRoomRouterListener.getLiveRoom().getRecorder().getObservableOfVolume()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(volumeLevel -> {
                    if (liveRoomRouterListener.getLiveRoom().getRecorder() != null && liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached())
                        view.showVolume(volumeLevel);
                });
        if (!liveRoomRouterListener.isTeacherOrAssistant()) {
            liveRoomRouterListener.disableSpeakerMode();
        }
        getSysRotationSetting();
        //横屏
        if (liveRoomRouterListener.getCurrentScreenOrientation() == 2) {
            view.showZoomOut();
        } else if (liveRoomRouterListener.getCurrentScreenOrientation() == 1) {
            //竖屏
            view.showZoomIn();
        }
        subscriptionOfClassStart = liveRoomRouterListener.getLiveRoom().getObservableOfClassStart()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer aVoid) {
                        if ((liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student &&
                                liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi) || liveRoomRouterListener.isTeacherOrAssistant()) {
                            view.enableSpeakerMode();
                        } else {
                            view.disableSpeakerMode();
                        }
                    }
                });
        subscriptionOfForbidAllAudio = liveRoomRouterListener.getLiveRoom().getObservableOfForbidAllAudioStatus()
                .subscribe(aBoolean -> {
                    if (liveRoomRouterListener.isTeacherOrAssistant())return;
                    if (aBoolean) {
                        //静音
                        if (liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()) {
                            liveRoomRouterListener.getLiveRoom().getRecorder().detachAudio();
                        }
                    }
//                        else {
//                            if (isSpeaking) {
//                                //发言状态，自动打开麦克风
//                                if (!liveRoomRouterListener.getLiveRoom().getRecorder().isPublishing()) {
//                                    liveRoomRouterListener.getLiveRoom().getRecorder().publish();
//                                }
//                                liveRoomRouterListener.attachLocalAudio();
//                            }
//                        }

                });
        if (liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student &&
                liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi) {
            if (liveRoomRouterListener.getLiveRoom().isClassStarted()) {
                view.enableSpeakerMode();
            } else {
                view.disableSpeakerMode();
            }
        }

        if (liveRoomRouterListener.getLiveRoom().isAudition())
            view.setAudition();
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfCamera);
        RxUtils.dispose(subscriptionOfMic);
        RxUtils.dispose(subscriptionOfVolume);
        RxUtils.dispose(subscriptionOfClassStart);
        RxUtils.dispose(subscriptionOfForbidAllAudio);

    }

    @Override
    public void destroy() {
        liveRoomRouterListener = null;
        view = null;
    }
}
