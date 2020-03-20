package com.baijiayun.live.ui.pptpanel.handsuplist

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.*
import com.baijiayun.live.ui.databinding.BjyPadItemHandsupBinding
import com.baijiayun.live.ui.databinding.FragmentHandsupListBinding
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager
import kotlinx.android.synthetic.main.fragment_handsup_list.*

class HandsUpListFragment : BaseDialogFragment() {
    private val handsUpAdapter by lazy {
        HandsUpAdapter()
    }
    private val handsupViewModel by lazy {
        getRouterViewModel()?.run {
            getViewModel { HandsUpViewModel(liveRoom,handsupList) }
        }
    }

    companion object {
        fun newInstance() = HandsUpListFragment()
    }

    override fun init(savedInstanceState: Bundle?, arguments: Bundle?) {
        hideTitleBar()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_handsup_list
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataBinding: FragmentHandsupListBinding? = contentView?.let {
            DataBindingUtil.bind(it)
        }
        dataBinding?.let {
            it.lifecycleOwner = this@HandsUpListFragment
            it.viewmodel = handsupViewModel
        }
        handsupViewModel?.subscribe()
        recyclerView.layoutManager = LinearLayoutWrapManager(context)
        recyclerView.adapter = handsUpAdapter
        observeActions()
    }

    private fun observeActions() {
        handsupViewModel?.run {
            handsupList?.run{
                observe(this@HandsUpListFragment, Observer {
                    handsUpAdapter.notifyDataSetChanged()
                })
            }
        }
    }

    inner class HandsUpAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(container: ViewGroup, position: Int): ViewHolder {
            val dataBinding: BjyPadItemHandsupBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.bjy_pad_item_handsup, container, false)
            return ViewHolder(dataBinding, dataBinding.root)
        }

        override fun getItemCount(): Int {
            return handsupViewModel?.handsupList?.value?.size ?: 0
        }

        override fun onBindViewHolder(viewholder: ViewHolder, position: Int) {
            viewholder.dataBinding.run {
                viewmodel = this@HandsUpListFragment.handsupViewModel?.get(position)
                handsupViewModel = this@HandsUpListFragment.handsupViewModel
                executePendingBindings()
            }
        }

    }

    class ViewHolder(val dataBinding: BjyPadItemHandsupBinding, itemView: View) : RecyclerView.ViewHolder(itemView)

}