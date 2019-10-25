package com.baijiayun.live.ui.setting;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.menu.rightmenu.RightMenuContract;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.models.LPRoomForbidChatResult;
import com.baijiayun.livecore.wrapper.LPPlayer;
import com.baijiayun.livecore.wrapper.LPRecorder;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/3/2.
 */

public class SettingPresenter implements SettingContract.Presenter {

    private SettingContract.View view;
    private LiveRoomRouterListener routerListener;
    private LPRecorder recorder;
    private LPPlayer player;
    private LiveRoom liveRoom;
    private Disposable subscriptionOfForbidAllChat, subscriptionOfMic, subscriptionOfCamera, subscriptionOfForbidRaiseHand,
            subscriptionOfUpLinkType, subscriptionOfDownLinkType, subscriptionOfForbidAllAudio;
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

        view.showDefinition(recorder.getMaxVideoDefinition());
        if (recorder.getVideoDefinition() == LPConstants.LPResolutionType.HIGH)
            view.showDefinitionHigh(null);
        else if(recorder.getVideoDefinition() == LPConstants.LPResolutionType._720)
            view.showDefinition_720(null);
        else if(recorder.getVideoDefinition() == LPConstants.LPResolutionType._1080)
            view.showDefinition_1080(null);
        else
            view.showDefinitionLow(null);
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

        if (liveRoom.getForbidStatus(LPConstants.LPForbidChatType.TYPE_GROUP)) {
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
                .subscribe(new Consumer<LPRoomForbidChatResult>() {
                    @Override
                    public void accept(LPRoomForbidChatResult result) {
                        if (result.type == LPConstants.LPForbidChatType.TYPE_ALL && liveRoom.getGroupId() != 0)
                            return;
                        if (result.isForbid) view.showForbidden();
                        else view.showNotForbidden();
                    }
                });
        subscriptionOfMic = liveRoom.getRecorder().getObservableOfMicOn().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (aBoolean) view.showMicOpen();
                        else view.showMicClosed();
                    }
                });
        subscriptionOfCamera = liveRoom.getRecorder().getObservableOfCameraOn().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (aBoolean) view.showCameraOpen();
                        else view.showCameraClosed();
                    }
                });
        subscriptionOfForbidRaiseHand = liveRoom.getObservableOfForbidRaiseHand().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (aBoolean) view.showForbidRaiseHandOn();
                        else view.showForbidRaiseHandOff();
                    }
                });
        subscriptionOfForbidAllAudio = liveRoom.getObservableOfForbidAllAudioStatus()
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
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
                .subscribe(new Consumer<LPConstants.LPLinkType>() {
                    @Override
                    public void accept(LPConstants.LPLinkType lpLinkType) {
                        if (lpLinkType == LPConstants.LPLinkType.TCP) {
                            view.showDownLinkTCP();
                        } else {
                            view.showDownLinkUDP();
                        }
                    }
                });
        subscriptionOfUpLinkType = liveRoom.getRecorder().getObservableOfLinkType().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LPConstants.LPLinkType>() {
                    @Override
                    public void accept(LPConstants.LPLinkType lpLinkType) {
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
        RxUtils.dispose(subscriptionOfForbidAllChat);
        RxUtils.dispose(subscriptionOfMic);
        RxUtils.dispose(subscriptionOfCamera);
        RxUtils.dispose(subscriptionOfForbidRaiseHand);
        RxUtils.dispose(subscriptionOfUpLinkType);
        RxUtils.dispose(subscriptionOfDownLinkType);
        RxUtils.dispose(subscriptionOfForbidAllAudio);
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

    private void switchDefinition(LPError lpError, LPConstants.LPResolutionType resolutionType) {
        switch (resolutionType) {
            case LOW:
                view.showDefinitionLow(lpError);
                break;
            case HIGH:
                view.showDefinitionHigh(lpError);
                break;
            case _720:
                view.showDefinition_720(lpError);
                break;
            case _1080:
                view.showDefinition_1080(lpError);
                break;
            default:
                view.showDefinitionLow(lpError);
                break;
        }
    }

    @Override
    public void setDefinitionLow() {
        LPError lpError = recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType.LOW);
        switchDefinition(lpError,recorder.getVideoDefinition());
    }

    @Override
    public void setDefinitionHigh() {
        LPError lpError = recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType.HIGH);
        switchDefinition(lpError,recorder.getVideoDefinition());
    }

    @Override
    public void setDefinition_720() {
        LPError lpError = recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType._720);
        switchDefinition(lpError,recorder.getVideoDefinition());
    }

    @Override
    public void setDefinition_1080() {
        LPError lpError = recorder.setCaptureVideoDefinition(LPConstants.LPResolutionType._1080);
        switchDefinition(lpError,recorder.getVideoDefinition());
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
        if (!canSwitchForbid()) {
            view.showSwitchForbid();
            return;
        }
        if (liveRoom.getForbidStatus(LPConstants.LPForbidChatType.TYPE_GROUP)) {
            liveRoom.requestForbidAllChat(false);
        } else {
            liveRoom.requestForbidAllChat(true);
        }
    }

    @Override
    public boolean isTeacherOrAssistant() {
        return routerListener.isTeacherOrAssistant() || routerListener.isGroupTeacherOrAssistant();
    }

    @Override
    public boolean canSwitchForbid() {
        return !(liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Assistant && liveRoom.getAdminAuth() != null
                && !liveRoom.getAdminAuth().forbidAndKick);
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

    @Override
    public boolean isUseWebRTC() {
        return routerListener.getLiveRoom().isUseWebRTC();
    }
}
