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
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.OldLiveRoomRouterListenerBridge
import com.baijiayun.live.ui.speakerlist.SpeakersContract
import com.baijiayun.live.ui.speakerlist.item.LocalItem
import com.baijiayun.live.ui.speakerlist.item.Playable
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.utils.LPRxUtils
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel
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
        container.findViewById<View>(R.id.item_status_placeholder_ll)
    }
    private val videoStatusIv by lazy {
        container.findViewById<ImageView>(R.id.item_status_placeholder_iv)
    }
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
                assistantLabel = if (TextUtils.isEmpty(assistantLabel)) "" else "($assistantLabel)"
                if (liveRoom.presenterUser != null && liveRoom.presenterUser.userId == currentUser.userId) {
                    assistantLabel = "(主讲)"
                }
                speakerNameTv.text = liveRoom.currentUser.name + assistantLabel
            }
            else -> speakerNameTv.text = liveRoom.currentUser.name
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun onDestroy() {
        super.onDestroy()
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }

    fun destroy() {
        onDestroy()
    }

    fun setZOrderMediaOverlay(isZOrderMediaOverlay: Boolean) {
        this.isZOrderMediaOverlay = isZOrderMediaOverlay
    }

    override fun streamVideo() {
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
        (presenter.routerListener as OldLiveRoomRouterListenerBridge).setMainVideo2FullScreen(isMainVideo())
    }

    override fun enableClearScreen(): Boolean {
        return true
    }

    override fun isSyncPPTVideo(): Boolean {
        return true
    }

    override fun syncPPTVideo() {
        if (liveRoom.isSyncPPTVideo && (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) && isMainVideo()) {
            showSwitchDialog()
        } else {
            switch2FullScreenLocal()
        }
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
                            if (liveRoom.isSyncPPTVideo && (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) && isMainVideo()) {
                                showSwitchDialog()
                            } else {
                                switch2FullScreenLocal()
                            }
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

    private fun isMainVideo() :Boolean{
        return (presenter.routerListener.liveRoom.teacherUser != null && (identity == presenter.routerListener.liveRoom.teacherUser.userId ||
                identity == presenter.routerListener.liveRoom.teacherUser.userId + "_1"))|| identity == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID
    }

    private fun switch2FullScreenLocal() {
        (presenter.routerListener as OldLiveRoomRouterListenerBridge).setMainVideo2FullScreen(isMainVideo())
        presenter.routerListener.fullScreenItem.switchBackToList()
        switchToFullScreen()
    }

    private fun switch2FullScreenSync() {
        liveRoom.requestPPTVideoSwitch(liveRoom.teacherUser != null && identity == liveRoom.teacherUser.userId)
        switch2FullScreenLocal()
    }

    private fun showSwitchDialog() {
        if (context == null || context !is LiveRoomBaseActivity) {
            return
        }
        if (context.isFinishing || context.isDestroyed) {
            return
        }
        context.let {
            MaterialDialog.Builder(it)
                    .title(getString(R.string.live_exit_hint_title))
                    .content(getString(R.string.live_pad_sync_video_ppt))
                    .contentColorRes(R.color.live_text_color_light)
                    .positiveText(R.string.live_pad_switch_sync)
                    .positiveColorRes(R.color.live_blue)
                    .negativeText(R.string.live_pad_switch_local)
                    .negativeColorRes(R.color.live_text_color_light)
                    .onPositive { _, _ -> switch2FullScreenSync() }
                    .onNegative { _, _ -> switch2FullScreenLocal() }
                    .show()
        }
    }

    override fun getView() = container

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
            //attention app处于后台状态不再允许操作视频推拉流（本来就已经停止推流了，处于后台开关摄像头不生效）
            if(!isInBackgroundStatus){
                if (shouldStreamVideo) {
                    streamVideo()
                } else {
                    recorder.detachVideo()
                    showVideoClose()
                }
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

    private fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override fun notifyAwardChange(count: Int) {
        if(count > 0){
            container.item_speak_speaker_award_count_tv.visibility = View.VISIBLE
            container.item_speak_speaker_award_count_tv.text = count.toString()
        }
    }
}