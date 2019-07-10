package com.baijiayun.live.ui.questionanswer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPQuestionPullResItem;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class QuestionAnswerFragment extends BaseFragment implements QuestionAnswerContract.View {

    protected RecyclerView mRecyclerView;
    private QuestionAnswerContract.Presenter presenter;
    private QuestionAdapter adapter;
    private boolean isFullBlank = false;

    private TextView inputTextNumber;

    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutWrapManager(getContext());
    };

    @Override
    protected void init(Bundle savedInstanceState) {
        view.setClickable(true);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_question_answer_list);
        adapter = new QuestionAdapter();
        mRecyclerView.setLayoutManager(getLayoutManager());
        mRecyclerView.setAdapter(adapter);

        inputTextNumber = view.findViewById(R.id.fragment_question_answer_input_text_number);

        view.findViewById(R.id.fragment_question_answer_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.closeFragment();
            }
        });

        EditText mEditText = view.findViewById(R.id.fragment_question_answer_input_edit);
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(140)});

        view.findViewById(R.id.fragment_question_answer_input_listener).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.fragment_question_answer_input).setVisibility(View.GONE);
                view.findViewById(R.id.fragment_question_answer_input_window).setVisibility(View.VISIBLE);

                mEditText.setFocusable(true);
                mEditText.setFocusableInTouchMode(true);
                mEditText.requestFocus();
                mEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null) return;
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                        if (imm == null) return;
                        imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 100);

            }
        });

        view.findViewById(R.id.fragment_question_answer_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.fragment_question_answer_input).setVisibility(View.VISIBLE);
                view.findViewById(R.id.fragment_question_answer_input_window).setVisibility(View.GONE);
                presenter.sendQuestion(mEditText.getEditableText().toString());

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    view.findViewById(R.id.fragment_question_answer_send).setEnabled(false);
                } else {
                    view.findViewById(R.id.fragment_question_answer_send).setEnabled(true);
                }
                inputTextNumber.setText(s.length() + "/140");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_question_answer;
    }

    @Override
    public void notifyDataChange() {
        adapter.notifyDataSetChanged();
    }

    public void scrollToBottom(){
        if (mRecyclerView != null)
            mRecyclerView.smoothScrollToPosition(presenter.getCount());
    }

    @Override
    public void showToast(String content) {
        if (getActivity() == null) return;
        Toast.makeText(getActivity(), content, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void sendSuccess() {
        ((TextView)view.findViewById(R.id.fragment_question_answer_input_edit)).setText("");
        scrollToBottom();
    }

    @Override
    public void showEmpty(boolean isEmpty) {
        if (isEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            view.findViewById(R.id.fragment_question_answer_empty).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.fragment_question_answer_empty).setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void forbidQuestion(boolean isForbid) {
        if (isForbid) {
            if (view.findViewById(R.id.fragment_question_answer_input_window).getVisibility() == View.VISIBLE) {
                view.findViewById(R.id.fragment_question_answer_input_window).setVisibility(View.GONE);

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.hideSoftInputFromWindow(view.findViewById(R.id.fragment_question_answer_input_edit).getWindowToken(), 0);
            }
            view.findViewById(R.id.fragment_question_answer_input).setVisibility(View.GONE);
            view.findViewById(R.id.fragment_question_answer_input_forbid).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.fragment_question_answer_input_forbid).setVisibility(View.GONE);
            view.findViewById(R.id.fragment_question_answer_input).setVisibility(View.VISIBLE);
            view.findViewById(R.id.fragment_question_answer_input_window).setVisibility(View.GONE);
        }
    }

    @Override
    public void setPresenter(QuestionAnswerContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    private class QuestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_QUESTION = 1;
        private static final int VIEW_TYPE_LOADING = 2;

        private int visibleThreshold = LPConstants.DEFAULT_COUNT_PER_PAGE;
        private int lastVisibleItem, totalItemCount;

        QuestionAdapter() {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (mRecyclerView == null) return;;
                    final LinearLayoutWrapManager linearLayoutManager = (LinearLayoutWrapManager) mRecyclerView.getLayoutManager();
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    if (!presenter.isLoading() && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        presenter.loadMore();
                    }
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            if (viewType == VIEW_TYPE_QUESTION) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_answer, parent, false);
                return new QuestionItem(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_user_loadmore, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            LPQuestionPullResItem item = presenter.getQuestion(position);
            if (holder instanceof QuestionItem) {
                QuestionItem questionHolder = (QuestionItem) holder;
                if (item.itemList.isEmpty()) return;
                questionHolder.questionSymbol.setText("问");
                questionHolder.questionSymbol.setBackground(getContext().getResources().getDrawable(R.drawable.ic_item_question_symbol));
                questionHolder.questionSymbol.setTextColor(getContext().getResources().getColor(R.color.live_red_question_symbol));
                questionHolder.questionName.setText(item.itemList.get(0).from.getName());
                questionHolder.questionTime.setText(getTime(item.itemList.get(0).time));
                questionHolder.questionContent.setText(item.itemList.get(0).content);
                if (item.itemList.size() > 1) { // 存在老师回复
                    questionHolder.questionReply.setVisibility(View.VISIBLE);
                    questionHolder.questionReply.removeAllViews();
                    for (int i = 1; i < item.itemList.size(); i++) {
                        LinearLayout questionReply = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.item_question_answer, null, false);
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.bottomMargin = DisplayUtils.dip2px(getContext(), 10);
                        questionReply.setBackground(getContext().getResources().getDrawable(R.drawable.bg_item_question_answer_reply));
                        TextView questionSymbol = ((TextView) questionReply.findViewById(R.id.fragment_question_answer_symbol));
                        questionSymbol.setText("答");
                        questionSymbol.setTextColor(getContext().getResources().getColor(R.color.live_blue_question_symbol));
                        questionSymbol.setBackground(getContext().getResources().getDrawable(R.drawable.ic_item_answer_symbol));
                        ((TextView) questionReply.findViewById(R.id.fragment_question_answer_name)).setText(item.itemList.get(i).from.getName());
                        ((TextView) questionReply.findViewById(R.id.fragment_question_answer_time)).setText(getTime(item.itemList.get(i).time));
                        ((TextView) questionReply.findViewById(R.id.fragment_question_answer_content)).setText(item.itemList.get(i).content);
                        questionHolder.questionReply.addView(questionReply, layoutParams);
                    }
                } else
                    questionHolder.questionReply.setVisibility(View.GONE);
            }else if (holder instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return presenter.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return presenter.getQuestion(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_QUESTION;
        }
    }

    private String getTime(long time) {
        String pattern = "HH:mm";
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    private class QuestionItem extends RecyclerView.ViewHolder {

        TextView questionSymbol, questionName, questionTime, questionContent;
        LinearLayout questionReply;

        public QuestionItem(View itemView) {
            super(itemView);
            questionSymbol = itemView.findViewById(R.id.fragment_question_answer_symbol);
            questionName = itemView.findViewById(R.id.fragment_question_answer_name);
            questionContent = itemView.findViewById(R.id.fragment_question_answer_content);
            questionTime = itemView.findViewById(R.id.fragment_question_answer_time);
            questionReply = itemView.findViewById(R.id.fragment_question_answer_reply);
        }

    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.item_online_user_progress);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }
        presenter = null;
    }


}
