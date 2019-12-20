package com.baijiayun.live.ui.pptpanel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.livecore.ppt.PPTView
import com.baijiayun.livecore.ppt.whiteboard.WhiteboardView

/**
 * Created by Shubo on 2019-10-15.
 */
@SuppressLint("ViewConstructor")
class MyPadPPTView(context: Context, val routerViewModel: RouterViewModel, attr: AttributeSet? = null) : PPTView(context, attr), Switchable {
    companion object{
        const val WHITEBOARD_URL = "https://img.baijiayun.com/0baijiatools/bed38ee1db799ecf13cfe2df92e46c1f/whiteboard.png"
    }
    enum class PPTStatus{
        FullScreen,MainVideo,BackList
    }
    var pptStatus = PPTStatus.FullScreen

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
        removeSwitchableFromParent(this)
        routerViewModel.switch2FullScreen.value = this
        pptStatus = PPTStatus.FullScreen
    }

    override fun switchBackToList() {
        removeSwitchableFromParent(this)
        if (routerViewModel.isMainVideo2FullScreen.value == true) {
            routerViewModel.switch2MainVideo.value = this
            pptStatus = PPTStatus.MainVideo
        } else {
            routerViewModel.switch2BackList.value = this
            pptStatus = PPTStatus.BackList
        }
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
            routerViewModel.clearScreen.value = if (routerViewModel.clearScreen.value == null) true else !routerViewModel.clearScreen.value!!
        }
        super.setOnDoubleTapListener {
            if (isInFullScreen) {
                setDoubleTapScaleEnable(true)
            } else {
                setDoubleTapScaleEnable(false)
                (routerViewModel.switch2FullScreen.value as Switchable).switchBackToList()
                switchToFullScreen()
            }
        }
        super.setPPTErrorListener { errorCode, _-> routerViewModel.action2PPTError.value = errorCode }
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
                        (routerViewModel.switch2FullScreen.value as Switchable).switchBackToList()
                        switchToFullScreen()
                    }
                    materialDialog.dismiss()
                }
                .show()
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