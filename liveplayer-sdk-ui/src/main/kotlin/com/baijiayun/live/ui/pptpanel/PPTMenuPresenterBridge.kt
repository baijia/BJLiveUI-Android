package com.baijiayun.live.ui.pptpanel

import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomRouterListener
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.menu.rightmenu.RightMenuContract
import com.baijiayun.live.ui.utils.RxUtils
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.listener.OnSpeakApplyCountDownListener
import com.baijiayun.livecore.models.imodels.IMediaControlModel
import com.baijiayun.livecore.models.roomresponse.LPResRoomMediaControlModel
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.util.concurrent.TimeUnit

class PPTMenuPresenterBridge(val view: PPTMenuContract.View, val liveRoomRouterListener: LiveRoomRouterListener,val routerViewModel: RouterViewModel) : PPTMenuContract.Presenter {
    private val disposables by lazy {
        CompositeDisposable()
    }
    private var currentUserType: LPConstants.LPUserType? = null
    private var isDrawing = false
    private var isGetDrawingAuth = false
    private var isWaitingRecordOpen = false
    
    init {
        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
    }
    override fun visitOnlineUser() {
    }

    override fun changeDrawing() {
        if (!isDrawing && !liveRoomRouterListener.canStudentDraw()) {
            view.showCantDraw()
            return
        }
        if (currentUserType == LPConstants.LPUserType.Assistant && liveRoomRouterListener.liveRoom.adminAuth != null &&
                !liveRoomRouterListener.liveRoom.adminAuth.painter) {
            view.showDrawDeny()
            return
        }
        if (!liveRoomRouterListener.isTeacherOrAssistant && !liveRoomRouterListener.liveRoom.isClassStarted) {
            view.showCantDrawCauseClassNotStart()
            return
        }
        isDrawing = !isDrawing
        liveRoomRouterListener.navigateToPPTDrawing(isDrawing)
        view.showDrawingStatus(isDrawing)
    }

    override fun managePPT() {
    }

    override fun speakApply() {
        if (!liveRoomRouterListener.liveRoom.isClassStarted) {
            view.showHandUpError()
            return
        }

        if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_NONE) {
            if (liveRoomRouterListener.liveRoom.forbidRaiseHandStatus) {
                view.showHandUpForbid()
                return
            }

            liveRoomRouterListener.liveRoom.speakQueueVM.requestSpeakApply(object : OnSpeakApplyCountDownListener {
                override fun onTimeOut() {
                    routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                    liveRoomRouterListener.liveRoom.speakQueueVM.cancelSpeakApply()
                    view.showSpeakApplyCanceled()
                    view.showHandUpTimeout()
                }

                override fun onTimeCountDown(counter: Int, timeOut: Int) {
                    view.showSpeakApplyCountDown(timeOut - counter, timeOut)
                }
            })
            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING
            view.showWaitingTeacherAgree()
        } else if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING) {
            // 取消发言请求
            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
            liveRoomRouterListener.liveRoom.speakQueueVM.cancelSpeakApply()
            view.showSpeakApplyCanceled()
        } else if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
            // 取消发言
            cancelStudentSpeaking()
        }
    }
    private fun disableSpeakerMode() {
        if (!liveRoomRouterListener.liveRoom.isTeacherOrAssistant && !liveRoomRouterListener.liveRoom.isGroupTeacherOrAssistant) {
            view.disableSpeakerMode()
        }
        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isPublishing) {
            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().stopPublishing()
        }
        liveRoomRouterListener.detachLocalVideo()
    }
    private fun cancelStudentSpeaking() {
        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
        disableSpeakerMode()
        view.showSpeakApplyCanceled()
        if (isDrawing) {
            // 如果画笔打开 关闭画笔模式
            changeDrawing()
        }
    }

    override fun onSpeakInvite(confirm: Int) {
        liveRoomRouterListener.liveRoom.sendSpeakInvite(confirm)
        if (confirm == 1) {
            //接受
            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING
            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().publish()
            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached)
                liveRoomRouterListener.attachLocalAudio()
            if (liveRoomRouterListener.liveRoom.autoOpenCameraStatus) {
                isWaitingRecordOpen = true
                val timer = Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached)
                                liveRoomRouterListener.attachLocalVideo()
                            isWaitingRecordOpen = false
                        }
                disposables.add(timer)
            }
            view.showForceSpeak(isEnableDrawing())
            view.enableSpeakerMode()
        }
    }

    override fun isWaitingRecordOpen(): Boolean {
        return isWaitingRecordOpen
    }

    override fun showTimer() {
    }

    override fun setRouter(liveRoomRouterListener: LiveRoomRouterListener) {
    }

    private fun isEnableDrawing(): Boolean {
        return isGetDrawingAuth || liveRoomRouterListener.liveRoom.partnerConfig.liveDisableGrantStudentBrush == 1
    }

    override fun subscribe() {
        currentUserType = liveRoomRouterListener.liveRoom.currentUser.type
        if (!liveRoomRouterListener.isCurrentUserTeacher) {
            view.hideTimer()
        }
        if (liveRoomRouterListener.isTeacherOrAssistant || liveRoomRouterListener.isGroupTeacherOrAssistant) {
            view.showTeacherRightMenu()
        } else {
            view.showStudentRightMenu()
            if (liveRoomRouterListener.liveRoom.partnerConfig.liveHideUserList == 1) {
                view.hideUserList()
            }
        }

        if (liveRoomRouterListener.isTeacherOrAssistant) {
            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM.observableOfSpeakApplyResResult
                    .subscribe { iMediaControlModel ->
                        if (!iMediaControlModel.isApplyAgreed) {
                            view.showMessage("发言人数已满，请先关闭其他人音视频。")
                        }
                    })
        }

        if (!liveRoomRouterListener.isTeacherOrAssistant && !liveRoomRouterListener.isGroupTeacherOrAssistant) {
            // 学生

            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM
                    .observableOfMediaDeny
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached && !liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached)
                            view.showForceSpeakDenyByServer()
                        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
                        }
                        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachVideo()
                            liveRoomRouterListener.detachLocalVideo()
                        }
                        if (liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi)
                            view.showAutoSpeak(isEnableDrawing())
                    })
            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM.observableOfSpeakApplyDeny
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        // 结束发言模式
                        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                        liveRoomRouterListener.liveRoom.speakQueueVM.cancelSpeakApply()
                        view.showSpeakClosedByServer()
                    })

            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM
                    .observableOfMediaControl
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { iMediaControlModel ->
                        if (iMediaControlModel.isApplyAgreed) {
                            // 强制发言
                            if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                                //已经在发言了
                                if (iMediaControlModel.isAudioOn) {
                                    liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().attachAudio()
                                } else if (!iMediaControlModel.isAudioOn) {
                                    if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached)
                                        liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
                                }
                                if (iMediaControlModel.isVideoOn && !liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                                    liveRoomRouterListener.attachLocalVideo()
                                } else if (!iMediaControlModel.isVideoOn && liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                                    liveRoomRouterListener.detachLocalVideo()
                                }

                            } else {
                                routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING
                                view.enableSpeakerMode()
                                view.showForceSpeak(isEnableDrawing())
                                showForceSpeakDlg(iMediaControlModel)
                            }
                        } else {
                            // 结束发言模式
                            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                            if (liveRoomRouterListener.liveRoom.roomType == LPConstants.LPRoomType.Multi) {
                                disableSpeakerMode()
                                if (isDrawing) {
                                    // 如果画笔打开 关闭画笔模式
                                    changeDrawing()
                                }
                            } else {
                                liveRoomRouterListener.detachLocalVideo()
                                if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isPublishing())
                                    liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().stopPublishing()
                            }

                            if (iMediaControlModel.senderUserId != liveRoomRouterListener.liveRoom.currentUser.userId) {
                                // 不是自己结束发言的
                                view.showSpeakClosedByTeacher(liveRoomRouterListener.liveRoom.roomType == LPConstants.LPRoomType.SmallGroup)
                            }
                        }
                        if (!iMediaControlModel.isAudioOn && !iMediaControlModel.isVideoOn
                                && liveRoomRouterListener.liveRoom.roomType == LPConstants.LPRoomType.Multi) {
                            cancelStudentSpeaking()
                        }
                    })

            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM.observableOfSpeakResponse
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { iMediaControlModel ->
                        if (iMediaControlModel.user.userId != liveRoomRouterListener.liveRoom.currentUser.userId) {
                            return@subscribe
                        }
                        // 请求发言的用户自己
                        if (iMediaControlModel.isApplyAgreed) {
                            // 进入发言模式
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().publish()
                            liveRoomRouterListener.attachLocalAudio()
                            view.showSpeakApplyAgreed(isEnableDrawing())
                            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING
                            view.enableSpeakerMode()
                            if (liveRoomRouterListener.liveRoom.autoOpenCameraStatus) {
                                isWaitingRecordOpen = true
                                val timer = Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { aLong ->
                                            liveRoomRouterListener.attachLocalVideo()
                                            isWaitingRecordOpen = false
                                        }
                                disposables.add(timer)
                            }
                        } else {
                            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                            if (iMediaControlModel.senderUserId != liveRoomRouterListener.liveRoom.currentUser.userId) {
                                // 不是自己结束发言的
                                view.showSpeakApplyDisagreed()
                            }
                        }
                    })

            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM.publishSubjectOfStudentDrawingAuth
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { aBoolean ->
                        if (aBoolean) {
                            if (isGetDrawingAuth) return@Consumer
                            view.showPPTDrawBtn()
                            isGetDrawingAuth = true
                        } else {
                            if (!isGetDrawingAuth) return@Consumer
                            view.hidePPTDrawBtn()
                            isGetDrawingAuth = false
                            liveRoomRouterListener.navigateToPPTDrawing(false)
                            isDrawing = false
                            view.showDrawingStatus(false)
                        }
                    }))
        } else if (liveRoomRouterListener.liveRoom.currentUser.type == LPConstants.LPUserType.Assistant) {
            // 助教
            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM
                    .observableOfMediaDeny
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached && !liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached)
                            view.showForceSpeakDenyByServer()
                        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
                        }
                        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachVideo()
                            liveRoomRouterListener.detachLocalVideo()
                        }

                        if (liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi)
                            view.showAutoSpeak(isEnableDrawing())
                    })
            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM.observableOfSpeakApplyDeny
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { iMediaModel ->
                        // 结束发言模式
                        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                        liveRoomRouterListener.liveRoom.speakQueueVM.cancelSpeakApply()
                        view.showSpeakClosedByServer()
                    })

            disposables.add(liveRoomRouterListener.liveRoom.speakQueueVM
                    .observableOfMediaControl
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { iMediaControlModel ->
                        if (!iMediaControlModel.isApplyAgreed) {
                            // 结束发言模式
                            disableSpeakerMode()
                            if (isDrawing) changeDrawing()
                        } else {
                            if (iMediaControlModel.isAudioOn && !liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().attachAudio()
                            } else if (!iMediaControlModel.isAudioOn && liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
                            }
                            if (iMediaControlModel.isVideoOn && !liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                                liveRoomRouterListener.attachLocalVideo()
                            } else if (!iMediaControlModel.isVideoOn && liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                                liveRoomRouterListener.detachLocalVideo()
                            }
                        }
                    })

        }

        disposables.add(liveRoomRouterListener.liveRoom.observableOfClassEnd
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aVoid ->
                    if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING) {
                        // 取消发言请求
                        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                        liveRoomRouterListener.liveRoom.speakQueueVM.cancelSpeakApply()
                        view.showSpeakApplyCanceled()
                    } else if (routerViewModel.speakApplyStatus.value == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                        // 取消发言
                        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_NONE
                        disableSpeakerMode()
                        view.showSpeakApplyCanceled()
                        if (isDrawing) {
                            // 如果画笔打开 关闭画笔模式
                            liveRoomRouterListener.navigateToPPTDrawing(false)
                            isDrawing = !isDrawing
                            view.showDrawingStatus(isDrawing)
                        }
                    }
                    isGetDrawingAuth = false
                })

        disposables.add(liveRoomRouterListener.liveRoom.observableOfClassStart
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (liveRoomRouterListener.liveRoom.currentUser.type == LPConstants.LPUserType.Student && liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi) {
                        routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING
                    }
                })

        if (liveRoomRouterListener.liveRoom.currentUser.type == LPConstants.LPUserType.Student && liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi) {
            view.showAutoSpeak(isEnableDrawing())
            routerViewModel.speakApplyStatus.value = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING
        }

        //邀请发言
        disposables.add(liveRoomRouterListener.liveRoom.observableOfSpeakInvite
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { lpSpeakInviteModel ->
                    if (liveRoomRouterListener.liveRoom.currentUser.userId == lpSpeakInviteModel.to) {
                        view.showSpeakInviteDlg(lpSpeakInviteModel.invite)
                    }
                })

        if (liveRoomRouterListener.liveRoom.isAudition) {
            view.setAudition()
        }
        disposables.add(liveRoomRouterListener.liveRoom.observableOfAdminAuth
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { lpAdminAuthModel ->
                    if (!lpAdminAuthModel.painter) {
                        view.showDrawingStatus(false)
                        liveRoomRouterListener.navigateToPPTDrawing(false)
                    }
                })
        disposables.add(liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().observableOfCameraOn
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aBoolean -> view.showVideoStatus(aBoolean) })
        disposables.add(liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().observableOfMicOn
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aBoolean -> view.showAudioStatus(aBoolean) })

        disposables.add(liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().observableOfVolume
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { volumeLevel ->
                    if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>() != null && liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached)
                        view.showVolume(volumeLevel)
                })
        if (!liveRoomRouterListener.isTeacherOrAssistant) {
            disableSpeakerMode()
        }
        disposables.add(liveRoomRouterListener.liveRoom.observableOfClassStart
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (liveRoomRouterListener.liveRoom.currentUser.type == LPConstants.LPUserType.Student && liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi || liveRoomRouterListener.isTeacherOrAssistant
                            || liveRoomRouterListener.isGroupTeacherOrAssistant) {
                        view.enableSpeakerMode()
                    } else {
                        view.disableSpeakerMode()
                    }
                })
        disposables.add(liveRoomRouterListener.liveRoom.observableOfForbidAllAudioStatus
                .subscribe { aBoolean ->
                    if (aBoolean && !liveRoomRouterListener.isTeacherOrAssistant) {
                        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
                        }
                    }
                })
        if (liveRoomRouterListener.liveRoom.currentUser.type == LPConstants.LPUserType.Student && liveRoomRouterListener.liveRoom.roomType != LPConstants.LPRoomType.Multi) {
            if (liveRoomRouterListener.liveRoom.isClassStarted) {
                view.enableSpeakerMode()
            } else {
                view.disableSpeakerMode()
            }
        }
    }

    private fun showForceSpeakDlg(iMediaControlModel: IMediaControlModel) {
        val lpResRoomMediaControlModel = iMediaControlModel as LPResRoomMediaControlModel
        if (liveRoomRouterListener.liveRoom.autoOpenCameraStatus) {
            liveRoomRouterListener.attachLocalVideo()
        }
        if (lpResRoomMediaControlModel.isAudioOn) {
            liveRoomRouterListener.attachLocalAudio()
        }
        var tipRes = R.string.live_force_speak_tip_all
        if (lpResRoomMediaControlModel.isAudioOn && liveRoomRouterListener.liveRoom.autoOpenCameraStatus) {
            tipRes = R.string.live_force_speak_tip_all
        } else if (lpResRoomMediaControlModel.isAudioOn) {
            tipRes = R.string.live_force_speak_tip_audio
        } else if (liveRoomRouterListener.liveRoom.autoOpenCameraStatus) {
            tipRes = R.string.live_force_speak_tip_video
        }
        view.showForceSpeakDlg(tipRes)
    }
    override fun changeAudio() {
        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
            liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().detachAudio()
            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().stopPublishing()
            }
        } else {
            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isPublishing) {
                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().publish()
            }
            liveRoomRouterListener.attachLocalAudio()
        }
    }

    override fun changeVideo() {
        if (liveRoomRouterListener.liveRoom.roomMediaType == LPConstants.LPMediaType.Audio) {
            view.showAudioRoomError()
            return
        }
        if (liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isVideoAttached) {
            liveRoomRouterListener.detachLocalVideo()
            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().stopPublishing()
            }
        } else {
            if (!liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().isPublishing) {
                liveRoomRouterListener.liveRoom.getRecorder<LPRecorder>().publish()
            }
            liveRoomRouterListener.attachLocalVideo()
        }
    }

    override fun unSubscribe() {
        RxUtils.dispose(disposables)
    }

    override fun destroy() {
    }
}