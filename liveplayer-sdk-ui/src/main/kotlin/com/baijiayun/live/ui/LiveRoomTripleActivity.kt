package com.baijiayun.live.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.arch.lifecycle.Observer
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.activity.LiveRoomRouterListener
import com.baijiayun.live.ui.base.*
import com.baijiayun.live.ui.chat.MessageSendPresenter
import com.baijiayun.live.ui.chat.MessageSentFragment
import com.baijiayun.live.ui.error.ErrorFragmentModel
import com.baijiayun.live.ui.error.ErrorPadFragment
import com.baijiayun.live.ui.interactivepanel.InteractiveFragment
import com.baijiayun.live.ui.loading.LoadingPadFragment
import com.baijiayun.live.ui.mainvideopanel.MainVideoFragment
import com.baijiayun.live.ui.ppt.pptmanage.PPTManageFragment
import com.baijiayun.live.ui.ppt.pptmanage.PPTManagePresenter
import com.baijiayun.live.ui.ppt.quickswitchppt.QuickSwitchPPTFragment
import com.baijiayun.live.ui.ppt.quickswitchppt.SwitchPPTFragmentPresenter
import com.baijiayun.live.ui.pptpanel.PPTFragment
import com.baijiayun.live.ui.rollcall.RollCallDialogFragment
import com.baijiayun.live.ui.rollcall.RollCallDialogPresenter
import com.baijiayun.live.ui.router.Router
import com.baijiayun.live.ui.router.RouterCode
import com.baijiayun.live.ui.setting.SettingDialogFragment
import com.baijiayun.live.ui.setting.SettingPresenter
import com.baijiayun.live.ui.share.LPShareDialog
import com.baijiayun.live.ui.speakerlist.AwardView
import com.baijiayun.live.ui.speakpanel.SpeakFragment
import com.baijiayun.live.ui.toolbox.announcement.AnnouncementFragment
import com.baijiayun.live.ui.toolbox.announcement.AnnouncementPresenter
import com.baijiayun.live.ui.toolbox.answersheet.QuestionShowFragment
import com.baijiayun.live.ui.toolbox.answersheet.QuestionShowPresenter
import com.baijiayun.live.ui.toolbox.answersheet.QuestionToolFragment
import com.baijiayun.live.ui.toolbox.answersheet.QuestionToolPresenter
import com.baijiayun.live.ui.toolbox.evaluation.EvaDialogFragment
import com.baijiayun.live.ui.toolbox.evaluation.EvaDialogPresenter
import com.baijiayun.live.ui.toolbox.questionanswer.QAInteractiveFragment
import com.baijiayun.live.ui.toolbox.quiz.QuizDialogFragment
import com.baijiayun.live.ui.toolbox.quiz.QuizDialogPresenter
import com.baijiayun.live.ui.toolbox.redpacket.RedPacketFragment
import com.baijiayun.live.ui.toolbox.redpacket.RedPacketPresenter
import com.baijiayun.live.ui.toolbox.timer.TimerFragment
import com.baijiayun.live.ui.toolbox.timer.TimerPresenter
import com.baijiayun.live.ui.toolbox.timer.TimerShowyFragment
import com.baijiayun.live.ui.topmenu.TopMenuFragment
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.utils.JsonObjectUtil
import com.baijiayun.live.ui.viewsupport.dialog.SimpleTextDialog
import com.baijiayun.livecore.LiveSDK
import com.baijiayun.livecore.alilog.AliYunLogHelper
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.listener.LPLaunchListener
import com.baijiayun.livecore.listener.OnPhoneRollCallListener
import com.baijiayun.livecore.models.*
import com.baijiayun.livecore.utils.CommonUtils
import com.baijiayun.livecore.utils.LPRxUtils
import com.baijiayun.livecore.utils.LPSdkVersionUtils
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.contentView
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 标准三分屏界面
 */
class LiveRoomTripleActivity : LiveRoomBaseActivity() {
    private val loadingFragment by lazy { LoadingPadFragment.newInstance() }
    private lateinit var routerViewModel: RouterViewModel
    private lateinit var liveRoomViewModel: LiveRoomViewModel
    private lateinit var oldBridge: OldLiveRoomRouterListenerBridge
    private var pptManagePresenter: PPTManagePresenter? = null
    private var showyTimerPresenter : TimerPresenter? = null
    private var quickSwitchPPTPresenter: SwitchPPTFragmentPresenter? = null
    private var rollCallDialogFragment: RollCallDialogFragment? = null
    private var rollCallDialogPresenter: RollCallDialogPresenter? = null
    private var quizFragment: QuizDialogFragment? = null
    private var quizPresenter: QuizDialogPresenter? = null
    private var questionToolFragment: QuestionToolFragment? = null
    private var questionShowFragment: QuestionShowFragment? = null
    private var announcementFragment: AnnouncementFragment? = null
    private var qaInteractiveFragment: QAInteractiveFragment? = null
    private var redPacketFragment: RedPacketFragment? = null
    private var redPacketPresenter: RedPacketPresenter? = null
    private var timerFragment: TimerFragment? = null
    private var timerShowyFragment: TimerShowyFragment? = null
    private var lpAnswerModel: LPAnswerModel? = null
    private var evaDialogFragment: EvaDialogFragment? = null
    private var mobileNetworkDialogShown = false
    private var minVolume = 0
    private var disposeOfTeacherAbsent: Disposable? = null
    private var disposeOfLoginConflict: Disposable? = null
    private var disposeOfMarquee: Disposable? = null
    private var disposeOfActionMain: Disposable? = null
    private var marqueeAnimator: ObjectAnimator? = null
    private val timerObserver by lazy {
        Observer<Pair<Boolean, LPBJTimerModel>> { it?.let { if (it.first && it.second.action != TimerPresenter.stop_timer) {
            if (routerListener.liveRoom.currentUser.type != LPConstants.LPUserType.Teacher || it.second.action != null)
                showTimerShowy(it.second)
            else
                showTimer(it.second)
        } else closeTimer() } }
    }
    private val answerObserver by lazy {
        Observer<LPAnswerModel> { it?.let { answerStart(it) } }
    }
    private val showRollCallObserver by lazy {
        Observer<Pair<Int, OnPhoneRollCallListener.RollCall>> { it?.let { showRollCallDlg(it.first, it.second) } }
    }
    private val dismissRollCallObserver by lazy {
        Observer<Unit> { it?.let { dismissRollCallDlg() } }
    }
    private val toastObserver by lazy {
        Observer<String> { it?.let { showMessage(it) } }
    }
    private val reportObserver by lazy {
        Observer<Unit> {
            routerViewModel.liveRoom.toolBoxVM.requestAttentionReport(
                    CommonUtils.isAppForeground(this), routerViewModel.liveRoom.currentUser as LPUserModel)
        }
    }
    private val showErrorObserver by lazy {
        Observer<LPError>{
            when (it?.code) {
                LPError.CODE_ERROR_LOGIN_KICK_OUT.toLong() -> showKickOutDlg(it) //被踢出房间后的登录
                LPError.CODE_ERROR_LOGIN_AUDITION.toLong() -> showAuditionEndDlg(it) // 试听结束
                else -> {
                    if (loadingFragment.isAdded) removeFragment(loadingFragment)
                    if (errorFragment.isAdded || findFragment(fullContainer.id) is ErrorPadFragment) return@Observer
                    showErrorDlg(it)
                }
            }
        }
    }
    private val timeOutObserver by lazy {
        Observer<Pair<String,Boolean>> {
            it?.run {
                if (second) {
                    val timeDispose = Observable.timer(30, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                //取消邀请
                                routerViewModel.liveRoom.sendSpeakInviteReq(first, false)
                            }
                    timeOutDisposes[first] = timeDispose
                } else {
                    LPRxUtils.dispose(timeOutDisposes[first])
                    timeOutDisposes.remove(first)
                }
            }
        }
    }
    private val timeOutDisposes = HashMap<String,Disposable>()

    //试听码dialog提示
    private var mAuditionEndDialog: SimpleTextDialog? = null
    private val fullContainer by lazy {
        findViewById<FrameLayout>(R.id.activity_live_room_pad_room_full_screen_container)
    }
    private val errorContainer by lazy {
        findViewById<FrameLayout>(R.id.activity_live_room_pad_room_error_container)
    }
    private val errorFragment by lazy {
        ErrorPadFragment()
    }
    private val messageSentFragment by lazy {
        MessageSentFragment.newInstance()
    }

    private var messageSendPresenter: MessageSendPresenter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFullScreen()
        setContentView(getContentResId())
        hideSysUIComponent()
        LiveSDK.checkTeacherUnique = true
        enterRoom()
    }

    private fun initFullScreen() {
        if (!isPad(this)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun getContentResId(): Int = if (isPad(this)) {
        R.layout.activity_live_room_pad
    } else {
        when {
            isAspectRatioNormal(this) -> R.layout.activity_live_room_pad
            isAspectRatioSmall(this) -> R.layout.activity_live_room_pad_16_10
            isAspectRatioLarge(this) -> R.layout.activity_live_room_pad_18_9
            else -> R.layout.activity_live_room_pad
        }
    }

    private fun initView() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.activity_live_room_pad_room_top_container, TopMenuFragment.newInstance())
                .replace(R.id.activity_live_room_pad_room_main_video_container, MainVideoFragment.newInstance())
                .replace(R.id.activity_live_room_pad_room_interaction_container, InteractiveFragment.newInstance())
                .replace(R.id.activity_live_room_pad_room_ppt_container, PPTFragment.newInstance())
                .replace(R.id.activity_live_room_pad_room_videos_container, SpeakFragment.newInstance())
                .commitNowAllowingStateLoss()
    }

    private var enterCount = 0//记录进入教室次数，临时排查问题增加

    /**
     * 进入教室或者重连
     */
    private fun enterRoom(liveRoom: LiveRoom? = null) {
        enterCount++
        replaceFragment(fullContainer.id, loadingFragment)
        fullContainer.visibility = View.VISIBLE
        routerViewModel = getViewModel { RouterViewModel() }
        liveRoomViewModel = getViewModel { LiveRoomViewModel(routerViewModel) }
        oldBridge = OldLiveRoomRouterListenerBridge(routerViewModel)
        if (liveRoom == null) {
            if (!code.isNullOrEmpty()) {
                routerViewModel.liveRoom = LiveSDK.enterRoom(this, code, name, null, avatar, loadingFragment.launchListener)
            } else {
                if (enterUser == null) {
                    AliYunLogHelper.getInstance().addErrorLog("进教室失败第${enterCount}次，roomId:${roomId} sign:${sign}")
                    showToastMessage("进教室失败")
                    finish()
                } else {
                    routerViewModel.liveRoom = LiveSDK.enterRoom(this, roomId, enterUser, sign, loadingFragment.launchListener)
                }
            }
        } else {
            routerViewModel.liveRoom = liveRoom
            liveRoom.reconnect(loadingFragment.launchListener)
        }
        observeActions()
        initView()
    }

    override fun onBackPressed() {
        if (exitListener != null) {
            super.onBackPressed()
        }
        routerViewModel.actionExit.value = Unit
    }

    private fun observeActions() = with(routerViewModel) {
        liveRoomViewModel.observeActions()
        actionExit.observe(this@LiveRoomTripleActivity, Observer {
            it?.let { showExitDialog() }
        })

        disposeOfActionMain = Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                .subscribe {
                    initLiveRoom()
                    navigateToMain()
                    observeSuccess()
                }

        speakListCount.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                this@LiveRoomTripleActivity.findViewById<View>(R.id.activity_live_room_pad_room_videos_container).visibility = if (it == 0) View.GONE else View.VISIBLE
            }
        })
        actionShowError.observeForever (showErrorObserver)
        actionDismissError.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                errorContainer.visibility = View.GONE
            }
        })
        actionReEnterRoom.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                doReEnterRoom(it.first,it.second)
            }
        })
        action2Setting.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                val settingFragment = SettingDialogFragment.newInstance(pptViewData.value?.didRoomContainsH5PPT()?:false)
                val settingPresenter = SettingPresenter(settingFragment)
                bindVP(settingFragment, settingPresenter)
                showDialogFragment(settingFragment)
            }
        })
        action2Share.observe(this@LiveRoomTripleActivity, Observer {
            it?.let { navigateToShare() }
        })
        actionShowQuickSwitchPPT.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                val quickSwitchPPTFragment = QuickSwitchPPTFragment.newInstance(it)
                quickSwitchPPTPresenter = SwitchPPTFragmentPresenter(quickSwitchPPTFragment, true /*TODO 是否启用多白板*/)
                quickSwitchPPTPresenter?.run {
                    bindVP(quickSwitchPPTFragment, this)
                    showDialogFragment(quickSwitchPPTFragment)
                }
            }
        })
        actionChangePPT2Page.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                quickSwitchPPTPresenter?.notifyMaxIndexChange(it)
            }
        })
        actionShowPPTManager.observe(this@LiveRoomTripleActivity, Observer {
            val pptManageFragment = PPTManageFragment.newInstance()
            if (pptManagePresenter == null) {
                pptManagePresenter = PPTManagePresenter()
                pptManagePresenter?.run {
                    setRouter(oldBridge)
                    subscribe()
                }
            }
            pptManageFragment.setPresenter(pptManagePresenter)
            showDialogFragment(pptManageFragment)
        })
        actionShowSendMessageFragment.observe(this@LiveRoomTripleActivity, Observer {
            if (messageSentFragment.isAdded) return@Observer
            messageSendPresenter = MessageSendPresenter(messageSentFragment)
            messageSendPresenter?.setView(messageSentFragment)
            bindVP(messageSentFragment, messageSendPresenter!!)
            if (routerViewModel.choosePrivateChatUser) {
                messageSentFragment.setAutoChoosePrivateChatUser(true)
                routerViewModel.choosePrivateChatUser = false
            }
            showDialogFragment(messageSentFragment)
        })
        actionShowAnnouncementFragment.observe(this@LiveRoomTripleActivity, Observer {
            if (it == true) {
                navigateToAnnouncement()
            }
        })
        actionShowQAInteractiveFragment.observe(this@LiveRoomTripleActivity, Observer {
            it?.let {
                navigateToQAInteractive()
            }
        })
        shouldShowTecSupport.observe(this@LiveRoomTripleActivity, Observer {
            if (it != null) {
                shouldShowTechSupport = it
            }
        })
        privateChatUser.observe(this@LiveRoomTripleActivity, Observer {
            messageSendPresenter?.onPrivateChatUserChange()
        })
    }

    private fun initLiveRoom() {
        LPSdkVersionUtils.setSdkVersion(LPSdkVersionUtils.MULTI_CLASS_UI + BuildConfig.VERSION_NAME)
        routerViewModel.liveRoom.setOnLiveRoomListener { error ->
            when (error.code.toInt()) {
                LPError.CODE_ERROR_ROOMSERVER_LOSE_CONNECTION -> routerViewModel.actionReEnterRoom.value = LiveSDK.checkTeacherUnique to false
                LPError.CODE_ERROR_NETWORK_FAILURE -> showMessage(error.message)
                LPError.CODE_ERROR_NETWORK_MOBILE -> {
                    if (!mobileNetworkDialogShown && isForeground) {
                        mobileNetworkDialogShown = true
                        try {
                            if (isFinishing) return@setOnLiveRoomListener
                            MaterialDialog.Builder(this)
                                    .content(getString(R.string.live_mobile_network_hint))
                                    .positiveText(getString(R.string.live_mobile_network_confirm))
                                    .positiveColor(ContextCompat.getColor(this, R.color.live_blue))
                                    .onPositive { materialDialog, _ -> materialDialog.dismiss() }
                                    .canceledOnTouchOutside(true)
                                    .build()
                                    .show()
                        } catch (e: WindowManager.BadTokenException) {
                            e.printStackTrace()
                        }
                    } else {
                        showMessage(getString(R.string.live_mobile_network_hint_less))
                    }
                }
                LPError.CODE_ERROR_LOGIN_CONFLICT -> {
                }
                LPError.CODE_ERROR_OPEN_AUDIO_RECORD_FAILED -> {
                    if (!TextUtils.isEmpty(error.message)) {
                        showMessage(error.message)
                    }
                }
                LPError.CODE_ERROR_OPEN_AUDIO_CAMERA_FAILED -> {
                    if (!TextUtils.isEmpty(error.message)) {
                        showMessage(error.message)
                    }
                    oldBridge.detachLocalVideo()
                }
                LPError.CODE_WARNING_PLAYER_LAG, LPError.CODE_WARNING_PLAYER_MEDIA_SUBSCRIBE_TIME_OUT -> {
                }
                else -> if (!TextUtils.isEmpty(error.message)) {
                    showMessage(error.message)
                }
            }
        }
    }

    /**
     * 重进房间或者重连
     * reEnterRoom： true重进 false重连
     */
    private fun doReEnterRoom(checkTeacherUnique: Boolean,reEnterRoom:Boolean = false) {
        LiveSDK.checkTeacherUnique = checkTeacherUnique
        if (reEnterRoom) {
            release(true)
            enterRoom()
        } else {
            val liveRoom = routerViewModel.liveRoom
            release()
            enterRoom(liveRoom)
        }
    }

    private fun observeSuccess() {
        liveRoomViewModel.run {
            showRollCall.observeForever(showRollCallObserver)
            dismissRollCall.observeForever(dismissRollCallObserver)
            extraMediaChange.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    if (it.first == LPConstants.MediaSourceType.ExtCamera) {
                        if (it.second) {
                            showMessage(getString(R.string.lp_override_role_teacher) + "打开了辅助摄像头")
                        } else {
                            showMessage(getString(R.string.lp_override_role_teacher) + "关闭了辅助摄像头")
                        }
                    } else {
                        if (it.second) {
                            showMessage(getString(R.string.lp_override_role_teacher) + "打开了屏幕分享")
                        } else {
                            showMessage(getString(R.string.lp_override_role_teacher) + "关闭了屏幕分享")
                        }
                    }
                }
            })
            mediaStatus.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    when (it) {
                        LiveRoomViewModel.MediaStatus.VIDEO_AUDIO_ON -> showMessage(getString(R.string.lp_override_role_teacher) + "打开了麦克风和摄像头")
                        LiveRoomViewModel.MediaStatus.VIDEO_ON -> showMessage(getString(R.string.lp_override_role_teacher) + "打开了摄像头")
                        LiveRoomViewModel.MediaStatus.AUDIO_ON -> showMessage(getString(R.string.lp_override_role_teacher) + "打开了麦克风")
                        LiveRoomViewModel.MediaStatus.VIDEO_AUDIO_CLOSE -> showMessage(getString(R.string.lp_override_role_teacher) + "关闭了麦克风和摄像头")
                        LiveRoomViewModel.MediaStatus.VIDEO_CLOSE -> showMessage(getString(R.string.lp_override_role_teacher) + "关闭了摄像头")
                        LiveRoomViewModel.MediaStatus.AUDIO_CLOSE -> showMessage(getString(R.string.lp_override_role_teacher) + "关闭了麦克风")
                    }
                }
            })
            forbidChatAllModel.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    showMessageForbidAllChat(it)
                }
            })
            classSwitch.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    showClassSwitch()
                }
            })
            showToast.observeForever(toastObserver)
            reportAttention.observeForever(reportObserver)
        }
        routerViewModel.run {
            showTeacherIn.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    if (it) {
                        showMessage(getString(R.string.lp_override_role_teacher) + "进入了" + getString(R.string.lp_override_classroom))
                    } else {
                        showMessage(getString(R.string.lp_override_role_teacher) + "离开了" + getString(R.string.lp_override_classroom))
                    }
                }
            })
            clearScreen.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    //sw600dp和sw400dp没有activity_live_room_pad_room_top_parent
                    this@LiveRoomTripleActivity.findViewById<View>(R.id.activity_live_room_pad_room_top_parent)?.visibility = if (it) View.GONE else View.VISIBLE
                }
            })
            action2RedPacketUI.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    if (it.first) {
                        showRedPacketUI(it.second)
                    } else {
                        dismissRedPacketUI()
                    }
                }
            })
            showEvaDlg.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    if (it) showEvaluation() else dismissEvaDialog()
                }
            })
            quizStatus.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    when (it.first) {
                        RouterViewModel.QuizStatus.START -> {
                            onQuizStartArrived(it.second)
                        }
                        RouterViewModel.QuizStatus.RES -> {
                            onQuizRes(it.second)
                        }
                        RouterViewModel.QuizStatus.END -> {
                            onQuizEndArrived(it.second)
                        }
                        RouterViewModel.QuizStatus.SOLUTION -> {
                            onQuizSolutionArrived(it.second)
                        }
                        RouterViewModel.QuizStatus.CLOSE -> dismissQuizDlg()
                        RouterViewModel.QuizStatus.NOT_INIT -> {
                        }
                    }
                }
            })
            answerStart.observeForever(answerObserver)
            answerEnd.observe(this@LiveRoomTripleActivity, Observer {
                it?.let { answerEnd(it) }
            })
            removeAnswer.observe(this@LiveRoomTripleActivity, Observer {
                it?.let { removeAnswer() }
            })
            showTimer.observeForever(timerObserver)
            showTimerShowy.observe(this@LiveRoomTripleActivity , Observer {
                it?.let { if(it.first) showTimerShowy(it.second) }
            })
            action2Award.observe(this@LiveRoomTripleActivity, Observer {
                it?.let {
                    showAwardAnimation(it)
                }
            })
            timeOutStart.observeForever(timeOutObserver)
        }
    }

    private fun showAwardAnimation(userName: String) {
        findViewById<AwardView>(R.id.award_view).startAnim()
        findViewById<AwardView>(R.id.award_view).visibility = View.VISIBLE
        findViewById<AwardView>(R.id.award_view).setUserName(userName)
    }

    /**
     * 大小班切换 liveRoom 需保持不变
     * 其余资源清空再重新初始化
     */
    private fun showClassSwitch() {
        showMessage(getString(R.string.live_room_switch))
        val liveRoom = routerViewModel.liveRoom
        release()
        routerViewModel = getViewModel { RouterViewModel() }
        liveRoomViewModel = getViewModel { LiveRoomViewModel(routerViewModel) }
        oldBridge = OldLiveRoomRouterListenerBridge(routerViewModel)
        routerViewModel.liveRoom = liveRoom
        observeActions()
        initView()
        routerViewModel.liveRoom.switchRoom(object : LPLaunchListener {
            override fun onLaunchSteps(i: Int, i1: Int) {}

            override fun onLaunchError(lpError: LPError) {
                routerViewModel.actionShowError.value = lpError
            }

            override fun onLaunchSuccess(liveRoom: LiveRoom) {
                Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                    .onNext(Unit)
            routerViewModel.actionNavigateToMain = true
            }
        })
    }
    private fun release(quitRoom: Boolean = false) {
        LPRxUtils.dispose(disposeOfTeacherAbsent)
        LPRxUtils.dispose(disposeOfLoginConflict)
        LPRxUtils.dispose(disposeOfMarquee)
        LPRxUtils.dispose(disposeOfActionMain)
        marqueeAnimator?.cancel()
        removeAllFragment()
        supportFragmentManager.executePendingTransactions()
        if (quitRoom) {
            routerViewModel.liveRoom.quitRoom()
        }
        removeObservers()
        viewModelStore.clear()
        Router.instance.release()
    }

    private fun navigateToMain() {
        routerViewModel.checkUnique = true
        liveRoomViewModel.counter = 0
        liveRoomViewModel.subscribe()
        routerViewModel.isClassStarted.value = routerViewModel.liveRoom.isClassStarted
        val audioManager = getSystemService(Context.AUDIO_SERVICE)
        if (audioManager is AudioManager) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, -1, 0)
            minVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currentVolume, 0)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            if (current <= minVolume) {
                routerViewModel.liveRoom.player.mute()
            }
        }
        disposeOfTeacherAbsent = routerViewModel.liveRoom.speakQueueVM.observableOfActiveUsers.subscribe {
            if (!routerViewModel.liveRoom.isTeacherOrAssistant && routerViewModel.liveRoom.teacherUser == null) {
                showMessage(getString(R.string.live_room_teacher_absent))
                routerViewModel.isTeacherIn.value = false
            } else {
                routerViewModel.isTeacherIn.value = true
            }
            LPRxUtils.dispose(disposeOfTeacherAbsent)
        }
        LPRxUtils.dispose(disposeOfLoginConflict)
        disposeOfLoginConflict = routerViewModel.liveRoom.observableOfLoginConflict.observeOn(AndroidSchedulers.mainThread())
                .subscribe { iLoginConflictModel ->
                    if (enterRoomConflictListener != null) {
                        enterRoomConflictListener.onConflict(this, iLoginConflictModel.conflictEndType, object : LiveSDKWithUI.LPRoomExitCallback {
                            override fun exit() {
                                super@LiveRoomTripleActivity.finish()
                            }

                            override fun cancel() {
                                super@LiveRoomTripleActivity.finish()
                            }
                        })
                    } else {
                        super@LiveRoomTripleActivity.finish()
                    }
                }
        if (shareListener != null) {
            routerViewModel.isShowShare.value = true
            shareListener.getShareData(this, routerViewModel.liveRoom.roomId)
        } else {
            routerViewModel.isShowShare.value = false
        }
        if (!routerViewModel.liveRoom.isTeacherOrAssistant) {
            startMarqueeTape()
        }
        shouldShowTechSupport = false
        LiveSDK.checkTeacherUnique = false
        fullContainer.visibility = View.GONE
    }

    /**
     * 跑马灯
     */
    private fun startMarqueeTape() {
        val lampFormServer = if (routerViewModel.liveRoom.partnerConfig.liveHorseLamp == null) null else routerViewModel.liveRoom.partnerConfig.liveHorseLamp.value
        val lamp = if (TextUtils.isEmpty(liveHorseLamp)) lampFormServer else liveHorseLamp
        if (TextUtils.isEmpty(lamp)) {
            return
        }
        disposeOfMarquee = Observable.interval(0, liveHorseLampInterval.toLong(), TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showMarqueeTape(lamp) }
    }

    private fun showMarqueeTape(lamp: String?) {
        val textView = TextView(this)
        textView.text = lamp
        textView.setTextColor(ContextCompat.getColor(this, R.color.live_half_transparent_white))
        textView.textSize = 10f
        textView.setLines(1)
        textView.setPadding(20, 10, 20, 10)
        textView.gravity = Gravity.CENTER
        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.live_half_transparent_mask))
        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = Random().nextInt(DisplayUtils.getScreenHeightPixels(this) - 120)
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        findViewById<RelativeLayout>(R.id.activity_live_room_pad_background).addView(textView, lp)
        val width = DisplayUtils.getScreenWidthPixels(this).toFloat()
        marqueeAnimator?.cancel()
        marqueeAnimator = ObjectAnimator.ofFloat(textView, "translationX", -width)
        marqueeAnimator?.run {
            duration = (width * 20).toLong()
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    findViewById<RelativeLayout>(R.id.activity_live_room_pad_background).removeView(textView)
                }
            })
            start()
        }
    }

    private fun navigateToShare() {
        if (shareListener == null || shareListener.setShareList() == null) {
            return
        }
        val shareDialog = LPShareDialog.newInstance(shareListener.setShareList())
        shareDialog.setListener { type -> shareListener.onShareClicked(this, type) }
        showDialogFragment(shareDialog)
    }

    private fun showMessageForbidAllChat(result: LPRoomForbidChatResult) {
        val message = if (result.type == LPConstants.LPForbidChatType.TYPE_ALL) {
            if (result.isForbid) {
                getString(R.string.string_live_uisdk_forbid_all_true)
            } else {
                getString(R.string.string_live_uisdk_forbid_all_false)
            }
        } else {
            if (result.isForbid) {
                getString(R.string.string_live_uisdk_forbid_group_true)
            } else {
                getString(R.string.string_live_uisdk_forbid_group_false)
            }
        }
        showMessage(message)
    }

    private fun showRedPacketUI(lpRedPacketModel: LPRedPacketModel) {
        if (redPacketFragment != null) {
            if (redPacketPresenter?.redPacketing == true) {
                return
            }
            removeFragment(redPacketFragment)
            redPacketPresenter?.unSubscribe()
            redPacketPresenter = null
        }
        redPacketFragment = RedPacketFragment()
        redPacketPresenter = RedPacketPresenter(redPacketFragment, lpRedPacketModel)
        bindVP(redPacketFragment!!, redPacketPresenter!!)
        addFragment(R.id.activity_live_room_red_packet_pad, redPacketFragment)
    }

    private fun dismissRedPacketUI() {
        if (redPacketFragment != null) {
            removeFragment(redPacketFragment)
            redPacketFragment = null
            redPacketPresenter?.unSubscribe()
            redPacketPresenter = null
        }
    }

    //显示公告
    private fun navigateToAnnouncement() {
        if (announcementFragment?.isAdded == true) return
        announcementFragment = AnnouncementFragment.newInstance()
        val presenter = AnnouncementPresenter(announcementFragment)
        bindVP(announcementFragment!!, presenter)
        showDialogFragment(announcementFragment)
    }
    //显示问答
    private fun navigateToQAInteractive() {
        if (qaInteractiveFragment?.isAdded == true) return
        qaInteractiveFragment = QAInteractiveFragment()
        qaInteractiveFragment?.setViewMode(routerViewModel)
        showDialogFragment(qaInteractiveFragment)
    }

    private fun showRollCallDlg(time: Int, rollCallListener: OnPhoneRollCallListener.RollCall) {
        rollCallDialogFragment = RollCallDialogFragment()
        rollCallDialogFragment?.isCancelable = false
        rollCallDialogPresenter = RollCallDialogPresenter(rollCallDialogFragment)
        rollCallDialogPresenter?.setRollCallInfo(time, rollCallListener)
        bindVP(rollCallDialogFragment!!, rollCallDialogPresenter!!)
        showDialogFragment(rollCallDialogFragment)
    }

    private fun dismissRollCallDlg() {
        rollCallDialogPresenter?.timeOut()
        if (tempDialogFragment is RollCallDialogFragment) {
            tempDialogFragment = null
        }
        removeFragment(rollCallDialogFragment)
    }

    private fun onQuizStartArrived(jsonModel: LPJsonModel) {
        dismissQuizDlg()
        quizFragment = QuizDialogFragment()
        val args = Bundle()
        val forceJoin = if (!JsonObjectUtil.isJsonNull(jsonModel.data, "force_join")) {
            JsonObjectUtil.getAsInt(jsonModel.data, "force_join")
        } else {
            0
        }
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, forceJoin == 1)
        quizFragment?.arguments = args
        quizFragment?.isCancelable = false
        quizPresenter = QuizDialogPresenter(quizFragment)
        quizFragment?.onStartArrived(jsonModel)
        bindVP(quizFragment!!, quizPresenter!!)
        showDialogFragment(quizFragment)
    }

    private fun onQuizEndArrived(jsonModel: LPJsonModel) {
        if (quizFragment == null) {
            return
        }
        quizFragment?.onEndArrived(jsonModel)
    }

    private fun onQuizSolutionArrived(jsonModel: LPJsonModel) {
        dismissQuizDlg()
        quizFragment = QuizDialogFragment()
        val args = Bundle()
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, false)
        quizFragment?.arguments = args
        quizPresenter = QuizDialogPresenter(quizFragment)
        quizFragment?.onSolutionArrived(jsonModel)
        bindVP(quizFragment!!, quizPresenter!!)
        showDialogFragment(quizFragment)
    }

    private fun onQuizRes(jsonModel: LPJsonModel) {
        dismissQuizDlg()
        quizFragment = QuizDialogFragment()
        val args = Bundle()
        val forceJoin = if (!JsonObjectUtil.isJsonNull(jsonModel.data, "force_join")) {
            JsonObjectUtil.getAsInt(jsonModel.data, "force_join")
        } else {
            0
        }
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, forceJoin == 1)
        quizFragment?.arguments = args
        quizFragment?.isCancelable = false
        quizPresenter = QuizDialogPresenter(quizFragment)
        quizFragment?.onQuizResArrived(jsonModel)
        bindVP(quizFragment!!, quizPresenter!!)
        showDialogFragment(quizFragment)
    }

    private fun dismissQuizDlg() {
        if (quizFragment?.isAdded == true && quizFragment?.isVisible == true) {
            quizFragment?.dismissAllowingStateLoss()
        }
    }

    private fun answerStart(model: LPAnswerModel) {
        this.lpAnswerModel = model
        removeAnswer()
        val questionToolPresenter = QuestionToolPresenter()
        questionToolPresenter.setRouter(oldBridge)
        questionToolPresenter.setLpQuestionToolModel(model)
        questionToolFragment = QuestionToolFragment()
        questionToolPresenter.setView(questionToolFragment)
        bindVP(questionToolFragment!!, questionToolPresenter)
        findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).visibility = View.VISIBLE
        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).layoutParams = layoutParams
        addFragment(R.id.activity_dialog_question_tool_pad, questionToolFragment)

        showFragment(questionToolFragment)
    }

    /**
     * 显示答题器Answer
     */
    private fun showAnswer() {
        val questionShowPresenter = QuestionShowPresenter()
        questionShowPresenter.setRouter(oldBridge)
        questionShowPresenter.setLpQuestionToolModel(lpAnswerModel)
        questionShowFragment = QuestionShowFragment()
        questionShowPresenter.setView(questionShowFragment)
        bindVP(questionShowFragment!!, questionShowPresenter)
        findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).visibility = View.VISIBLE
        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).layoutParams = layoutParams
        addFragment(R.id.activity_dialog_question_tool_pad, questionShowFragment)

        showFragment(questionShowFragment)
    }

    private fun answerEnd(ended: Boolean) {
        if (questionToolFragment?.isAdded == true) {
            removeFragment(questionToolFragment)
            if (ended) {
                showToastMessage(getString(R.string.pad_class_answer_time_out))
            }
            findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).visibility = View.GONE
            questionToolFragment = null
        }
        if (lpAnswerModel?.isShowAnswer == true && ended) {
            showAnswer()
        }
    }

    private fun removeAnswer() {
        if (questionShowFragment?.isAdded == true) {
            removeFragment(questionShowFragment)
            findViewById<DragFrameLayout>(R.id.activity_dialog_question_tool_pad).visibility = View.GONE
            questionShowFragment = null
        }
    }

    private fun showTimer(lpbjTimerModel: LPBJTimerModel) {
        var lpbjTimerModel_t : LPBJTimerModel = lpbjTimerModel
        if(timerShowyFragment != null && showyTimerPresenter != null) {
            lpbjTimerModel_t = showyTimerPresenter!!.lpbjTimerModel
        }

        timerFragment = TimerFragment()
        val timerPresenter = TimerPresenter()
        timerPresenter.setRouter(oldBridge)
        timerPresenter.setTimerModel(lpbjTimerModel_t)
        timerPresenter.setRouterViewModel(routerViewModel)
        timerPresenter.setIsSetting(true)
        timerPresenter.setView(timerFragment)
        bindVP(timerFragment!!, timerPresenter)
        showDialogFragment(timerFragment)
    }

    private fun showTimerShowy(lpbjTimerModel: LPBJTimerModel) {
        if (timerFragment != null && timerFragment?.isAdded == true){
            removeFragment(timerFragment)
            timerFragment = null
        }
        if (timerShowyFragment != null && timerShowyFragment?.isVisible == true) {
            return
        }
        showyTimerPresenter = TimerPresenter()
        timerShowyFragment = TimerShowyFragment()
        showyTimerPresenter!!.setRouter(oldBridge)
        showyTimerPresenter!!.setRouterViewModel(routerViewModel)
        showyTimerPresenter!!.setTimerModel(lpbjTimerModel)
        showyTimerPresenter!!.setView(timerShowyFragment)
        bindVP(timerShowyFragment!!, showyTimerPresenter!!)

        val showy = findViewById<DragFrameLayout>(R.id.activity_dialog_timer_pad);
        showy.visibility = View.VISIBLE
        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        layoutParams.marginStart = DisplayUtils.dip2px(this ,16f)
        layoutParams.topMargin = DisplayUtils.dip2px(this ,34f)
        showy.layoutParams = layoutParams
        showy.assignParent(findViewById(R.id.activity_live_room_pad_background));
        addFragment(R.id.activity_dialog_timer_pad, timerShowyFragment)
        showFragment(timerShowyFragment)
    }

    private fun closeTimer() {
        if (timerShowyFragment?.isAdded == true) {
            removeFragment(timerShowyFragment)
            findViewById<DragFrameLayout>(R.id.activity_dialog_timer_pad).visibility = View.GONE
            timerShowyFragment!!.onDestroy()
            timerShowyFragment = null
        }
    }

    /**
     * 显示课后评价
     */
    private fun showEvaluation() {
        if (evaDialogFragment != null) {
            dismissEvaDialog()
        }
        evaDialogFragment = EvaDialogFragment()
        val evaDialogPresenter = EvaDialogPresenter(evaDialogFragment)
        val lpJsonModel = LPJsonModel()
        val data = JsonObject()
        data.addProperty("message_type", "class_end")
        lpJsonModel.data = data
        evaDialogFragment?.onClassEnd(lpJsonModel)
        bindVP(evaDialogFragment!!, evaDialogPresenter)
        showDialogFragment(evaDialogFragment)
    }

    private fun dismissEvaDialog() {
        if (evaDialogFragment?.isAdded == true && evaDialogFragment?.isVisible == true) {
            evaDialogFragment?.dismissAllowingStateLoss()
        }
    }

    private fun showMessage(message: String) {
        showToastMessage(message)
    }

    private fun <V : BaseView<P>, P : BasePresenter> bindVP(view: V, presenter: P) {
        presenter.setRouter(oldBridge)
        /*TODO 考虑用inline解决泛型擦除的问题*/
//        val c = view as BaseView<P>
        view.setPresenter(presenter)
    }

    private fun hideSysUIComponent() {
        supportActionBar?.hide()
    }

    private fun showExitDialog() = when {
        routerViewModel.liveRoom.isClassStarted && (routerViewModel.liveRoom.isTeacherOrAssistant||routerViewModel.liveRoom.isGroupTeacherOrAssistant) ->
            MaterialDialog.Builder(this)
                .apply {
                    title(getString(R.string.live_exit_hint_title))
                    content(getString(R.string.live_exit_hint_content))
                    contentColorRes(R.color.live_text_color_light)
                }
                .apply {
                    positiveText(getString(R.string.live_exit_hint_end_class_and_exit))
                    positiveColorRes(R.color.live_red)
                    onPositive { _, _ ->
                        routerViewModel.liveRoom.requestClassEnd()
                        finish()
                    }
                }
                .apply {
                    negativeText(getString(R.string.live_exit_hint_confirm))
                    negativeColorRes(R.color.live_blue)
                    onNegative { _, _ -> finish() }
                }
                .apply {
                    neutralText(getString(R.string.live_cancel))
                    neutralColorRes(R.color.live_blue)
                    onNeutral { dialog, _ -> dialog.dismiss() }
                }
                .build().show()
        else -> MaterialDialog.Builder(this)
                .apply {
                    title(getString(R.string.live_exit_hint_title))
                    content(getString(R.string.live_exit_hint_content))
                    contentColorRes(R.color.live_text_color_light)
                }
                .apply {
                    positiveText(getString(R.string.live_exit_hint_confirm))
                    positiveColorRes(R.color.live_red)
                    onPositive { _, _ -> finish() }
                }
                .apply {
                    negativeText(getString(R.string.live_cancel))
                    negativeColorRes(R.color.live_blue)
                    onNegative { dialog, _ -> dialog.dismiss() }
                }
                .build().show()
    }

    private fun showKickOutDlg(error: LPError) {
        routerViewModel.kickOut.value = Unit
        release()
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setMessage(error.message)
                .setPositiveButton(R.string.live_quiz_dialog_confirm) { dialog1, _ ->
                    dialog1.dismiss()
                    this.finish()
                }.create()
        dialog.setCancelable(false)
        if (!isFinishing) {
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this,R.color.live_blue))
        }
    }

    private fun showAuditionEndDlg(it: LPError) {
        if (mAuditionEndDialog == null) {
            mAuditionEndDialog = SimpleTextDialog(this, it)
            mAuditionEndDialog?.setCancelable(false)
            mAuditionEndDialog?.setOnOkClickListener { url2 ->
                var url = url2
                mAuditionEndDialog?.dismiss()
                mAuditionEndDialog = null

                //跳转link
                CommonUtils.startActivityByUrl(this,url)
                this.finish()
            }
        }
        mAuditionEndDialog?.isShowing?.let {
            mAuditionEndDialog?.show()
        }
    }

    private fun showErrorDlg(it: LPError?) {
        val errorCode = it?.code?.toInt()
        val errorModel = getViewModel { ErrorFragmentModel() }
        when (errorCode) {
            LPError.CODE_ERROR_LOGIN_UNIQUE_CONFLICT -> {
                routerViewModel.checkUnique = false
                errorModel.init(ErrorPadFragment.ErrorType.ERROR_HANDLE_CONFILICT)
                errorModel.checkUnique = false
            }
            LPError.CODE_ERROR_NEW_SMALL_COURSE, LPError.CODE_ERROR_MAX_STUDENT, LPError.CODE_ERROR_ENTER_ROOM_FORBIDDEN -> {
                errorModel.init(ErrorPadFragment.ErrorType.ERROR_HANDLE_REENTER, false, getString(R.string.live_override_error), it.message)
            }
            LPError.CODE_ERROR_HOST_UNKNOW -> {
                errorModel.init(ErrorPadFragment.ErrorType.ERROR_HANDLE_REENTER, false, getString(R.string.live_host_unknow), it.message)
            }
            LPError.CODE_ERROR_CLASS_EXPIRED -> {
                errorModel.init(ErrorPadFragment.ErrorType.ERROR_HANDLE_FINISH, false, getString(R.string.live_enter_deny), it.message)
            }
            else -> errorModel.init(ErrorPadFragment.ErrorType.ERROR_HANDLE_REENTER, true, getString(R.string.live_override_error), it!!.message)
        }
        replaceFragment(errorContainer.id, errorFragment)
    }

    private fun removeObservers() {
        routerViewModel.answerStart.removeObserver(answerObserver)
        routerViewModel.showTimer.removeObserver(timerObserver)
        routerViewModel.timeOutStart.removeObserver(timeOutObserver)
        routerViewModel.actionShowError.removeObserver(showErrorObserver)
        liveRoomViewModel.showToast.removeObserver(toastObserver)
        liveRoomViewModel.showRollCall.removeObserver(showRollCallObserver)
        liveRoomViewModel.dismissRollCall.removeObserver(dismissRollCallObserver)
        liveRoomViewModel.reportAttention.removeObserver(reportObserver)
        timeOutDisposes.forEach { (_, dispose) -> LPRxUtils.dispose(dispose) }
    }

    override fun getRouterListener(): LiveRoomRouterListener = oldBridge

    override fun onResume() {
        super.onResume()
        if (roomLifeCycleListener != null) {
            roomLifeCycleListener.onResume(this) { _, _ ->
                doReEnterRoom(LiveSDK.checkTeacherUnique, true)
            }
        }
    }

    override fun onDestroy() {
        routerViewModel.liveRoom.quitRoom()
        removeObservers()
        super.onDestroy()
        pptManagePresenter?.destroy()
        LPRxUtils.dispose(disposeOfTeacherAbsent)
        LPRxUtils.dispose(disposeOfLoginConflict)
        LPRxUtils.dispose(disposeOfMarquee)
        LPRxUtils.dispose(disposeOfActionMain)
        marqueeAnimator?.cancel()
        viewModelStore.clear()
        Router.instance.release()
    }
}


