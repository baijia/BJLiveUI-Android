package com.baijiayun.live.ui.mainvideopanel

import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.Observer
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.isMainVideoItem
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.removeSwitchableFromParent
import com.baijiayun.live.ui.speakerlist.item.LocalItem
import com.baijiayun.live.ui.speakerlist.item.SpeakItem
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.speakpanel.LocalVideoItem
import com.baijiayun.live.ui.speakpanel.RemoteVideoItem
import com.baijiayun.live.ui.speakpanel.SpeakPresenterBridge
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LiveRoomImpl
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.utils.LPRxUtils
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.layout_item_video.view.*

/**
 * Created by Shubo on 2019-10-10.
 * 老师视频窗口
 */
class MainVideoFragment : BasePadFragment() {
    private val presenter by lazy {
        SpeakPresenterBridge((activity as LiveRoomBaseActivity).routerListener)
    }
    private val container by lazy {
        view as ViewGroup
    }
    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }
    private val placeholderItem by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_item_video, container, false) as ViewGroup
    }
    private val kickOutObserver by lazy {
        Observer<Unit> {
            it?.let {
                speakItems.forEach { (_, item) ->
                    if (item is LifecycleObserver) {
                        lifecycle.removeObserver(item)
                    }
                }
            }
        }
    }
    private var disposableOfCloudRecordAllowed: Disposable? = null
    private var disposableOfCloudRecord: Disposable? = null
    private var isCloudRecording = false
    //item集合：合流时老师可能有多个流
    private var speakItems = HashMap<String, SpeakItem>()
    private var lastMixOn = false

    override fun init(view: View) {
    }

    companion object {
        fun newInstance() = MainVideoFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_main_video


    private fun createLocalPlayableItem(): LocalVideoItem = kotlin.run {
        val localItem = LocalVideoItem(container, presenter)
        localItem.setZOrderMediaOverlay(true)
        lifecycle.addObserver(localItem)
        localItem
    }

    private fun initLocalAV(shouldStreamVideo: Boolean, shouldStreamAudio: Boolean) {
        if (liveRoom.currentUser.type != LPConstants.LPUserType.Teacher) {
            return
        }
        if (!liveRoom.getRecorder<LPRecorder>().isPublishing) {
            liveRoom.getRecorder<LPRecorder>().publish()
        }
        if (checkCameraAndMicPermission()) {
            val localVideoItem = createLocalPlayableItem()
            speakItems[routerViewModel.liveRoom.currentUser.userId] = localVideoItem
            with(localVideoItem) {
                setShouldStreamAudio(shouldStreamAudio)
                setShouldStreamVideo(shouldStreamVideo)
                refreshPlayable()
            }
            removeAllViews()
            container.addView(localVideoItem.view)
            routerViewModel.mainVideoItem.value = localVideoItem
        }
    }

    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer {
            if (it != true) {
                return@Observer
            }
            initSuccess()
        })
    }

    private fun showRemoteStatus(isLeave: Boolean) {
        if (isLeave) {
            placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_leave_room)
            placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_leave_room)
        } else {
            placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_camera_close)
            placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_camera_closed)
        }
        var teacherLabel = liveRoom.customizeTeacherLabel
        teacherLabel = if (TextUtils.isEmpty(teacherLabel)) context?.getString(R.string.live_teacher_hint) else "($teacherLabel)"
        placeholderItem.item_local_speaker_name.text = if (liveRoom.teacherUser != null) liveRoom.teacherUser.name + teacherLabel else ""
        placeholderItem.item_local_speaker_name.visibility = View.VISIBLE
        placeholderItem.visibility = View.VISIBLE
        if (isLeave) {
            //老师离开教室把原来在老师位置的ppt移到全屏位置
            val myPadPPTView = routerViewModel.pptViewData.value
            if (myPadPPTView is MyPadPPTView && myPadPPTView.pptStatus == MyPadPPTView.PPTStatus.MainVideo) {
                myPadPPTView.switch2FullScreenLocal()
            }
            speakItems.clear()
        }
        removeAllViews()
        container.addView(placeholderItem)
    }

    @SuppressLint("SetTextI18n")
    private fun showLocalStatus() {
        placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_camera_close)
        placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_camera_closed)
        val teacherLabel = liveRoom.customizeTeacherLabel ?: getString(R.string.live_teacher_hint)
        placeholderItem.item_local_speaker_name.text = liveRoom.currentUser.name + teacherLabel
        placeholderItem.visibility = View.VISIBLE
        removeAllViews()
        container.addView(placeholderItem)
    }

    private fun initSuccess() {
        removeAllViews()
        container.addView(placeholderItem)
        if (!routerViewModel.liveRoom.isTeacher) {
            showRemoteStatus(false)
            routerViewModel.isTeacherIn.observe(this, Observer {
                it?.let {
                    showRemoteStatus(!it)
                }
            })
        } else {
            showLocalStatus()
        }

        routerViewModel.isClassStarted.observe(this, Observer {
            if (it == true) {
                container.postDelayed({
                    initLocalAV(shouldStreamVideo = true, shouldStreamAudio = true)
                    autoRequestCloudRecord()
                }, 500)
            }
        })

        routerViewModel.notifyRemotePlayableChanged.observe(this, Observer<IMediaModel> {
            it?.let {
                //合流会出现userId,userNumber 都为空的问题
                if (it.user.userId == null) {
                    return@Observer
                }
                if (routerViewModel.isTeacherIn.value != true) {
                    return@Observer
                }
                val isMixStream = it.user.userId == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID
                if (!isMixStream && it.user.type != LPConstants.LPUserType.Teacher) {
                    return@Observer
                }
                if (!isMixStream && (it.mediaSourceType == LPConstants.MediaSourceType.ExtCamera
                                || it.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare)) {
                    return@Observer
                }
                //合流只显示userId = -1的流，之前播放的流关闭
                if (routerViewModel.liveRoom.isMixModeOn && !isMixStream) {
                    val speakItem = speakItems[it.user.userId]
                    if (speakItem is RemoteVideoItem) {
                        speakItem.stopStreamingOnly(it)
                    }
                    return@Observer
                }
                //非合流，之前合流关闭
                if (!routerViewModel.liveRoom.isMixModeOn && isMixStream) {
                    val speakItem = speakItems[it.user.userId]
                    if (speakItem is RemoteVideoItem) {
                        speakItem.stopStreamingOnly(it)
                    }
                    return@Observer
                }
                var remoteVideoItem = speakItems[it.user.userId]
                val mixModelChange = lastMixOn xor routerViewModel.liveRoom.isMixModeOn
                if (remoteVideoItem == null || remoteVideoItem is LocalVideoItem || mixModelChange) {
                    remoteVideoItem = RemoteVideoItem(container, it, presenter)
                    remoteVideoItem.setZOrderMediaOverlay(true)
                    lifecycle.addObserver(remoteVideoItem)
                    val myPadPPTView = routerViewModel.pptViewData.value as MyPadPPTView
                    if (mixModelChange && myPadPPTView.pptStatus == MyPadPPTView.PPTStatus.MainVideo) {
                        routerViewModel.switch2FullScreen.value?.run {
                            removeSwitchableFromParent(this)
                        }
                        remoteVideoItem.switchToFullScreen()
                    } else {
                        removeAllViews()
                        container.addView(remoteVideoItem.view)
                    }
                    speakItems[it.user.userId] = remoteVideoItem
                    routerViewModel.mainVideoItem.value = remoteVideoItem
                }
                remoteVideoItem as RemoteVideoItem
                remoteVideoItem.setMediaModel(it)
                remoteVideoItem.refreshPlayable()
                lastMixOn = routerViewModel.liveRoom.isMixModeOn
            }
        })

        routerViewModel.notifyLocalPlayableChanged.observe(this, Observer {
            it?.run {
                if (!liveRoom.isTeacher) {
                    return@run
                }
                val localVideoItem = speakItems[routerViewModel.liveRoom.currentUser.userId]
                if (localVideoItem != null && localVideoItem is LocalVideoItem) {
                    localVideoItem.setShouldStreamVideo(it.first)
                    localVideoItem.setShouldStreamAudio(it.second)
                    localVideoItem.refreshPlayable()
                } else {
                    if (it.first || it.second) {
                        initLocalAV(it.first, it.second)
                    }
                }
            }
        })

        routerViewModel.actionWithLocalAVideo.observe(this, Observer {
            it?.let {
                if (!liveRoom.isTeacher) {
                    return@Observer
                }
                if (it.second) {
                    attachLocalAudio()
                } else {
                    routerViewModel.liveRoom.getRecorder<LPRecorder>().detachAudio()
                }
                if (it.first) {
                    attachLocalVideo()
                } else {
                    detachLocalVideo()
                }
            }
        })

        routerViewModel.actionWithAttachLocalAudio.observe(this, Observer {
            it?.run {
                if (!liveRoom.isTeacher) {
                    return@run
                }
                attachLocalAudio()
            }
        })

        routerViewModel.switch2MainVideo.observe(this, Observer {
            it?.let {
                if (isMainVideoItem(it,liveRoom)) {
                    removeSwitchableFromParent(it)
                    addView(it.view)
                }
            }
        })

        routerViewModel.kickOut.observeForever(kickOutObserver)

        disposableOfCloudRecord = liveRoom.observableOfCloudRecordStatus.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    isCloudRecording = it
                }

        container.postDelayed({
            if ((liveRoom as LiveRoomImpl).roomLoginModel.started) {
                autoRequestCloudRecord()
            }
        }, 500)
    }

    private fun addView(child: View) {
        container.addView(child, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }
    private fun removeAllViews() {
        speakItems.forEach { (_, speakItem) ->
            if (speakItem is LifecycleObserver) {
                lifecycle.removeObserver(speakItem)
            }
        }
        container.removeAllViews()
    }

    /**
     * 三分屏仅允许老师开启录制
     */
    private fun autoRequestCloudRecord() {
        if (liveRoom.autoStartCloudRecordStatus == 1) {
            doRequestCloudRecord()
        }
    }

    private fun doRequestCloudRecord() {
        if (routerViewModel.liveRoom.isTeacher && !isCloudRecording) {
            LPRxUtils.dispose(disposableOfCloudRecordAllowed)
            disposableOfCloudRecordAllowed = liveRoom.requestIsCloudRecordAllowed()
                    .subscribe { lpCheckRecordStatusModel ->
                        if (lpCheckRecordStatusModel.recordStatus == 1) {
                            liveRoom.requestCloudRecord(true)
                        } else {
                            showToastMessage(lpCheckRecordStatusModel.reason)
                        }
                    }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        routerViewModel.kickOut.removeObserver(kickOutObserver)
        removeAllViews()
        speakItems.clear()
        LPRxUtils.dispose(disposableOfCloudRecordAllowed)
        LPRxUtils.dispose(disposableOfCloudRecord)
    }
}