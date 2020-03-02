package com.baijiayun.live.ui.activity;

import android.text.TextUtils;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.utils.JsonObjectUtil;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.utils.LPLogger;
import com.baijiayun.livecore.utils.LPRxUtils;
import com.baijiayun.livecore.wrapper.impl.LPCameraView;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * Created by Shubo on 2017/5/11.
 */

public class GlobalPresenter implements BasePresenter {

    private LiveRoomRouterListener routerListener;

    private boolean teacherVideoOn, teacherAudioOn;

    private Disposable subscriptionOfClassStart, subscriptionOfClassEnd, subscriptionOfForbidAllStatus,
            subscriptionOfTeacherMedia, subscriptionOfUserIn, subscriptionOfUserOut, subscriptionOfQuizStart,
            subscriptionOfQuizRes, subscriptionOfQuizEnd, subscriptionOfQuizSolution, subscriptionOfDebug,
            subscriptionOfAnnouncement, subscriptionOfClassSwitch, subscriptionOfAnswerStart, subscriptionOfAnswerEnd,
            subscriptionOfTimerStart,subscriptionOfTimerEnd, subscriptionOfIsCloudRecordAllowed,
            subscriptionOfAttentionAlert;

    //红包
    private Disposable mSubscriptionRedPacket, subscriptionOfCloudRecord;

    private boolean isVideoManipulated = false;

    private int counter = 0;

    private boolean isForbidChatChanged = false;
    private boolean isCloudRecording = false;
    private LPCameraView mCameraView;

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        subscriptionOfClassStart = routerListener.getLiveRoom().getObservableOfClassStart()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    routerListener.showMessageClassStart();
                    routerListener.enableStudentSpeakMode();
                    autoRequestCloudRecord();
                });
        subscriptionOfClassEnd = routerListener.getLiveRoom().getObservableOfClassEnd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                    if (routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student &&
                            routerListener.getLiveRoom().isShowEvaluation()) {
                        routerListener.showEvaluation();
                    }
                    routerListener.showMessageClassEnd();
                    teacherVideoOn = false;
                    teacherAudioOn = false;
                });

        // 大小班教室切换
        subscriptionOfClassSwitch = routerListener.getLiveRoom().getObservableOfClassSwitch()
                .delay(new Random().nextInt(2) + 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> routerListener.showClassSwitch());

        subscriptionOfForbidAllStatus = routerListener.getLiveRoom().getObservableOfForbidAllChatStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lpRoomForbidChatResult -> {
                    if (counter == 0) {
                        counter++;
                        return;
                    }
                    routerListener.showMessageForbidAllChat(lpRoomForbidChatResult);
                });

        if (!routerListener.isCurrentUserTeacher()) {

            // 学生监听老师音视频状态
            subscriptionOfTeacherMedia = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaPublish()
                    .filter(new Predicate<IMediaModel>() {
                        @Override
                        public boolean test(IMediaModel iMediaModel) {
                            return !routerListener.isTeacherOrAssistant() && iMediaModel.getUser().getType() == LPConstants.LPUserType.Teacher;
                        }
                    })
                    .throttleLast(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(iMediaModel -> {
                        if (!routerListener.getLiveRoom().isClassStarted()) {
                            return;
                        }
                        if (iMediaModel.isVideoOn() && iMediaModel.isAudioOn()) {
                            if (!teacherVideoOn && !teacherAudioOn) {
                                routerListener.showMessageTeacherOpenAV(true, true, iMediaModel.getMediaSourceType());
                            } else if (!teacherAudioOn) {
                                routerListener.showMessageTeacherOpenAV(false, true, iMediaModel.getMediaSourceType());
                            } else if(!teacherVideoOn){
                                routerListener.showMessageTeacherOpenAV(true, false, iMediaModel.getMediaSourceType());
                            }
                        } else if (iMediaModel.isVideoOn()) {
                            if (teacherAudioOn && teacherVideoOn) {
                                routerListener.showMessageTeacherCloseAV(true, false, iMediaModel.getMediaSourceType());
                            } else if (!teacherVideoOn) {
                                routerListener.showMessageTeacherOpenAV(true, false, iMediaModel.getMediaSourceType());
                            }
                        } else if (iMediaModel.isAudioOn()) {
                            if (teacherAudioOn && teacherVideoOn) {
                                routerListener.showMessageTeacherCloseAV(false, true, iMediaModel.getMediaSourceType());
                            } else if (!teacherAudioOn) {
                                routerListener.showMessageTeacherOpenAV(false, true, iMediaModel.getMediaSourceType());
                            }
                        } else {
                            routerListener.showMessageTeacherCloseAV(false, false, iMediaModel.getMediaSourceType());
                        }
                        setTeacherMedia(iMediaModel);
                    });

            subscriptionOfUserIn = routerListener.getLiveRoom().getObservableOfUserIn().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(iUserInModel -> {
                        if (iUserInModel.getUser().getType() == LPConstants.LPUserType.Teacher) {
                            routerListener.showMessageTeacherEnterRoom();
                        }
                    });

            subscriptionOfUserOut = routerListener.getLiveRoom().getObservableOfUserOut().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        if (TextUtils.isEmpty(s)) return;
                        if (routerListener.getLiveRoom().getTeacherUser() == null) return;
                        if (s.equals(routerListener.getLiveRoom().getTeacherUser().getUserId())) {
                            routerListener.showMessageTeacherExitRoom();
                        }
                    });
            //点名
            routerListener.getLiveRoom().setOnRollCallListener(new OnPhoneRollCallListener() {
                @Override
                public void onRollCall(int time, RollCall rollCallListener) {
                    routerListener.showRollCallDlg(time, rollCallListener);
                }

                @Override
                public void onRollCallTimeOut() {
                    routerListener.dismissRollCallDlg();
                }
            });

            //开始小测
            subscriptionOfQuizStart = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizStart()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonModel -> {
                        if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant()) {
                            routerListener.onQuizStartArrived(jsonModel);
                        }
                    });
            //中途打开
            subscriptionOfQuizRes = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizRes()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonModel -> {
                        if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant()) {
                            if (jsonModel != null && jsonModel.data != null) {
                                String quizId = JsonObjectUtil.getAsString(jsonModel.data, "quiz_id");
                                boolean solutionStatus = false;
                                if (!jsonModel.data.has("solution")) {
                                    //没有solution
                                    solutionStatus = true;
                                } else if (jsonModel.data.getAsJsonObject("solution").entrySet().isEmpty()) {
                                    //"solution":{}
                                    solutionStatus = true;
                                } else if (jsonModel.data.getAsJsonObject("solution").isJsonNull()) {
                                    //"solution":"null"
                                    solutionStatus = true;
                                }
                                boolean endFlag = jsonModel.data.get("end_flag").getAsInt() == 1;
                                //quizid非空、solution是空、没有结束答题 才弹窗
                                if (!TextUtils.isEmpty(quizId) && solutionStatus && !endFlag) {
                                    routerListener.onQuizRes(jsonModel);
                                }
                            }

                        }
                    });
            //结束，只转发h5
            subscriptionOfQuizEnd = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizEnd()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<LPJsonModel>() {
                        @Override
                        public void accept(LPJsonModel jsonModel) {
                            if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant()) {
                                routerListener.onQuizEndArrived(jsonModel);
                            }
                        }
                    });

            //发答案啦
            subscriptionOfQuizSolution = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizSolution()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonModel -> {
                        if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant()) {
                            routerListener.onQuizSolutionArrived(jsonModel);
                        }
                    });
            //debug信息
            subscriptionOfDebug = routerListener.getLiveRoom().getObservableOfDebug()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((lpResRoomDebugModel) -> {
                        if (lpResRoomDebugModel != null && lpResRoomDebugModel.data != null) {
                            String commandType = "";
                            if (JsonObjectUtil.isJsonNull(lpResRoomDebugModel.data, "command_type")) {
                                return;
                            }
                            commandType = lpResRoomDebugModel.data.get("command_type").getAsString();
                            if ("logout".equals(commandType)) {
                                if (lpResRoomDebugModel.data.has("code")) {
                                    int code = lpResRoomDebugModel.data.get("code").getAsInt();
                                    if (code == 2){
                                        String[] tip = routerListener.getLiveRoom().getAuditionTip();
                                        routerListener.showError(LPError.getNewError(LPError.CODE_ERROR_LOGIN_AUDITION, tip[0], tip[1]));
                                    } else {
                                        routerListener.showError(LPError.getNewError(LPError.CODE_ERROR_LOGIN_KICK_OUT, "用户被请出房间"));
                                    }

                                } else {
                                    routerListener.showError(LPError.getNewError(LPError.CODE_ERROR_LOGIN_KICK_OUT, "用户被请出房间"));
                                }
                            }
                        }
                    });

            subscriptionOfAnswerStart = routerListener.getLiveRoom().getToolBoxVM().getObservableOfAnswerStart()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpAnswerModel -> {
                        if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant())
                            routerListener.answerStart(lpAnswerModel);
                    });

            subscriptionOfAnswerEnd = routerListener.getLiveRoom().getToolBoxVM().getObservableOfAnswerEnd()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpAnswerEndModel -> {
                        if (!routerListener.isTeacherOrAssistant() && !routerListener.isGroupTeacherOrAssistant())
                            routerListener.answerEnd(!lpAnswerEndModel.isRevoke);
                    });
            subscriptionOfTimerStart = routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerStart()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpbjTimerModel -> {
                        if (!routerListener.isCurrentUserTeacher()) {
                            routerListener.showTimer(lpbjTimerModel);
                        }
                    });
            subscriptionOfTimerEnd = routerListener.getLiveRoom().getToolBoxVM().getObservableOfBJTimerEnd()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(b -> {
                        if (!routerListener.isCurrentUserTeacher()) {
                            routerListener.closeTimer();
                        }
                    });
            subscriptionOfAttentionAlert = routerListener.getLiveRoom().getToolBoxVM().getObservableOfAttentionAlert()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpAttentionAlertModel -> routerListener.showMessage(lpAttentionAlertModel.content));
        }
        if (!routerListener.isTeacherOrAssistant()) {
            // 公告变了
            observeAnnouncementChange();
            routerListener.getLiveRoom().requestAnnouncement();
        }

        //红包
        mSubscriptionRedPacket = routerListener.getLiveRoom().getObservableOfRedPacket()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lpRedPacketModel -> routerListener.switchRedPacketUI(true, lpRedPacketModel));

        subscriptionOfCloudRecord = routerListener.getLiveRoom().getObservableOfCloudRecordStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    isCloudRecording = aBoolean;
                });
    }

    public void observeAnnouncementChange() {
        if (routerListener.isCurrentUserTeacher()) return;
        subscriptionOfAnnouncement = routerListener.getLiveRoom().getObservableOfAnnouncementChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(iAnnouncementModel -> {
                    if (!TextUtils.isEmpty(iAnnouncementModel.getLink()) ||
                            !TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                        routerListener.navigateToAnnouncement();
                    }
                });
    }

    void setTeacherMedia(IMediaModel media) {
        teacherVideoOn = media.isVideoOn();
        teacherAudioOn = media.isAudioOn();
    }

    public boolean isVideoManipulated() {
        return isVideoManipulated;
    }

    public void setVideoManipulated(boolean videoManipulated) {
        isVideoManipulated = videoManipulated;
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfClassStart);
        RxUtils.dispose(subscriptionOfClassEnd);
        RxUtils.dispose(subscriptionOfForbidAllStatus);
        RxUtils.dispose(subscriptionOfTeacherMedia);
        RxUtils.dispose(subscriptionOfUserIn);
        RxUtils.dispose(subscriptionOfUserOut);
        RxUtils.dispose(subscriptionOfQuizStart);
        RxUtils.dispose(subscriptionOfQuizRes);
        RxUtils.dispose(subscriptionOfQuizEnd);
        RxUtils.dispose(subscriptionOfQuizSolution);
        RxUtils.dispose(subscriptionOfDebug);
        RxUtils.dispose(subscriptionOfAnnouncement);
        RxUtils.dispose(subscriptionOfClassSwitch);
        RxUtils.dispose(subscriptionOfAnswerStart);
        RxUtils.dispose(subscriptionOfAnswerEnd);
        RxUtils.dispose(mSubscriptionRedPacket);
        RxUtils.dispose(subscriptionOfTimerStart);
        RxUtils.dispose(subscriptionOfTimerEnd);
        RxUtils.dispose(subscriptionOfIsCloudRecordAllowed);
        RxUtils.dispose(subscriptionOfAttentionAlert);
        RxUtils.dispose(subscriptionOfCloudRecord);
    }

    @Override
    public void destroy() {
        unSubscribe();
        routerListener = null;
    }

    public void switchBackstage(boolean isBackstage) {
        if (routerListener.getLiveRoom().getRecorder() == null) return;
        if (isBackstage) {
            //后台
            if (routerListener.getLiveRoom().getRecorder().isPublishing()) {
                LPLogger.d("GlobalPresenter", "switchBackstage : stopPublishing");
                mCameraView = routerListener.getLiveRoom().getRecorder().getCameraView();
                routerListener.getLiveRoom().getRecorder().stopPublishing();
            }
        } else {
            //前台
            if (mCameraView != null
                    && !routerListener.getLiveRoom().getRecorder().isPublishing()) {
                LPLogger.d("GlobalPresenter", "switchBackstage : startPublishing");
                if (routerListener.getLiveRoom().isUseWebRTC()) {
                    routerListener.getLiveRoom().getRecorder().setPreview(mCameraView);
                }
                routerListener.getLiveRoom().getRecorder().publish();
                mCameraView = null;
            }
        }
    }

    /**
     * 自动开启云端录制
     */
    private void autoRequestCloudRecord(){
        //老师/助教进教室后，开启自动录制主动发cloud_record广播
        if (routerListener.getLiveRoom().getAutoStartCloudRecordStatus() == 1) {
            doRequestCloudRecord();
        }
    }

    void doRequestCloudRecord() {
        if (routerListener.getLiveRoom().isTeacherOrAssistant() && !isCloudRecording) {
            LPRxUtils.dispose(subscriptionOfIsCloudRecordAllowed);
            subscriptionOfIsCloudRecordAllowed = routerListener.getLiveRoom().requestIsCloudRecordAllowed()
                    .subscribe(lpCheckRecordStatusModel -> {
                        if (lpCheckRecordStatusModel.recordStatus == 1) {
                            routerListener.getLiveRoom().requestCloudRecord(true);
                        } else {
                            routerListener.showMessage(lpCheckRecordStatusModel.reason);
                        }
                    });
        }
    }
}


