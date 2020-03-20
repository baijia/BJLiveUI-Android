package com.baijiayun.live.ui.speakpanel

import android.arch.lifecycle.Observer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.speakerlist.item.LocalItem
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPUserModel
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.utils.CommonUtils
import com.baijiayun.livecore.utils.LPRxUtils
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.layout_item_video.view.*
import java.util.concurrent.TimeUnit

/**
 * 1v1 学生容器(自动上台)
 */
class SingleSpeakFragment : BasePadFragment() {
    private val presenter by lazy {
        SpeakPresenterBridge((activity as LiveRoomBaseActivity).routerListener)
    }
    private val container by lazy {
        view as ViewGroup
    }
    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }
    private val speakViewModel by lazy {
        getViewModel { SpeakViewModel(routerViewModel) }
    }
    private val kickOutObserver by lazy {
        Observer<Unit> {
            it?.let {
                if (remoteVideoItem == null) {
                    lifecycle.removeObserver(localVideoItem)
                } else {
                    lifecycle.removeObserver(remoteVideoItem!!)
                }
            }
        }
    }
    private var remoteVideoItem: RemoteVideoItem? = null

    private var timeDisposable: Disposable? = null

    private val localVideoItem by lazy {
        val localVideoItem = createLocalPlayableItem()
        container.removeAllViews()
        container.addView(localVideoItem.view)
        localVideoItem
    }
    private val placeholderItem by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_item_video, container, false) as ViewGroup
    }

    override fun init(view: View) {
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_main_video

    override fun observeActions() {
        super.observeActions()
        routerViewModel.actionNavigateToMain.observe(this, Observer {
            if (it != true) {
                return@Observer
            }
            initSuccess()
            speakViewModel.subscribe()
        })
    }

    private fun initSuccess() {
        container.removeAllViews()
        container.addView(placeholderItem)
        if (routerViewModel.liveRoom.isTeacher) {
            showRemoteStatus(false, LPUserModel())
            speakViewModel.singleSpeakerChange.observe(this, Observer {
                it?.let {
                    showRemoteStatus(!it.first, it.second)
                }
            })
        }

        routerViewModel.isClassStarted.observe(this, Observer {
            if (it == true) {
                enableAutoSpeak()
            }
        })

        routerViewModel.notifyRemotePlayableChanged.observe(this, Observer {
            it?.let {
                if (it.user.userId == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID) {
                    return@Observer
                }
                if (it.user.type != LPConstants.LPUserType.Student) {
                    return@Observer
                }
                if (speakViewModel.singleSpeakerChange.value?.first == false) {
                    return@Observer
                }
                if (remoteVideoItem == null) {
                    remoteVideoItem = createRemotePlayableItem(it)
                    container.removeAllViews()
                    addView(remoteVideoItem!!.view)
                }
                remoteVideoItem?.run {
                    notifyAwardChange(getAwardCount(it.user.number))
                    setMediaModel(it)
                    refreshPlayable()
                }
            }
        })

        routerViewModel.notifyLocalPlayableChanged.observe(this, Observer {
            it?.run {
                if (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) {
                    return@run
                }
                localVideoItem.run {
                    notifyAwardChange(getAwardCount(this.user.number))
                    setShouldStreamVideo(it.first)
                    setShouldStreamAudio(it.second)
                    refreshPlayable()
                }
            }
        })

        routerViewModel.actionWithLocalAVideo.observe(this, Observer {
            it?.let {
                if (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) {
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
                if (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) {
                    return@run
                }
                attachLocalAudio()
            }
        })

        routerViewModel.switch2BackList.observe(this, Observer {
            it?.let {
                context?.run {
                    if (liveRoom.teacherUser != null && (it.identity == liveRoom.teacherUser.userId ||
                                    it.identity == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID)) {
                        return@let
                    }
                    removeSwitchableFromParent(it)
                    addView(it.view)
                }
            }
        })

        routerViewModel.notifyAward.observe(this, Observer {
            it?.let {
                if (it.isFromCache && it.value.recordAward != null) {
                    return@Observer
                }
                if (routerViewModel.liveRoom.currentUser.number == it.value.to) {
                    localVideoItem.notifyAwardChange(getAwardCount(it.value.to))
                    routerViewModel.action2Award.value = CommonUtils.getEncodePhoneNumber(routerViewModel.liveRoom.currentUser.name)
                } else {
                    if (remoteVideoItem != null) {
                        remoteVideoItem?.notifyAwardChange(getAwardCount(it.value.to))
                        routerViewModel.action2Award.value = CommonUtils.getEncodePhoneNumber(remoteVideoItem?.user?.name)
                    }
                }
            }
        })
        routerViewModel.notifyCloseRemoteVideo.observe(this, Observer {
            it?.let {
                if (liveRoom.teacherUser != null && (it.identity == liveRoom.teacherUser.userId || it.identity == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID)) {
                    return@Observer
                }
                it.refreshItemType()
            }
        })
        routerViewModel.kickOut.observeForever(kickOutObserver)
    }

    private fun isAutoSpeak() = routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.Single ||
            routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.SmallGroup || routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.OneOnOne

    /**
     * 一对一、小班课上课学生自动举手上麦，试听不上麦
     * 申请权限时，2种权限要合并一起申请，单独申请需要连续申请,
     * 即在onRequestPermissionsResult回调里面再申请另一个
     */
    private fun enableAutoSpeak() = with(routerViewModel) {
        if (isAutoSpeak() && liveRoom.currentUser.type == LPConstants.LPUserType.Student && !liveRoom.isAudition) {
            liveRoom.getRecorder<LPRecorder>().publish()
            if (liveRoom.autoOpenCameraStatus) {
                if (checkCameraAndMicPermission()) {
                    if (!liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                        attachLocalAudio()
                    }
                    if (liveRoom.autoOpenCameraStatus) {
                        timeDisposable = Observable.timer(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                                .subscribe { attachLocalVideo() }
                    }
                }
            } else {
                if (!liveRoom.getRecorder<LPRecorder>().isAudioAttached) {
                    attachLocalAudio()
                }
            }
        }
    }

    private fun showRemoteStatus(isLeave: Boolean, user: IUserModel?) {
        if (isLeave) {
            placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_leave_room)
            placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_leave_room_single_speaker)
        } else {
            placeholderItem.item_status_placeholder_iv.setImageResource(R.drawable.ic_pad_leave_room)
            placeholderItem.item_status_placeholder_tv.text = getString(R.string.pad_not_onseat)
        }
        placeholderItem.item_local_speaker_name.text = user?.name ?: ""
        placeholderItem.item_local_speaker_name.visibility = View.VISIBLE
        placeholderItem.visibility = View.VISIBLE
        if (isLeave) {
            //学生离开教室把原来在学生位置的ppt移到全屏位置
            val myPadPPTView = routerViewModel.pptViewData.value as MyPadPPTView
            if (myPadPPTView.pptStatus == MyPadPPTView.PPTStatus.BackList) {
                myPadPPTView.switch2FullScreenLocal()
            }
            remoteVideoItem = null
        }
        container.removeAllViews()
        container.addView(placeholderItem)
    }

    private fun addView(child: View) {
        container.addView(child, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun createLocalPlayableItem(): LocalVideoItem = kotlin.run {
        val localItem = LocalVideoItem(container, presenter)
        lifecycle.addObserver(localItem)
        localItem
    }

    private fun createRemotePlayableItem(iMediaModel: IMediaModel): RemoteVideoItem = iMediaModel.run {
        val remoteItem = RemoteVideoItem(container, this, presenter)
        lifecycle.addObserver(remoteItem)
        remoteItem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LPRxUtils.dispose(timeDisposable)
        routerViewModel.kickOut.removeObserver(kickOutObserver)
        (view as ViewGroup).removeAllViews()
    }

    private fun removeSwitchableFromParent(switchable: Switchable) {
        val view = switchable.view ?: return
        val viewParent = view.parent ?: return
        (viewParent as ViewGroup).removeView(view)
    }

    companion object {
        fun newInstance() = SingleSpeakFragment()
    }

    private fun getAwardCount(number: String?): Int {
        return routerViewModel.awardRecord[number]?.count ?:0
    }
}