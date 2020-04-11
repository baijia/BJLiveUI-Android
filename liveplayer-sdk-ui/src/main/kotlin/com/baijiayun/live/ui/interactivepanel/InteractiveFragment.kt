package com.baijiayun.live.ui.interactivepanel

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.graphics.Color
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.chat.ChatPadFragment
import com.baijiayun.live.ui.chat.ChatViewModel
import com.baijiayun.live.ui.onlineuser.OnlineUserFragment
import com.baijiayun.live.ui.onlineuser.OnlineUserViewModel
import com.baijiayun.live.ui.router.Router
import com.baijiayun.live.ui.router.RouterCode
import com.baijiayun.live.ui.toolbox.questionanswer.QAViewModel
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.widget.DragResizeFrameLayout
import kotlinx.android.synthetic.main.fragment_pad_interactive.*

/**
 * Created by Shubo on 2019-10-10.
 */
class InteractiveFragment : BasePadFragment(), DragResizeFrameLayout.OnResizeListener {

    private lateinit var interactiveContainer: ViewGroup
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager

    //底部dragFrameLayout的红点提示
    private lateinit var bottomDragRedPointTv: TextView

    private var chatRedTipTv: TextView? = null
    private var qaRedPointTv: TextView? = null
    private lateinit var dragResizeFrameLayout: DragResizeFrameLayout
    private lateinit var userTabItemTv: TextView

    //底部可拖拽item名称
    private lateinit var dragTabTextView: TextView

    //底部可拖拽item icon
    private lateinit var dragTabImageView: ImageView

    private val userViewModel by lazy {
        getViewModel { OnlineUserViewModel(routerViewModel.liveRoom) }
    }
    private val qaViewModel by lazy {
        activity?.run {
            getViewModel { QAViewModel(routerViewModel) }
        }
    }

    private val chatViewModel by lazy {
        activity?.run {
            getViewModel { ChatViewModel(routerViewModel) }
        }
    }

    private var shouldShowMessageRedPoint = true
    private var shouldShowQARedPoint = true

    /**
     * 学生和老师的tabs分别读取配置项
     * 问答和用户列表tabs和各自的配置项都满足才显示
     */
    private val liveFeatureTabs by lazy {
        if ((routerViewModel.liveRoom.partnerConfig.liveFeatureTabs.isNullOrEmpty() && isTeacherOrAssistant())
                || (routerViewModel.liveRoom.partnerConfig.liveStudentFeatureTabs.isNullOrEmpty() && !isTeacherOrAssistant())) {
            ArrayList()
        } else {
            val list =
                    if (isTeacherOrAssistant()) routerViewModel.liveRoom.partnerConfig.liveFeatureTabs.split(",")
                    else routerViewModel.liveRoom.partnerConfig.liveStudentFeatureTabs.split(",")
            val tabList = ArrayList(list)
            tabList.remove(LABEL_SPEAKER)
            tabList.remove(LABEL_ANSWER)
            //问答调整至ppt页面，tab只有user和chat
            if (tabList.size >= 3) {
                val iterator = tabList.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next != LABEL_CHAT && next != LABEL_USER) {
                        iterator.remove()
                    }
                }
            }
            if (routerViewModel.liveRoom.partnerConfig.liveHideUserList == 1) {
                tabList.remove(LABEL_USER)
            }
            tabList
        }
    }

    private val questionAnswerEnable by lazy {
        routerViewModel.liveRoom.partnerConfig.enableLiveQuestionAnswer == 1 && liveFeatureTabs.contains(LABEL_ANSWER)
    }

    @SuppressLint("SetTextI18n")
    override fun observeActions() {
        compositeDisposable.add(Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                .subscribe {
                    initView()
                    shouldShowQARedPoint = liveFeatureTabs.size > 1 && liveFeatureTabs[0] != LABEL_ANSWER
                    shouldShowMessageRedPoint = liveFeatureTabs.size > 1 && liveFeatureTabs[0] != LABEL_CHAT
                    if (liveFeatureTabs.contains(LABEL_USER)) {
                        userViewModel.subscribe()
                        userViewModel.onlineUserCount.observe(this, Observer {
                            userTabItemTv.text = "${getString(R.string.user)}($it)"
                        })
                    }

                    if (liveFeatureTabs.size > 1 && liveFeatureTabs.contains(LABEL_CHAT)) {
                        chatViewModel?.redPointNumber?.observe(this, Observer {
                            it?.let {
                                showChatRedPoint(it)
                            }
                        })
                    }

                    if (questionAnswerEnable) {
                        qaViewModel?.allQuestionList?.observe(this, Observer {
                            showAnswerRedPoint()
                        })
                    }
                    //少于3个不显示底部可拖拽控件
                    if (liveFeatureTabs.size != 3) {
                        dragResizeFrameLayout.visibility = View.GONE
                    }
                })
    }

    private fun showChatRedPoint(redPointNumber: Int) {
        if (liveFeatureTabs.size == 3 && liveFeatureTabs.contains(LABEL_CHAT) && liveFeatureTabs[liveFeatureTabs.size - 1] == LABEL_CHAT) {
            if (dragResizeFrameLayout.status == DragResizeFrameLayout.Status.MINIMIZE && redPointNumber > 0) {
                bottomDragRedPointTv.visibility = View.VISIBLE
            } else {
                bottomDragRedPointTv.visibility = View.GONE
            }
        } else {
            if (shouldShowMessageRedPoint && redPointNumber > 0) {
                chatRedTipTv?.visibility = View.VISIBLE
                chatRedTipTv?.text = if (redPointNumber > 99) ".." else redPointNumber.toString()
            } else {
                chatRedTipTv?.visibility = View.GONE
            }
        }
    }

    private fun showAnswerRedPoint() {
        if (liveFeatureTabs.size == 3 && liveFeatureTabs.contains(LABEL_ANSWER) && liveFeatureTabs[liveFeatureTabs.size - 1] == LABEL_ANSWER) {
            if (dragResizeFrameLayout.status == DragResizeFrameLayout.Status.MINIMIZE) {
                bottomDragRedPointTv.visibility = View.VISIBLE
            } else {
                bottomDragRedPointTv.visibility = View.GONE
            }
        } else {
            qaRedPointTv?.visibility = if (shouldShowQARedPoint) View.VISIBLE else View.GONE
        }
    }

    override fun init(view: View) {
        dragResizeFrameLayout = view.findViewById(R.id.qa_resize_layout)
        interactiveContainer = view.findViewById(R.id.interactive_container)
        tabLayout = view.findViewById(R.id.user_chat_tablayout)
        viewPager = view.findViewById(R.id.user_chat_viewpager)
        bottomDragRedPointTv = view.findViewById(R.id.qa_red_point)
        dragTabTextView = view.findViewById(R.id.qa_title_tv)
        dragTabImageView = view.findViewById(R.id.qa_iv)

        with(dragResizeFrameLayout) {
            minHeight = DisplayUtils.dip2px(context, 32.0f)
            post {
                dragResizeFrameLayout.maxHeight = interactiveContainer.height - tabLayout.height
            }
            setOnResizeListener(this@InteractiveFragment)
            setOnClickListener {
                if (status != DragResizeFrameLayout.Status.MIDDLE) {
                    middle()
                }
            }
        }
    }

    private fun initView() {
        routerViewModel.chatLabelVisiable = liveFeatureTabs.contains(LABEL_CHAT)
        if (liveFeatureTabs.size == 1) {
            tabLayout.setSelectedTabIndicatorColor(Color.TRANSPARENT)
        }
        initViewpager()
        initTabLayout()
    }

    private fun initTabLayout() {
        routerViewModel.action2Chat.value = !liveFeatureTabs.isNullOrEmpty() && liveFeatureTabs[0] == LABEL_CHAT
        tabLayout.setupWithViewPager(viewPager)
        for (i in liveFeatureTabs.indices) {
            val tabView = getTabView()
            tabLayout.getTabAt(i)?.customView = tabView
            val tabItemTv = tabView.findViewById<TextView>(R.id.item_chat_tv)
            when (liveFeatureTabs[i]) {
                LABEL_CHAT -> chatRedTipTv = tabView.findViewById(R.id.item_red_tip_tv)
                LABEL_ANSWER -> qaRedPointTv = tabView.findViewById(R.id.item_red_tip_tv)
                LABEL_USER -> {
                    userTabItemTv = tabItemTv
                }
            }
            if (i == 0) {
                context?.run { ContextCompat.getColor(this, R.color.live_pad_selected_tab_blue) }?.let {
                    tabItemTv.setTextColor(it)
                }
            }
            tabItemTv.text = getStringByTag(liveFeatureTabs[i])
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                context?.run { ContextCompat.getColor(this, R.color.live_pad_grey) }?.let {
                    tab?.customView?.findViewById<TextView>(R.id.item_chat_tv)?.setTextColor(it)
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                context?.run { ContextCompat.getColor(this, R.color.live_pad_selected_tab_blue) }?.let {
                    tab?.customView?.findViewById<TextView>(R.id.item_chat_tv)?.setTextColor(it)
                }
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
            }
        })
    }

    private fun getTabView(): View {
        return LayoutInflater.from(context).inflate(R.layout.chat_custom_tab_item, user_chat_tablayout, false)
    }

    private fun initViewpager() {
        viewPager.adapter = object : FragmentStatePagerAdapter(childFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return getFragmentByTag(liveFeatureTabs[position])
            }

            override fun getCount(): Int {
                return if (liveFeatureTabs.size == 3) liveFeatureTabs.size - 1 else liveFeatureTabs.size
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                shouldShowMessageRedPoint = liveFeatureTabs[p0] != LABEL_CHAT
                if (!shouldShowMessageRedPoint) {
                    chatViewModel?.redPointNumber?.value = 0
                    routerViewModel.action2Chat.value = true
                } else {
                    routerViewModel.action2Chat.value = false
                }
                shouldShowQARedPoint = liveFeatureTabs[p0] != LABEL_ANSWER
                if (!shouldShowQARedPoint) {
                    qaRedPointTv?.visibility = View.GONE
                }
            }
        })
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_interactive

    private fun isChatBottom(): Boolean = routerViewModel.actionNavigateToMain && liveFeatureTabs.size == 3
            && liveFeatureTabs.contains(LABEL_CHAT) && liveFeatureTabs[liveFeatureTabs.size - 1] == LABEL_CHAT

    override fun onMaximize() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MAXIMIZE
        bottomDragRedPointTv.visibility = View.GONE
        qaRedPointTv?.visibility = View.GONE
        if (isChatBottom()) {
            routerViewModel.action2Chat.value = true
        }
    }

    override fun onMiddle() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MIDDLE
        bottomDragRedPointTv.visibility = View.GONE
        qaRedPointTv?.visibility = View.GONE
        if (isChatBottom()) {
            routerViewModel.action2Chat.value = true
        }
    }

    override fun onMinimize() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MINIMIZE
        if (isChatBottom()) {
            routerViewModel.action2Chat.value = false
        }
    }

    companion object {
        const val LABEL_CHAT = "chat"
        const val LABEL_ANSWER = "answer"
        const val LABEL_USER = "user"
        const val LABEL_SPEAKER = "speaker"
        fun newInstance() = InteractiveFragment()
    }

    private fun getFragmentByTag(tag: String): Fragment {
        return when (tag) {
            LABEL_CHAT -> ChatPadFragment()
            else -> OnlineUserFragment()
        }
    }

    private fun getStringByTag(tag: String): String? {
        return when (tag) {
            LABEL_CHAT -> context?.getString(R.string.chat)
            LABEL_USER -> context?.getString(R.string.user)
            else -> ""
        }
    }
}