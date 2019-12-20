package com.baijiayun.live.ui.chat

import android.arch.lifecycle.MutableLiveData
import com.baijiahulian.common.networkv2.BJProgressCallback
import com.baijiahulian.common.networkv2.BJResponse
import com.baijiahulian.common.networkv2.HttpException
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.LPShortResult
import com.baijiayun.livecore.models.LPUploadDocModel
import com.baijiayun.livecore.models.imodels.IMessageModel
import com.baijiayun.livecore.utils.LPChatMessageParser
import com.baijiayun.livecore.utils.LPJsonUtils
import com.baijiayun.livecore.utils.LPLogger
import com.google.gson.JsonObject
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by yongjiaming on 2019-10-28
 * Describe:
 */
class ChatViewModel(val liveRoom: LiveRoom) : BaseViewModel() {

    var receivedNewMessageNumber : MutableLiveData<Int> = MutableLiveData()
    var redPointNumber = MutableLiveData<Int>()
    val notifyItemChange = MutableLiveData<Int>()
    val notifyItemInsert = MutableLiveData<Int>()
    val notifyDataSetChange = MutableLiveData<Unit>()
    val pressions = HashMap<String, String>()

    private val imageMessageUploadingQueue : LinkedBlockingQueue<UploadingImageModel> by lazy {
        LinkedBlockingQueue<UploadingImageModel>()
    }

    override fun subscribe() {
        for (lpExpressionModel in liveRoom.chatVM.expressions) {
            pressions.put("[" + lpExpressionModel.name + "]", lpExpressionModel.url)
        }
        compositeDisposable.add(liveRoom.chatVM
                .observableOfNotifyDataChange
                .onBackpressureBuffer(1000)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    notifyDataSetChange.value = Unit
                })

        compositeDisposable.add(liveRoom.chatVM
                .observableOfReceiveMessage
                .onBackpressureBuffer()
                .doOnNext {
                    if (it.from.userId != liveRoom.currentUser.userId) {
                        val count = receivedNewMessageNumber.value
                        receivedNewMessageNumber.value = if(count == null) 1 else count + 1
                        val redPointCount = redPointNumber.value
                        redPointNumber.value = if(redPointCount == null) 1 else redPointCount + 1
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if(it.messageType == LPConstants.MessageType.Image && it.from.userId == liveRoom.currentUser.userId){
                        notifyItemChange.value = getCount() - imageMessageUploadingQueue.size - 1
                    }
                    notifyItemInsert.value = getCount() - 1
                })
    }

    fun getCount() : Int{
       return liveRoom.chatVM.messageCount + imageMessageUploadingQueue.size
    }

    fun getMessage(position : Int) : IMessageModel{
        return if(position < liveRoom.chatVM.messageCount){
            liveRoom.chatVM.getMessage(position)
        } else{
            imageMessageUploadingQueue.toArray()[position - liveRoom.chatVM.messageCount] as IMessageModel
        }
    }

    fun sendImageMessage(path : String){
        val model = UploadingImageModel(path, liveRoom.currentUser, null)
        imageMessageUploadingQueue.offer(model)
        continueUploadQueue()
    }

    fun continueUploadQueue(){
        val model = imageMessageUploadingQueue.peek() ?: return
        notifyItemInsert.value = liveRoom.chatVM.messageCount + imageMessageUploadingQueue.size - 1
        liveRoom.docListVM.uploadImageWithProgress(model.url, this, object : BJProgressCallback() {
            override fun onProgress(l: Long, l1: Long) {
                LPLogger.d("$l/$l1")
            }

            override fun onFailure(e: HttpException) {
                model.status = UploadingImageModel.STATUS_UPLOAD_FAILED
                notifyDataSetChange.value = Unit
            }

            override fun onResponse(bjResponse: BJResponse) {
                val shortResult: LPShortResult<*>
                try {
                    shortResult = LPJsonUtils.parseString(bjResponse.response.body()!!.string(), LPShortResult::class.java)
                    val uploadModel = LPJsonUtils.parseJsonObject(shortResult.data as JsonObject, LPUploadDocModel::class.java)
                    val imageContent = LPChatMessageParser.toImageMessage(uploadModel.url)
                    liveRoom.chatVM.sendImageMessageToUser(model.getToUser(), imageContent, uploadModel.width, uploadModel.height)
                    imageMessageUploadingQueue.poll()
                    continueUploadQueue()
                } catch (e: Exception) {
                    model.status = UploadingImageModel.STATUS_UPLOAD_FAILED
                    notifyDataSetChange.value = Unit
                    e.printStackTrace()
                }

            }
        })
    }

}