package com.baijiayun.live.ui.activity;

import android.support.annotation.StringRes;

import com.baijiayun.live.ui.speakerlist.item.Switchable;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;
import com.baijiayun.livecore.models.LPAnswerModel;
import com.baijiayun.livecore.models.LPBJTimerModel;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.LPRedPacketModel;
import com.baijiayun.livecore.models.LPRoomForbidChatResult;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.imodels.IMediaControlModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.ppt.PPTView;

/**
 * Created by Shubo on 2017/2/15.
 */

public interface LiveRoomRouterListener {

    LiveRoom getLiveRoom();

    void setLiveRoom(LiveRoom liveRoom);

    void navigateToMain();

    void clearScreen();

    void unClearScreen();

    void switchClearScreen();

    void navigateToMessageInput();

    void navigateToQuickSwitchPPT(int index, int maxIndex);

    void updateQuickSwitchPPTMaxIndex(int index);

    void notifyPageCurrent(int position);

    void navigateToPPTDrawing(boolean isAllowDrawing);

    LPConstants.LPPPTShowWay getPPTShowType();

    void setPPTShowType(LPConstants.LPPPTShowWay type);

    void navigateToUserList();

    void navigateToPPTWareHouse();

    /**
     * 多白板 : 添加白板
     */
    void addPPTWhiteboardPage();

    /**
     * 多白板 ：删除白板
     */
    void deletePPTWhiteboardPage(int pageId);

    /**
     * 翻页
     *
     * @param docId   文档docId
     * @param pageNum 当前docId的页码
     */
    void changePage(String docId, int pageNum);

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

    void doReEnterRoom(boolean checkUnique,boolean reEnterRoom);

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

    void showMessageForbidAllChat(LPRoomForbidChatResult lpRoomForbidChatResult);

    void showMessageTeacherOpenAV(boolean notifyVideoOn, boolean notifyAudioOn, LPConstants.MediaSourceType mediaSourceType);

    void showMessageTeacherCloseAV(boolean notifyVideoClose, boolean notifyAudioClose, LPConstants.MediaSourceType mediaSourceType);

    void showMessageTeacherEnterRoom();

    void showMessageTeacherExitRoom();

    boolean getVisibilityOfShareBtn();

    void changeBackgroundContainerSize(boolean isShrink);

    /**
     * 获得全屏的Item
     *
     * @return 全屏的item
     */
    Switchable getFullScreenItem();

    /**
     * 设置全屏Item
     *
     * @param screenItem 全屏Item
     */
    void setFullScreenItem(Switchable screenItem);

    /**
     * 移除全屏Item到列表
     *
     * @param switchable
     */
    void switchBackToList(Switchable switchable);

    PPTView getPPTView();

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

    void showPPTLoadErrorDialog(int errorCode, String description);

    boolean enableAnimPPTView(boolean b);

    void answerStart(LPAnswerModel model);

    void answerEnd(boolean ended);

    void removeAnswer();

    void showAwardAnimation(String userName);

    void showQuestionAnswer(boolean showFragment);

    void setQuestionAnswerCache(LPAnswerModel lpAnswerModel);

    boolean isQuestionAnswerShow();

    void setRemarksEnable(boolean isEnable);

    void switchRedPacketUI(boolean isShow, LPRedPacketModel lpRedPacketModel);

    void updateRedPacket();

    void showTimer(LPBJTimerModel lpbjTimerModel);

    void showTimer();

    void closeTimer();

    void showEvaluation();

    void dismissEvaDialog();

    void setShouldShowTecSupport(boolean showTecSupport);

    void reportAttention(LPUserModel userModel);
}
