package com.baijiayun.live.ui.error

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.view.View
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.databinding.FragmentPadErrorBinding

/**
 * Created by yongjiaming on 2019-10-15
 * Describe:
 */
class ErrorPadFragment : BasePadFragment() {
    private lateinit var errorModel: ErrorFragmentModel
    @SuppressLint("ClickableViewAccessibility")
    override fun init(view: View) {
        activity?.let {
            errorModel = it.getViewModel { ErrorFragmentModel() }
            val binding = DataBindingUtil.bind<FragmentPadErrorBinding>(view)
            binding?.lifecycleOwner = this
            binding?.checkUnique = routerViewModel.checkUnique
            binding?.errorModel = errorModel
            binding?.errorFragment = this
            if (routerViewModel.liveRoom.customerSupportDefaultExceptionMessage.isNullOrEmpty() || !errorModel.shouldShowTechContact) {
                binding?.fragmentErrorSuggestion?.visibility = View.GONE
            } else {
                binding?.fragmentErrorSuggestion?.visibility = View.VISIBLE
                binding?.fragmentErrorSuggestion?.text = routerViewModel.liveRoom.customerSupportDefaultExceptionMessage
            }
            view.setOnTouchListener { _, _ -> true }
        }
    }

    fun showTechSupport() = LiveRoomBaseActivity.getShowTechSupport()

    override fun getLayoutId() = R.layout.fragment_pad_error

    fun onBack() {
        activity?.finish()
    }

    fun retry() {
        when (errorModel.handlerWay) {
            ErrorType.ERROR_HANDLE_RECONNECT, ErrorType.ERROR_HANDLE_REENTER -> {
                routerViewModel.actionReEnterRoom.value = false to true
            }
            ErrorType.ERROR_HANDLE_CONFILICT -> {
                routerViewModel.actionReEnterRoom.value = routerViewModel.checkUnique to true
            }
            ErrorType.ERROR_HANDLE_FINISH ->{
                activity?.finish()
            }
            else -> {
                routerViewModel.actionDismissError.value = Unit
            }
        }
    }

    enum class ErrorType {
        ERROR_HANDLE_RECONNECT, ERROR_HANDLE_REENTER, ERROR_HANDLE_NOTHING, ERROR_HANDLE_CONFILICT,ERROR_HANDLE_FINISH
    }

}