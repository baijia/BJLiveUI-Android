package com.baijiayun.live.ui.interactivepanel

import android.arch.lifecycle.Observer
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
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
import com.baijiayun.live.ui.toolbox.questionanswer.QAInteractiveFragment
import com.baijiayun.live.ui.toolbox.questionanswer.QAViewModel
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.widget.DragResizeFrameLayout
import kotlinx.android.synthetic.main.fragment_pad_interactive.*

/**
 * Created by Shubo on 2019-10-10.
 */
class InteractiveFragment : BasePadFragment(), DragResizeFrameLayout.OnResizeListener {

    private lateinit var interactiveContainer : ViewGroup
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
            getViewModel { QAViewModel(routerViewModel.liveRoom) }
        }
    }

    private val chatViewModel by lazy {
        activity?.run {
            getViewModel { ChatViewModel(routerViewModel) }
        }
    }

    private var shouldShowMessageRedPoint = true
    private var shouldShowQARedPoint = true

    private val liveFeatureTabs by lazy {
        val tabList = routerViewModel.liveRoom.partnerConfig.liveFeatureTabs.split(",") as ArrayList<String>
        tabList.remove("speaker")
        //仅当enableLiveQuestionAnswer和liveFeatureTabs同时满足才显示问答
        if(routerViewModel.liveRoom.partnerConfig.enableLiveQuestionAnswer == 0){
            tabList.remove("answer")
        }
        tabList
    }

    private val questionAnswerEnable by lazy{
        routerViewModel.liveRoom.partnerConfig.enableLiveQuestionAnswer == 1 && liveFeatureTabs.contains("answer")
    }

    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer { it2 ->
            if (it2 != true) {
                return@Observer
            }
            initView()
            shouldShowQARedPoint = liveFeatureTabs[0] != "answer"
            shouldShowMessageRedPoint = liveFeatureTabs[0] != "user"
            if(liveFeatureTabs.contains("user")){
                userViewModel.subscribe()
                userViewModel.onlineUserCount.observe(this, Observer {
                    userTabItemTv.text = "${getString(R.string.user)}($it)"
                })
            }

            if(liveFeatureTabs.contains("chat")){
                chatViewModel?.redPointNumber?.observe(this, Observer {
                    if (shouldShowMessageRedPoint && it != null && it > 0) {
                        chatRedTipTv?.visibility = View.VISIBLE
                        chatRedTipTv?.text = if (it > 99) ".." else it.toString()
                    } else {
                        chatRedTipTv?.visibility = View.GONE
                    }
                })
            }

            if(questionAnswerEnable){
                qaViewModel?.allQuestionList?.observe(this, Observer {
                    if(shouldShowQARedPoint) qaRedPointTv?.visibility = View.VISIBLE else qaRedPointTv?.visibility = View.GONE
                })
            }
            if(liveFeatureTabs[liveFeatureTabs.size -1] == "answer"){
                qaViewModel?.allQuestionList?.observe(this, Observer {
                    if (dragResizeFrameLayout.status == DragResizeFrameLayout.Status.MINIMIZE) {
                        bottomDragRedPointTv.visibility = View.VISIBLE
                    } else{
                        bottomDragRedPointTv.visibility = View.GONE
                    }
                })
            } else if(liveFeatureTabs[liveFeatureTabs.size -1] == "chat"){
                chatViewModel?.redPointNumber?.observe(this, Observer {
                    if (dragResizeFrameLayout.status == DragResizeFrameLayout.Status.MINIMIZE && it != null && it > 0) {
                        bottomDragRedPointTv.visibility = View.VISIBLE
                    } else {
                        bottomDragRedPointTv.visibility = View.GONE
                    }
                })
            }
        })
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

    private fun initView(){
        initViewpager()
        initTabLayout()
        //初始化底部可拖拽部分
        val lastTabTag = liveFeatureTabs[liveFeatureTabs.size - 1]
        childFragmentManager.beginTransaction().add(R.id.qa_container, getFragmentByTag(lastTabTag)).commitAllowingStateLoss()
        dragTabTextView.text = getStringByTag(lastTabTag)
        dragTabImageView.visibility = if(lastTabTag == "answer") View.VISIBLE else View.GONE
    }

    private fun initTabLayout(){
        routerViewModel.action2Chat.value = !liveFeatureTabs.isNullOrEmpty() && liveFeatureTabs[0] == "chat"
        tabLayout.setupWithViewPager(viewPager)
        for (i in liveFeatureTabs.indices) {
            val tabView = getTabView()
            tabLayout.getTabAt(i)?.customView = tabView
            val tabItemTv = tabView.findViewById<TextView>(R.id.item_chat_tv)
            when(liveFeatureTabs[i]){
                "chat" -> chatRedTipTv = tabView.findViewById(R.id.item_red_tip_tv)
                "answer" -> qaRedPointTv = tabView.findViewById(R.id.item_red_tip_tv)
                "user" -> {
                    userTabItemTv = tabItemTv
                }
            }
            if (i == 0) {
                context?.resources?.getColor(R.color.live_pad_selected_tab_blue)?.let {
                    tabItemTv.setTextColor(it)
                }
            }
            tabItemTv.text = getStringByTag(liveFeatureTabs[i])
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
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
        return LayoutInflater.from(context).inflate(R.layout.chat_custom_tab_item, user_chat_tablayout, false)
    }

    private fun initViewpager() {
        viewPager.adapter = object : FragmentStatePagerAdapter(childFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return getFragmentByTag(liveFeatureTabs[position])
            }

            override fun getCount() = liveFeatureTabs.size - 1
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                shouldShowMessageRedPoint = liveFeatureTabs[p0] != "chat"
                if (!shouldShowMessageRedPoint) {
                    chatViewModel?.redPointNumber?.value = 0
                    routerViewModel.action2Chat.value = true
                } else {
                    routerViewModel.action2Chat.value = false
                }
                shouldShowQARedPoint = liveFeatureTabs[p0] != "answer"
                if(!shouldShowQARedPoint){
                    qaRedPointTv?.visibility = View.GONE
                }
            }
        })
    }

    override fun getLayoutId(): Int = R.layout.fragment_pad_interactive

    override fun onMaximize() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MAXIMIZE
        bottomDragRedPointTv.visibility = View.GONE
        qaRedPointTv?.visibility = View.GONE
    }

    override fun onMiddle() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MIDDLE
        bottomDragRedPointTv.visibility = View.GONE
        qaRedPointTv?.visibility = View.GONE
    }

    override fun onMinimize() {
        qaViewModel?.notifyDragStatusChange?.value = DragResizeFrameLayout.Status.MINIMIZE
    }

    companion object {
        fun newInstance() = InteractiveFragment()
    }

    private fun getFragmentByTag(tag: String): Fragment {
        return when (tag) {
            "chat" -> ChatPadFragment()
            "answer" -> QAInteractiveFragment()
            else -> OnlineUserFragment()
        }
    }

    private fun getStringByTag(tag: String): String?{
        return when (tag) {
            "chat" -> context?.getString(R.string.chat)
            "answer" -> context?.getString(R.string.live_room_question_answer_text)
            "user" -> context?.getString(R.string.user)
            else -> ""
        }
    }
}