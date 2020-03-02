package com.baijiayun.live.ui.topmenu

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.LiveRoomTripleActivity
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.livecore.context.LPConstants
import kotlinx.android.synthetic.main.fragment_pad_top_menu.*

/**
 * Created by Shubo on 2019-10-10.
 */
class TopMenuFragment : BasePadFragment() {

    private val topMenuViewModel by lazy { getViewModel { TopMenuViewModel(routerViewModel.liveRoom) } }

    companion object {
        fun newInstance() = TopMenuFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_top_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer {
            if (it != true) {
                return@Observer
            }
            fragment_pad_top_menu_title.text = routerViewModel.liveRoom.roomTitle
            topMenuViewModel.subscribe()
            initSuccess()
        })

        topMenuViewModel.classStarTimeCount.observe(this, Observer {
            fragment_pad_top_menu_time.text = it
        })
        topMenuViewModel.showToast.observe(this, Observer {
            it?.let {
                showToastMessage(it)
            }
        })
        topMenuViewModel.recordStatus.observe(this, Observer {
            it?.let {
                fragment_pad_top_menu_record.isChecked = it
                if (routerViewModel.liveRoom.isTeacherOrAssistant || routerViewModel.liveRoom.isGroupTeacherOrAssistant) {
                    if (it && !topMenuViewModel.lastRecordStatus) {
                        showToastMessage(getString(R.string.live_cloud_record_start))
                    }
                }
            }
        })
        topMenuViewModel.downLinkLossRate.observe(this, Observer {
            it?.let {
                fragment_pad_top_menu_downlossrate.text = it.first
                context?.run {
                    fragment_pad_top_menu_downlossrate.setTextColor(ContextCompat.getColor(this, it.second))
                }
            }
        })
        topMenuViewModel.upLinkLossRate.observe(this, Observer {
            it?.let {
                fragment_pad_top_menu_uplossrate.text = it.first
                context?.run {
                    fragment_pad_top_menu_uplossrate.setTextColor(ContextCompat.getColor(this, it.second))
                }
            }
        })
    }

    private fun initSuccess() {
        if (routerViewModel.liveRoom.isAudition) {
            fragment_pad_top_menu_setting.visibility = View.GONE
        }
        routerViewModel.isShowShare.observe(this, Observer {
            it?.let {
                fragment_pad_top_menu_share.visibility = if (it && routerViewModel.liveRoom.featureConfig?.isShareEnable == true) View.VISIBLE else View.GONE
            }
        })
    }

    private fun initView() {
        fragment_pad_top_menu_exit.setOnClickListener {
            if (LiveRoomBaseActivity.getExitListener() != null) {
                activity?.finish()
            }
            routerViewModel.actionExit.value = Unit
        }
        fragment_pad_top_menu_setting.setOnClickListener {
            if (routerViewModel.isClassStarted.value != true) {
                if (routerViewModel.liveRoom.isTeacher) {
                    showToastMessage(getString(R.string.pad_class_start_tip))
                } else {
                    showToastMessage("课程未开始")
                }
                return@setOnClickListener
            }
            routerViewModel.action2Setting.value = Unit
        }
        fragment_pad_top_menu_share.setOnClickListener { routerViewModel.action2Share.value = Unit }
        fragment_pad_top_menu_record.setOnClickListener {
            if (routerViewModel.liveRoom.currentUser.type != LPConstants.LPUserType.Teacher &&
                    routerViewModel.liveRoom.currentUser.type != LPConstants.LPUserType.Assistant) {
                return@setOnClickListener
            }
            if (routerViewModel.isClassStarted.value != true) {
                if (routerViewModel.liveRoom.isTeacher) {
                    showToastMessage(getString(R.string.pad_class_start_tip))
                } else {
                    showToastMessage("课程未开始")
                }
                return@setOnClickListener
            }
            if (topMenuViewModel.recordStatus.value == true) {
                context?.let {
                    MaterialDialog.Builder(it)
                            .apply {
                                title(getString(R.string.live_exit_hint_title))
                                content(getString(R.string.live_cloud_recording_content))
                                contentColorRes(R.color.live_text_color_light)
                            }
                            .apply {
                                positiveText(getString(R.string.live_cloud_record_setting_end))
                                positiveColorRes(R.color.live_red)
                                onPositive { _, _ -> topMenuViewModel.switchCloudRecord() }
                            }
                            .apply {
                                negativeText(getString(R.string.live_cancel))
                                negativeColorRes(R.color.live_blue)
                                onNegative { dialog, _ -> dialog.dismiss() }
                            }
                            .build().show()
                }
            } else {
                topMenuViewModel.switchCloudRecord()
            }
        }
    }

    override fun init(view: View) {
    }
}