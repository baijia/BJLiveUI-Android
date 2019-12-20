package com.baijiayun.live.ui.mainvideopanel

import android.arch.lifecycle.Observer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.baijiayun.live.ui.LiveRoomTripleActivity
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.speakpanel.LocalVideoItem
import com.baijiayun.live.ui.speakpanel.RemoteVideoItem
import com.baijiayun.live.ui.speakpanel.SpeakPresenterBridge
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.wrapper.LPRecorder
import kotlinx.android.synthetic.main.layout_item_video.view.*

/**
 * Created by Shubo on 2019-10-10.
 * 老师视频窗口
 */
class MainVideoFragment : BasePadFragment() {
    private val presenter by lazy {
        SpeakPresenterBridge((activity as LiveRoomTripleActivity).getRouterListener())
    }
    private val container by lazy {
        view as ViewGroup
    }
    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }
    private lateinit var localVideoItem: LocalVideoItem
    private lateinit var remoteVideoItem: RemoteVideoItem
    private val placeholderItem by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_item_video, container, false) as ViewGroup
    }

    override fun init(view: View) {
    }

    companion object {
        fun newInstance() = MainVideoFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_main_video


    private fun createLocalPlayableItem(): LocalVideoItem {
        val localItem = LocalVideoItem(container, presenter)
        localItem.setZOrderMediaOverlay(true)
        lifecycle.addObserver(localItem)
        return localItem
    }

    private fun initLocalAV(shouldStreamVideo: Boolean, shouldStreamAudio: Boolean) {
        if (routerViewModel.liveRoom.currentUser.type != LPConstants.LPUserType.Teacher) {
            return
        }
        val recoder: LPRecorder = liveRoom.getRecorder()
        recoder.publish()
        if (checkCameraAndMicPermission()) {
            localVideoItem = createLocalPlayableItem()
            with(localVideoItem) {
                setShouldStreamAudio(shouldStreamAudio)
                setShouldStreamVideo(shouldStreamVideo)
                refreshPlayable()
            }
            container.addView(localVideoItem.view)
            localVideoItem.view.visibility = View.VISIBLE
            placeholderItem.visibility = View.GONE
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
        placeholderItem.item_local_speaker_name.visibility = View.GONE
        if (::remoteVideoItem.isInitialized) {
            remoteVideoItem.view.visibility = View.GONE
        }
        placeholderItem.visibility = View.VISIBLE
    }
    private fun showLocalStatus() {
        placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_camera_close)
        placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_camera_closed)
        val teacherLabel = liveRoom.customizeTeacherLabel?: getString(R.string.live_teacher_hint)
        placeholderItem.item_local_speaker_name.text = liveRoom.currentUser.name + teacherLabel
        if (::localVideoItem.isInitialized) {
            localVideoItem.view.visibility = View.GONE
        }
        placeholderItem.visibility = View.VISIBLE
    }

    private fun initSuccess() {
        container.addView(placeholderItem)
        if (!routerViewModel.liveRoom.isTeacher) {
            showRemoteStatus(routerViewModel.liveRoom.isTeacher)
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
                if (routerViewModel.liveRoom.currentUser.type == LPConstants.LPUserType.Teacher) {
                    container.postDelayed({
                        initLocalAV(true,true)
                        requestCloudRecord()
                    }, 500)
                }
            }
        })

        routerViewModel.notifyRemotePlayableChanged.observe(this, Observer<IMediaModel> {
            it?.let {
                if (it.user.type != LPConstants.LPUserType.Teacher) {
                    return@Observer
                }
                if (it.mediaSourceType == LPConstants.MediaSourceType.ExtCamera || it.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare) {
                    return@Observer
                }
                if (!::remoteVideoItem.isInitialized) {
                    remoteVideoItem = RemoteVideoItem(container, it, presenter)
                    remoteVideoItem.setZOrderMediaOverlay(true)
                    lifecycle.addObserver(remoteVideoItem)
                    container.addView(remoteVideoItem.view)
                }
                if (!it.isAudioOn && !it.isVideoOn && remoteVideoItem.isInFullScreen) {
                    remoteVideoItem.switchBackToList()
                    (routerViewModel.pptViewData.value as MyPadPPTView).switchToFullScreen()
                }
                remoteVideoItem.view.visibility = View.VISIBLE
                placeholderItem.visibility = View.GONE
                remoteVideoItem.setMediaModel(it)
                remoteVideoItem.refreshPlayable()
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

        routerViewModel.notifyLocalPlayableChanged.observe(this, Observer {
            it?.run {
                if (!liveRoom.isTeacher) {
                    return@run
                }
                if (::localVideoItem.isInitialized) {
                    if (!it.first && !it.second && localVideoItem.isInFullScreen) {
                        localVideoItem.switchBackToList()
                        (routerViewModel.pptViewData.value as MyPadPPTView).switchToFullScreen()
                    }
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
                if ((liveRoom.teacherUser != null && it.identity == liveRoom.teacherUser.userId )|| it.identity == "PPT") {
                    container.addView(it.view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
            }
        })
    }

    private fun requestCloudRecord() {
        if (!routerViewModel.liveRoom.isTeacherOrAssistant && !routerViewModel.liveRoom.isGroupTeacherOrAssistant) {
            return
        }
        if (liveRoom.autoStartCloudRecordStatus == 1) {
            compositeDisposable.add(liveRoom.requestIsCloudRecordAllowed()
                    .subscribe { lpCheckRecordStatusModel ->
                        if (lpCheckRecordStatusModel.recordStatus == 1) {
                            liveRoom.requestCloudRecord(true)
                        } else {
                            showToastMessage(lpCheckRecordStatusModel.reason)
                        }
                    })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        container.removeAllViews()
    }
}