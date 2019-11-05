package com.baijiayun.live.ui.toolbox.answersheet;

import android.os.Bundle;
import android.text.TextUtils;
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

import org.w3c.dom.Text;

/**
 * Created by yangjingming on 2018/6/5.
 */

public class QuestionToolFragment extends BaseFragment implements QuestionToolContract.View {

    private QueryPlus $;
    private QuestionToolContract.Presenter presenter;
    private LinearLayout newLayout;
    private boolean isSubmit = false;

    @Override
    public int getLayoutId() {
        return R.layout.dialog_question_tool;
    }

    @Override
    protected void init(Bundle savedInstanceState) {

        $ = QueryPlus.with(view);
        int index = 0;


        ($.id(R.id.dialog_close)).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.removeQuestionTool(false);
            }
        });

        $.id(R.id.dialog_question_tool_desc).visibility(TextUtils.isEmpty(presenter.getDesc()) ? View.GONE : View.VISIBLE);
        $.id(R.id.dialog_question_tool_desc).text(presenter.getDesc());

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
                        if (presenter.isJudgement()) {
                            int otherIndex = (currentIndex & 0x01) == 0 ? 1 : 2;
                            if ((Boolean) buttonOption.getTag()) {
                                buttonOption.setBackgroundResource(R.drawable.live_question_tool_roundoption_checked);
                                buttonOption.setTextColor(getResources().getColor(R.color.live_white));
                                buttonOption.setTag(false);
                                presenter.addCheckedOption(currentIndex);
                                if (presenter.isItemChecked(otherIndex)) {
                                    TextView tv = (TextView) newLayout.getChildAt(otherIndex - 1);
                                    tv.setTextColor(getResources().getColor(R.color.live_blue));
                                    tv.setBackgroundResource(R.drawable.live_question_tool_roundoption_unchecked);
                                    tv.setTag(true);
                                    presenter.deleteCheckedOption(otherIndex);
                                }
                            } else {
                                buttonOption.setTextColor(getResources().getColor(R.color.live_blue));
                                buttonOption.setBackgroundResource(R.drawable.live_question_tool_roundoption_unchecked);
                                buttonOption.setTag(true);
                                presenter.deleteCheckedOption(currentIndex);
                            }
                        }else {
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
                    }
                });
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        DisplayUtils.dip2px(getContext(), 35), DisplayUtils.dip2px(getContext(), 35));
                if (index % 4 == 1) {
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
