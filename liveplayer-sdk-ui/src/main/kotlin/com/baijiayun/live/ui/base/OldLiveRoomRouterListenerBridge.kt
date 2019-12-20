package com.baijiayun.live.ui.base

import com.baijiayun.live.ui.activity.LiveRoomRouterListener
import com.baijiayun.live.ui.menu.rightmenu.RightMenuContract
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.listener.OnPhoneRollCallListener
import com.baijiayun.livecore.models.*
import com.baijiayun.livecore.models.imodels.IMediaControlModel
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.ppt.PPTView
import com.baijiayun.livecore.wrapper.LPRecorder

/**
 * Created by Shubo on 2019-10-16.
 */
class OldLiveRoomRouterListenerBridge(private val routerViewModel: RouterViewModel) : LiveRoomRouterListener {

    override fun getLiveRoom(): LiveRoom {
        return routerViewModel.liveRoom
    }

    fun setMainVideo2FullScreen(isMainVideo2FullScreen: Boolean) {
        routerViewModel.isMainVideo2FullScreen.value = isMainVideo2FullScreen
    }

    fun notifyCloseRemoteVideo(remoteItem: RemoteItem?) {
        routerViewModel.notifyCloseRemoteVideo.value = remoteItem
    }

    override fun updateQuickSwitchPPTMaxIndex(index: Int) {
    }

    override fun navigateToQuickSwitchPPT(index: Int, maxIndex: Int) {
    }

    override fun setLiveRoom(liveRoom: LiveRoom?) {}

    override fun navigateToMain() {}

    override fun clearScreen() {
    }

    override fun unClearScreen() {
    }

    override fun switchClearScreen() {
        routerViewModel.clearScreen.value = if (routerViewModel.clearScreen.value == null) true else !routerViewModel.clearScreen.value!!
    }

    override fun navigateToMessageInput() {

    }

    override fun notifyPageCurrent(position: Int) {
        routerViewModel.notifyPPTPageCurrent.value = position
    }

    override fun navigateToPPTDrawing(isAllowDrawing: Boolean) {
        routerViewModel.actionNavigateToPPTDrawing.postValue(isAllowDrawing)
    }

    override fun getPPTShowType(): LPConstants.LPPPTShowWay = routerViewModel.pptViewData.value?.pptShowWay
            ?: LPConstants.LPPPTShowWay.SHOW_COVERED

    override fun setPPTShowType(type: LPConstants.LPPPTShowWay?) {
        routerViewModel.pptViewData.value?.pptShowWay = type
    }

    override fun navigateToUserList() {
    }

    override fun navigateToPPTWareHouse() {
    }

    override fun addPPTWhiteboardPage() {
        routerViewModel.addPPTWhiteboardPage.value = Unit
    }

    override fun deletePPTWhiteboardPage(pageId: Int) {
        routerViewModel.deletePPTWhiteboardPage.value = pageId
    }

    override fun changePage(docId: String?, pageNum: Int) {
        routerViewModel.changePPTPage.value = Pair(docId, pageNum)
    }

    override fun disableSpeakerMode() {
    }

    override fun enableSpeakerMode() {
    }

    override fun showMorePanel(anchorX: Int, anchorY: Int) {
    }

    override fun navigateToShare() {
    }

    override fun navigateToAnnouncement() {
    }

    override fun navigateToCloudRecord(recordStatus: Boolean) {
    }

    override fun navigateToHelp() {
    }

    override fun navigateToSetting() {
    }

    override fun isTeacherOrAssistant(): Boolean = liveRoom.isTeacherOrAssistant

    override fun isGroupTeacherOrAssistant(): Boolean = liveRoom.isGroupTeacherOrAssistant


    override fun attachLocalVideo() {
        routerViewModel.actionWithLocalAVideo.value = true to liveRoom.getRecorder<LPRecorder>().isAudioAttached
    }

    override fun attachLocalAudio() {
        routerViewModel.actionWithAttachLocalAudio.value = true
    }

    override fun detachLocalVideo() {
        routerViewModel.actionWithLocalAVideo.value = false to liveRoom.getRecorder<LPRecorder>().isAudioAttached
    }

    override fun isPPTMax(): Boolean = false

    override fun clearPPTAllShapes() {
    }

    override fun changeScreenOrientation() {
    }

    override fun getCurrentScreenOrientation(): Int = 90

    override fun getSysRotationSetting(): Int = 0

    override fun letScreenRotateItself() {
    }

    override fun forbidScreenRotateItself() {
    }

    override fun showBigChatPic(url: String?) {

    }

    override fun sendImageMessage(path: String?) {
        routerViewModel.sendPictureMessage.value = path
    }

    override fun showMessage(message: String?) {
    }

    override fun showMessage(strRes: Int) {
    }

    override fun saveTeacherMediaStatus(model: IMediaModel?) {
    }

    override fun showSavePicDialog(bmpArray: ByteArray?) {
        routerViewModel.showSavePicDialog.value = bmpArray
    }

    override fun realSaveBmpToFile(bmpArray: ByteArray?) {
        routerViewModel.saveChatPictureToGallery.value = bmpArray
    }

    override fun doReEnterRoom(checkUnique: Boolean) {
    }

    override fun doHandleErrorNothing() {
    }

    override fun showError(error: LPError?) {
    }

    override fun canStudentDraw(): Boolean {
        return liveRoom.isTeacherOrAssistant || routerViewModel.pptViewData.value?.isCurrentMaxPage ?: false
    }

    override fun isCurrentUserTeacher(): Boolean = liveRoom.currentUser.type == LPConstants.LPUserType.Teacher


    override fun isVideoManipulated(): Boolean = false

    override fun setVideoManipulated(b: Boolean) {
    }

    override fun getSpeakApplyStatus(): Int = routerViewModel.speakApplyStatus.value
            ?: RightMenuContract.STUDENT_SPEAK_APPLY_NONE

    override fun showMessageClassEnd() {
    }

    override fun showMessageClassStart() {
    }

    override fun showMessageForbidAllChat(lpRoomForbidChatResult: LPRoomForbidChatResult?) {
    }

    override fun showMessageTeacherOpenAV(notifyVideoOn: Boolean, notifyAudioOn: Boolean, mediaSourceType: LPConstants.MediaSourceType?) {
    }

    override fun showMessageTeacherCloseAV(notifyVideoClose: Boolean, notifyAudioClose: Boolean, mediaSourceType: LPConstants.MediaSourceType?) {
    }


    override fun showMessageTeacherEnterRoom() {
    }

    override fun showMessageTeacherExitRoom() {
    }

    override fun getVisibilityOfShareBtn(): Boolean = false

    override fun changeBackgroundContainerSize(isShrink: Boolean) {
    }

    override fun getFullScreenItem(): Switchable? = routerViewModel.switch2FullScreen.value

    override fun setFullScreenItem(screenItem: Switchable?) {
        if (screenItem?.view is MyPadPPTView && (screenItem as MyPadPPTView).isEditable) {
            routerViewModel.changeDrawing.value = true
        }
        routerViewModel.switch2FullScreen.value = screenItem
    }

    override fun switchBackToList(switchable: Switchable?) {
        if (routerViewModel.isMainVideo2FullScreen.value == true) {
            if (routerViewModel.liveRoom.teacherUser != null && switchable?.identity == routerViewModel.liveRoom.teacherUser.userId) {
                routerViewModel.switch2MainVideo.value = switchable
            }else if (routerViewModel.liveRoom.teacherUser == null) {
                //老师离开教室
                routerViewModel.switch2MainVideo.value = switchable
            } else {
                val pptview = routerViewModel.pptViewData.value as MyPadPPTView
                if (pptview.pptStatus != MyPadPPTView.PPTStatus.MainVideo) {
                    pptview.switchBackToList()
                }
                routerViewModel.switch2BackList.value = switchable
            }
        } else {
            if (routerViewModel.liveRoom.teacherUser != null && switchable?.identity == routerViewModel.liveRoom.teacherUser.userId) {
                val pptview = routerViewModel.pptViewData.value as MyPadPPTView
                if (pptview.pptStatus != MyPadPPTView.PPTStatus.BackList) {
                    pptview.switchBackToList()
                }
                routerViewModel.switch2MainVideo.value = switchable
            } else {
                routerViewModel.switch2BackList.value = switchable
            }
        }
    }

    override fun getPPTView(): PPTView? = routerViewModel.pptViewData.value

    override fun showRollCallDlg(time: Int, rollCallListener: OnPhoneRollCallListener.RollCall?) {
    }

    override fun dismissRollCallDlg() {
    }

    override fun onQuizStartArrived(jsonModel: LPJsonModel?) {
    }

    override fun onQuizEndArrived(jsonModel: LPJsonModel?) {
    }

    override fun onQuizSolutionArrived(jsonModel: LPJsonModel?) {
    }

    override fun onQuizRes(jsonModel: LPJsonModel?) {

    }

    override fun dismissQuizDlg() {
        routerViewModel.quizStatus.postValue(RouterViewModel.QuizStatus.CLOSE to LPJsonModel())
    }

    override fun checkCameraPermission() = true

    override fun checkTeacherCameraPermission(liveRoom: LiveRoom?) = true


    override fun showForceSpeakDlg(iMediaControlModel: IMediaControlModel?) {
    }

    override fun showSpeakInviteDlg(invite: Int) {
    }

    override fun getRoomType(): LPConstants.LPRoomType = LPConstants.LPRoomType.Multi

    override fun showHuiyinDebugPanel() {

    }

    override fun showStreamDebugPanel() {

    }

    override fun showDebugBtn() {

    }

    override fun showCopyLogDebugPanel() {

    }

    override fun enableStudentSpeakMode() {

    }

    override fun showClassSwitch() {

    }

    override fun onPrivateChatUserChange(iUserModel: IUserModel?) {

    }

    override fun getPrivateChatUser(): IUserModel = liveRoom.currentUser

    override fun isPrivateChat(): Boolean = false

    override fun changeNewChatMessageReminder(isNeedShow: Boolean, newMessageNumber: Int) {

    }

    override fun showNoSpeakers() {

    }

    override fun showHavingSpeakers() {

    }

    override fun showPPTLoadErrorDialog(errorCode: Int, description: String?) {

    }

    override fun enableAnimPPTView(b: Boolean): Boolean = routerViewModel.pptViewData.value?.setAnimPPTEnable(b)
            ?: false

    override fun answerStart(model: LPAnswerModel?) {

    }

    override fun answerEnd(ended: Boolean) {
        routerViewModel.answerEnd.value = ended
    }

    override fun removeAnswer() {
        routerViewModel.removeAnswer.value = Unit
    }

    override fun showAwardAnimation(userName: String?) {
    }

    override fun showQuestionAnswer(showFragment: Boolean) {
    }

    override fun setQuestionAnswerCahce(lpAnswerModel: LPAnswerModel?) {
    }

    override fun isQuestionAnswerShow(): Boolean = true

    override fun setRemarksEnable(isEnable: Boolean) {
    }

    override fun switchRedPacketUI(isShow: Boolean, lpRedPacketModel: LPRedPacketModel?) {
        lpRedPacketModel?.let {
            routerViewModel.action2RedPacketUI.value = isShow to it
        }
    }

    override fun updateRedPacket() {
        routerViewModel.action2RedPacketUI.value = false to LPRedPacketModel("", 0)
    }

    override fun showEvaluation() {
    }

    override fun dismissEvaDialog() {
        routerViewModel.showEvaDlg.postValue(false)
    }

    override fun showTimer(lpbjTimerModel: LPBJTimerModel?) {
    }

    override fun showTimer() {
    }

    override fun closeTimer() {
        routerViewModel.showTimer.value = false to LPBJTimerModel()
    }

    fun requestAward(number: String?) {
        val awardRecord = routerViewModel.awardRecord
        val count = if (awardRecord[number] != null) awardRecord[number] else 0
        if (number == null || count == null) {
            return
        }
        awardRecord[number] = count.plus(1)
        routerViewModel.liveRoom.requestAward(number, awardRecord)
    }
}