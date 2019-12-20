package com.baijiayun.live.ui.loading

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import com.baijiayun.live.ui.LiveRoomTripleActivity
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.databinding.FragmentLoadingPadBinding
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.listener.LPLaunchListener

/**
 * Created by yongjiaming on 2019-10-12
 * Describe:
 */
class LoadingPadFragment : BasePadFragment(){

    private lateinit var animator : ObjectAnimator
    private lateinit var progressBar : ProgressBar

    val launchListener  = object : LPLaunchListener{
        override fun onLaunchSteps(currentStep: Int, totalSteps: Int) {
            val start = progressBar.progress
            val end = currentStep * 100 / totalSteps
            if (this@LoadingPadFragment::animator.isInitialized && animator.isRunning) {
                animator.cancel()
            }
            animator = ObjectAnimator.ofInt(progressBar, "progress", start, end)
            with(animator){
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }

        override fun onLaunchError(error: LPError?) {
            routerViewModel.actionShowError.value = error
        }

        override fun onLaunchSuccess(liveRoom: LiveRoom?) {
            liveRoom?.also {
                if(liveRoom.isUseWebRTC){
                    if(liveRoom.isTeacher){
                        if(checkTeacherCameraPermission()){
                            navigateToMain()
                        }
                    } else{
                        if(liveRoom.roomType != LPConstants.LPRoomType.Multi){
                            if(checkTeacherCameraPermission()){
                                navigateToMain()
                            }
                        } else{
                            navigateToMain()
                        }
                    }
                } else{
                    navigateToMain()
                }
            }
        }

        fun navigateToMain(){
            routerViewModel.actionNavigateToMain.value = true
        }
    }

    override fun init(view : View) {
        val dataBinding = FragmentLoadingPadBinding.bind(view)
        dataBinding.lifecycleOwner = this
        dataBinding.loadingFragment = this
        progressBar = view.findViewById(R.id.fragment_loading_pb)
    }
    fun showTechSupport() :Boolean{
        return (activity as LiveRoomTripleActivity).getShowTechSupport()
    }

    override fun getLayoutId() = R.layout.fragment_loading_pad

    companion object{
        fun newInstance() : LoadingPadFragment{
            return LoadingPadFragment()
        }
    }
}