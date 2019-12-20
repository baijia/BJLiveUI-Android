package com.baijiayun.live.ui.speakpanel

import android.app.Dialog
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.support.annotation.StringRes
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.OldLiveRoomRouterListenerBridge
import com.baijiayun.live.ui.speakerlist.SpeakersContract
import com.baijiayun.live.ui.speakerlist.item.LocalItem
import com.baijiayun.live.ui.speakerlist.item.Playable
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.wrapper.impl.LPCameraView
import kotlinx.android.synthetic.main.layout_item_video.view.*
import java.util.*

/**
 * Created by yongjiaming on 2019-10-18
 * Describe:
 */
class LocalVideoItem(private val rootView: ViewGroup, speakPresenter: SpeakersContract.Presenter) : LocalItem(rootView, speakPresenter), Playable, LifecycleObserver {

    private val videoContainer by lazy {
        container.item_local_speaker_avatar_container
    }
    private val container by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_item_video, rootView, false) as ViewGroup
    }
    private val speakerNameTv by lazy {
        container.findViewById<TextView>(R.id.item_local_speaker_name)
    }
    private val videoStatusTv by lazy {
        container.findViewById<TextView>(R.id.item_status_placeholder_tv)
    }
    private val videoStatusContainer by lazy {
        container.findViewById<LinearLayout>(R.id.item_status_placeholder_ll)
    }
    private val videoStatusIv by lazy {
        container.findViewById<ImageView>(R.id.item_status_placeholder_iv)
    }
    private var shouldStreamVideo = false
    private var shouldStreamAudio = false
    private var dialog: Dialog? = null
    private var isZOrderMediaOverlay = false

    init {
        refreshNameTable()
        registerClickEvent(container)
    }

    override fun initView() {
    }

    override fun refreshNameTable() {
        val currentUser = liveRoom.currentUser
        when {
            currentUser.type == LPConstants.LPUserType.Teacher -> {
                var teacherLabel = liveRoom.customizeTeacherLabel
                teacherLabel = if (TextUtils.isEmpty(teacherLabel)) context.getString(R.string.live_teacher_hint) else "($teacherLabel)"
                speakerNameTv.text = liveRoom.currentUser.name + teacherLabel
            }
            currentUser.type == LPConstants.LPUserType.Assistant -> {
                var assistantLabel = liveRoom.customizeAssistantLabel
                assistantLabel = if (TextUtils.isEmpty(assistantLabel)) "(助教)" else "($assistantLabel)"
                if (liveRoom.presenterUser != null && liveRoom.presenterUser.userId == currentUser.userId) {
                    assistantLabel = "(主讲)"
                }
                speakerNameTv.text = liveRoom.currentUser.name + assistantLabel
            }
            else -> speakerNameTv.text = liveRoom.currentUser.name
        }
    }

    private var attachVideoOnResume: Boolean = false
    private var attachAudioOnResume: Boolean = false

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun onResume() {
        if (attachAudioOnResume) {
            streamAudio()
            attachAudioOnResume = false
        }
        if (attachVideoOnResume) {
            streamVideo()
            attachVideoOnResume = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    override fun onPause() {
        attachAudioOnResume = isAudioStreaming
        attachVideoOnResume = isVideoStreaming
        stopStreaming()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }

    fun destroy() {
        onDestroy()
    }

    private fun streamAudio() {
        if (!recorder.isPublishing) {
            recorder.publish()
        }
        recorder.attachAudio()
    }

    fun setZOrderMediaOverlay(isZOrderMediaOverlay:Boolean) {
        this.isZOrderMediaOverlay = isZOrderMediaOverlay
    }

    private fun streamVideo() {
        if (cameraView == null) {
            cameraView = LPCameraView(context)
        }
        videoContainer.removeAllViews()
        videoStatusContainer.visibility = View.GONE
        videoContainer.addView(cameraView)
        recorder.setPreview(cameraView)
        cameraView.setZOrderMediaOverlay(isZOrderMediaOverlay)
        if (!recorder.isPublishing) {
            recorder.publish()
        }
        recorder.attachVideo()
    }

    override fun doOnSwitch() {
        super.doOnSwitch()
        (presenter.routerListener as OldLiveRoomRouterListenerBridge).setMainVideo2FullScreen(
                identity == presenter.routerListener.liveRoom.teacherUser.userId)
    }

    override fun enableClearScreen(): Boolean {
        return true
    }

    override fun showOptionDialog() {
        val options = ArrayList<String>()
        options.add(getString(R.string.live_full_screen))
        if (liveRoom.partnerConfig.isEnableSwitchPresenter == 1 && liveRoom.currentUser.type == LPConstants.LPUserType.Teacher
                && liveRoom.presenterUser != null && liveRoom.presenterUser.userId != liveRoom.currentUser.userId)
            options.add(getString(R.string.live_set_to_presenter))
        options.add(getString(R.string.live_recorder_switch_camera))
        if (recorder.isBeautyFilterOn) {
            options.add(getString(R.string.live_recorder_pretty_filter_off))
        } else {
            options.add(getString(R.string.live_recorder_pretty_filter_on))
        }
        if (recorder.isVideoAttached) {
            options.add(getString(R.string.live_close_video))
        } else {
            options.add(getString(R.string.live_open_video))
        }
        if (context.isFinishing) return
        dialog = MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback { materialDialog, _, _, charSequence ->
                    if (context.isFinishing || context.isDestroyed) {
                        return@itemsCallback
                    }
                    when {
                        getString(R.string.live_close_video) == charSequence.toString() -> presenter.routerListener.detachLocalVideo()
                        getString(R.string.live_open_video) == charSequence.toString() -> presenter.routerListener.attachLocalVideo()
                        getString(R.string.live_full_screen) == charSequence.toString() -> {
                            (presenter.routerListener as OldLiveRoomRouterListenerBridge).setMainVideo2FullScreen(
                                    identity == presenter.routerListener.liveRoom.teacherUser.userId)
                            presenter.routerListener.fullScreenItem.switchBackToList()
                            switchToFullScreen()
                        }
                        getString(R.string.live_set_to_presenter) == charSequence.toString() -> liveRoom.speakQueueVM.requestSwitchPresenter(liveRoom.currentUser.userId)
                        getString(R.string.live_recorder_pretty_filter_on) == charSequence.toString() -> recorder.openBeautyFilter()
                        getString(R.string.live_recorder_pretty_filter_off) == charSequence.toString() -> recorder.closeBeautyFilter()
                        getString(R.string.live_recorder_switch_camera) == charSequence.toString() -> recorder.switchCamera()
                    }
                    materialDialog.dismiss()
                }
                .show()
    }

    override fun getIdentity() = liveRoom.currentUser.userId

    override fun getItemType() = SpeakItemType.Record

    override fun getView() = container

    override fun hasVideo() = shouldStreamVideo

    override fun hasAudio() = shouldStreamAudio

    override fun isVideoStreaming() = recorder.isVideoAttached

    override fun isAudioStreaming() = recorder.isAudioAttached

    override fun isStreaming() = recorder.isPublishing

    override fun stopStreaming() {
        showVideoClose()
        recorder.stopPublishing()
    }

    private fun showVideoClose() {
        videoContainer.removeAllViews()
        videoStatusContainer.visibility = View.VISIBLE
        videoStatusTv.text = getString(R.string.pad_camera_closed)
        videoStatusIv.setImageResource(R.drawable.ic_pad_camera_close)
    }


    override fun refreshPlayable() {
        if (shouldStreamVideo || shouldStreamAudio) {
            if (shouldStreamVideo) {
                streamVideo()
            } else {
                recorder.detachVideo()
                showVideoClose()
            }
            if (shouldStreamAudio) {
                streamAudio()
            } else {
                recorder.detachAudio()
            }
        } else {
            stopStreaming()
        }
    }

    override fun getUser(): IUserModel = liveRoom.currentUser

    override fun notifyAwardChange(count: Int) {

    }

    override fun setShouldStreamAudio(shouldStreamAudio: Boolean) {
        this.shouldStreamAudio = shouldStreamAudio
    }

    override fun setShouldStreamVideo(shouldStreamVideo: Boolean) {
        this.shouldStreamVideo = shouldStreamVideo
    }

    private fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }
}