package com.baijiayun.live.ui.onlineuser

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.chat.ChatOptMenuHelper
import com.baijiayun.live.ui.chat.widget.ChatMessageView
import com.baijiayun.live.ui.databinding.BjyPadItemHandsupBinding
import com.baijiayun.live.ui.databinding.BjyPadLayoutItemOnlineUserBinding
import com.baijiayun.live.ui.users.group.GroupExtendableListViewAdapter
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPGroupItem
import com.baijiayun.livecore.models.LPUserModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.utils.DisplayUtils
import kotlinx.android.synthetic.main.fragment_pad_user_list.elv_online_group
import java.util.ArrayList

/**
 * Created by yongjiaming on 2019-10-23
 * Describe:
 */
class OnlineUserFragment : BasePadFragment() {

    private lateinit var onlineUserRecyclerView: RecyclerView
    private lateinit var onlineGroupTitleTv: TextView
    private lateinit var groupAdapter: GroupExtendableListViewAdapter

    private val onlineUserAdapter by lazy { OnlineUserAdapter() }

    private val onlineUserViewModel by lazy {
        getViewModel { OnlineUserViewModel(routerViewModel.liveRoom) }
    }

    private var isLoading = false

    override fun init(view: View) {
    }

    override fun getLayoutId() = R.layout.fragment_pad_user_list

    @SuppressLint("SetTextI18n")
    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer { b ->
            if (b != true) {
                return@Observer
            }
            initExpandableListView()
            with(onlineUserViewModel) {
                subscribe()
                onlineUserList.observe(this@OnlineUserFragment, Observer {
                    isLoading = false
                    onlineUserAdapter.notifyDataSetChanged()
                })
                onlineUserGroup.observe(this@OnlineUserFragment, Observer {
                    onlineGroupTitleTv.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
                    onlineGroupTitleTv.text = resources.getString(R.string.string_group) + "(${it?.size})"
                    groupAdapter.setDate(it)
                    groupAdapter.notifyDataSetChanged()
                })
            }
        })
    }

    private fun initExpandableListView() {
        context?.let {
            onlineUserRecyclerView = RecyclerView(it)
            with(onlineUserRecyclerView) {
                layoutManager = LinearLayoutWrapManager(context)
                setHasFixedSize(true)
                adapter = onlineUserAdapter
            }
        }

        onlineGroupTitleTv = TextView(context)
        onlineGroupTitleTv.textSize = 16f
        onlineGroupTitleTv.height = DisplayUtils.dip2px(context, 30f)
        onlineGroupTitleTv.visibility = View.GONE

        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(onlineUserRecyclerView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        linearLayout.addView(onlineGroupTitleTv)

        groupAdapter = GroupExtendableListViewAdapter(onlineUserViewModel.getAssistantLabel(), onlineUserViewModel.getGroupId())
        groupAdapter.setOnUpdateListener {
            onlineUserViewModel.loadMore(it)
        }

        elv_online_group.addHeaderView(linearLayout)
        elv_online_group.setAdapter(groupAdapter)
        elv_online_group.setOnGroupExpandListener {
            var i = 0
            val count = groupAdapter.groupCount
            while (i < count) {
                if (i != it) {
                    elv_online_group.collapseGroup(i)
                }
                i++
            }

            val item = groupAdapter.getGroup(it) as LPGroupItem
            onlineUserViewModel.updateGroupInfo(item)
        }
        elv_online_group.setOnChildClickListener { _, view, groupPosition, childPosition, _ ->
            kotlin.run {
                val userModel = groupAdapter.getChild(groupPosition, childPosition) as LPUserModel
                ChatOptMenuHelper.showOptMenu(context, routerViewModel, view, userModel)
                true
            }
        }
        onlineUserAdapter.setOnItemClickListener(object :OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                val user = onlineUserAdapter.getUser(position) as LPUserModel
                ChatOptMenuHelper.showOptMenu(context, routerViewModel,view, user)
            }
        })
    }


    inner class OnlineUserAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_USER = 0
        private val VIEW_TYPE_LOADING = 1

        private val visibleThreshold = 5
        private var lastVisibleItem = 0
        private var totalItemCount = 0
        private var onItemClickListener: OnItemClickListener? = null

        init {
            onlineUserRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy == 0) {
                        return
                    }
                    val linearLayoutManager = recyclerView.layoutManager as LinearLayoutWrapManager
                    totalItemCount = linearLayoutManager.itemCount
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()

                    if (!isLoading && totalItemCount <= lastVisibleItem + visibleThreshold) {
                        onlineUserViewModel.loadMore()
                        isLoading = true
                    }
                }
            })
        }

        override fun getItemViewType(position: Int): Int {
            if (isLoading && position == itemCount - 1) {
                return VIEW_TYPE_LOADING
            }
            return VIEW_TYPE_USER
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): RecyclerView.ViewHolder {
            return if (getItemViewType(position) == VIEW_TYPE_LOADING) {
                val itemView = LayoutInflater.from(context).inflate(R.layout.bjy_item_online_user_loadmore, viewGroup, false)
                LoadingViewHolder(itemView)
            } else {
                val dataBinding: BjyPadLayoutItemOnlineUserBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.bjy_pad_layout_item_online_user, viewGroup, false)
                OnlineUserViewHolder(dataBinding, dataBinding.root)
            }
        }

        override fun getItemCount(): Int {
            if (isLoading) {
                return routerViewModel.liveRoom.onlineUserVM.userCount.plus(1)
            }
            return routerViewModel.liveRoom.onlineUserVM.userCount
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            viewHolder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(viewHolder.itemView,position)
            }
            when (viewHolder) {
                is LoadingViewHolder -> {
                    viewHolder.progressBar.isIndeterminate = true
                }
                is OnlineUserViewHolder -> {
                    val currentUser = getUser(position)
                    viewHolder.dataBinding.user = currentUser
//                    context?.run {
//                        val avatar = if (currentUser.avatar.startsWith("//")) "https:" + currentUser.avatar else currentUser.avatar
//                        Glide.with(this).load(avatar).into(TextViewHolder.avatarIv)
//                    }
//                    TextViewHolder.nameTv.text = currentUser.name
                    when (currentUser.type) {
                        LPConstants.LPUserType.Teacher -> {
                            viewHolder.roleTextView.visibility = View.VISIBLE
                            viewHolder.roleTextView.text = context?.resources?.getString(R.string.live_teacher)
                            viewHolder.roleTextView.setTextColor(context?.let { ContextCompat.getColor(it,R.color.live_blue) } ?: Color.BLACK)
                            viewHolder.roleTextView.background = context?.let{ ContextCompat.getDrawable(it,R.drawable.item_online_user_teacher_bg)}
                        }
                        LPConstants.LPUserType.Assistant -> {
                            viewHolder.roleTextView.visibility = View.VISIBLE
                            viewHolder.roleTextView.text = context?.resources?.getString(R.string.live_assistent)
                            viewHolder.roleTextView.setTextColor(
                                    context?.let { ContextCompat.getColor(it,R.color.live_pad_orange) } ?: Color.BLACK)
                            viewHolder.roleTextView.background = context?.let{ ContextCompat.getDrawable(it,R.drawable.item_online_user_assistant_bg)}
                        }
                        else -> {
                            viewHolder.roleTextView.visibility = View.GONE
                        }
                    }
                }
                else -> {

                }
            }
        }

        fun getUser(position: Int): IUserModel {
            return onlineUserViewModel.getUser(position) ?: LPUserModel()
        }

        fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
            this.onItemClickListener = onItemClickListener
        }

    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var progressBar: ProgressBar = itemView.findViewById(R.id.item_online_user_progress)
    }

    class OnlineUserViewHolder(val dataBinding: BjyPadLayoutItemOnlineUserBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roleTextView: TextView = itemView.findViewById(R.id.item_online_user_role)
    }
}