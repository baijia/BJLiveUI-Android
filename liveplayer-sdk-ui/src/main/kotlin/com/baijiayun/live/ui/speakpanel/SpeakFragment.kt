package com.baijiayun.live.ui.speakpanel

import android.arch.lifecycle.Observer
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.getSpeaksVideoParams
import com.baijiayun.live.ui.isSpeakVideoItem
import com.baijiayun.live.ui.pptpanel.MyPadPPTView
import com.baijiayun.live.ui.speakerlist.ItemPositionHelper
import com.baijiayun.live.ui.speakerlist.item.Playable
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.live.ui.speakerlist.item.SpeakItem
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.viewsupport.BJTouchHorizontalScrollView
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPAwardUserInfo
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.utils.CommonUtils
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel
import com.baijiayun.livecore.wrapper.LPRecorder

/**
 * Created by Shubo on 2019-10-10.
 */
class SpeakFragment : BasePadFragment() {
    private lateinit var presenter: SpeakPresenterBridge
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
        if (context is LiveRoomBaseActivity) {
            presenter = SpeakPresenterBridge(context.routerListener)
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
        })
        routerViewModel.pptViewData.observe(this, Observer {
            it?.let {
                if (activity is LiveRoomBaseActivity) {
                    positionHelper.setRouterListener((activity as LiveRoomBaseActivity).routerListener)
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
                //合流会出现userId,userNumber 都为空的问题
                if (it.user.userId == null) {
                    return@Observer
                }
                if (it.user.userId == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID) {
                    return@Observer
                }
                if (it.user.type == LPConstants.LPUserType.Teacher) {
                    if (it.mediaSourceType == LPConstants.MediaSourceType.ExtCamera || it.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare) {
                        handleExtMedia(it)
                    }
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
                        item?.run {
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
                } else if (lastPresenterItem is RemoteVideoItem) {
                    (lastPresenterItem as RemoteVideoItem).refreshNameTable()
                    (lastPresenterItem as RemoteVideoItem).refreshItemType()
                }
                if (speakItem is LocalVideoItem) {
                    speakItem.refreshNameTable()
                } else if (speakItem is RemoteVideoItem) {
                    speakItem.refreshNameTable()
                    speakItem.refreshItemType()
                }
                lastPresenterItem = speakItem
            }
        })

        speakViewModel.notifyPresenterDesktopShareAndMedia.observe(this, Observer {
            it?.let {
                if (it) {
                    val fullScreenItem = routerViewModel.switch2FullScreen.value
                    if (fullScreenItem?.identity != routerViewModel.liveRoom.teacherUser.userId && fullScreenItem?.identity != LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID) {
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
                    if (!isSpeakVideoItem(it,liveRoom)) {
                        return@let
                    }
                    val index = positionHelper.getItemSwitchBackPosition(it)
                    addView(it.view, index)
                    routerViewModel.speakListCount.value = container.childCount
                }
            }
        })

        routerViewModel.notifyCloseRemoteVideo.observe(this, Observer {
            it?.let {
                if (!isSpeakVideoItem(it,liveRoom)) {
                    return@Observer
                }
                closeRemoteVideo(it)
            }
        })

        routerViewModel.notifyAward.observe(this, Observer {
            it?.let { awardModel ->
                if (awardModel.isFromCache && awardModel.value.recordAward != null) {
                    awardModel.value.recordAward.forEach{ entry ->
                        val playable: Playable? = positionHelper.getPlayableItemByUserNumber(entry.key)
                        playable?.notifyAwardChange(entry.value.count)
                    }
                    return@Observer
                }
                val playable = positionHelper.getPlayableItemByUserNumber(awardModel.value.to)
                if (playable == null) {
                    routerViewModel.action2Award.value = awardModel.value.to
                } else {
                    val userInfo: LPAwardUserInfo? = awardModel.value.recordAward?.get(awardModel.value.to)
                    playable.notifyAwardChange(userInfo?.count ?: 0)
                    routerViewModel.action2Award.value = CommonUtils.getEncodePhoneNumber(playable.user.name)
                }
            }
        })
    }

    private fun handleExtMedia(iMediaModel: IMediaModel) {
        val pptview = routerViewModel.pptViewData.value as MyPadPPTView
        val remoteItem: Switchable?
        if (iMediaModel.user.userId == routerViewModel.extCameraData.value?.first) {
            remoteItem = routerViewModel.extCameraData.value?.second
        } else {
            val switchable = routerViewModel.extCameraData.value?.second
            if (switchable != null) {
                removeSwitchableFromParent(switchable)
            }
            remoteItem = createRemotePlayableItem(iMediaModel) as RemoteVideoItem
            routerViewModel.extCameraData.value = iMediaModel.user.userId to remoteItem
        }
        if (iMediaModel.isVideoOn) {
            if (pptview.closeByExtCamera) {
                return
            }
            if (!pptview.isInFullScreen) {
                pptview.closePPTbyExtCamera()
                routerViewModel.switch2FullScreen.value?.switchBackToList()
            } else {
                pptview.closePPTbyExtCamera()
            }
            remoteItem?.switchToFullScreen()
        } else {
            if (!pptview.closeByExtCamera) {
                pptview.closePPTbyExtCamera()
            }
            if (remoteItem?.isInFullScreen == true) {
                removeSwitchableFromParent(remoteItem)
            } else {
                removeSwitchableFromParent(remoteItem!!)
                routerViewModel.switch2FullScreen.value?.switchBackToList()
            }
            pptview.closeByExtCamera = false
            pptview.switchToFullScreen()
        }
        remoteItem as RemoteVideoItem
        remoteItem.setMediaModel(iMediaModel)
        remoteItem.refreshPlayable()
    }

    private fun addView(child: View, index: Int = -1) {
        if (index == -1) {
            container.addView(child, getSpeaksVideoParams(context!!))
        } else {
            var value = index
            if ((routerViewModel.pptViewData.value as MyPadPPTView).pptStatus == MyPadPPTView.PPTStatus.MainVideo ||
                    (routerViewModel.pptViewData.value as MyPadPPTView).pptStatus == MyPadPPTView.PPTStatus.Close) {
                value -= 1
            }
            container.addView(child, value, getSpeaksVideoParams(context!!))
        }
    }

    private fun closeRemoteVideo(remoteItem: RemoteItem) {
        val actionList = positionHelper.processUserCloseAction(remoteItem)
        takeItemActions(actionList)
    }

    private fun createLocalPlayableItem(): LocalVideoItem? {
        return if (::presenter.isInitialized) {
            val localItem = LocalVideoItem(container, presenter)
            localItem.notifyAwardChange(getAwardCount(liveRoom.currentUser.number))
            lifecycle.addObserver(localItem)
            localItem
        } else {
            null
        }
    }

    private fun createRemotePlayableItem(iMediaModel: IMediaModel): SpeakItem? {
        return if (::presenter.isInitialized) {
            val remoteItem = RemoteVideoItem(container, iMediaModel, presenter)
            remoteItem.notifyAwardChange(getAwardCount(iMediaModel.user.number))
            lifecycle.addObserver(remoteItem)
            remoteItem
        } else {
            null
        }
    }

    private fun takeItemActions(actions: List<ItemPositionHelper.ItemAction>?) {
        actions?.run {
            for (action in actions) {
                when (action.action) {
                    ItemPositionHelper.ActionType.ADD -> {
                        val value = action.value
                        addView(action.speakItem.view, value)
                        routerViewModel.speakListCount.value = container.childCount
                    }
                    ItemPositionHelper.ActionType.REMOVE -> {
                        if (action.speakItem is LocalVideoItem) {
                            (action.speakItem as LocalVideoItem).destroy()
                        } else if (action.speakItem is RemoteVideoItem) {
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

    private fun removeSwitchableFromParent(switchable: Switchable) {
        val view = switchable.view ?: return
        val viewParent = view.parent ?: return
        (viewParent as ViewGroup).removeView(view)
    }

    companion object {
        fun newInstance() = SpeakFragment()
    }

    private fun getAwardCount(number: String?): Int {
        return routerViewModel.awardRecord[number]?.count ?:0
    }
}