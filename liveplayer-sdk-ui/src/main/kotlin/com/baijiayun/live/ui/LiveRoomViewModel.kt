package com.baijiayun.live.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.MutableLiveData
import android.text.TextUtils
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.utils.JsonObjectUtil
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.listener.OnPhoneRollCallListener
import com.baijiayun.livecore.models.*
import com.baijiayun.livecore.models.imodels.IAnnouncementModel
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserInModel
import com.baijiayun.livecore.models.responsedebug.LPResRoomDebugModel
import com.baijiayun.livecore.utils.LPLogger
import com.baijiayun.livecore.wrapper.LPRecorder
import com.baijiayun.livecore.wrapper.impl.LPCameraView
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
    var teacherVideoOn = false
    var teacherAudioOn = false
    @SuppressLint("StaticFieldLeak")
    var cameraView: LPCameraView? = null
    var counter = 0
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
                liveRoom.observableOfUserOut
                        .filter { !TextUtils.isEmpty(it) && liveRoom.teacherUser != null && it == liveRoom.teacherUser.userId}
                        .subscribe(object : DisposingObserver<String>() {
                            override fun onNext(t: String) {
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
                //debug信息
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
        }
    }

    fun setTeacherMedia(media: IMediaModel) {
        teacherVideoOn = media.isVideoOn
        teacherAudioOn = media.isAudioOn
    }
    fun switchBackstage(isBackstage: Boolean) {
        if (routerViewModel.liveRoom.getRecorder<LPRecorder>() == null) return
        if (isBackstage) {
            //后台
            if (routerViewModel.liveRoom.getRecorder<LPRecorder>().isPublishing) {
                LPLogger.d("LiveRoomViewModel", "switchBackstage : stopPublishing")
                cameraView = routerViewModel.liveRoom.getRecorder<LPRecorder>().cameraView
                routerViewModel.liveRoom.getRecorder<LPRecorder>().stopPublishing()
            }
        } else {
            //前台
            if (cameraView != null && !routerViewModel.liveRoom.getRecorder<LPRecorder>().isPublishing) {
                LPLogger.d("LiveRoomViewModel", "switchBackstage : startPublishing")
                if (routerViewModel.liveRoom.isUseWebRTC) {
                    routerViewModel.liveRoom.getRecorder<LPRecorder>().setPreview(cameraView)
                }
                routerViewModel.liveRoom.getRecorder<LPRecorder>().publish()
                cameraView = null
            }
        }
    }

    enum class MediaStatus {
        VIDEO_ON, AUDIO_ON, VIDEO_AUDIO_ON, VIDEO_CLOSE, AUDIO_CLOSE, VIDEO_AUDIO_CLOSE
    }
}