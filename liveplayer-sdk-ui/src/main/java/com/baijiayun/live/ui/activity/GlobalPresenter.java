package com.baijiayun.live.ui.activity;

import android.text.TextUtils;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.utils.JsonObjectUtil;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;
import com.baijiayun.livecore.models.LPJsonModel;
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
            subscriptionOfAnnouncement, subscriptionOfClassSwitch, subscriptionOfAnswerStart, subscriptionOfAnswerEnd;

    private boolean isVideoManipulated = false;

    private int counter = 0;

    private boolean isForbidChatChanged = false;

    //前后台切换
    private boolean isBackstage = false;
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
                });
        subscriptionOfClassEnd = routerListener.getLiveRoom().getObservableOfClassEnd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
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
                .subscribe(aBoolean -> {
                    if (counter == 0) {
                        isForbidChatChanged = aBoolean;
                        counter++;
                        return;
                    }
                    if (isForbidChatChanged == aBoolean) return;
                    isForbidChatChanged = aBoolean;
                    routerListener.showMessageForbidAllChat(aBoolean);
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
                                routerListener.showMessageTeacherOpenAV();
                            } else if (!teacherAudioOn) {
                                routerListener.showMessageTeacherOpenAudio();
                            } else if(!teacherVideoOn){
                                routerListener.showMessageTeacherOpenVideo();
                            }
                        } else if (iMediaModel.isVideoOn()) {
                            if (teacherAudioOn && teacherVideoOn) {
                                routerListener.showMessageTeacherCloseAudio();
                            } else if (!teacherVideoOn) {
                                routerListener.showMessageTeacherOpenVideo();
                            }
                        } else if (iMediaModel.isAudioOn()) {
                            if (teacherAudioOn && teacherVideoOn) {
                                routerListener.showMessageTeacherCloseVideo();
                            } else if (!teacherAudioOn) {
                                routerListener.showMessageTeacherOpenAudio();
                            }
                        } else {
                            routerListener.showMessageTeacherCloseAV();
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
                        if (!routerListener.isTeacherOrAssistant()) {
                            routerListener.onQuizStartArrived(jsonModel);
                        }
                    });
            //中途打开
            subscriptionOfQuizRes = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizRes()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonModel -> {
                        if (!routerListener.isTeacherOrAssistant()) {
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
                            if (!routerListener.isTeacherOrAssistant()) {
                                routerListener.onQuizEndArrived(jsonModel);
                            }
                        }
                    });

            //发答案啦
            subscriptionOfQuizSolution = routerListener.getLiveRoom().getQuizVM().getObservableOfQuizSolution()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonModel -> {
                        if (!routerListener.isTeacherOrAssistant()) {
                            routerListener.onQuizSolutionArrived(jsonModel);
                        }
                    });
            //debug信息
            subscriptionOfDebug = routerListener.getLiveRoom().getObservableOfDebug()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpResRoomDebugModel -> {
                        if (lpResRoomDebugModel != null && lpResRoomDebugModel.data != null) {
                            String commandType = "";
                            if (JsonObjectUtil.isJsonNull(lpResRoomDebugModel.data, "command_type")) {
                                return;
                            }
                            commandType = lpResRoomDebugModel.data.get("command_type").getAsString();
                            if ("logout".equals(commandType)) {
                                routerListener.showError(LPError.getNewError(LPError.CODE_ERROR_LOGIN_KICK_OUT, "您已被踢出房间"));
                            }
                        }
                    });

            subscriptionOfAnswerStart = routerListener.getLiveRoom().getObservableOfAnswerSheetStart()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lpAnswerSheetModel -> {
                        if (!routerListener.isTeacherOrAssistant())
                            routerListener.answerStart(lpAnswerSheetModel);

                    });

            subscriptionOfAnswerEnd = routerListener.getLiveRoom().getObservableOfAnswerSheetEnd()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aBoolean -> {
                        if (!routerListener.isTeacherOrAssistant())
                            routerListener.answerEnd(aBoolean);
                    });
        }
        if (!routerListener.isTeacherOrAssistant()) {
            // 公告变了
            observeAnnouncementChange();
            routerListener.getLiveRoom().requestAnnouncement();
        }
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

    public void unObserveAnnouncementChange() {
        LPRxUtils.dispose(subscriptionOfAnnouncement);
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
}
