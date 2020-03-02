package com.baijiayun.live.ui.pptpanel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.livecore.ppt.PPTView
import com.baijiayun.livecore.ppt.photoview.OnDoubleTapListener
import com.baijiayun.livecore.ppt.whiteboard.WhiteboardView
import com.baijiayun.livecore.ppt.whiteboard.shape.Shape

/**
 * Created by Shubo on 2019-10-15.
 */
@SuppressLint("ViewConstructor")
class MyPadPPTView(context: Context, val routerViewModel: RouterViewModel, attr: AttributeSet? = null) : PPTView(context, attr), Switchable {
    companion object{
        const val WHITEBOARD_URL = "https://img.baijiayun.com/0baijiatools/bed38ee1db799ecf13cfe2df92e46c1f/whiteboard.png"
    }
    enum class PPTStatus{
        FullScreen,MainVideo,BackList,Close
    }
    var pptStatus = PPTStatus.FullScreen
    var closeByExtCamera = false //和辅助摄像头互斥

    override fun getPositionInParent(): Int {
        return 0
    }

    override fun isInFullScreen(): Boolean {
        return PPTStatus.FullScreen == pptStatus
    }

    override fun getIdentity(): String {
        return "PPT"
    }

    override fun getItemType(): SpeakItemType {
        return SpeakItemType.PPT
    }

    override fun getView(): View {
        return this
    }

    override fun switchToFullScreen() {
        if (closeByExtCamera) {
            switchExtCameraToFullScreen()
        } else {
            removeSwitchableFromParent(this)
            routerViewModel.switch2FullScreen.value = this
            pptStatus = PPTStatus.FullScreen
        }
    }

    private fun switchExtCameraToFullScreen() {
        routerViewModel.extCameraData.value?.second?.let {
            removeSwitchableFromParent(it)
            routerViewModel.switch2FullScreen.value = it
        }
    }

    private fun switchExtCameraToBackList() {
        routerViewModel.extCameraData.value?.second?.let {
            removeSwitchableFromParent(it)
            if (routerViewModel.isMainVideo2FullScreen.value == true) {
                routerViewModel.switch2MainVideo.value = it
            } else {
                routerViewModel.switch2BackList.value = it
            }
        }
    }

    override fun switchBackToList() {
        if (closeByExtCamera) {
            pptStatus = PPTStatus.Close
            switchExtCameraToBackList()
            return
        }
        removeSwitchableFromParent(this)
        routerViewModel.changeDrawing.value = true
        if (routerViewModel.isMainVideo2FullScreen.value == true) {
            routerViewModel.switch2MainVideo.value = this
            pptStatus = PPTStatus.MainVideo
        } else {
            routerViewModel.switch2BackList.value = this
            pptStatus = PPTStatus.BackList
        }
    }
    fun closePPTbyExtCamera() {
        removeSwitchableFromParent(this)
        closeByExtCamera = true
        pptStatus = PPTStatus.Close
    }

    fun start() {
        mPageTv.setOnClickListener {
            if (isInFullScreen) {
                if (isEditable) {
                    routerViewModel.changeDrawing.value = true
                }
                routerViewModel.actionShowQuickSwitchPPT.value = Bundle().apply {
                    putInt("currentIndex", currentPageIndex)
                    putInt("maxIndex", maxPage)
                }
            }
        }
        super.setOnViewTapListener { _, _, _ ->
            if (!isInFullScreen) {
                showOptionDialog()
                return@setOnViewTapListener
            }
            if (!routerViewModel.penChecked) {
                routerViewModel.clearScreen.value = if (routerViewModel.clearScreen.value == null) true else !routerViewModel.clearScreen.value!!
            }
        }
        super.setOnDoubleTapListener(object : OnDoubleTapListener {

            override fun onDoubleTapConfirmed() {
                if (isInFullScreen) {
                    setDoubleTapScaleEnable(true)
                } else {
                    setDoubleTapScaleEnable(false)
                    with(routerViewModel) {
                        if (liveRoom.isSyncPPTVideo && (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) && isMainVideo()) {
                            showSwitchDialog()
                        } else {
                            switch2FullScreenLocal()
                        }
                    }
                }
            }


            override fun onDoubleTapOnShape(shape: Shape?) {
            }
        })
        super.setPPTErrorListener { errorCode, description ->
            routerViewModel.action2PPTError.value = errorCode to description
        }
        super.setOnPageSelectedListener { _, _ -> super.setAnimPPTAuth(true) }
    }

    override fun getPPTBgColor(): Int {
        return ContextCompat.getColor(context,R.color.live_pad_ppt_background)
    }

    override fun getWhiteboardPageInfo(): WhiteboardView.DocPageInfo {
        val pageInfo =  WhiteboardView.DocPageInfo()
        pageInfo.urlPrefix = WHITEBOARD_URL
        pageInfo.url = pageInfo.urlPrefix
        return pageInfo
    }

    private fun showOptionDialog() {
        if (context == null) return
        val options = listOf(context.getString(R.string.live_full_screen))
        MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback { materialDialog, _, _, charSequence ->
                    if (context == null) {
                        return@itemsCallback
                    }
                    if (context.getString(R.string.live_full_screen) == charSequence.toString()) {
                        with(routerViewModel) {
                            if (liveRoom.isSyncPPTVideo && (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) && isMainVideo()) {
                                showSwitchDialog()
                            } else {
                                switch2FullScreenLocal()
                            }
                        }
                    }
                    materialDialog.dismiss()
                }
                .show()
    }
    private fun isMainVideo() :Boolean{
        return pptStatus == PPTStatus.MainVideo
    }

    fun switch2FullScreenLocal() {
        routerViewModel.switch2FullScreen.value?.switchBackToList()
        switchToFullScreen()
    }

    private fun switch2FullScreenSync() {
        routerViewModel.liveRoom.requestPPTVideoSwitch(false)
        switch2FullScreenLocal()
    }

    private fun showSwitchDialog() {
        if ((context as LiveRoomBaseActivity).supportFragmentManager?.isStateSaved == true) {
            return
        }
        context.let {
            MaterialDialog.Builder(it)
                    .title(it.getString(R.string.live_exit_hint_title))
                    .content(it.getString(R.string.live_pad_sync_video_ppt))
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
    private fun removeSwitchableFromParent(switchable: Switchable) {
        val view = switchable.view ?: return
        val viewParent = view.parent ?: return
        (viewParent as ViewGroup).removeView(view)
    }

    override fun setMaxPage(maxIndex: Int) {
        super.setMaxPage(maxIndex)
        routerViewModel.actionChangePPT2Page.value = maxIndex
    }
}