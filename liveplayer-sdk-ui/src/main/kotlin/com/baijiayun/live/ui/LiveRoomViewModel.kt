package com.baijiayun.live.ui

import android.arch.lifecycle.MutableLiveData
import android.text.TextUtils
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.utils.JsonObjectUtil
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.listener.OnPhoneRollCallListener
import com.baijiayun.livecore.models.*
import com.baijiayun.livecore.models.imodels.*
import com.baijiayun.livecore.models.responsedebug.LPResRoomDebugModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LiveRoomViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {
    val classSwitch = MutableLiveData<Unit>()
    val forbidChatAllModel = MutableLiveData<LPRoomForbidChatResult>()
    val mediaStatus = MutableLiveData<MediaStatus>()
    var extraMediaChange = MutableLiveData<Pair<LPConstants.MediaSourceType, Boolean>>()
    val showRollCall = MutableLiveData<Pair<Int,OnPhoneRollCallListener.RollCall>>()
    val dismissRollCall = MutableLiveData<Unit>()
    val showToast = MutableLiveData<String>()
    val reportAttention = MutableLiveData<Unit>()
    var teacherVideoOn = false
    var teacherAudioOn = false
    var counter = 0
    //进教室之前监听
    fun observeActions() {
        with(routerViewModel) {
            liveRoom.observableOfDebug.observeOn(AndroidSchedulers.mainThread())
                    .filter { it.data != null && !JsonObjectUtil.isJsonNull(it.data,"command_type")}
                    .subscribe(object : DisposingObserver<LPResRoomDebugModel>() {
                        override fun onNext(lpResRoomDebugModel: LPResRoomDebugModel) {
                            val commandType = lpResRoomDebugModel.data.get("command_type").asString
                            if ("logout" == commandType) {
                                if (lpResRoomDebugModel.data.has("code")) {
                                    val code = lpResRoomDebugModel.data.get("code").asInt
                                    if (code == 2) {
                                        val tip = liveRoom.auditionTip
                                        actionShowError.value = LPError.getNewError(LPError.CODE_ERROR_LOGIN_AUDITION, tip[0], tip[1])
                                    } else {
                                        actionShowError.value = LPError.getNewError(LPError.CODE_ERROR_LOGIN_KICK_OUT.toLong(), "用户被请出房间")
                                    }

                                } else {
                                    actionShowError.value = LPError.getNewError(LPError.CODE_ERROR_LOGIN_KICK_OUT.toLong(), "用户被请出房间")
                                }
                            }
                        }
                    })
        }
    }
    override fun subscribe() {
        with(routerViewModel) {
            liveRoom.observableOfClassStart.observeOn(AndroidSchedulers.mainThread()).subscribe(object : DisposingObserver<Int>() {
                override fun onNext(t: Int) {
                    isClassStarted.value = true
                }
            })
            liveRoom.observableOfClassEnd.observeOn(AndroidSchedulers.mainThread()).subscribe(object : DisposingObserver<Int>() {
                override fun onNext(t: Int) {
                    if (liveRoom.currentUser.type == LPConstants.LPUserType.Student && liveRoom.isShowEvaluation) {
                        routerViewModel.showEvaDlg.value = true
                    }
                    awardRecord.clear()
                    classEnd.value = Unit
                    isClassStarted.value = false
                    teacherVideoOn = false
                    teacherAudioOn = false
                }
            })
            liveRoom.observableOfClassSwitch.delay(Random.nextInt(2) + 1L, TimeUnit.SECONDS)
                    .subscribe(object : DisposingObserver<Int>() {
                        override fun onNext(t: Int) {
                            classSwitch.postValue(Unit)
                        }
                    })
            liveRoom.observableOfForbidAllChatStatus
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<LPRoomForbidChatResult>() {
                        override fun onNext(t: LPRoomForbidChatResult) {
                            if (counter == 0) {
                                counter++
                                return
                            }
                            forbidChatAllModel.value = t
                        }
                    })
            if (liveRoom.currentUser.type != LPConstants.LPUserType.Teacher) {
                liveRoom.speakQueueVM.observableOfMediaPublish
                        .filter { !liveRoom.isTeacherOrAssistant && it.user.type == LPConstants.LPUserType.Teacher }
                        .filter { liveRoom.isClassStarted }
                        .filter { !liveRoom.isMixModeOn }
                        .throttleFirst(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<IMediaModel>() {
                            override fun onNext(iMediaModel: IMediaModel) {
                                if (iMediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtCamera ||
                                        iMediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare ||
                                        iMediaModel.mediaSourceType == LPConstants.MediaSourceType.MainScreenShare) {
                                    extraMediaChange.value = iMediaModel.mediaSourceType to iMediaModel.isVideoOn
                                    return
                                }
                                if (iMediaModel.isVideoOn && iMediaModel.isAudioOn) {
                                    if (!teacherVideoOn && !teacherAudioOn) {
                                        mediaStatus.value = MediaStatus.VIDEO_AUDIO_ON
                                    } else if (!teacherAudioOn) {
                                        mediaStatus.value = MediaStatus.AUDIO_ON
                                    } else if (!teacherVideoOn) {
                                        mediaStatus.value = MediaStatus.VIDEO_ON
                                    }
                                } else if (iMediaModel.isVideoOn) {
                                    if (teacherAudioOn && teacherVideoOn) {
                                        mediaStatus.value = MediaStatus.AUDIO_CLOSE
                                    } else if (!teacherVideoOn) {
                                        mediaStatus.value = MediaStatus.VIDEO_ON
                                    }
                                } else if (iMediaModel.isAudioOn) {
                                    if (teacherAudioOn && teacherVideoOn) {
                                        mediaStatus.value = MediaStatus.VIDEO_CLOSE
                                    } else if (!teacherAudioOn) {
                                        mediaStatus.value = MediaStatus.AUDIO_ON
                                    }
                                } else {
                                    mediaStatus.value = MediaStatus.VIDEO_AUDIO_CLOSE
                                }
                                setTeacherMedia(iMediaModel)
                            }
                        })
                liveRoom.observableOfUserIn
                        .filter { it.user.type == LPConstants.LPUserType.Teacher }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<IUserInModel>() {
                            override fun onNext(iUserInModel: IUserInModel) {
                                isTeacherIn.value = true
                                showTeacherIn.value = true
                            }
                        })
                liveRoom.observableOfUserOutWithUserModel
                        .filter { it.type == LPConstants.LPUserType.Teacher}
                        .subscribe(object : DisposingObserver<IUserModel>() {
                            override fun onNext(userModel: IUserModel) {
                                isTeacherIn.value = false
                                showTeacherIn.value = false
                            }
                        })
                //点名
                liveRoom.setOnRollCallListener(object : OnPhoneRollCallListener {
                    override fun onRollCall(time: Int, rollCallListener: OnPhoneRollCallListener.RollCall) {
                        showRollCall.value = time to rollCallListener
                    }

                    override fun onRollCallTimeOut() {
                        dismissRollCall.value = Unit
                    }
                })
                //小测
                liveRoom.quizVM.observableOfQuizStart
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPJsonModel>() {
                            override fun onNext(lpJsonModel: LPJsonModel) {
                                quizStatus.value = RouterViewModel.QuizStatus.START to lpJsonModel
                            }
                        })
                //中途打开
                liveRoom.quizVM.observableOfQuizRes
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant && it.data != null }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPJsonModel>() {
                            override fun onNext(lpJsonModel: LPJsonModel) {
                                val quizId = JsonObjectUtil.getAsString(lpJsonModel.data, "quiz_id")
                                var solutionStatus = false
                                if (!lpJsonModel.data.has("solution")) {
                                    //没有solution
                                    solutionStatus = true
                                } else if (lpJsonModel.data.getAsJsonObject("solution").entrySet().isEmpty()) {
                                    //"solution":{}
                                    solutionStatus = true
                                } else if (lpJsonModel.data.getAsJsonObject("solution").isJsonNull) {
                                    //"solution":"null"
                                    solutionStatus = true
                                }
                                val endFlag = lpJsonModel.data.get("end_flag").getAsInt() == 1
                                //quizid非空、solution是空、没有结束答题 才弹窗
                                if (!TextUtils.isEmpty(quizId) && solutionStatus && !endFlag) {
                                    quizStatus.value = RouterViewModel.QuizStatus.RES to lpJsonModel
                                }
                            }
                        })
                //小测结束
                liveRoom.quizVM.observableOfQuizEnd
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPJsonModel>() {
                            override fun onNext(lpJsonModel: LPJsonModel) {
                                quizStatus.value = RouterViewModel.QuizStatus.END to lpJsonModel
                            }
                        })
                //发答案
                liveRoom.quizVM.observableOfQuizSolution
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPJsonModel>() {
                            override fun onNext(lpJsonModel: LPJsonModel) {
                                quizStatus.value = RouterViewModel.QuizStatus.SOLUTION to lpJsonModel
                            }
                        })
                liveRoom.toolBoxVM.observableOfAnswerStart
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant }
                        .subscribe(object : DisposingObserver<LPAnswerModel>() {
                            override fun onNext(lpAnswerModel: LPAnswerModel) {
                                answerStart.postValue(lpAnswerModel)
                            }
                        })
                liveRoom.toolBoxVM.observableOfAnswerEnd
                        .filter { !liveRoom.isTeacherOrAssistant && !liveRoom.isGroupTeacherOrAssistant }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPAnswerEndModel>() {
                            override fun onNext(lpAnswerEndModel: LPAnswerEndModel) {
                                answerEnd.value = !lpAnswerEndModel.isRevoke
                            }
                        })
                liveRoom.toolBoxVM.observableOfBJTimerStart
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<LPBJTimerModel>() {
                            override fun onNext(lpbjTimerModel: LPBJTimerModel) {
                                showTimer.value = true to lpbjTimerModel
                            }
                        })
                liveRoom.toolBoxVM.observableOfBJTimerEnd
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<Boolean>() {
                            override fun onNext(boolean: Boolean) {
                                showTimer.value= false to LPBJTimerModel()
                            }
                        })
                liveRoom.toolBoxVM.observableOfAttentionDetection
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object :DisposingObserver<LPJsonModel>(){
                            override fun onNext(t: LPJsonModel) {
                                reportAttention.value = Unit
                            }
                        })
                liveRoom.toolBoxVM.observableOfAttentionAlert
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<String>() {
                            override fun onNext(content: String) {
                                showToast.value = content
                            }
                        })
            }
            if (!liveRoom.isTeacherOrAssistant) {
                liveRoom.observableOfAnnouncementChange
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposingObserver<IAnnouncementModel>(){
                            override fun onNext(t: IAnnouncementModel) {
                                if (!t.link.isNullOrEmpty() || !t.content.isNullOrEmpty()) {
                                    routerViewModel.actionShowAnnouncementFragment.value = true
                                }
                            }
                        })
                liveRoom.requestAnnouncement()
            }
            liveRoom.observableOfRedPacket.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<LPRedPacketModel>() {
                        override fun onNext(lpRedPacketModel: LPRedPacketModel) {
                            action2RedPacketUI.value = true to lpRedPacketModel
                        }
                    })
            liveRoom.observableOfPPTVideoSwitch.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<Boolean>() {
                        override fun onNext(isVideoInMain: Boolean) {
                            //isVideoInMain 是否mainVideo在ppt容器区
                            //mainItem 是老师视频（区别于老师视频区）
                            val mainItem = routerViewModel.mainVideoItem.value ?: return
                            routerViewModel.isMainVideo2FullScreen.value = true
                            if (isVideoInMain) {
                                if (mainItem.isInFullScreen) {
                                    return
                                }
                                //PPT容器区
                                val fullScreenItem = routerViewModel.switch2FullScreen.value?:return
                                fullScreenItem.switchBackToList()
                                mainItem.switchToFullScreen()
                            } else {
                                if (!mainItem.isInFullScreen) {
                                    return
                                }
                                //右上角老师区
                                val mainVideoItem = routerViewModel.switch2MainVideo.value?:return
                                mainItem.switchBackToList()
                                mainVideoItem.switchToFullScreen()
                            }
                        }
                    })
            if (liveRoom.currentUser.type == LPConstants.LPUserType.Teacher || liveRoom.currentUser.type == LPConstants.LPUserType.Assistant) {
                liveRoom.observableOfSpeakInvite.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object :DisposingObserver<LPSpeakInviteModel>(){
                            override fun onNext(t: LPSpeakInviteModel) {
                                if (t.invite == 1) {
                                    routerViewModel.invitingUserIds.add(t.to)
                                } else {
                                    routerViewModel.invitingUserIds.remove(t.to)
                                    routerViewModel.timeOutStart.value = t.to to false
                                }
                            }
                        })
                liveRoom.observableOfSpeakInviteRes.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object :DisposingObserver<LPSpeakInviteConfirmModel>(){
                            override fun onNext(t: LPSpeakInviteConfirmModel) {
                                routerViewModel.invitingUserIds.remove(t.userId)
                                routerViewModel.timeOutStart.value = t.userId to false
                            }
                        })
                liveRoom.observableOfForbidList.observeOn(AndroidSchedulers.mainThread())
                        .map { it.userList }
                        .flatMap { Observable.fromIterable(it) }
                        .subscribe(object :DisposingObserver<LPForbidUserModel>(){
                            override fun onNext(lpForbidUserModel: LPForbidUserModel) {
                                if (lpForbidUserModel.duration > 0) {
                                    routerViewModel.forbidChatUserNums.add(lpForbidUserModel.number)
                                } else {
                                    routerViewModel.forbidChatUserNums.remove(lpForbidUserModel.number)
                                }
                            }
                        })
                liveRoom.observableOfForbidChat.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object :DisposingObserver<IForbidChatModel>(){
                            override fun onNext(t: IForbidChatModel) {
                                if (t.duration > 0) {
                                    routerViewModel.forbidChatUserNums.add(t.forbidUser.number)
                                } else {
                                    routerViewModel.forbidChatUserNums.remove(t.forbidUser.number)
                                }
                            }
                        })
                liveRoom.requestForbidList()
            }
        }
    }

    fun setTeacherMedia(media: IMediaModel) {
        teacherVideoOn = media.isVideoOn
        teacherAudioOn = media.isAudioOn
    }

    enum class MediaStatus {
        VIDEO_ON, AUDIO_ON, VIDEO_AUDIO_ON, VIDEO_CLOSE, AUDIO_CLOSE, VIDEO_AUDIO_CLOSE
    }
}