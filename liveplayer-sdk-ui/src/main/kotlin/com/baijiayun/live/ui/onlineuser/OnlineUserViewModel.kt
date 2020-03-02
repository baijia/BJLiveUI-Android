package com.baijiayun.live.ui.onlineuser

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.baijiayun.live.ui.LiveSDKWithUI
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.LPGroupItem
import com.baijiayun.livecore.models.imodels.IUserModel
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by yongjiaming on 2019-10-26
 * Describe:
 */
class OnlineUserViewModel(val liveRoom : LiveRoom) : BaseViewModel(){

    val onlineUserCount = MutableLiveData<Int>()
    val onlineUserList = MutableLiveData<List<IUserModel>>()
    val onlineUserGroup = MutableLiveData<List<LPGroupItem>>()

    override fun subscribe() {
        liveRoom.onlineUserVM
                .observableOfOnLineUserCount
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposingObserver<Int>() {
                    override fun onNext(t: Int) {
                        onlineUserCount.value = t
                    }
                })

        liveRoom.onlineUserVM
                .observableOfOnlineUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposingObserver<List<IUserModel>>(){
                    override fun onNext(t: List<IUserModel>) {
                        onlineUserList.value = t
                        onlineUserCount.value = liveRoom.onlineUserVM.allCount
                    }
                })

        liveRoom.onlineUserVM
                .observableOfOnGroupItem
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposingObserver<List<LPGroupItem>>() {
                    override fun onNext(t: List<LPGroupItem>) {
                        onlineUserGroup.value = t
                        onlineUserCount.value = liveRoom.onlineUserVM.allCount
                    }
                })
        liveRoom.onlineUserVM.requestGroupInfoReq()
        onlineUserCount.value = liveRoom.onlineUserVM.allCount
        loadMore(-1)
    }

    fun loadMore(groupId : Int = -1){
        liveRoom.onlineUserVM.loadMoreUser(groupId)
    }

    fun getUser(position : Int) : IUserModel?{
        return liveRoom.onlineUserVM.getUser(position)
    }

    fun getAssistantLabel(): String = liveRoom.customizeAssistantLabel

    fun getGroupId() = liveRoom.currentUser.group

    fun updateGroupInfo(item: LPGroupItem){
        liveRoom.onlineUserVM.loadMoreUser(item.id)
    }
}