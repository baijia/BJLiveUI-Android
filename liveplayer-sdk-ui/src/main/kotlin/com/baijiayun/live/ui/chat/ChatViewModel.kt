package com.baijiayun.live.ui.chat

import android.arch.lifecycle.MutableLiveData
import com.baijiahulian.common.networkv2.BJProgressCallback
import com.baijiahulian.common.networkv2.BJResponse
import com.baijiahulian.common.networkv2.HttpException
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.chat.widget.ChatMessageView
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
class ChatViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {

    var receivedNewMessageNumber : MutableLiveData<Int> = MutableLiveData()
    var redPointNumber = MutableLiveData<Int>()
    val notifyItemChange = MutableLiveData<Int>()
    val notifyItemInsert = MutableLiveData<Int>()
    val hasMyNewMessage = MutableLiveData<Boolean>()
    val notifyDataSetChange = MutableLiveData<Unit>()
    //表情 [name] - url
    val expressions = HashMap<String, String>()
    //表情 [key] - [name]
    val expressionNames = HashMap<String,String>()
    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }

    private val imageMessageUploadingQueue : LinkedBlockingQueue<UploadingImageModel> by lazy {
        LinkedBlockingQueue<UploadingImageModel>()
    }

    override fun subscribe() {
        for (lpExpressionModel in liveRoom.chatVM.expressions) {
            expressions["[" + lpExpressionModel.name + "]"] = lpExpressionModel.url
            expressionNames["[" + lpExpressionModel.key + "]"] = "[" + lpExpressionModel.name + "]"
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
                        receivedNewMessageNumber.value = if (count == null) 1 else count + 1
                        val redPointCount = redPointNumber.value
                        if (routerViewModel.action2Chat.value == true) {
                            redPointNumber.value = 0
                        } else {
                            redPointNumber.value = if (redPointCount == null) 1 else redPointCount + 1
                        }
                    } else {
                        //自己发送的消息滑动到底部
                        hasMyNewMessage.value = true
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
        notifyDataSetChange.value = Unit
        continueUploadQueue()
    }

    fun continueUploadQueue(){
        val model = imageMessageUploadingQueue.peek() ?: return
        liveRoom.chatVM.uploadImageWithProgress(model.url, this, object : BJProgressCallback() {
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
                    notifyDataSetChange.value = Unit
                    continueUploadQueue()
                } catch (e: Exception) {
                    model.status = UploadingImageModel.STATUS_UPLOAD_FAILED
                    e.printStackTrace()
                }

            }
        })
    }

    fun getRecallStatus(message: IMessageModel): Int {
        if (liveRoom.currentUser.number == message.from.number) {
            return ChatMessageView.RECALL
        }
        return if (liveRoom.currentUser.type == LPConstants.LPUserType.Assistant || liveRoom.currentUser.type == LPConstants.LPUserType.Teacher) {
            ChatMessageView.DELETE
        } else ChatMessageView.NONE
    }

     fun reCallMessage(message: IMessageModel) {
        liveRoom.chatVM.requestMsgRevoke(message.id, message.from.userId)
    }
}