package com.baijiayun.live.ui.speakpanel

import android.app.Dialog
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.OldLiveRoomRouterListenerBridge
import com.baijiayun.live.ui.speakerlist.SpeakersContract
import com.baijiayun.live.ui.speakerlist.item.Playable
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.wrapper.impl.LPVideoView
import kotlinx.android.synthetic.main.layout_item_video.view.*
import java.util.*

/**
 * Created by yongjiaming on 2019-10-18
 * Describe: 大班课远端视频
 */
class RemoteVideoItem(private val rootView: ViewGroup, media: IMediaModel, speakPresenter: SpeakersContract.Presenter)
    : RemoteItem(rootView, media, speakPresenter), Playable,LifecycleObserver {

    private var isVideoPlaying = false
    private var isAudioPlaying = false

    private var videoView: LPVideoView? = null
    private var itemType: SpeakItemType? = null
    private var showItemType: SpeakItemType? = null
    private var dialog: Dialog? = null
    private lateinit var loadingListener: LoadingListener
    private var loadingViewAnimation: Animation? = null
    private var isZOrderMediaOverlay = false


    private val videoContainer by lazy {
        container.item_local_speaker_avatar_container
    }
    private val container by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_item_video, rootView, false) as ViewGroup
    }

    private val mediaModelTv by lazy {
        container.item_local_speaker_name
    }
    private val speakerNameTv by lazy {
        container.item_local_speaker_name
    }
    init {
        initRemoteView()
        refreshNameTable()
    }

    override fun initView() {
    }

    private fun initRemoteView() {
        mediaModelTv.text = mediaModel.user.name
        registerClickEvent(container)
    }

    override fun refreshNameTable() {
        val remoteUser = mediaModel.user
        when {
            remoteUser.type == LPConstants.LPUserType.Teacher -> {
                var teacherLabel = liveRoom.customizeTeacherLabel
                teacherLabel = if (TextUtils.isEmpty(teacherLabel)) context.getString(R.string.live_teacher_hint) else "($teacherLabel)"
                speakerNameTv.text = remoteUser.name + teacherLabel
            }
            remoteUser.type == LPConstants.LPUserType.Assistant -> {
                var assistantLabel = liveRoom.customizeAssistantLabel
                assistantLabel = if (TextUtils.isEmpty(assistantLabel)) "(助教)" else "($assistantLabel)"
                if (liveRoom.presenterUser != null && liveRoom.presenterUser.userId == remoteUser.userId) {
                    assistantLabel = "(主讲)"
                }
                speakerNameTv.text = remoteUser.name + assistantLabel
            }
            else -> speakerNameTv.text = remoteUser.name
        }
    }

    override fun hasVideo(): Boolean = mediaModel.isVideoOn

    override fun hasAudio(): Boolean = mediaModel.isAudioOn

    override fun isVideoStreaming(): Boolean = isVideoPlaying

    override fun isAudioStreaming(): Boolean = isAudioPlaying

    override fun isStreaming(): Boolean = isVideoPlaying || isAudioPlaying

    override fun stopStreaming() {
        hideLoading()
        player.playAVClose(mediaModel.mediaId)
        isAudioPlaying = false
        isVideoPlaying = false
        videoContainer.removeAllViews()
        container.item_status_placeholder_ll.visibility = View.VISIBLE
        container.item_status_placeholder_tv.text = context.getString(R.string.pad_camera_closed)
        container.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_camera_close)
    }

    override fun refreshPlayable() {
        if (mediaModel.isVideoOn && !isVideoClosedByUser) {
            stopStreaming()
            streamVideo()
        } else if (mediaModel.isAudioOn) {
            stopStreaming()
            streamAudio()
        } else {
            stopStreaming()
        }
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
        when (showItemType) {
            SpeakItemType.Presenter -> {
                if (!mediaModel.isVideoOn && !mediaModel.isAudioOn) return  // 主讲人音视频未开启
                if (isVideoPlaying) {
                    options.add(getString(R.string.live_full_screen))
                    if (mediaModel.videoDefinitions.size > 1)
                        options.add(getString(R.string.live_switch_definitions))
                    options.add(getString(R.string.live_close_video))

                } else if (mediaModel.isVideoOn && !isVideoPlaying) {
                    options.add(getString(R.string.live_open_video))
                }
                if (canCurrentUserSetPresenter() && mediaModel.user.type == LPConstants.LPUserType.Assistant) {
                    options.add(getString(R.string.live_unset_presenter))
                }
            }
            SpeakItemType.Video -> {
                options.add(getString(R.string.live_full_screen))
                if (canCurrentUserSetPresenter() && isThisTeacherOrAssistant)
                    options.add(getString(R.string.live_set_to_presenter))
                if (mediaModel.user.type == LPConstants.LPUserType.Student && liveRoom.isTeacherOrAssistant) {
                    if (liveRoom.partnerConfig.liveDisableGrantStudentBrush != 1) {
                        if (liveRoom.speakQueueVM.studentsDrawingAuthList.contains(mediaModel.user.number))
                            options.add(getString(R.string.live_unset_auth_drawing))
                        else
                            options.add(getString(R.string.live_set_auth_drawing))
                    }
                }
                if (mediaModel.videoDefinitions.size > 1)
                    options.add(getString(R.string.live_switch_definitions))
                if (liveRoom.isTeacherOrAssistant && mediaModel.user.type == LPConstants.LPUserType.Student) {
                    options.add(getString(R.string.live_award))
                }
                options.add(getString(R.string.live_close_video))
                if (liveRoom.isTeacherOrAssistant && liveRoom.roomType == LPConstants.LPRoomType.Multi && mediaModel.user.type != LPConstants.LPUserType.Teacher)
                    options.add(getString(R.string.live_close_speaking))
            }
            SpeakItemType.Audio -> {
                if (mediaModel.isVideoOn)
                    options.add(getString(R.string.live_open_video))
                if (canCurrentUserSetPresenter() && (mediaModel.user.type == LPConstants.LPUserType.Teacher || mediaModel.user.type == LPConstants.LPUserType.Assistant))
                    options.add(getString(R.string.live_set_to_presenter))
                if (mediaModel.user.type == LPConstants.LPUserType.Student && liveRoom.isTeacherOrAssistant) {
                    if (liveRoom.partnerConfig.liveDisableGrantStudentBrush != 1) {
                        if (liveRoom.speakQueueVM.studentsDrawingAuthList.contains(mediaModel.user.number))
                            options.add(getString(R.string.live_unset_auth_drawing))
                        else
                            options.add(getString(R.string.live_set_auth_drawing))
                    }
                }
                if (liveRoom.isTeacherOrAssistant && liveRoom.roomType == LPConstants.LPRoomType.Multi)
                    options.add(getString(R.string.live_close_speaking))
            }
            else -> {
            }
        }
        if (options.size <= 0) return
        if (context.isFinishing) return
        dialog = MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback { materialDialog, _, _, charSequence ->
                    if (context.isFinishing || context.isDestroyed) {
                        return@itemsCallback
                    }
                    when (charSequence.toString()) {
                        getString(R.string.live_close_video)-> {
                            stopStreaming()
                            streamAudio()
                            setVideoCloseByUser(true)
                            presenter.handleUserCloseAction(this)
                        }
                        getString(R.string.live_close_speaking) ->{
                            liveRoom.speakQueueVM.closeOtherSpeak(mediaModel.user.userId)
                        }
                        getString(R.string.live_open_video) ->{
                            stopStreaming()
                            streamVideo()
                            setVideoCloseByUser(false)
                            presenter.handleUserCloseAction(this)
                        }
                        getString(R.string.live_full_screen)->{
                            (presenter.routerListener as OldLiveRoomRouterListenerBridge).setMainVideo2FullScreen(
                                    identity == presenter.routerListener.liveRoom.teacherUser.userId)
                            presenter.routerListener.fullScreenItem.switchBackToList()
                            switchToFullScreen()
                        }
                        getString(R.string.live_switch_definitions)->{
                            showVideoDefinitionSwitchDialog()
                        }
                        getString(R.string.live_set_to_presenter)->{
                            liveRoom.speakQueueVM.requestSwitchPresenter(mediaModel.user.userId)
                        }
                        getString(R.string.live_unset_presenter)->{
                            liveRoom.speakQueueVM.requestSwitchPresenter(liveRoom.currentUser.userId)
                        }
                        getString(R.string.live_set_auth_drawing)->{
                            liveRoom.speakQueueVM.requestStudentDrawingAuthChange(true, mediaModel.user.number)
                        }
                        getString(R.string.live_unset_auth_drawing)->{
                            liveRoom.speakQueueVM.requestStudentDrawingAuthChange(false, mediaModel.user.number)
                        }
                        getString(R.string.live_award)->{
                            presenter.requestAward(mediaModel.user.number)
                        }
                        else ->{
                        }
                    }
                    materialDialog.dismiss()
                }
                .show()
    }

    private fun streamAudio() {
        player.playAudio(mediaModel.mediaId)
        isAudioPlaying = true
    }

    fun setZOrderMediaOverlay(isZOrderMediaOverlay:Boolean) {
        this.isZOrderMediaOverlay = isZOrderMediaOverlay
    }

    private fun streamVideo() {
        if (videoView == null) {
            videoView = LPVideoView(context)
            videoView?.aspectRatio = LPConstants.LPAspectRatio.Fit
            videoView?.setZOrderMediaOverlay(isZOrderMediaOverlay)
        }
        videoContainer.removeAllViews()
        videoContainer.addView(videoView, layoutParams)
        container.item_status_placeholder_ll.visibility = View.GONE
        mediaModelTv.visibility = View.VISIBLE
        showLoading()

        player.playVideo(mediaModel.mediaId, videoView)
        isAudioPlaying = true
        isVideoPlaying = true
    }

    override fun getUser(): IUserModel = mediaModel.user

    override fun notifyAwardChange(count: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemType(): SpeakItemType = itemType!!

    override fun getView(): View = container

    override fun getIdentity(): String {
        return if (mediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtCamera || mediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare) {
            mediaModel.user.userId + "_1"
        } else mediaModel.user.userId
    }

    override fun setMediaModel(_mediaModel: IMediaModel) {
        mediaModel = _mediaModel
        refreshItemType()
        refreshNameTable()
    }

    override fun refreshItemType() {
        showItemType = if (liveRoom.presenterUser != null && mediaModel.user.userId == liveRoom.presenterUser.userId) {
            SpeakItemType.Presenter
        } else {
            if (mediaModel.isVideoOn && !isVideoClosedByUser) {
                SpeakItemType.Video
            } else {
                SpeakItemType.Audio
            }
        }
        itemType =  if (mediaModel.isVideoOn && !isVideoClosedByUser) {
            SpeakItemType.Video
        } else {
            SpeakItemType.Audio
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }

    /**
     * 手动删除item生命周期<=fragment
     */
    fun destroy() {
        onDestroy()
    }

    fun showTeacherLeave(isLeave : Boolean = true){
        if(isLeave){
            container.item_status_placeholder_ll.visibility = View.VISIBLE
            container.item_status_placeholder_tv.text = context.getString(R.string.pad_leave_room)
            container.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_leave_room)
            mediaModelTv.visibility = View.GONE
        } else{
            container.item_status_placeholder_ll.visibility = View.VISIBLE
            container.item_status_placeholder_tv.text = context.getString(R.string.pad_camera_closed)
            container.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_camera_close)
            mediaModelTv.visibility = View.GONE
        }
    }

    private fun showLoading() {
        if (!::loadingListener.isInitialized) {
            loadingListener = LoadingListener(this)
            player.addPlayerListener(loadingListener)
        }
        container.item_speak_speaker_loading_container.visibility = View.VISIBLE
        if (loadingViewAnimation == null) {
            loadingViewAnimation = AnimationUtils.loadAnimation(context, R.anim.live_video_loading)
            loadingViewAnimation?.interpolator = LinearInterpolator()
        }
        container.item_speak_speaker_loading_img.startAnimation(loadingViewAnimation)
    }

    override fun hideLoading() {
        container.item_speak_speaker_loading_container.visibility = View.GONE
        loadingViewAnimation?.cancel()
        container.item_speak_speaker_loading_img.clearAnimation()
    }
}