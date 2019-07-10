package com.baijiayun.live.ui.answersheet;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.models.LPAnswerSheetOptionModel;

/**
 * Created by yangjingming on 2018/6/5.
 */

public class QuestionToolFragment extends BaseFragment implements QuestionToolContract.View {

    private QueryPlus $;
    private QuestionToolContract.Presenter presenter;
    private LinearLayout newLayout;
    private StringBuilder rightAnswer = new StringBuilder("");
    private boolean isSubmit = false;

    @Override
    public int getLayoutId() {
        return R.layout.dialog_question_tool;
    }

    @Override
    protected void init(Bundle savedInstanceState) {

        $ = QueryPlus.with(view);
        int index = 0;


        ($.id(R.id.dialog_cross)).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.removeQuestionTool(false);
            }
        });

        ($.id(R.id.dialog_btn_submit)).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("确定".equals(((TextView) ($.id(R.id.dialog_btn_submit)).view()).getText().toString())) {
                    presenter.removeQuestionTool(false);
                    return;
                }
                if (presenter.submitAnswers()) {
                    Toast.makeText(getContext(), "提交成功！", Toast.LENGTH_SHORT).show();
                    isSubmit = true;
                    ((TextView) ($.id(R.id.dialog_question_tool_answer_text)).view()).setText(rightAnswer.toString());
                    ($.id(R.id.dialog_question_tool_answer)).visibility(View.VISIBLE);
                    ((Button) ($.id(R.id.dialog_btn_submit)).view()).setText("确定");
//                    dismissAllowingStateLoss();
                } else {
                    Toast.makeText(getContext(), "请选择选项", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout optionsLayout = (LinearLayout) ($.id(R.id.dialog_question_tool_options).view());
        if (presenter != null && presenter.getOptions() != null && !presenter.getOptions().isEmpty()) {
            presenter.subscribe();
            for (final LPAnswerSheetOptionModel model : presenter.getOptions()) {
                index++;
                final TextView buttonOption = new TextView(getContext());
                if (model.isCorrect) {
                    rightAnswer.append(model.text);
                    rightAnswer.append(" ");
                }
                buttonOption.setText(model.text);
                buttonOption.setTextColor(getResources().getColor(R.color.live_blue));
                buttonOption.setBackgroundResource(R.drawable.live_question_tool_roundoption_unchecked);
                buttonOption.setGravity(Gravity.CENTER);
                buttonOption.setTag(true);
                final int currentIndex = index;
                buttonOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isSubmit) return;
                        if ((Boolean) buttonOption.getTag()) {
                            buttonOption.setBackgroundResource(R.drawable.live_question_tool_roundoption_checked);
                            buttonOption.setTextColor(getResources().getColor(R.color.live_white));
                            buttonOption.setTag(false);
                            presenter.addCheckedOption(currentIndex);
                        } else {
                            buttonOption.setTextColor(getResources().getColor(R.color.live_blue));
                            buttonOption.setBackgroundResource(R.drawable.live_question_tool_roundoption_unchecked);
                            buttonOption.setTag(true);
                            presenter.deleteCheckedOption(currentIndex);
                        }
                    }
                });
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        DisplayUtils.dip2px(getContext(), 35), DisplayUtils.dip2px(getContext(), 35));
                if (index % 4 == 1) {
                    newLayout = new LinearLayout(getContext());
                    newLayout.setOrientation(LinearLayout.HORIZONTAL);
                    newLayout.setGravity(Gravity.LEFT);
                    LinearLayout.LayoutParams newLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    newLayoutParams.setMargins(DisplayUtils.dip2px(getContext(), 21), DisplayUtils.dip2px(getContext(), 8), 0, 0);
                    newLayout.addView(buttonOption, layoutParams);
                    optionsLayout.setOrientation(LinearLayout.VERTICAL);
                    optionsLayout.addView(newLayout, newLayoutParams);
                } else {
                    layoutParams.setMargins(DisplayUtils.dip2px(getContext(), 14), 0, 0, 0);
                    if (newLayout != null)
                        newLayout.addView(buttonOption, layoutParams);
                }
            }
        }
    }

    @Override
    public void setPresenter(QuestionToolContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void timeDown(String down) {
        ((TextView) $.id(R.id.dialog_question_tool_countDown).view()).setText(down);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        presenter = null;
    }
}
