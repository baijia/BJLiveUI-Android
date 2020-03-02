package com.baijiayun.live.ui.pptpanel

import android.arch.lifecycle.MutableLiveData
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.imodels.IMediaControlModel
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel

class PPTViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {
    val liveRoom by lazy {
        routerViewModel.liveRoom
    }
    val handsupList by lazy {
        routerViewModel.handsupList
    }
    val hasRead = MutableLiveData<Boolean>()
    private fun loadHandsUpList() {
        hasRead.postValue(false)
        handsupList.postValue(liveRoom.speakQueueVM.applyList)
    }
    init {
        loadHandsUpList()
        subscribe()
    }
    override fun subscribe() {
        liveRoom.speakQueueVM.run {
            observableOfSpeakApply.mergeWith(observableOfSpeakApplyDeny).subscribe(object : DisposingObserver<IMediaModel>() {
                override fun onNext(t: IMediaModel) {
                    loadHandsUpList()
                }
            })
            observableOfSpeakResponse.subscribe(object : DisposingObserver<IMediaControlModel>() {
                override fun onNext(t: IMediaControlModel) {
                    loadHandsUpList()
                }
            })
        }
    }
    fun isTeacherOrAssistant():Boolean{
        return routerViewModel.liveRoom.isTeacherOrAssistant || routerViewModel.liveRoom.isGroupTeacherOrAssistant
    }
    fun startClass() {
        liveRoom.requestClassStart()
    }
}