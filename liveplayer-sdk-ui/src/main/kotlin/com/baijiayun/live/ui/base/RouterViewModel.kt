package com.baijiayun.live.ui.base

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.*
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.ppt.PPTView

/**
 * activity与fragment、fragment之间的通信，不包含业务逻辑
 * Created by Shubo on 2019-10-10.
 */
class RouterViewModel : ViewModel() {
    enum class QuizStatus{
        NOT_INIT,START,RES,END,SOLUTION,CLOSE
    }
    lateinit var liveRoom: LiveRoom

    val action2PPTError = MutableLiveData<Int>()

    var checkUnique = true

    var showTechSupport = true

    val showEvaDlg = MutableLiveData<Boolean>()

    val quizStatus = MutableLiveData<Pair<QuizStatus, LPJsonModel>>()

    val showTimer = MutableLiveData<Pair<Boolean, LPBJTimerModel>>()

    val answerStart = MutableLiveData<LPAnswerModel>()

    val answerEnd = MutableLiveData<Boolean>()

    val removeAnswer = MutableLiveData<Unit>()

    val actionWithAttachLocalAudio = MutableLiveData<Boolean>()

    val notifyLocalPlayableChanged = MutableLiveData<Pair<Boolean, Boolean>>()

    val actionWithLocalAVideo = MutableLiveData<Pair<Boolean, Boolean>>()

    val notifyCloseRemoteVideo = MutableLiveData<RemoteItem>()

    val actionExit = MutableLiveData<Unit>()

    val actionNavigateToMain = MutableLiveData<Boolean>()

    val actionShowQuickSwitchPPT = MutableLiveData<Bundle>()

    val actionChangePPT2Page = MutableLiveData<Int>()

    val notifyPPTPageCurrent = MutableLiveData<Int>()

    val addPPTWhiteboardPage = MutableLiveData<Unit>()

    val deletePPTWhiteboardPage = MutableLiveData<Int>()

    val changePPTPage = MutableLiveData<Pair<String?, Int>>()

    val action2Share = MutableLiveData<Unit>()

    val isShowShare = MutableLiveData<Boolean>()

    val action2Setting = MutableLiveData<Unit>()

    val actionShowError = MutableLiveData<LPError>()

    val actionReEnterRoom = MutableLiveData<Boolean>()

    val actionDismissError = MutableLiveData<Unit>()

    val handsupList = MutableLiveData<List<IUserModel>>()

    val actionShowPPTManager = MutableLiveData<Unit>()

    val switch2FullScreen = MutableLiveData<Switchable>()

    val switch2BackList = MutableLiveData<Switchable>()

    val switch2MainVideo = MutableLiveData<Switchable>()

    val isMainVideo2FullScreen = MutableLiveData<Boolean>()

    val speakApplyStatus = MutableLiveData<Int>()

    val pptViewData = MutableLiveData<PPTView>()

    val actionNavigateToPPTDrawing = MutableLiveData<Boolean>()

    val isClassStarted = MutableLiveData<Boolean>()

    val classEnd = MutableLiveData<Unit>()

    val action2RedPacketUI = MutableLiveData<Pair<Boolean, LPRedPacketModel>>()

    val sendPictureMessage = MutableLiveData<String>()

    val showSavePicDialog = MutableLiveData<ByteArray?>()

    val saveChatPictureToGallery = MutableLiveData<ByteArray?>()

    val speakListCount = MutableLiveData<Int>()

    val notifyRemotePlayableChanged = MutableLiveData<IMediaModel>()

    val notifyAward = MutableLiveData<LPInteractionAwardModel>()

    val action2Award = MutableLiveData<String>()

    val awardRecord = HashMap<String,Int>()

    val isTeacherIn = MutableLiveData<Boolean>()

    val showTeacherIn = MutableLiveData<Boolean>()

    val clearScreen = MutableLiveData<Boolean>()

    val actionShowSendMessageFragment = MutableLiveData<Boolean>()

    val actionShowAnnouncementFragment = MutableLiveData<Boolean>()

    val changeDrawing = MutableLiveData<Boolean>()

    override fun onCleared() {
        super.onCleared()
        liveRoom.quitRoom()
    }
}