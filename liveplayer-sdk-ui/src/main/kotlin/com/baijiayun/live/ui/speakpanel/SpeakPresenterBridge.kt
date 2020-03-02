package com.baijiayun.live.ui.speakpanel

import com.baijiayun.live.ui.activity.LiveRoomRouterListener
import com.baijiayun.live.ui.base.OldLiveRoomRouterListenerBridge
import com.baijiayun.live.ui.speakerlist.SpeakersContract
import com.baijiayun.live.ui.speakerlist.item.RemoteItem
import com.baijiayun.livecore.models.imodels.IUserModel

/**
 * Created by yongjiaming on 2019-10-18
 * Describe:
 */
class SpeakPresenterBridge(private val roomRouterListener: LiveRoomRouterListener) : SpeakersContract.Presenter{
    override fun localShowAwardAnimation(userNumber: String?) {
    }

    override fun setRouter(liveRoomRouterListener: LiveRoomRouterListener) {

    }

    override fun getRouterListener(): LiveRoomRouterListener {
        return roomRouterListener
    }

    override fun destroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun agreeSpeakApply(userId: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun disagreeSpeakApply(userId: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun requestAward(number: IUserModel?) {
        (routerListener as OldLiveRoomRouterListenerBridge).requestAward(number)
    }

    override fun getAwardCount(number: String?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleUserCloseAction(remoteItem: RemoteItem?) {
        (routerListener as OldLiveRoomRouterListenerBridge).notifyCloseRemoteVideo(remoteItem)
    }

    override fun closeSpeaking(userId: String?) {

    }

    override fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unSubscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}