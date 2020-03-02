package com.baijiayun.live.ui.toolbox.answersheet;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.livecore.models.LPAnswerModel;
import com.baijiayun.livecore.models.LPAnswerSheetOptionModel;

import java.util.ArrayList;
import java.util.List;

public class QuestionShowFragment extends BaseFragment implements QuestionShowContract.View{
    private Context context;
    private LinearLayout newLayout;
    private QuestionShowContract.Presenter presenter;

    @Override
    public int getLayoutId() {
        return R.layout.bjy_dialog_question_show;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
    }

    @Override
    public void setPresenter(QuestionShowContract.Presenter presenter) {
        setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void onShowAnswer(LPAnswerModel lpAnswerModel) {
        $.id(R.id.dialog_close).clicked(v -> presenter.removeQuestionShow());
        LinearLayout answerContainer = (LinearLayout) $.id(R.id.ll_show_container).view();
        List<LPAnswerSheetOptionModel> options = lpAnswerModel.options;
        List<String> myAnswers = new ArrayList<>();
        List<String> standardAnswers = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int size = options.size();
        for (int i = 0; i < size; i++) {
            LPAnswerSheetOptionModel optionModel = options.get(i);
            if (optionModel.isCorrect || optionModel.isRight) {
                standardAnswers.add(optionModel.text);
                sb.append(optionModel.text);
                sb.append(" ");
            }
            if (optionModel.isActive) {
                myAnswers.add(optionModel.text);
            }
        }
        showAnswerOption(answerContainer, myAnswers, standardAnswers);
        String text = context.getString(R.string.string_standard_answer, sb.toString());
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.black));
        int index = text.indexOf("：");
        if (index != -1) {
            builder.setSpan(whiteSpan, index + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        $.id(R.id.tv_standard_answer).text(builder);
    }
    private void showAnswerOption(LinearLayout optionsLayout, List<String> myAnswers, List<String> standardAnswers) {
        int size = myAnswers.size();
        $.id(R.id.tv_no_answer).visibility(size == 0 ? View.VISIBLE : View.GONE);
        optionsLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < size; i++) {
            String text = myAnswers.get(i);
            CheckedTextView buttonOption = new CheckedTextView(getContext());
            buttonOption.setTextColor(getResources().getColor(R.color.live_white));
            buttonOption.setBackgroundResource(R.drawable.sel_show_answer);
            buttonOption.setGravity(Gravity.CENTER);
            buttonOption.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            buttonOption.setText(text);
            buttonOption.setChecked(!standardAnswers.contains(text));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    DisplayUtils.dip2px(getContext(), 35), DisplayUtils.dip2px(getContext(), 35));
            if (i % 4 == 0) {
                newLayout = new LinearLayout(getContext());
                newLayout.setOrientation(LinearLayout.HORIZONTAL);
                newLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams newLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                newLayoutParams.setMargins(DisplayUtils.dip2px(getContext(), 8), DisplayUtils.dip2px(getContext(), 8), DisplayUtils.dip2px(getContext(), 8), DisplayUtils.dip2px(getContext(), 8));
                optionsLayout.setOrientation(LinearLayout.VERTICAL);
                optionsLayout.addView(newLayout, newLayoutParams);
            }
            layoutParams.setMargins(DisplayUtils.dip2px(getContext(), 8), 0, DisplayUtils.dip2px(getContext(), 8), 0);
            if (newLayout != null) {
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                newLayout.addView(buttonOption, layoutParams);
            }
        }
    }
}
