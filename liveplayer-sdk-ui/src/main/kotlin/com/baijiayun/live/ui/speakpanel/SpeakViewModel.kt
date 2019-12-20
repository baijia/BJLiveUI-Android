package com.baijiayun.live.ui.speakpanel

import android.arch.lifecycle.MutableLiveData
import android.text.TextUtils
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPInteractionAwardModel
import com.baijiayun.livecore.models.LPMediaModel
import com.baijiayun.livecore.models.LPUserModel
import com.baijiayun.livecore.models.imodels.IMediaModel
import com.baijiayun.livecore.models.imodels.IUserModel
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by yongjiaming on 2019-10-17
 * Describe:
 */
class SpeakViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {
    val notifyPresenterDesktopShareAndMedia = MutableLiveData<Boolean>()
    val notifyPresenterChange = MutableLiveData<Pair<String, IMediaModel>>()

    override fun subscribe() {
        with(routerViewModel) {
            liveRoom.speakQueueVM.observableOfActiveUsers
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<List<IMediaModel>>() {
                        override fun onNext(list: List<IMediaModel>) {
                            for (mediaModel in list) {
                                notifyRemotePlayableChanged.value = mediaModel
                                if (!mediaModel.hasExtraStreams()) {
                                    continue
                                }
                                for (extMediaModel in mediaModel.extraStreams) {
                                    if (extMediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtCamera || extMediaModel.mediaSourceType == LPConstants.MediaSourceType.ExtScreenShare) {
                                        notifyRemotePlayableChanged.value = extMediaModel
                                    }
                                }
                            }
                        }
                    })

            liveRoom.speakQueueVM.observableOfMediaPublish
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<IMediaModel>(){
                        override fun onNext(t: IMediaModel) {
                            notifyRemotePlayableChanged.value = t
                        }
                    })

            // 老师播放媒体文件和屏幕分享自动全屏
            liveRoom.observableOfPlayMedia.mergeWith(liveRoom.observableOfShareDesktop)
                    .filter { liveRoom.currentUser != null && liveRoom.currentUser.type != LPConstants.LPUserType.Teacher }
                    .filter { aBoolean -> aBoolean && liveRoom.presenterUser != null && liveRoom.teacherUser != null && TextUtils.equals(liveRoom.presenterUser.userId, liveRoom.teacherUser.userId) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<Boolean>() {
                        override fun onNext(t: Boolean) {
                            notifyPresenterDesktopShareAndMedia.value = t
                        }
                    })

            liveRoom.speakQueueVM.observableOfPresenterChange.toObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<String>() {
                        override fun onNext(s: String) {
                            var defaultMediaModel: IMediaModel? = null
                            for (mediaModel in liveRoom.speakQueueVM.speakQueueList) {
                                if (mediaModel.user.userId == s) {
                                    defaultMediaModel = mediaModel
                                    break
                                }
                            }
                            if (defaultMediaModel == null) {
                                defaultMediaModel = LPMediaModel()
                            }
                            if (defaultMediaModel.user == null) {
                                var userModel: IUserModel? = liveRoom.onlineUserVM.getUserById(s)
                                if (userModel == null) {
                                    val fakeUser = LPUserModel()
                                    fakeUser.userId = s
                                    userModel = fakeUser
                                }
                                (defaultMediaModel as LPMediaModel).user = userModel as LPUserModel?
                            }
                            notifyPresenterChange.value = s to defaultMediaModel
                        }
                    })
            liveRoom.observableOfAward.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<LPInteractionAwardModel>() {
                        override fun onNext(awardModel: LPInteractionAwardModel) {
                            awardRecord.putAll(awardModel.value.record)
                            notifyAward.value = awardModel
                        }
                    })
            liveRoom.speakQueueVM.requestActiveUsers()
        }
    }
}