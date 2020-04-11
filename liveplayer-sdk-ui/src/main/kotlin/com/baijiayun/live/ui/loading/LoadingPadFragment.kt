package com.baijiayun.live.ui.loading

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.router.Router
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.databinding.FragmentLoadingPadBinding
import com.baijiayun.live.ui.router.RouterCode
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.listener.LPLaunchListener
import kotlinx.android.synthetic.main.fragment_loading_pad.*

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
            if(currentStep == 2){
                val hideBJYSupportMessage = routerViewModel.liveRoom.partnerConfig?.hideBJYSupportMessage?:1
                routerViewModel.shouldShowTecSupport.value = hideBJYSupportMessage == 0
                tv_fragment_loading_tech_support?.visibility = if(hideBJYSupportMessage == 0) View.VISIBLE else View.GONE
            }
        }

        override fun onLaunchError(error: LPError?) {
            routerViewModel.actionShowError.value = error
        }

        override fun onLaunchSuccess(liveRoom: LiveRoom?) {
            liveRoom?.also {
                if(liveRoom.isUseWebRTC){
                    //webrtc推流前必须要有视频权限
                    if(liveRoom.isTeacherOrAssistant){
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
            Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                    .onNext(Unit)
            routerViewModel.actionNavigateToMain = true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun init(view : View) {
        val dataBinding = FragmentLoadingPadBinding.bind(view)
        dataBinding.lifecycleOwner = this
        dataBinding.loadingFragment = this
        progressBar = view.findViewById(R.id.fragment_loading_pb)
        view.setOnTouchListener { _, _ -> true }
        fragment_loading_back.setOnClickListener { activity?.finish() }
    }

    fun showTechSupport() = LiveRoomBaseActivity.getShowTechSupport()

    override fun getLayoutId() = R.layout.fragment_loading_pad

    companion object{
        fun newInstance() : LoadingPadFragment{
            return LoadingPadFragment()
        }
    }
}