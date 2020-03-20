package com.baijiayun.live.ui.toolbox.questionanswer

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.base.BaseDialogFragment
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.widget.GravityCompoundDrawable


/**
 * Created by yongjiaming on 2019-11-03
 * Describe:
 */

const val QUESTION_ID = "questionId"
const val QUOTE_CONTENT = "quoteContent"
const val GENERATE_QUESTION = "generateQuestion"
const val QUESTION_TAB_STATUS = "tabStatus"

class QuestionSendFragment : BaseDialogFragment() {

    private lateinit var questionEditTextView: EditText
    private lateinit var sendBtn: ImageView
    private lateinit var wordsTipTv: TextView
    private lateinit var saveBtn : TextView
    private lateinit var cancelBtn : TextView
    private lateinit var quoteTextView: TextView

    private lateinit var qaViewModel: QAViewModel
    private lateinit var questionId: String
    private lateinit var quoteContent: String
    private lateinit var tabStatus: QADetailFragment.QATabStatus

    override fun init(savedInstanceState: Bundle?, arguments: Bundle?) {
        super.hideTitleBar()
        questionEditTextView = contentView.findViewById(R.id.qa_send_et)
        sendBtn = contentView.findViewById(R.id.qa_send_btn)
        wordsTipTv = contentView.findViewById(R.id.qa_input_text)
        quoteTextView = contentView.findViewById(R.id.qa_copy_text)
        saveBtn = contentView.findViewById(R.id.qa_save_btn)
        cancelBtn = contentView.findViewById(R.id.qa_cancel_btn)

        questionId = getArguments()?.getString(QUESTION_ID) ?: ""
        quoteContent = getArguments()?.getString(QUOTE_CONTENT) ?: ""
        tabStatus = getArguments()?.getSerializable(QUESTION_TAB_STATUS) as QADetailFragment.QATabStatus

        questionEditTextView.postDelayed({
            activity?.run {
                val innerDrawable = this.resources.getDrawable(R.drawable.ic_pad_qa_hint_pencil)
                val gravityCompoundDrawable = GravityCompoundDrawable(innerDrawable)
                innerDrawable.setBounds(0, 0, innerDrawable.intrinsicWidth, innerDrawable.intrinsicHeight)
                gravityCompoundDrawable.setBounds(0, 0, innerDrawable.intrinsicWidth, innerDrawable.intrinsicHeight)
                questionEditTextView.setCompoundDrawables(gravityCompoundDrawable, null, null, null)
                showSoftInputWindow()
            }
        }, 100)

        activity?.run {
            val routerViewModel = getViewModel { RouterViewModel() }
            qaViewModel = getViewModel { QAViewModel(routerViewModel.liveRoom) }
        }

        if(quoteContent.isNotBlank()){
            quoteTextView.visibility = View.VISIBLE
            quoteTextView.text = quoteContent
            questionEditTextView.hint = context?.resources?.getString(R.string.qa_input_answer)
        } else{
            quoteTextView.visibility = View.GONE
        }
        initListener()
    }

    override fun getLayoutId() = R.layout.fragment_pad_question_send

    override fun setWindowParams(windowParams: WindowManager.LayoutParams) {
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.gravity = Gravity.BOTTOM or GravityCompat.END
        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim
    }

    private fun initListener(){
        questionEditTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendBtn.isEnabled = !s.isNullOrBlank()
                wordsTipTv.text = "${s?.length ?: 0}/280"
            }
        })

        sendBtn.setOnClickListener {
            hideSoftInputWindow()
            if (!questionId.isBlank()) {
                if (questionId == GENERATE_QUESTION) {
                    qaViewModel.generateQuestion(questionEditTextView.text.toString())
                } else {
                    qaViewModel.publishAnswer(questionId, questionEditTextView.text.toString())
                }
            }
            questionEditTextView.setText("")
            dismissAllowingStateLoss()
        }

        cancelBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }

        saveBtn.setOnClickListener {
            qaViewModel.saveQuestion(questionId, questionEditTextView.text.toString(), tabStatus)
            dismissAllowingStateLoss()
        }
    }

    private fun showSoftInputWindow() {
        activity?.run {
            val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(questionEditTextView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideSoftInputWindow() {
        activity?.run {
            val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (imm.isActive) {//isOpen若返回true，则表示输入法打开
                imm.hideSoftInputFromWindow(questionEditTextView.windowToken, 0)
            }
        }
    }

    companion object {
        fun newInstance(questionId: String = "", quoteContent: String = "", tabStatus: QADetailFragment.QATabStatus): BaseDialogFragment {
            val fragment = QuestionSendFragment()
            val bundle = Bundle()
            bundle.putString(QUESTION_ID, questionId)
            bundle.putString(QUOTE_CONTENT, quoteContent)
            bundle.putSerializable(QUESTION_TAB_STATUS, tabStatus)
            fragment.arguments = bundle
            return fragment
        }
    }

}