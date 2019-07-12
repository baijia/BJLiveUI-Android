package com.baijiayun.live.ui.activity;

import android.support.annotation.StringRes;
import android.widget.RelativeLayout;

import com.baijiayun.live.ui.ppt.MyPPTView;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;
import com.baijiayun.livecore.models.LPAnswerModel;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.LPAnswerSheetModel;
import com.baijiayun.livecore.models.LPRedPacketModel;
import com.baijiayun.livecore.models.imodels.IMediaControlModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2017/2/15.
 */

public interface LiveRoomRouterListener {

    LiveRoom getLiveRoom();

    void setLiveRoom(LiveRoom liveRoom);

    void navigateToMain();

    void clearScreen();

    void unClearScreen();

    void navigateToMessageInput();

    void navigateToQuickSwitchPPT(int index, int maxIndex);

    void updateQuickSwitchPPTMaxIndex(int index);

    void notifyPageCurrent(int position);

    void navigateToPPTDrawing(boolean isAllowDrawing);

    LPConstants.LPPPTShowWay getPPTShowType();

    void setPPTShowType(LPConstants.LPPPTShowWay type);

    void navigateToUserList();

    void navigateToPPTWareHouse();

    void disableSpeakerMode();

    void enableSpeakerMode();

    void showMorePanel(int anchorX, int anchorY);

    void navigateToShare();

    void navigateToAnnouncement();

    void navigateToCloudRecord(boolean recordStatus);

    void navigateToHelp();

    void navigateToSetting();

    boolean isTeacherOrAssistant();

    boolean isGroupTeacherOrAssistant();

    void attachLocalVideo();

    void detachLocalVideo();

    boolean isPPTMax();

    void clearPPTAllShapes();

    void changeScreenOrientation();

    int getCurrentScreenOrientation();

    int getSysRotationSetting();

    //允许自由转屏
    void letScreenRotateItself();

    //不允许自由转屏
    void forbidScreenRotateItself();

    void showBigChatPic(String url);

    void sendImageMessage(String path);

    void showMessage(String message);

    void showMessage(@StringRes int strRes);

    void saveTeacherMediaStatus(IMediaModel model);

    void showSavePicDialog(byte[] bmpArray);

    void realSaveBmpToFile(byte[] bmpArray);

    void doReEnterRoom(boolean checkUnique);

    void doHandleErrorNothing();

    void showError(LPError error);

    boolean canStudentDraw();

    boolean isCurrentUserTeacher();

    // 学生是否操作过老师视频
    boolean isVideoManipulated();

    void setVideoManipulated(boolean b);

    int getSpeakApplyStatus();

    void showMessageClassEnd();

    void showMessageClassStart();

    void showMessageForbidAllChat(boolean isOn);

    void showMessageTeacherOpenAudio();

    void showMessageTeacherOpenVideo();

    void showMessageTeacherOpenAV();

    void showMessageTeacherCloseAV();

    void showMessageTeacherCloseAudio();

    void showMessageTeacherCloseVideo();

    void showMessageTeacherEnterRoom();

    void showMessageTeacherExitRoom();

    boolean getVisibilityOfShareBtn();

    void changeBackgroundContainerSize(boolean isShrink);

    android.view.View removeFullScreenView();

    RelativeLayout getBackgroundContainer();

    void setFullScreenView(android.view.View view);

    MyPPTView getPPTView();

    void showRollCallDlg(int time, OnPhoneRollCallListener.RollCall rollCallListener);

    void dismissRollCallDlg();

    /*小测v2*/
    void onQuizStartArrived(LPJsonModel jsonModel);

    void onQuizEndArrived(LPJsonModel jsonModel);

    void onQuizSolutionArrived(LPJsonModel jsonModel);

    void onQuizRes(LPJsonModel jsonModel);

    void dismissQuizDlg();

    boolean checkCameraPermission();

    boolean checkTeacherCameraPermission(LiveRoom liveRoom);

    void attachLocalAudio();

    void notifyPPTResumeInSpeakers();

    void setIsStopPublish(boolean b);

    void notifyFullScreenPresenterStatusChange(String id, boolean isPresenter);

    void showForceSpeakDlg(IMediaControlModel iMediaControlModel);

    void showSpeakInviteDlg(int invite); //0 取消 1邀请

    LPConstants.LPRoomType getRoomType();

    void showHuiyinDebugPanel(); //弹出debug面板

    void showStreamDebugPanel();

    void showDebugBtn();

    void showCopyLogDebugPanel();

    void enableStudentSpeakMode();

    void showClassSwitch();

    void onPrivateChatUserChange(IUserModel iUserModel);

    IUserModel getPrivateChatUser();

    boolean isPrivateChat();

    void changeNewChatMessageReminder(boolean isNeedShow, int newMessageNumber);

    void showNoSpeakers();

    void showHavingSpeakers();

    boolean isPPTInSpeakersList();

    void showOptionDialog();

    void showPPTLoadErrorDialog(int errorCode, String description);

    boolean enableAnimPPTView(boolean b);

    void answerStart(LPAnswerModel model);

    void answerEnd(boolean ended);

    void showAwardAnimation(String userName);

    void showQuestionAnswer(boolean showFragment);

    boolean isQuestionAnswerShow();

    void setRemarksEnable(boolean isEnable);

    void switchRedPacketUI(boolean isShow, LPRedPacketModel lpRedPacketModel);

    void updateRedPacket();
}
