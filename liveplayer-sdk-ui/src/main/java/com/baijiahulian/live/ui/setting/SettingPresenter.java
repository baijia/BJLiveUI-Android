package com.baijiahulian.live.ui.setting;

import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.live.ui.rightmenu.RightMenuContract;
import com.baijiahulian.live.ui.utils.RxUtils;
import com.baijiahulian.livecore.context.LPConstants;
import com.baijiahulian.livecore.context.LiveRoom;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;
import com.baijiahulian.livecore.wrapper.LPPlayer;
import com.baijiahulian.livecore.wrapper.LPRecorder;

import java.util.ArrayList;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.baijiahulian.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/3/2.
 */

public class SettingPresenter implements SettingContract.Presenter {

    private SettingContract.View view;
    private LiveRoomRouterListener routerListener;
    private LPRecorder recorder;
    private LPPlayer player;
    private LiveRoom liveRoom;
    private Subscription subscriptionOfForbidAllChat, subscriptionOfMic, subscriptionOfCamera, subscriptionOfForbidRaiseHand,
            subscriptionOfForbidAllAudio, subscriptionOfUpLinkType, subscriptionOfDownLinkType;
    private ArrayList<String> tcpCdnTag;

    public SettingPresenter(SettingContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.routerListener = liveRoomRouterListener;
        recorder = routerListener.getLiveRoom().getRecorder();
        player = routerListener.getLiveRoom().getPlayer();
        liveRoom = routerListener.getLiveRoom();
//        liveRoom.getPartnerConfig().msConfig.get("")
    }

    @Override
    public void subscribe() {
        checkNotNull(routerListener);

        if (recorder.getLinkType() == LPConstants.LPLinkType.TCP)
            view.showUpLinkTCP();
        else
            view.showUpLinkUDP();

        if (player.getLinkType() == LPConstants.LPLinkType.TCP)
            view.showDownLinkTCP();
        else
            view.showDownLinkUDP();

        if (routerListener.getPPTView().isAnimPPTEnable())
            view.showPPTViewTypeAnim();
        else
            view.showPPTViewTypeStatic();

        if (recorder.isAudioAttached())
            view.showMicOpen();
        else
            view.showMicClosed();

        if (recorder.isVideoAttached())
            view.showCameraOpen();
        else
            view.showCameraClosed();

        if (recorder.isBeautyFilterOn())
            view.showBeautyFilterEnable();
        else
            view.showBeautyFilterDisable();

        if (recorder.getVideoDefinition() == LPConstants.LPResolutionType.HIGH)
            view.showDefinitionHigh();
        else
            view.showDefinitionLow();

        if (routerListener.getPPTShowType() == LPConstants.LPPPTShowWay.SHOW_FULL_SCREEN)
            view.showPPTFullScreen();
        else
            view.showPPTOverspread();

        if (recorder.getCameraOrientation()) {
            view.showCameraFront();
        } else {
            view.showCameraBack();
        }

        view.showCameraSwitchStatus(recorder.isVideoAttached());

        if (recorder.getCameraOrientation()) {
            view.showCameraFront();
        } else {
            view.showCameraBack();
        }

        if (liveRoom.getForbidStatus()) {
            view.showForbidden();
        } else {
            view.showNotForbidden();
        }

        if (liveRoom.getForbidRaiseHandStatus()) {
            view.showForbidRaiseHandOn();
        } else {
            view.showForbidRaiseHandOff();
        }

        if (liveRoom.getForbidAllAudioStatus()) {
            view.showForbidAllAudioOn();
        } else {
            view.showForbidAllAudioOff();
        }

        if (liveRoom.getPartnerConfig().PPTAnimationDisable == 0) {
            view.hidePPTShownType();
        }

        subscriptionOfForbidAllChat = liveRoom.getObservableOfForbidAllChatStatus().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) view.showForbidden();
                        else view.showNotForbidden();
                    }
                });
        subscriptionOfMic = liveRoom.getRecorder().getObservableOfMicOn().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) view.showMicOpen();
                        else view.showMicClosed();
                    }
                });
        subscriptionOfCamera = liveRoom.getRecorder().getObservableOfCameraOn().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) view.showCameraOpen();
                        else view.showCameraClosed();
                    }
                });
        subscriptionOfForbidRaiseHand = liveRoom.getObservableOfForbidRaiseHand().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) view.showForbidRaiseHandOn();
                        else view.showForbidRaiseHandOff();
                    }
                });
        subscriptionOfForbidAllAudio = liveRoom.getObservableOfForbidAllAudioStatus()
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (liveRoom.isTeacherOrAssistant()) {
                            //老师或助教，同步静音状态
                            if (aBoolean) view.showForbidAllAudioOn();
                            else view.showForbidAllAudioOff();
                            return;
                        }
                        //学生
                        if (aBoolean && recorder.isAudioAttached()) {
                            //静音
                            recorder.detachAudio();
                        }
//                        else if (!aBoolean) {
//                            //取消静音
//                            if (!recorder.isPublishing())
//                                recorder.publish();
//                            if (!recorder.isAudioAttached())
//                                recorder.attachAudio();
//                            view.showMicOpen();
//                        }
                    }
                });
        subscriptionOfDownLinkType = liveRoom.getPlayer().getObservableOfLinkType().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<LPConstants.LPLinkType>() {
                    @Override
                    public void call(LPConstants.LPLinkType lpLinkType) {
                        if (lpLinkType == LPConstants.LPLinkType.TCP) {
                            view.showDownLinkTCP();
                        } else {
                            view.showDownLinkUDP();
                        }
                    }
                });
        subscriptionOfUpLinkType = liveRoom.getRecorder().getObservableOfLinkType().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<LPConstants.LPLinkType>() {
                    @Override
                    public void call(LPConstants.LPLinkType lpLinkType) {
                        if (lpLinkType == LPConstants.LPLinkType.TCP) {
                            view.showUpLinkTCP();
                        } else {
                            view.showUpLinkUDP();
                        }
                    }
                });

        try {
            tcpCdnTag = (ArrayList<String>) liveRoom.getPartnerConfig().msConfig.get("live_stream_cdn_list");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unSubscribe() {
        RxUtils.unSubscribe(subscriptionOfForbidAllChat);
        RxUtils.unSubscribe(subscriptionOfMic);
        RxUtils.unSubscribe(subscriptionOfCamera);
        RxUtils.unSubscribe(subscriptionOfForbidRaiseHand);
        RxUtils.unSubscribe(subscriptionOfForbidAllAudio);
        RxUtils.unSubscribe(subscriptionOfUpLinkType);
        RxUtils.unSubscribe(subscriptionOfDownLinkType);
    }

    @Override
    public void destroy() {
        routerListener = null;
        recorder = null;
        player = null;
        view = null;
        liveRoom = null;
    }

    @Override
    public void changeMic() {
        switch (routerListener.getLiveRoom().getCurrentUser().getType()) {
            case Teacher:
            case Assistant:
                if (routerListener.getLiveRoom().getGroupId() != 0) {
                    view.showSmallGroupFail();
                    return;
                }
                if (!recorder.isPublishing()) {
                    recorder.publish();
                }
                if (recorder.isAudioAttached()) {
                    recorder.detachAudio();
                } else {
                    routerListener.attachLocalAudio();
                }
                break;
            case Student:
                if (routerListener.getSpeakApplyStatus() != RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                    view.showStudentFail();
                    return;
                }
                if (recorder.isAudioAttached()) {
                    recorder.detachAudio();
                } else {
                    routerListener.attachLocalAudio();
                }
                break;
            case Visitor:
                view.showVisitorFail();
                break;
        }
    }

    @Override
    public void changeCamera() {
        if (routerListener.getLiveRoom().getRoomMediaType() == LPConstants.LPMediaType.Audio) {
            view.showAudioRoomError();
            return;
        }
        switch (routerListener.getLiveRoom().getCurrentUser().getType()) {
            case Teacher:
            case Assistant:
                if (routerListener.getLiveRoom().getGroupId() != 0) {
                    view.showSmallGroupFail();
                    return;
                }
                if (!recorder.isPublishing()) {
                    recorder.publish();
                }
                if (recorder.isVideoAttached()) {
                    routerListener.detachLocalVideo();
                } else {
                    routerListener.attachLocalVideo();
                }
                break;
            case Student:
                if (routerListener.getSpeakApplyStatus() != RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                    view.showStudentFail();
                    return;
                }
                if (recorder.isVideoAttached()) {
                    routerListener.detachLocalVideo();
                } else {
                    routerListener.attachLocalVideo();
                }
                break;
            case Visitor:
                view.showVisitorFail();
                break;
        }
    }

    @Override
    public void changeBeautyFilter() {
        if (recorder.isBeautyFilterOn()) {
            recorder.closeBeautyFilter();
            view.showBeautyFilterDisable();
        } else {
            recorder.openBeautyFilter();
            view.showBeautyFilterEnable();
        }
    }

    @Override
    public void setPPTViewAnim() {
        if (routerListener.enableAnimPPTView(true))
            view.showPPTViewTypeAnim();
        else
            view.showSwitchPPTFail();
    }

    @Override
    public void setPPTViewStatic() {
        if (routerListener.enableAnimPPTView(false))
            view.showPPTViewTypeStatic();
        else
            view.showSwitchPPTFail();

    }

    @Override
    public void setPPTFullScreen() {
        routerListener.setPPTShowType(LPConstants.LPPPTShowWay.SHOW_FULL_SCREEN);
        view.showPPTFullScreen();
    }

    @Override
    public void setPPTOverspread() {
        routerListener.setPPTShowType(LPConstants.LPPPTShowWay.SHOW_COVERED);
        view.showPPTOverspread();
    }

    @Override
    public void setDefinitionLow() {
        recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType.LOW);
        view.showDefinitionLow();
    }

    @Override
    public void setDefinitionHigh() {
        recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType.HIGH);
        view.showDefinitionHigh();
    }

    @Override
    public void setUpLinkTCP() {
        if (recorder.setLinkType(LPConstants.LPLinkType.TCP)) view.showUpLinkTCP();
        else view.showSwitchLinkTypeError();
    }

    @Override
    public void setUpLinkUDP() {
        if (recorder.setLinkType(LPConstants.LPLinkType.UDP)) view.showUpLinkUDP();
        else view.showSwitchLinkTypeError();
    }

    @Override
    public void setDownLinkTCP() {
        if (player.setLinkType(LPConstants.LPLinkType.TCP)) view.showDownLinkTCP();
        else view.showSwitchLinkTypeError();
    }

    @Override
    public void setDownLinkUDP() {
        if (player.setLinkType(LPConstants.LPLinkType.UDP)) view.showDownLinkUDP();
        else view.showSwitchLinkTypeError();
    }

    @Override
    public void switchCamera() {
        if (recorder != null) {
            recorder.switchCamera();
            if (recorder.getCameraOrientation()) {
                view.showCameraFront();
            } else {
                view.showCameraBack();
            }
        }
    }

    @Override
    public void switchForbidStatus() {
        if (liveRoom.getForbidStatus()) {
            liveRoom.requestForbidAllChat(false);
        } else {
            liveRoom.requestForbidAllChat(true);
        }
    }

    @Override
    public boolean isTeacherOrAssistant() {
        return routerListener.isTeacherOrAssistant();
    }

    @Override
    public void switchForbidRaiseHand() {
        liveRoom.requestForbidRaiseHand(!liveRoom.getForbidRaiseHandStatus());
    }

    @Override
    public void switchForbidAllAudio() {
        liveRoom.requestForbidAllAudio(!liveRoom.getForbidAllAudioStatus());
    }

    @Override
    public LPConstants.LPRoomType getRoomType() {
        return liveRoom.getRoomType();
    }

    @Override
    public int getCDNCount() {
        return tcpCdnTag == null ? 0 : tcpCdnTag.size();
    }

    @Override
    public void setUpCDNLink(int order) {
        if(liveRoom.getRecorder().setTcpWithCdn(tcpCdnTag.get(order))){
            view.showUpLinkTCP();
        }else{
            view.showSwitchLinkTypeError();
        }
    }

    @Override
    public void setDownCDNLink(int order) {
        if(liveRoom.getPlayer().setLinkTypeTcpWithCdn(tcpCdnTag.get(order))){
            view.showDownLinkTCP();
        }else{
            view.showSwitchLinkTypeError();
        }
    }
}
