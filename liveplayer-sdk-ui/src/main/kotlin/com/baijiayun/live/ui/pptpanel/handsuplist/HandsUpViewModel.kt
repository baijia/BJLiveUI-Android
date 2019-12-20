package com.baijiayun.live.ui.pptpanel.handsuplist

import android.arch.lifecycle.MutableLiveData
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.utils.LPLogger
import com.baijiayun.livecore.utils.LPRxUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class HandsUpViewModel(val liveRoom: LiveRoom,val handsupList: MutableLiveData<List<IUserModel>>? ) : BaseViewModel() {

    companion object {
        const val TAG = "HandsUpViewModel"
    }

    override fun subscribe() {
    }

    fun get(index: Int): IUserModel? {
        return handsupList?.value?.get(index)
    }

    fun disagree(userId: String) {
        LPLogger.d(TAG, "speak apply disagree")
        liveRoom.speakQueueVM.disagreeSpeakApply(userId)
    }

    fun agree(userId: String) {
        LPLogger.d(TAG, "speak apply agree")
        liveRoom.speakQueueVM.agreeSpeakApply(userId)
    }
}
