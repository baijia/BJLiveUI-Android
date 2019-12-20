package com.baijiayun.live.ui.speakpanel

import android.arch.lifecycle.Observer
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.baijiayun.live.ui.LiveRoomTripleActivity
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.speakerlist.ItemPositionHelper
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.live.ui.speakerlist.item.SpeakItem
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.viewsupport.BJTouchHorizontalScrollView
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

/**
 * Created by Shubo on 2019-10-10.
 */
class SpeakFragment : BasePadFragment() {
    private lateinit var presenter : SpeakPresenterBridge
    private val speakViewModel by lazy {
        getViewModel { SpeakViewModel(routerViewModel) }
    }
    private val positionHelper by lazy {
        ItemPositionHelper()
    }
    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }

    private lateinit var scrollView: BJTouchHorizontalScrollView
    private lateinit var container: LinearLayout
    private lateinit var nextBtn: TextView
    private var lastPresenterItem: SpeakItem? = null

    override fun init(view: View) {
        scrollView = view.findViewById(R.id.fragment_speakers_scroll_view)
        container = view.findViewById(R.id.fragment_speakers_container)
        routerViewModel.speakListCount.value = 0
        nextBtn = view.findViewById(R.id.fragment_speakers_new_request_toast)
        nextBtn.setOnClickListener {
            it.visibility = View.GONE
            scrollView.fullScroll(View.FOCUS_RIGHT)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is LiveRoomTripleActivity){
            presenter = SpeakPresenterBridge(context.getRouterListener())
        }
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_speakers

    override fun observeActions() {
        super.observeActions()
        routerViewModel.actionNavigateToMain.observe(this, Observer {
            if (it != true) {
                return@Observer
            }
            initSuccess()
            speakViewModel.subscribe()
            enableStudentSpeak()
        })
        routerViewModel.pptViewData.observe(this, Observer {
            it?.let {
                if(activity is LiveRoomTripleActivity){
                    positionHelper.setRouterListener((activity as LiveRoomTripleActivity).getRouterListener())
                }
            }
        })
    }

    private fun initSuccess() {
        if (routerViewModel.liveRoom.presenterUser != null) {
            lastPresenterItem = positionHelper.getSpeakItemByIdentity(routerViewModel.liveRoom.presenterUser.userId)
        }
        routerViewModel.notifyRemotePlayableChanged.observe(this, Observer<IMediaModel> {
            it?.let {
                if (it.user.type == LPConstants.LPUserType.Teacher && it.mediaSourceType != LPConstants.MediaSourceType.ExtCamera &&
                                it.mediaSourceType != LPConstants.MediaSourceType.ExtScreenShare) {
                    return@Observer
                }
                var item = if (it.mediaSourceType == LPConstants.MediaSourceType.ExtCamera || it.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare) {
                    positionHelper.getSpeakItemByIdentity(it.user.userId + "_1")
                } else {
                    positionHelper.getSpeakItemByIdentity(it.user.userId)
                }

                if (item == null) {
                    item = createRemotePlayableItem(it)
                }
                val remoteItem = item as RemoteVideoItem?
                remoteItem?.run {
                    remoteItem.setMediaModel(it)
                    val isSpeakClosed = !remoteItem.hasAudio() && !remoteItem.hasVideo()
                    if (!remoteItem.isVideoClosedByUser || isSpeakClosed) {
                        takeItemActions(positionHelper.processItemActions(item))
                    }
                    remoteItem.refreshPlayable()
                }
            }
        })

        routerViewModel.actionWithLocalAVideo.observe(this, Observer {
            it?.let {
                if (liveRoom.isTeacher) {
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
                if (liveRoom.isTeacher) {
                    return@run
                }
                var item = positionHelper.getSpeakItemByIdentity(routerViewModel.liveRoom.currentUser.userId)
                var itemActions: List<ItemPositionHelper.ItemAction>? = null
                val isVideoOn = it.first
                val isAudioOn = it.second
                if (item == null) {
                    if (isVideoOn || isAudioOn) {
                        item = createLocalPlayableItem()
                        item?.run{
                            (item as LocalVideoItem).setShouldStreamVideo(isVideoOn)
                            (item as LocalVideoItem).setShouldStreamAudio(isAudioOn)
                            itemActions = positionHelper.processItemActions(item)
                        }
                    }
                } else if (item is LocalVideoItem) {
                    (item as LocalVideoItem).setShouldStreamVideo(isVideoOn)
                    (item as LocalVideoItem).setShouldStreamAudio(isAudioOn)
                    itemActions = positionHelper.processItemActions(item)
                }
                takeItemActions(itemActions)
                if (item is LocalVideoItem)
                    (item as LocalVideoItem).refreshPlayable()
            }
        })

        routerViewModel.actionWithAttachLocalAudio.observe(this, Observer {
            it?.run {
                if (liveRoom.isTeacher) {
                    return@run
                }
                attachLocalAudio()
            }
        })

        speakViewModel.notifyPresenterChange.observe(this, Observer<Pair<String, IMediaModel>> {
            it?.let {
                val speakItem = positionHelper.getSpeakItemByIdentity(it.first)
                if (lastPresenterItem is LocalVideoItem) {
                    (lastPresenterItem as LocalVideoItem).refreshNameTable()
                }else if (lastPresenterItem is RemoteVideoItem) {
                    (lastPresenterItem as RemoteVideoItem).refreshNameTable()
                }
                if (speakItem is LocalVideoItem) {
                    speakItem.refreshNameTable()
                }else if (speakItem is RemoteVideoItem) {
                    speakItem.refreshNameTable()
                }
                lastPresenterItem = speakItem
            }
        })

        speakViewModel.notifyPresenterDesktopShareAndMedia.observe(this, Observer {
            it?.let {
                if (it) {
                    val fullScreenItem = routerViewModel.switch2FullScreen.value
                    if (fullScreenItem?.identity != routerViewModel.liveRoom.teacherUser.userId) {
                        val speakItem = positionHelper.getSpeakItemByIdentity(routerViewModel.liveRoom.teacherUser.userId)
                        if (speakItem is RemoteItem) {
                            if (speakItem.isVideoClosedByUser) {
                                return@Observer
                            }
                            fullScreenItem?.switchBackToList()
                            speakItem.switchToFullScreen()
                        }
                    }
                }
            }
        })

        routerViewModel.switch2BackList.observe(this, Observer {
            it?.let {
                context?.run {
                    if (liveRoom.teacherUser !=null && it.identity == liveRoom.teacherUser.userId) {
                        return@let
                    }
                    val index = positionHelper.getItemSwitchBackPosition(it)
                    container.addView(it.view, index, LinearLayout.LayoutParams(DisplayUtils.dip2px(this, 240f), DisplayUtils.dip2px(this, 135f)))
                    routerViewModel.speakListCount.value = container.childCount
                }
            }
        })

        routerViewModel.notifyCloseRemoteVideo.observe(this, Observer {
            it?.let {
                if (liveRoom.teacherUser != null && it.identity == liveRoom.teacherUser.userId) {
                    return@Observer
                }
                closeRemoteVideo(it)
            }
        })

        routerViewModel.notifyAward.observe(this, Observer {
            it?.let {
                if (it.isFromCache && it.value.record != null) {
                    return@Observer
                }
                val playable = positionHelper.getPlayableItemByUserNumber(it.value.to)
                if (playable == null) {
                    if (routerViewModel.liveRoom.currentUser.number == it.value.to) {
                        routerViewModel.action2Award.value = routerViewModel.liveRoom.currentUser.name
                    }
                } else {
                    routerViewModel.action2Award.value = playable.user.name
                }
            }
        })
    }

    private fun closeRemoteVideo(remoteItem: RemoteItem) {
        val actionList = positionHelper.processUserCloseAction(remoteItem)
        takeItemActions(actionList)
    }

    private fun createLocalPlayableItem(): LocalVideoItem? {
        return if(::presenter.isInitialized){
            val localItem = LocalVideoItem(container, presenter)
            lifecycle.addObserver(localItem)
            localItem
        } else{
            null
        }
    }

    private fun createRemotePlayableItem(iMediaModel: IMediaModel): SpeakItem? {
        return if(::presenter.isInitialized){
            val remoteItem = RemoteVideoItem(container, iMediaModel, presenter)
            lifecycle.addObserver(remoteItem)
            remoteItem
        } else{
            null
        }
    }

    private fun takeItemActions(actions: List<ItemPositionHelper.ItemAction>?) {
        actions?.run {
            for (action in actions) {
                when (action.action) {
                    ItemPositionHelper.ActionType.ADD -> {
                        var value = action.value
                        if ((routerViewModel.pptViewData.value as MyPadPPTView).pptStatus == MyPadPPTView.PPTStatus.MainVideo) {
                            value -= 1
                        }
                        container.addView(action.speakItem.view, value)
                        routerViewModel.speakListCount.value = container.childCount
                    }
                    ItemPositionHelper.ActionType.REMOVE -> {
                        if (action.speakItem is LocalVideoItem ) {
                            (action.speakItem as LocalVideoItem).destroy()
                        }else if (action.speakItem is RemoteVideoItem) {
                            (action.speakItem as RemoteVideoItem).destroy()
                        }
                        container.removeView(action.speakItem.view)
                        routerViewModel.speakListCount.value = container.childCount
                    }
                    ItemPositionHelper.ActionType.FULLSCREEN -> (action.speakItem as Switchable).switchToFullScreen()
                    else -> {
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (view as ViewGroup).removeAllViews()
    }

    private fun enableStudentSpeak() {
        // 一对一、小班课上课学生自动举手上麦
        if (liveRoom.currentUser.type == LPConstants.LPUserType.Student && !liveRoom.isAudition) {
            //试听进入不上台
            if (liveRoom.roomType == LPConstants.LPRoomType.Single || liveRoom.roomType == LPConstants.LPRoomType.SmallGroup) {
                if (liveRoom.isClassStarted) {
                    val lpRecorder = liveRoom.getRecorder<LPRecorder>()
                    lpRecorder.publish()
                    if (checkCameraAndMicPermission()) {
                        if (!lpRecorder.isAudioAttached) {
                            attachLocalAudio()
                        }
                        if (liveRoom.autoOpenCameraStatus) {
                            val timer = Observable.timer(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {
                                        attachLocalVideo()
                                    }
                            compositeDisposable.add(timer)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = SpeakFragment()
    }
}