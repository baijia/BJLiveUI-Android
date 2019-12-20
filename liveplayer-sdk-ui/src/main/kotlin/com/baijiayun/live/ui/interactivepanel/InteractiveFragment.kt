package com.baijiayun.live.ui.interactivepanel

import android.arch.lifecycle.Observer
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseDialogFragment
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.chat.ChatPadFragment
import com.baijiayun.live.ui.chat.ChatViewModel
import com.baijiayun.live.ui.onlineuser.OnlineUserFragment
import com.baijiayun.live.ui.onlineuser.OnlineUserViewModel
import com.baijiayun.live.ui.toolbox.questionanswer.*
import com.baijiayun.live.ui.widget.DragResizeFrameLayout
import com.baijiayun.livecore.utils.DisplayUtils
import kotlinx.android.synthetic.main.fragment_pad_interactive.*

/**
 * Created by Shubo on 2019-10-10.
 */
class InteractiveFragment : BasePadFragment(), DragResizeFrameLayout.OnResizeListener {

    private lateinit var redTipTv: TextView

    private lateinit var questionSendFragment: BaseDialogFragment

    private val userViewModel by lazy {
        getViewModel { OnlineUserViewModel(routerViewModel.liveRoom) }
    }
    private val qaViewModel by lazy {
        activity?.run {
            getViewModel { QAViewModel(routerViewModel.liveRoom) }
        }
    }

    private val chatViewModel by lazy {
        activity?.run {
            getViewModel { ChatViewModel(routerViewModel.liveRoom) }
        }
    }

    private var shouldShowMessageRedPoint = true


    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer { it2 ->
            if (it2 != true) {
                return@Observer
            }
            userViewModel.subscribe()
            userViewModel.onlineUserCount.observe(this, Observer {
                user_chat_tablayout.getTabAt(0)?.customView?.findViewById<TextView>(R.id.item_chat_tv)?.text = "${getString(R.string.user)}($it)"
            })

            chatViewModel?.redPointNumber?.observe(this, Observer {
                if (shouldShowMessageRedPoint && it != null && it > 0) {
                    redTipTv.visibility = View.VISIBLE
                    redTipTv.text = if (it > 99) ".." else it.toString()
                } else {
                    redTipTv.visibility = View.GONE
                }
            })

            qaViewModel?.allQuestionList?.observe(this, Observer {
                if (qa_resize_layout.status == DragResizeFrameLayout.Status.MINIMIZE) {
                    qa_red_point.visibility = View.VISIBLE
                }
            })
        })
    }

    override fun init(view: View) {
        initViewpager()
        initTabLayout()
        with(qa_resize_layout) {
            minHeight = DisplayUtils.dip2px(context, 32.0f)
            post {
                qa_resize_layout.maxHeight = interactive_container.height - user_chat_tablayout.height
            }
            setOnResizeListener(this@InteractiveFragment)
            setOnClickListener {
                if (status != DragResizeFrameLayout.Status.MIDDLE) {
                    middle()
                }
            }
        }
        addFragment(R.id.qa_container, QAInteractiveFragment())

        view.findViewById<ImageView>(R.id.send_qa_btn).setOnClickListener {
            if (!::questionSendFragment.isInitialized) {
                questionSendFragment = QuestionSendFragment.newInstance(GENERATE_QUESTION, "", QADetailFragment.QATabStatus.ToAnswer)
            } else {
                if (questionSendFragment.isAdded) {
                    return@setOnClickListener
                }
                showDialogFragment(questionSendFragment)
            }
        }
    }

    private fun initTabLayout(){
        user_chat_tablayout.setupWithViewPager(user_chat_viewpager)
        for (i in 0..1) {
            val tabView = getTabView()
            user_chat_tablayout.getTabAt(i)?.customView = tabView
            val tabItemTv = tabView.findViewById<TextView>(R.id.item_chat_tv)
            if (i == 0) {
                context?.resources?.getColor(R.color.live_pad_selected_tab_blue)?.let {
                    tabItemTv.setTextColor(it)
                }
                tabItemTv.text = context?.getString(R.string.user)
            } else {
                tabItemTv.text = context?.getString(R.string.chat)
                redTipTv = tabView.findViewById(R.id.item_red_tip_tv)
            }
        }
        user_chat_tablayout.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
            override fun onTabUnselected(p0: TabLayout.Tab?) {
                context?.resources?.getColor(R.color.live_pad_grey)?.let {
                    p0?.customView?.findViewById<TextView>(R.id.item_chat_tv)?.setTextColor(it)
                }
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                context?.resources?.getColor(R.color.live_pad_selected_tab_blue)?.let {
                    p0?.customView?.findViewById<TextView>(R.id.item_chat_tv)?.setTextColor(it)
                }
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
            }
        })
    }

    private fun getTabView(): View {
        return LayoutInflater.from(context).inflate(R.layout.chat_custom_tab_item, null)
    }

    private fun initViewpager() {
        user_chat_viewpager.adapter = object : FragmentStatePagerAdapter(fragmentManager) {

            override fun getItem(position: Int): Fragment {
                return if (position == 0) {
                    OnlineUserFragment()
                } else {
                    ChatPadFragment()
                }
            }

            override fun getCount() = 2
        }

        user_chat_viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                shouldShowMessageRedPoint = p0 != 1
                chatViewModel?.redPointNumber?.value = 0
            }
        })
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_interactive

    override fun onMaximize() {
        qaViewModel?.notifySizeChange?.value = DragResizeFrameLayout.Status.MAXIMIZE
        qa_red_point.visibility = View.GONE
    }

    override fun onMiddle() {
        qaViewModel?.notifySizeChange?.value = DragResizeFrameLayout.Status.MIDDLE
        qa_red_point.visibility = View.GONE
    }

    override fun onMinimize() {
        qaViewModel?.notifySizeChange?.value = DragResizeFrameLayout.Status.MINIMIZE
    }

    companion object {
        fun newInstance() = InteractiveFragment()
    }
}