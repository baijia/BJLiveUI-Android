package com.baijiayun.live.ui.pptpanel

import android.arch.lifecycle.Observer
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.canShowDialog
import com.baijiayun.live.ui.databinding.LayoutPptMenuBinding
import com.baijiayun.live.ui.isPad
import com.baijiayun.live.ui.menu.rightmenu.RightMenuContract
import com.baijiayun.live.ui.pptpanel.handsuplist.HandsUpListFragment
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.utils.RxUtils
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.ppt.listener.OnPPTStateListener
import com.baijiayun.livecore.utils.LPRxUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_pad_ppt.*
import kotlinx.android.synthetic.main.layout_ppt_menu.view.*
import java.util.concurrent.TimeUnit

/**
 * Created by Shubo on 2019-10-10.
 */
class PPTFragment : BasePadFragment(), PPTMenuContract.View {
    private val pptViewModel by lazy {
        getViewModel { PPTViewModel(routerViewModel) }
    }
    private val handsUpListFragment by lazy {
        HandsUpListFragment.newInstance()
    }
    private val toolBars by lazy {
        LayoutInflater.from(context).inflate(R.layout.layout_ppt_menu, null)
    }
    private val pptView by lazy {
        context?.let {
            MyPadPPTView(it, routerViewModel)
        }
    }
    private val presenter by lazy {
        PPTMenuPresenterBridge(this, (activity as LiveRoomBaseActivity).routerListener, routerViewModel)
    }
    private val disposables by lazy {
        CompositeDisposable()
    }
    private var menuDataBinding: LayoutPptMenuBinding? = null
    private var disposeOfClickable: Disposable? = null
    private var speakInviteDlg: MaterialDialog? = null

    override fun init(view: View) {
    }

    private fun initView() {
        routerViewModel.pptViewData.value = pptView
        presenter.subscribe()
        menuDataBinding = DataBindingUtil.bind(toolBars)
        menuDataBinding?.let {
            it.pptviewmodel = this.pptViewModel
            it.lifecycleOwner = this@PPTFragment
        }
        toolBars.run {
            if (!isPad(context) && !pptViewModel.isTeacherOrAssistant()) {
                if (llAVideo.layoutParams is ConstraintLayout.LayoutParams) {
                    val layoutParams = llAVideo.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.bottomToTop = R.id.rlSpeakWrapper
                }
            }
            ivHandsUpImg.setOnClickListener {
                showDialogFragment(handsUpListFragment)
                pptViewModel.hasRead.value = true
            }
            tvPenClear.setOnClickListener {
                pptView?.eraseAllShapes()
            }
            tvPPTFiles.setOnClickListener {
                routerViewModel.actionShowPPTManager.value = Unit
            }
            tvPen.setOnClickListener {
                presenter.changeDrawing()
            }
            disposables.add(RxUtils.clicks(tvVideo).throttleFirst(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (routerViewModel.isClassStarted.value != true) {
                            showToastMessage(getString(R.string.pad_class_start_tip))
                            return@subscribe
                        }
                        if (!clickableCheck()) {
                            showToastMessage(getString(R.string.live_frequent_error))
                        } else {
                            presenter.changeVideo()
                        }
                    })
            disposables.add(RxUtils.clicks(tvAudio).throttleFirst(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (routerViewModel.isClassStarted.value != true) {
                            showToastMessage(getString(R.string.pad_class_start_tip))
                            return@subscribe
                        }
                        if (!clickableCheck()) {
                            showToastMessage(getString(R.string.live_frequent_error))
                        } else {
                            presenter.changeAudio()
                        }
                    })
            disposables.add(RxUtils.clicks(tvSpeakApply).throttleFirst(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (routerViewModel.isClassStarted.value != true) {
                            showToastMessage(getString(R.string.live_hand_up_error))
                            return@subscribe
                        }
                        if (!clickableCheck()) {
                            showToastMessage(getString(R.string.live_frequent_error))
                        } else {
                            if (!presenter.isWaitingRecordOpen) {
                                if (checkCameraPermission()) {
                                    presenter.speakApply()
                                }
                            }
                        }
                    })
            when (routerViewModel.liveRoom.currentUser.type) {
                LPConstants.LPUserType.Teacher, LPConstants.LPUserType.Assistant -> {
                    rlSpeakWrapper.visibility = View.GONE
                }
                else -> {
                    viewDiv.visibility = View.GONE
                    tvPPTFiles.visibility = View.GONE
                    tvHandsUpCount.visibility = View.GONE
                    ivHandsUpImg.visibility = View.GONE
                }
            }
        }
        routerViewModel.switch2FullScreen.value = pptView
        menuContainer.addView(toolBars, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this@PPTFragment, Observer {
            if (it != true) {
                return@Observer
            }
            pptView?.run {
                attachLiveRoom(routerViewModel.liveRoom)
                start()
                setOnPPTStateListener(object : OnPPTStateListener {
                    override fun onSuccess(code: Int, successMessage: String) {
                        if (code == OnPPTStateListener.CODE_PPT_WHITEBOARD_ADD) {
                            pptView?.switchPPTPage("0", Integer.valueOf(successMessage))
                        }
                    }

                    override fun onError(code: Int, errorMessage: String) {
                        showMessage(errorMessage)
                    }
                })
                initPPTViewObserve()
            }
            initView()
        })
    }

    private fun clickableCheck(): Boolean {
        if (disposeOfClickable != null && !disposeOfClickable!!.isDisposed) {
            return false
        }
        disposeOfClickable = Observable.timer(1, TimeUnit.SECONDS).subscribe { RxUtils.dispose(disposeOfClickable) }
        return true
    }

    private fun initPPTViewObserve() {
        routerViewModel.run {
            notifyPPTPageCurrent.observe(this@PPTFragment, Observer {
                it?.run {
                    pptView?.updatePage(it, true, false)
                }
            })
            addPPTWhiteboardPage.observe(this@PPTFragment, Observer {
                it?.run {
                    pptView?.addPPTWhiteboardPage()
                }
            })
            deletePPTWhiteboardPage.observe(this@PPTFragment, Observer {
                it?.run {
                    pptView?.deletePPTWhiteboardPage(it)
                }
            })
            changePPTPage.observe(this@PPTFragment, Observer {
                it?.run {
                    pptView?.switchPPTPage(it.first, it.second)
                }
            })
            actionNavigateToPPTDrawing.observe(this@PPTFragment, Observer {
                it?.let {
                    pptView?.run {
                        setPPTCanvasMode(it)
                        if (isEditable) {
                            if (!isInFullScreen) {
                                (routerViewModel.switch2FullScreen.value as Switchable).switchBackToList()
                                switchToFullScreen()
                            }
                        }
                    }
                }
            })
            switch2FullScreen.observe(this@PPTFragment, Observer {
                it?.let {
                    if (it.view == pptView && pptView?.isEditable == true) {
                        presenter.changeDrawing()
                    }
                    pptContainer.addView(it.view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                }
            })
            isClassStarted.observe(this@PPTFragment, Observer {
                it?.let {
                    if (it) {
                        showMessage(getString(R.string.live_message_le, getString(R.string.lp_override_class_start)))
                        if (isAutoSpeak() && liveRoom.currentUser.type == LPConstants.LPUserType.Student && !liveRoom.isAudition) {
                            showAutoSpeak(liveRoom.partnerConfig.liveDisableGrantStudentBrush == 1)
                        }
                    }
                }
            })
            classEnd.observe(this@PPTFragment, Observer {
                it?.let {
                    showMessage(getString(R.string.live_message_le, getString(R.string.lp_override_class_end)))
                }
            })
            clearScreen.observe(this@PPTFragment, Observer {
                it?.let {
                    toolBars?.run {
                        if (it) {
                            llAVideo.visibility = View.GONE
                            if (!routerViewModel.penChecked) {
                                llPenMenu.visibility = View.GONE
                            }
                            rlSpeakWrapper.visibility = View.GONE
                        } else {
                            //fix llAVideo must not be null
                            llAVideo.visibility = View.VISIBLE
                            llPenMenu.visibility = View.VISIBLE
                            if (!isAutoSpeak()) {
                                rlSpeakWrapper.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })
            action2PPTError.observe(this@PPTFragment, Observer {
                it?.let {
                    if (!canShowDialog()) return@Observer
                    try {
                        context?.run {
                            var title = getString(R.string.live_room_ppt_load_error, it.first)
                            if(it.first == -10086){
                                title = it.second ?: ""
                            }
                            MaterialDialog.Builder(this)
                                    .title(title)
                                    .content(getString(R.string.live_room_ppt_switch))
                                    .contentColor(ContextCompat.getColor(this, R.color.live_text_color))
                                    .positiveColor(ContextCompat.getColor(this, R.color.live_blue))
                                    .positiveText(getString(R.string.live_room_ppt_switch_confirm))
                                    .negativeColor(ContextCompat.getColor(this, R.color.live_text_color))
                                    .negativeText(getString(R.string.live_cancel))
                                    .onPositive { _, _ -> pptView?.isAnimPPTEnable = false }
                                    .onNegative { materialDialog, _ -> materialDialog.dismiss() }
                                    .build()
                                    .show()
                        }
                    } catch (ignore: Exception) {
                    }
                }
            })
            changeDrawing.observe(this@PPTFragment, Observer {
                if (it == true && toolBars.tvPen.isChecked) {
                    presenter.changeDrawing()
                }
            })
        }
    }

    private fun isAutoSpeak() = routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.Single ||
            routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.SmallGroup || routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.OneOnOne

    override fun showSpeakClosedByTeacher(isSmallGroup: Boolean) {
        with(toolBars) {
            tvSpeakApply.isChecked = false
            if (!isSmallGroup) {
                tvPen.visibility = View.GONE
                tvPenClear.visibility = View.GONE
            }
            tvCountDown.visibility = View.INVISIBLE
        }
    }

    override fun showSpeakClosedByServer() {
        showToastMessage(getString(R.string.live_media_speak_closed_by_server))
        with(toolBars) {
            tvSpeakApply.isChecked = false
            tvPen.visibility = View.GONE
            tvPenClear.visibility = View.GONE
            tvCountDown.visibility = View.INVISIBLE
            llAVideo.visibility = View.GONE
        }
    }

    override fun showForceSpeakDenyByServer() {
        showToastMessage(getString(R.string.live_force_speak_closed_by_server))
        with(toolBars) {
            tvSpeakApply.isChecked = false
            tvPen.visibility = View.GONE
            tvPenClear.visibility = View.GONE
            tvCountDown.visibility = View.INVISIBLE
            llAVideo.visibility = View.GONE
        }
    }

    override fun showDrawingStatus(isEnable: Boolean) {
        toolBars.tvPen.isChecked = isEnable
        routerViewModel.penChecked = isEnable
        routerViewModel.clearScreen.value = isEnable
    }

    override fun showSpeakApplyCountDown(countDownTime: Int, total: Int) {
        with(toolBars) {
            tvCountDown.visibility = View.VISIBLE
            tvCountDown.ratio = countDownTime / total.toFloat()
            tvCountDown.invalidate()
        }
    }

    override fun showSpeakApplyAgreed(isEnableDrawing: Boolean) {
        showToastMessage(getString(R.string.live_media_speak_apply_agree))
        with(toolBars) {
            tvSpeakApply.isChecked = true
            if (isEnableDrawing) {
                tvPen.visibility = View.VISIBLE
                tvPenClear.visibility = View.VISIBLE
            }
            tvCountDown.visibility = View.INVISIBLE
        }
    }

    override fun showSpeakApplyDisagreed() {
        with(toolBars) {
            tvSpeakApply.isEnabled = true
            tvSpeakApply.isChecked = false
            showToastMessage(getString(R.string.live_media_speak_apply_disagree))
            tvCountDown.visibility = View.INVISIBLE
        }
    }

    override fun showSpeakApplyCanceled() {
        with(toolBars) {
            tvSpeakApply.isEnabled = true
            tvSpeakApply.isChecked = false
            tvPen.visibility = View.GONE
            tvPenClear.visibility = View.GONE
            tvCountDown.visibility = View.INVISIBLE
        }
    }

    override fun showTeacherRightMenu() {
        with(toolBars) {
            tvPen.visibility = View.VISIBLE
            tvPenClear.visibility = View.VISIBLE
            tvPPTFiles.visibility = View.VISIBLE
            tvSpeakApply.visibility = View.GONE
        }
    }

    override fun showStudentRightMenu() {
        with(toolBars) {
            tvPen.visibility = View.GONE
            tvPenClear.visibility = View.GONE
            tvPPTFiles.visibility = View.GONE
            tvSpeakApply.visibility = View.VISIBLE
        }
    }

    override fun showForbiddenHand() {
        with(toolBars) {
            tvSpeakApply.isEnabled = false
            tvCountDown.visibility = View.INVISIBLE
        }
    }

    override fun showNotForbiddenHand() {
        with(toolBars) {
            tvSpeakApply.isChecked = false
            tvSpeakApply.isEnabled = true
        }
    }

    override fun hidePPTDrawBtn() {
        showToastMessage(getString(R.string.live_student_no_auth_drawing))
        toolBars.tvPen.visibility = View.GONE
        toolBars.tvPenClear.visibility = View.GONE
    }

    override fun showPPTDrawBtn() {
        showToastMessage(getString(R.string.live_student_auth_drawing))
        toolBars.tvPen.visibility = View.VISIBLE
        toolBars.tvPenClear.visibility = View.VISIBLE
    }

    override fun showHandUpError() {
        showToastMessage(getString(R.string.live_hand_up_error))
    }

    override fun showHandUpForbid() {
        showToastMessage(getString(R.string.live_forbid_send_message))
    }

    override fun showCantDraw() {
        showToastMessage(getString(R.string.live_cant_draw))
    }

    override fun showCantDrawCauseClassNotStart() {
        showToastMessage(getString(R.string.live_cant_draw_class_not_start))
    }

    override fun showWaitingTeacherAgree() {
        showToastMessage(getString(R.string.live_waiting_speak_apply_agree))
    }


    override fun showAutoSpeak(isDrawingEnable: Boolean) {
        if (isDrawingEnable) {
            toolBars.tvPen.visibility = View.VISIBLE
            toolBars.tvPenClear.visibility = View.VISIBLE
        }
        toolBars.rlSpeakWrapper.visibility = View.GONE
    }

    override fun showForceSpeak(isDrawingEnable: Boolean) {
        toolBars.tvSpeakApply.isChecked = true
        if (isDrawingEnable) {
            toolBars.tvPen.visibility = View.VISIBLE
            toolBars.tvPenClear.visibility = View.VISIBLE
        }
        toolBars.tvCountDown.visibility = View.INVISIBLE
    }

    override fun hideUserList() {
    }

    override fun hideSpeakApply() {}

    override fun showHandUpTimeout() {
        showToastMessage(getString(R.string.live_media_speak_apply_timeout))
    }

    override fun setAudition() {
        toolBars.run {
            tvPPTFiles.visibility = View.GONE
            tvPenClear.visibility = View.GONE
            tvPen.visibility = View.GONE
            tvAudio.visibility = View.GONE
            tvVideo.visibility = View.GONE
        }
    }

    override fun showDrawDeny() {
        showToastMessage(getString(R.string.live_room_paint_permission_forbid))
    }

    override fun hideTimer() {
    }

    override fun setPresenter(presenter: RightMenuContract.Presenter?) {}
    override fun showVideoStatus(isOn: Boolean) {
        toolBars.tvVideo.isChecked = isOn
        showToastMessage(if (isOn) getString(R.string.live_camera_on) else getString(R.string.live_camera_off))
    }

    override fun showAudioStatus(isOn: Boolean) {
        toolBars.tvAudio.isChecked = isOn
        showToastMessage(if (isOn) getString(R.string.live_mic_on) else getString(R.string.live_mic_off))
    }

    override fun enableSpeakerMode() {
        with(toolBars) {
            tvVideo.visibility = View.VISIBLE
            tvAudio.visibility = View.VISIBLE
        }
    }

    override fun disableSpeakerMode() {
        with(toolBars) {
            tvVideo.visibility = View.GONE
            tvAudio.visibility = View.GONE
        }
    }

    override fun showVolume(level: LPConstants.VolumeLevel) {
        //无音频大小
    }

    override fun showAudioRoomError() {
        showToastMessage(getString(R.string.live_audio_room_error))
    }

    override fun showMessage(s: String) {
        showToastMessage(s)
    }

    override fun showSpeakInviteDlg(invite: Int) {
        if (invite == 0) { // 取消邀请;
            if (speakInviteDlg?.isShowing == true) {
                speakInviteDlg?.dismiss()
            }
            return
        }
        if (speakInviteDlg?.isShowing == true) {
            return
        }
        context?.let {
            speakInviteDlg = MaterialDialog.Builder(it)
                    .content(R.string.live_invite_speak_tip)
                    .positiveText(getString(R.string.live_agree))
                    .negativeText(getString(R.string.live_disagree))
                    .cancelable(false)
                    .positiveColor(ContextCompat.getColor(it, R.color.live_blue))
                    .negativeColor(ContextCompat.getColor(it, R.color.live_blue))
                    .onPositive { materialDialog, _ ->
                        presenter.onSpeakInvite(1)
                        materialDialog.dismiss()
                    }
                    .onNegative { dialog, _ ->
                        presenter.onSpeakInvite(0)
                        dialog.dismiss()
                    }
                    .build()

            if (canShowDialog()) {
                speakInviteDlg?.show()
            }
        }
    }

    override fun showForceSpeakDlg(tipRes: Int) {
        if (canShowDialog()) {
            context?.let {
                MaterialDialog.Builder(it)
                        .content(tipRes)
                        .positiveText(getString(R.string.live_i_got_it))
                        .positiveColor(ContextCompat.getColor(it, R.color.live_blue))
                        .onPositive { materialDialog, _ -> materialDialog.dismiss() }
                        .canceledOnTouchOutside(true)
                        .build()
                        .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.unSubscribe()
        pptContainer.removeAllViews()
        menuContainer.removeAllViews()
        LPRxUtils.dispose(disposeOfClickable)
        LPRxUtils.dispose(disposables)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.speakApply()
            } else if (grantResults.isNotEmpty()) {
                showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA)
            }
        }
    }

    companion object {
        fun newInstance() = PPTFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_ppt
}