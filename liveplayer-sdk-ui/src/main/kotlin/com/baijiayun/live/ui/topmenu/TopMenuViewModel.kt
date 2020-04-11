package com.baijiayun.live.ui.topmenu

import android.arch.lifecycle.MutableLiveData
import com.baijiayun.bjyrtcengine.BJYRtcEventObserver
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.utils.RxUtils
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.LPCheckRecordStatusModel
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Shubo on 2019-10-10.
 */
class TopMenuViewModel(val liveRoom: LiveRoom) : BaseViewModel() {
    val classStartTimeDesc = MutableLiveData<String>()
    val showToast = MutableLiveData<String>()
    var lastRecordStatus = false
    var recordStatus = MutableLiveData<Boolean>()
    var upLinkLossRate = MutableLiveData<Pair<String, Int>>()
    var downLinkLossRate = MutableLiveData<Pair<String, Int>>()
    private var disposableOfCount: Disposable? = null
    private val defaultStartTimeStr = "直播未开始"
    override fun subscribe() {
        liveRoom.observableOfRealStartTime.observeOn(AndroidSchedulers.mainThread())
                .filter { t -> t != 0L }
                .subscribe(object : DisposingObserver<Long>() {
                    override fun onNext(t: Long) {
                        startCount(t)
                    }
                })

        liveRoom.observableOfClassEnd.observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposingObserver<Int>() {
                    override fun onNext(t: Int) {
                        RxUtils.dispose(disposableOfCount)
                        if (liveRoom.currentUser.type == LPConstants.LPUserType.Teacher) {
                            liveRoom.requestCloudRecord(false)
                        }
                        classStartTimeDesc.value = defaultStartTimeStr
                    }
                })
        classStartTimeDesc.value = defaultStartTimeStr
        liveRoom.observableOfCloudRecordStatus.observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposingObserver<Boolean>() {
                    override fun onNext(boolean: Boolean) {
                        lastRecordStatus = recordStatus.value == true
                        recordStatus.value = boolean
                    }
                })
        recordStatus.value = liveRoom.cloudRecordStatus
        lastRecordStatus = recordStatus.value == true
        if (liveRoom.getRecorder<LPRecorder>() != null) {
            liveRoom.getRecorder<LPRecorder>().observableOfUpPacketLossRate.toObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<BJYRtcEventObserver.LocalStreamStats>() {
                        override fun onNext(localStreamStats: BJYRtcEventObserver.LocalStreamStats) {
                            val isVideoOn = liveRoom.getRecorder<LPRecorder>().isVideoAttached
                            val lossRate = if (isVideoOn) localStreamStats.videoPacketsLostRateSent else localStreamStats.audioPacketsLostRateSent
                            upLinkLossRate.value = String.format(Locale.getDefault(), "%.2f", lossRate) + "%" to getNetworkQualityColor(lossRate)
                        }
                    })
        }
        if (liveRoom.player != null) {
            liveRoom.player.observableOfDownLinkLossRate
                    .buffer(1, TimeUnit.SECONDS)
                    .filter { it.size > 0 }
                    .toObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : DisposingObserver<List<BJYRtcEventObserver.RemoteStreamStats>>() {
                        override fun onNext(localStreamStats: List<BJYRtcEventObserver.RemoteStreamStats>) {
                            var sum = 0.0
                            for (streamStats in localStreamStats) {
                                sum += streamStats.receivedVideoLostRate
                            }
                            val lossRate = sum / localStreamStats.size
                            downLinkLossRate.value = String.format(Locale.getDefault(), "%.2f", lossRate) + "%" to getNetworkQualityColor(lossRate)
                        }
                    })
        }
    }

    private fun startCount(t: Long) {
        val hasPast = (System.currentTimeMillis() - t) / 1000
        disposableOfCount = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val past = hasPast + it
                    val date = "${String.format("%02d", past / 3600)}:${String.format("%02d", past / 60 % 60)}:${String.format("%02d", past % 60)}"
                    classStartTimeDesc.value = "直播中：$date"
                }
    }

    private fun getNetworkQuality(lossRate: Double): LPConstants.MediaNetworkQuality {
        val packetLossRateLevel = liveRoom.partnerConfig.packetLossRate.packetLossRateLevel
        if (packetLossRateLevel.isEmpty() || packetLossRateLevel.size < 3) {
            return LPConstants.MediaNetworkQuality.GOOD
        }
        return when {
            lossRate < packetLossRateLevel[0] -> LPConstants.MediaNetworkQuality.EXCELLENT
            lossRate < packetLossRateLevel[1] -> LPConstants.MediaNetworkQuality.GOOD
            lossRate < packetLossRateLevel[2] -> LPConstants.MediaNetworkQuality.BAD
            else -> LPConstants.MediaNetworkQuality.TERRIBLE
        }
    }

    private fun getNetworkQualityColor(lossRate: Double): Int {
        val packetLossRateLevel = liveRoom.partnerConfig.packetLossRate.packetLossRateLevel
        if (packetLossRateLevel.isEmpty() || packetLossRateLevel.size < 3) {
            return R.color.pad_class_net_normal
        }
        return when {
            lossRate < packetLossRateLevel[0] -> R.color.pad_class_net_good
            lossRate < packetLossRateLevel[1] -> R.color.pad_class_net_normal
            lossRate < packetLossRateLevel[2] -> R.color.pad_class_net_bad
            else -> R.color.pad_class_net_terrible
        }
    }

    private fun canOperateCloudRecord(): Boolean {
        return !(liveRoom.currentUser.type == LPConstants.LPUserType.Assistant &&
                liveRoom.adminAuth != null && !liveRoom.adminAuth.cloudRecord)
    }

    fun switchCloudRecord() {
        if (!canOperateCloudRecord()) {
            showToast.value = "云端录制权限已被禁用"
            return
        }
        if (recordStatus.value != true) {
            liveRoom.requestIsCloudRecordAllowed()
                    .subscribe(object : DisposingObserver<LPCheckRecordStatusModel>() {
                        override fun onNext(lpCheckRecordStatusModel: LPCheckRecordStatusModel) {
                            if (lpCheckRecordStatusModel.recordStatus == 1) {
                                liveRoom.requestCloudRecord(true)
                            } else {
                                showToast.value = lpCheckRecordStatusModel.reason
                            }
                        }
                    })
        } else {
            liveRoom.requestCloudRecord(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        RxUtils.dispose(disposableOfCount)
    }
}