package com.baijiayun.live.ui.toolbox.questionanswer

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import com.baijiayun.live.ui.DatabindingUtils
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseDialogFragment
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager
import com.baijiayun.live.ui.widget.DragResizeFrameLayout
import com.baijiayun.livecore.models.LPQuestionPullListItem
import com.baijiayun.livecore.models.LPQuestionPullResItem
import com.baijiayun.livecore.utils.CommonUtils
import kotlin.math.roundToInt


/**
 * Created by yongjiaming on 2019-10-30
 * Describe: 单种问答状态展示页面
 */
enum class QuestionStatus(val status: Int) {
    QuestionPublished(1 shl 0),     //1  已发布
    QuestionUnPublished(1 shl 1),   //2  未发布
    QuestionReplied(1 shl 2),       //4  已回复
    QuestionUnReplied(1 shl 3),     //8  未回复
    QuesitonAllState((1 shl 4) - 1) //15 全部
}

class QADetailFragment : BasePadFragment() {

    private var tabStatus: QATabStatus = QATabStatus.Published
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QAAdapter
    private lateinit var questionList: MutableLiveData<List<LPQuestionPullResItem>>
    private lateinit var qaViewModel: QAViewModel
    private lateinit var emptyView : ImageView

    private var isLoading = false
    private lateinit var questionSendFragment: BaseDialogFragment

    private val clipboardManager: ClipboardManager? by lazy {
        activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabStatus = arguments?.get("status") as QATabStatus
    }

    override fun init(view: View) {
        activity?.run {
            qaViewModel = getViewModel { QAViewModel(routerViewModel.liveRoom) }
        }
        questionList = when (tabStatus) {
            QATabStatus.ToAnswer -> {
                qaViewModel.toAnswerQuestionList
            }
            QATabStatus.ToPublish -> {
                qaViewModel.toPublishQuestionList
            }
            QATabStatus.Published -> {
                qaViewModel.publishedQuestionList
            }
            QATabStatus.AllStatus -> {
                qaViewModel.allQuestionList
            }
        }
        recyclerView = view.findViewById(R.id.qa_recyclerview)
        recyclerView.layoutManager = LinearLayoutWrapManager(context)
        adapter = QAAdapter()
        recyclerView.adapter = adapter

        emptyView = view.findViewById(R.id.qa_empty_iv)
    }

    override fun observeActions() {

        routerViewModel.actionNavigateToMain.observe(this, Observer {
            if(!qaViewModel.isSubscribe){
                qaViewModel.subscribe()
            }

            questionList.observe(this, Observer {
                if(it?.isEmpty() == true){
                    emptyView.visibility = View.VISIBLE
                } else{
                    emptyView.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                }
            })

            qaViewModel.notifyLoadComplete.observe(this, Observer {
                isLoading = it != true
                adapter.notifyDataSetChanged()

            })
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val visibleItemCount = layoutManager.childCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    if (!isLoading && newState == SCROLL_STATE_IDLE && visibleItemCount > 0 && lastVisibleItemPosition > totalItemCount - 2) {
                        val lpError = routerViewModel.liveRoom.loadMoreQuestions()
                        isLoading = lpError == null
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        })
    }

    override fun getLayoutId() = R.layout.fragment_qa_detail

    inner class QAAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_QUESTION = 1
        private val VIEW_TYPE_LOADING = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_LOADING) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.bjy_item_online_user_loadmore, parent, false)
                LoadingViewHolder(view)
            } else {
                val view = LayoutInflater.from(context).inflate(R.layout.item_pad_qa, parent, false)
                QAItemViewHolder(view)
            }
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            if (viewHolder is QAItemViewHolder) {
                questionList.value?.let {
                    val questionPullResItem: LPQuestionPullResItem = it[position]
                    if (!questionPullResItem.itemList.isNullOrEmpty()) {
                        viewHolder.nameTv.text = CommonUtils.getEncodePhoneNumber(questionPullResItem.itemList[0].from.name)
                        viewHolder.timeTv.text = DatabindingUtils.formatTime(questionPullResItem.itemList[0].time * 1000)
                        viewHolder.qaContainer.removeAllViews()
                        for (i in questionPullResItem.itemList.indices) {
                            val item = questionPullResItem.itemList[i]
                            if (i == 0) {
                                buildQuestionView(questionPullResItem.id, questionPullResItem.status, viewHolder.qaContainer, item)
                            } else {
                                buildAnswerView(questionPullResItem.id, viewHolder.qaContainer, item, questionPullResItem.status)
                            }
                        }
                    }
                }
            } else {
                (viewHolder as LoadingViewHolder).progressBar.isIndeterminate = true
            }
        }

        override fun getItemCount(): Int {
            return if (isLoading) {
                (questionList.value?.size ?: 0) + 1
            } else {
                questionList.value?.size ?: 0
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < questionList.value?.size ?: 0) VIEW_TYPE_QUESTION else VIEW_TYPE_LOADING
        }
    }

    private fun buildQuestionView(questionId: String, status: Int, container: ViewGroup, item: LPQuestionPullListItem) {
        val ctx = container.context
        val spannableString = SpannableString("image${item.content}")
        spannableString.setSpan(ImageSpan(ctx, R.drawable.ic_pad_qa_question), 0, 5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        val textView = TextView(ctx)
        textView.text = spannableString
        textView.setTextColor(ContextCompat.getColor(ctx,R.color.live_pad_title))
        container.addView(textView)
        if(routerViewModel.liveRoom.isTeacherOrAssistant || routerViewModel.liveRoom.isGroupTeacherOrAssistant){
            textView.setOnClickListener {
                showPopupWindow(textView, container.width, questionId, status, item)
            }
        }
    }

    private fun buildAnswerView(questionId: String, container: ViewGroup, item: LPQuestionPullListItem, status: Int) {
        val ctx = container.context
        val spannableString = SpannableString("image${item.content}")
        val drawableId = if ((status and QuestionStatus.QuestionPublished.status) > 0) R.drawable.ic_pad_qa_answer else R.drawable.ic_pad_qa_unpublish_answer
        spannableString.setSpan(ImageSpan(ctx, drawableId), 0, 5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        val textView = TextView(ctx)
        textView.text = spannableString
        textView.setTextColor(ContextCompat.getColor(ctx,R.color.live_pad_title))
        container.addView(textView)
        if(routerViewModel.liveRoom.isTeacherOrAssistant || routerViewModel.liveRoom.isGroupTeacherOrAssistant){
            textView.setOnClickListener {
                showPopupWindow(textView, container.width, questionId, status, item)
            }
        }
    }

    private fun buildPopupWindowTipArray(status: Int, ctx: Context): Array<String> {
        val apply: String = if ((status and QuestionStatus.QuestionUnReplied.status) > 0) ctx.resources.getString(R.string.qa_replay) else ctx.resources.getString(R.string.qa_append_apply)
        val publish: String = if ((status and QuestionStatus.QuestionPublished.status) > 0) ctx.resources.getString(R.string.qa_cancel_publish) else ctx.resources.getString(R.string.qa_publish)
        val copy: String = ctx.resources.getString(R.string.qa_copy_question)
        return arrayOf(apply, publish, copy)
    }

    private fun showPopupWindow(anchorView : View, containerWidth : Int, questionId: String, status: Int, item: LPQuestionPullListItem){
        val popupWindow = ListPopupWindow(anchorView.context)
        with(popupWindow) {
            this.anchorView = anchorView
            height = ListPopupWindow.WRAP_CONTENT
            width = anchorView.context.resources.getDimension(R.dimen.qa_item_popup_window_width).roundToInt()
            setDropDownGravity(Gravity.START)
            horizontalOffset = (containerWidth - width) / 2
            setAdapter(ArrayAdapter(anchorView.context, R.layout.item_pad_qa_menu, buildPopupWindowTipArray(status, anchorView.context)))
            setOnItemClickListener { _, _, position, _ ->
                val quoteContent = if((status and QuestionStatus.QuestionReplied.status) > 0){
                    "${CommonUtils.getEncodePhoneNumber(item.from.user.name)} : ${item.content}"
                } else {
                    ""
                }
                questionSendFragment = QuestionSendFragment.newInstance(questionId, quoteContent, tabStatus)
                if (questionSendFragment.isAdded) {
                    return@setOnItemClickListener
                }
                when (position) {
                    //回复or追加回复
                    0 -> {
                        showDialogFragment(questionSendFragment)
                    }
                    //发布or取消发布
                    1 -> {
                        if ((status and QuestionStatus.QuestionPublished.status) > 0) {
                            qaViewModel.unPublishQuestion(questionId, item.content)
                        } else {
                            qaViewModel.publishQuestion(questionId, item.content)
                        }
                    }
                    //复制
                    2 -> {
                        copyQuestion(item)
                    }
                }
                dismiss()
            }
            show()
        }
    }


    class LoadingViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.item_online_user_progress)
    }

    class QAItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTv: TextView = itemView.findViewById(R.id.qa_ask_from_name)
        val timeTv: TextView = itemView.findViewById(R.id.qa_ask_time)
        val qaContainer: LinearLayout = itemView.findViewById(R.id.qa_content_container)
    }

    private fun copyQuestion(item: LPQuestionPullListItem) {
        val content = "${CommonUtils.getEncodePhoneNumber(item.from.user.name)} 提问: ${item.content}"
        val clipData = ClipData.newPlainText("Label", content)
        clipboardManager?.primaryClip = clipData
    }

    companion object {
        fun newInstance(tabStatus: QATabStatus): QADetailFragment {
            val fragment = QADetailFragment()
            val bundle = Bundle()
            bundle.putSerializable("status", tabStatus)
            fragment.arguments = bundle
            return fragment
        }
    }

    enum class QATabStatus {
        ToAnswer, ToPublish, Published, AllStatus
    }
}