package com.baijiayun.live.ui.toolbox.questionanswer

import android.arch.lifecycle.MutableLiveData
import com.baijiayun.live.ui.base.BaseViewModel
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.widget.DragResizeFrameLayout
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.context.LPError
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.models.LPQuestionPubTriggerModel
import com.baijiayun.livecore.models.LPQuestionPullListItem
import com.baijiayun.livecore.models.LPQuestionPullResItem
import com.baijiayun.livecore.models.LPUserModel

/**
 * Created by yongjiaming on 2019-10-30
 * Describe:
 */
class QAViewModel(val routerViewModel: RouterViewModel) : BaseViewModel() {

    val toAnswerQuestionList = MutableLiveData<List<LPQuestionPullResItem>>()
    val toPublishQuestionList = MutableLiveData<List<LPQuestionPullResItem>>()
    val publishedQuestionList = MutableLiveData<List<LPQuestionPullResItem>>()

    val notifyDragStatusChange = MutableLiveData<DragResizeFrameLayout.Status>()

    //学生为已发布&未发布(自己发布的)；老师为全部状态的问答列表
    val allQuestionList = MutableLiveData<List<LPQuestionPullResItem>>()


    val toAnswerList = ArrayList<LPQuestionPullResItem>()
    val toPublishList = ArrayList<LPQuestionPullResItem>()
    val publishedList = ArrayList<LPQuestionPullResItem>()

    val notifyLoadComplete = MutableLiveData<Boolean>()

    var isSubscribe = false

    private val liveRoom by lazy {
        routerViewModel.liveRoom
    }

    override fun subscribe() {
        isSubscribe = true
        liveRoom.observableOfQuestionQueue
                .subscribe(object : DisposingObserver<List<LPQuestionPullResItem>>() {
                    override fun onNext(t: List<LPQuestionPullResItem>) {
                        if(liveRoom.currentUser.type == LPConstants.LPUserType.Student){
                            val questionList = ArrayList<LPQuestionPullResItem>()
                            for(item in t){
                                if ((item.status and QuestionStatus.QuestionPublished.status) > 0) {
                                    questionList.add(item)
                                }else if(item.itemList[0].from == liveRoom.currentUser){
                                    questionList.add(item)
                                }
                            }
                            allQuestionList.value = questionList
                            routerViewModel.hasNewQa.value = true
                        } else{
                            for (item in t) {
                                toAnswerList.remove(item)
                                publishedList.remove(item)
                                toPublishList.remove(item)
                                //8
                                if ((item.status and QuestionStatus.QuestionUnReplied.status) > 0) {
                                    toAnswerList.add(item)
                                }
                                //1
                                if ((item.status and QuestionStatus.QuestionPublished.status) > 0) {
                                    publishedList.add(item)
                                }
                                //2
                                if ((item.status and QuestionStatus.QuestionUnPublished.status) > 0) {
                                    toPublishList.add(item)
                                }
                            }
                            toAnswerQuestionList.value = ArrayList(toAnswerList)
                            toPublishQuestionList.value = ArrayList(toPublishList)
                            publishedQuestionList.value = ArrayList(publishedList)
                            allQuestionList.value = ArrayList(t)
                            routerViewModel.hasNewQa.value = true
                        }
                        notifyLoadComplete.value = true
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        notifyLoadComplete.value = true
                    }
                })

        liveRoom.loadMoreQuestions()
    }

    /**
     * 发布问题
     */
    fun publishQuestion(questionId: String, content: String) {
        val triggerModel = LPQuestionPubTriggerModel(questionId, QuestionStatus.QuestionReplied.status or QuestionStatus.QuestionUnReplied.status or QuestionStatus.QuestionPublished.status,
                content, liveRoom.currentUser)
        liveRoom.requestQuestionPub(triggerModel)
    }

    //回复问题
    fun publishAnswer(questionId: String, content: String) {
        val triggerModel = LPQuestionPubTriggerModel(questionId, QuestionStatus.QuestionReplied.status or QuestionStatus.QuestionPublished.status, content, liveRoom.currentUser)
        liveRoom.requestQuestionPub(triggerModel)
    }

    //取消发布
    fun unPublishQuestion(questionId: String, content: String = "") {
        val triggerModel = LPQuestionPubTriggerModel(questionId, QuestionStatus.QuestionReplied.status or QuestionStatus.QuestionUnReplied.status or QuestionStatus.QuestionUnPublished.status, liveRoom.currentUser)
        if (content.isNotBlank()) {
            triggerModel.content = content
        }
        liveRoom.requestQuestionPub(triggerModel)
    }

    //生成问题
    fun generateQuestion(content: String): LPError? {
        return liveRoom.sendQuestion(content)
    }

    /**
     * 临时保存问题到内存
     */
    fun saveQuestion(questionId: String, content: String, tabStatus: QADetailFragment.QATabStatus) {
        when (tabStatus) {
            QADetailFragment.QATabStatus.ToAnswer -> {
                addReplyItem(questionId, content, toAnswerList)
                toAnswerQuestionList.value = ArrayList(toAnswerList)
            }
            QADetailFragment.QATabStatus.ToPublish -> {
                addReplyItem(questionId, content, toPublishList)
                toPublishQuestionList.value = ArrayList(toPublishList)
            }
            QADetailFragment.QATabStatus.Published -> {
                addReplyItem(questionId, content, publishedList)
                publishedQuestionList.value = ArrayList(publishedList)
            }
            else -> {

            }
        }
    }


    private fun addReplyItem(questionId: String, content: String, list: ArrayList<LPQuestionPullResItem>) {
        for (item in list) {
            if (item.id == questionId) {
                val replayItem = LPQuestionPullListItem()
                replayItem.from = liveRoom.currentUser as LPUserModel
                replayItem.content = content
                item.itemList.add(replayItem)
                break
            }
        }
    }
}