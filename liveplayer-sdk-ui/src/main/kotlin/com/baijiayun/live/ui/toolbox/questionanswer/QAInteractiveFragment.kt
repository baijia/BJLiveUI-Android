package com.baijiayun.live.ui.toolbox.questionanswer

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseDialogFragment
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import kotlinx.android.synthetic.main.fragment_qa_interactive.*

/**
 * Created by yongjiaming on 2019-10-30
 * Describe:问答交互主页面，分为待回复、待发布、已发布三个tab，老师或助教才这么显示。
 * 每个tab都是QADetailFragment
 */
class QAInteractiveFragment : BasePadFragment() {

    private lateinit var viewPager: ViewPager
    private lateinit var tablayout: TabLayout
    private lateinit var questionSendFragment: BaseDialogFragment


    private val qaViewModel by lazy {
        activity?.run {
            getViewModel { QAViewModel(routerViewModel.liveRoom) }
        }
    }

    override fun init(view: View) {
        viewPager = view.findViewById(R.id.qa_viewpager)
        tablayout = view.findViewById(R.id.qa_tablayout)
        initViewpager(routerViewModel.liveRoom.isTeacherOrAssistant)

        send_qa_btn.setOnClickListener {
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

    override fun getLayoutId() = R.layout.fragment_qa_interactive


    private fun initViewpager(showThreeTabs : Boolean = false) {
        if(showThreeTabs){
            val fragmentList = mutableListOf<Fragment>(QADetailFragment.newInstance(QADetailFragment.QATabStatus.ToAnswer),
                    QADetailFragment.newInstance(QADetailFragment.QATabStatus.ToPublish), QADetailFragment.newInstance(QADetailFragment.QATabStatus.Published))

            viewPager.offscreenPageLimit = 2
            viewPager.adapter = object : FragmentStatePagerAdapter(childFragmentManager) {

                override fun getItem(position: Int) = fragmentList[position]

                override fun getCount() = fragmentList.size

                override fun getPageTitle(position: Int): CharSequence? {
                    return when (position) {
                        0 -> {
                            context?.resources?.getString(R.string.qa_to_answer) ?: ""
                        }
                        1 -> {
                            context?.resources?.getString(R.string.qa_to_publish) ?: ""
                        }
                        2 -> {
                            context?.resources?.getString(R.string.qa_published) ?: ""
                        }
                        else -> {
                            ""
                        }
                    }
                }
            }
            tablayout.visibility = View.VISIBLE
        } else{
            viewPager.adapter = object : FragmentStatePagerAdapter(childFragmentManager) {
                override fun getItem(p0: Int): Fragment {
                    return QADetailFragment.newInstance(QADetailFragment.QATabStatus.AllStatus)
                }

                override fun getCount() = 1
            }
            tablayout.visibility = View.GONE
        }
    }
}