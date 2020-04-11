package com.baijiayun.live.ui.chat

import android.arch.lifecycle.MutableLiveData
import com.baijiahulian.common.networkv2.BJProgressCallback
import com.baijiahulian.common.networkv2.BJResponse
import com.baijiahulian.common.networkv2.HttpException
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.chat.widget.ChatMessageView
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPMessageTranslateModel
import com.baijiayun.livecore.models.LPShortResult
import com.baijiayun.livecore.models.LPUploadDocModel
import com.baijiayun.livecore.models.imodels.IMessageModel
import com.baijiayun.livecore.utils.LPChatMessageParser
import com.baijiayun.livecore.utils.LPJsonUtils
import com.baijiayun.livecore.utils.LPLogger
import com.google.gson.JsonObject
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Predicate
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by yongjiaming on 2019-10-28
 * Describe:
 */
class ChatViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {

    var redPointNumber = MutableLiveData<Int>()
    val notifyItemChange = MutableLiveData<Int>()
    val notifyItemInsert = MutableLiveData<Int>()
    val notifyDataSetChange = MutableLiveData<Unit>()
    //表情 [name] - url
    val expressions = HashMap<String, String>()
    //表情 [key] - [name]
    val expressionNames = HashMap<String, String>()
    //不是自己发送的消息
    var receivedNewMsgNum = 0
    private var isSelfForbidden: Boolean = false
    //过滤只看老师
    var filterMessage = false
    var forbidPrivateChat = false


    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }

    private val imageMessageUploadingQueue by lazy {
        LinkedBlockingQueue<UploadingImageModel>()
    }

    private val translateMessageModels by lazy {
        ConcurrentHashMap<String, LPMessageTranslateModel>()
    }
    private val privateChatMessagePool by lazy {
        ConcurrentHashMap<String, ArrayList<IMessageModel>>()
    }
    private val privateChatMessageFilterList by lazy {
        ArrayList<IMessageModel>()
    }
    private val chatMessageFilterList by lazy {
        ArrayList<IMessageModel>()
    }

    override fun subscribe() {
        for (lpExpressionModel in liveRoom.chatVM.expressions) {
            expressions["[" + lpExpressionModel.name + "]"] = lpExpressionModel.url
            expressionNames["[" + lpExpressionModel.key + "]"] = "[" + lpExpressionModel.name + "]"
        }
        chatMessageFilterList.clear()
        chatMessageFilterList.addAll(getFilterMessageList(liveRoom.chatVM.messageList))
        notifyDataSetChange.value = Unit
        compositeDisposable.add(liveRoom.chatVM
                .observableOfNotifyDataChange
                .onBackpressureBuffer(1000)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    chatMessageFilterList.clear()
                    chatMessageFilterList.addAll(getFilterMessageList(it))
                    notifyDataSetChange.value = Unit
                })

        compositeDisposable.add(liveRoom.chatVM
                .observableOfReceiveMessage
                .onBackpressureBuffer()
                .doOnNext {
                    if (it.from.userId != liveRoom.currentUser.userId) {
                        receivedNewMsgNum = receivedNewMsgNum.plus(1)
                        val redPointCount = redPointNumber.value
                        if (routerViewModel.action2Chat.value == true) {
                            redPointNumber.value = 0
                        } else {
                            redPointNumber.value = if (redPointCount == null) 1 else redPointCount + 1
                        }
                    }
                    if (it.isPrivateChat && it.toUser != null) {
                        val userNumber = if (it.from.number == liveRoom.currentUser.number) {
                            it.toUser.number
                        } else {
                            it.from.number
                        }
                        var messageList: MutableList<IMessageModel>? = privateChatMessagePool[userNumber]
                        if (messageList == null) {
                            messageList = java.util.ArrayList()
                            privateChatMessagePool[userNumber] = messageList
                        }
                        messageList.add(it)
                        if (isPrivateChatMode()) {
                            privateChatMessageFilterList.clear()
                            privateChatMessageFilterList.addAll(getFilterMessageList(privateChatMessagePool[routerViewModel.privateChatUser.value!!.getNumber()]))
                        }
                    }
                }
                .filter {
                    val filter = when {
                        isPrivateChatMode() -> true
                        "-1" == it.to -> false
                        it.toUser == null -> false
                        else -> {
                            val privateChatUser = routerViewModel.privateChatUser.value
                            if (privateChatUser == null) true else privateChatUser.number == it.toUser.number || privateChatUser.number == it.from.number
                        }
                    }
                    filter
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.messageType == LPConstants.MessageType.Image && it.from.userId == liveRoom.currentUser.userId) {
                        notifyItemChange.value = getCount() - imageMessageUploadingQueue.size - 1
                    }
                    notifyItemInsert.value = getCount() - 1
                })
        compositeDisposable.add(liveRoom.chatVM
                .observableOfReceiveTranslateMessage
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    translateMessageModels[it.messageId] = it
                    notifyDataSetChange.value = Unit
                }
        )
        compositeDisposable.add(liveRoom.chatVM.observableOfMsgRevoke
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it.messageId != null }
                .subscribe { lpMessageRevoke ->
                    if (lpMessageRevoke.fromId != liveRoom.currentUser.userId) {
                        receivedNewMsgNum = receivedNewMsgNum.minus(1)
                    }
                    for (iMessageModel in imageMessageUploadingQueue) {
                        if (lpMessageRevoke.messageId == iMessageModel.id) {
                            imageMessageUploadingQueue.remove(iMessageModel)
                            break
                        }
                    }
                    for (iMessageModel in privateChatMessageFilterList) {
                        if (lpMessageRevoke.messageId == iMessageModel.id) {
                            privateChatMessageFilterList.remove(iMessageModel)
                            break
                        }
                    }
                    for (iMessageModel in chatMessageFilterList) {
                        if (lpMessageRevoke.messageId == iMessageModel.id) {
                            chatMessageFilterList.remove(iMessageModel)
                            break
                        }
                    }
                    if (isPrivateChatMode()) {
                        val iMessageModels = privateChatMessagePool[routerViewModel.privateChatUser.value!!.number]
                        if (iMessageModels != null) {
                            for (iMessageModel in iMessageModels) {
                                if (lpMessageRevoke.messageId == iMessageModel.id) {
                                    iMessageModels.remove(iMessageModel)
                                    break
                                }
                            }
                        }
                    }
                    notifyDataSetChange.value = Unit
                }
        )
        compositeDisposable.add(liveRoom.observableOfIsSelfChatForbid
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    isSelfForbidden = it
                })
    }

    fun isForbiddenByTeacher(): Boolean = if (liveRoom.isTeacherOrAssistant || liveRoom.isGroupTeacherOrAssistant) {
        false
    } else {
        isSelfForbidden
    }

    fun isPrivateChatMode() = routerViewModel.privateChatUser.value != null

    fun getCount(): Int {
        return if (isPrivateChatMode()) {
            var list = privateChatMessagePool[routerViewModel.privateChatUser.value!!.number]
            if (filterMessage) {
                list = privateChatMessageFilterList
            }
            if (list == null) 0 else list.size + imageMessageUploadingQueue.size
        } else {
            if (filterMessage) {
                chatMessageFilterList.size + imageMessageUploadingQueue.size
            } else liveRoom.chatVM.messageCount + imageMessageUploadingQueue.size
        }
    }

    fun getMessage(position: Int): IMessageModel {
        if (isPrivateChatMode()) {
            var list = privateChatMessagePool[routerViewModel.privateChatUser.value!!.getNumber()]
            if (filterMessage) {
                list = privateChatMessageFilterList
            }
            val messageCount = list?.size ?: 0
            return if (position < messageCount) {
                list!![position]
            } else {
                imageMessageUploadingQueue.toTypedArray()[position - messageCount]
            }
        } else {
            var messageCount = liveRoom.chatVM.messageCount
            if (filterMessage) {
                messageCount = chatMessageFilterList.size
            }
            return if (position < messageCount) {
                if (filterMessage) {
                    chatMessageFilterList[position]
                } else liveRoom.chatVM.getMessage(position)
            } else {
                imageMessageUploadingQueue.toTypedArray()[position - messageCount]
            }
        }
    }

    fun sendImageMessage(path: String) {
        val model = UploadingImageModel(path, liveRoom.currentUser, null)
        imageMessageUploadingQueue.offer(model)
        notifyDataSetChange.value = Unit
        continueUploadQueue()
    }

    fun continueUploadQueue() {
        val model = imageMessageUploadingQueue.peek() ?: return
        liveRoom.chatVM.uploadImageWithProgress(model.url, this, object : BJProgressCallback() {
            override fun onProgress(l: Long, l1: Long) {
                LPLogger.d("$l/$l1")
            }

            override fun onFailure(e: HttpException) {
                model.status = UploadingImageModel.STATUS_UPLOAD_FAILED
                notifyDataSetChange.postValue(Unit)
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
                    e.printStackTrace()
                }finally {
                    notifyDataSetChange.postValue(Unit)
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

    /**
     * 从所有消息过滤出老师/助教的消息
     *
     * @param allMessages 所有消息
     * @return
     */
    private fun getFilterMessageList(allMessages: List<IMessageModel>?): List<IMessageModel> {
        val messageModelList = ArrayList<IMessageModel>()
        if (allMessages == null) {
            return messageModelList
        }
        val size = allMessages.size
        for (i in 0 until size) {
            val iMessageModel = allMessages[i]
            if (iMessageModel.from.type == LPConstants.LPUserType.Teacher
                    || iMessageModel.from.type == LPConstants.LPUserType.Assistant) {
                messageModelList.add(iMessageModel)
            }
        }
        return messageModelList
    }

    fun getTranslateResult(position: Int): String {
        val iMessageModel = getMessage(position)
        val lpMessageTranslateModel = translateMessageModels[iMessageModel.from?.userId + iMessageModel.time?.time]
        return if (lpMessageTranslateModel != null) {
            if (lpMessageTranslateModel.code == 0) {
                lpMessageTranslateModel.result
            } else {
                if (Locale.getDefault().country.equals("cn", ignoreCase = true)) "翻译失败" else "Translate Fail!"
            }
        } else {
            ""
        }
    }

    fun translateMessage(message: String, messageId: String, fromLanguage: String, toLanguage: String) {
        routerViewModel.liveRoom.chatVM.sendTranslateMessage(message, messageId, routerViewModel.liveRoom.roomId.toString(), routerViewModel.liveRoom.currentUser.userId, fromLanguage, toLanguage)
    }

    override fun onCleared() {
        super.onCleared()
        translateMessageModels.clear()
        privateChatMessageFilterList.clear()
        privateChatMessagePool.clear()
        chatMessageFilterList.clear()
        imageMessageUploadingQueue.clear()
    }
}