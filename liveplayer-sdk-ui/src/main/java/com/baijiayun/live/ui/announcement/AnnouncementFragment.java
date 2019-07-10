package com.baijiayun.live.ui.announcement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.QueryPlus;

/**
 * Created by Shubo on 2017/4/19.
 */

public class AnnouncementFragment extends BaseDialogFragment implements AnnouncementContract.View {

    private AnnouncementContract.Presenter presenter;
    private QueryPlus $;
    private boolean isTeacherView = true;
    private TextWatcher textWatcher;

    public static AnnouncementFragment newInstance() {

        Bundle args = new Bundle();

        AnnouncementFragment fragment = new AnnouncementFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setPresenter(AnnouncementContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_announcement;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.title(getString(R.string.live_announcement)).editText("");
        $ = QueryPlus.with(contentView);
        final EditText editText = (EditText) $.id(R.id.dialog_announcement_et).view();
        final EditText editUrl = (EditText) $.id(R.id.dialog_announcement_url_et).view();
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.checkInput(editText.getText().toString(), editUrl.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        editText.addTextChangedListener(textWatcher);
        editUrl.addTextChangedListener(textWatcher);
    }


    @Override
    public void showTeacherView() {
        super.editable(true);
        isTeacherView = true;
        $.id(R.id.dialog_announcement_et).visible();
        $.id(R.id.dialog_announcement_url_container).visible();
        $.id(R.id.dialog_announcement_view_container).gone();
        $.id(R.id.dialog_announcement_view_hint).gone();
    }

    @Override
    public void showStudentView() {
        super.editable(false);
        isTeacherView = false;
        $.id(R.id.dialog_announcement_et).gone();
        $.id(R.id.dialog_announcement_url_container).gone();
        $.id(R.id.dialog_announcement_view_container).visible();
        $.id(R.id.dialog_announcement_view_hint).gone();
    }

    @Override
    public void showAnnouncementText(String text) {
        if (isTeacherView) {
            $.id(R.id.dialog_announcement_et).text(text);
        } else {
            if (TextUtils.isEmpty(text)) {
                $.id(R.id.dialog_announcement_view_tv).text(getString(R.string.live_announcement_none));
            } else {
                $.id(R.id.dialog_announcement_view_tv).text(text);
            }
        }
    }

    @Override
    public void showAnnouncementUrl(final String url) {
        if (isTeacherView) {
            $.id(R.id.dialog_announcement_url_et).text(url);
        } else {
            if (TextUtils.isEmpty(url)) {
                $.id(R.id.dialog_announcement_view_container).clicked(null);
                $.id(R.id.dialog_announcement_view_hint).gone();
            } else {
                $.id(R.id.dialog_announcement_view_container).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri uri = Uri.parse(url);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });
                $.id(R.id.dialog_announcement_view_hint).visible();
            }
        }
    }

    @Override
    public void showCheckStatus(int status) {
        if (!isTeacherView) return;
        switch (status) {
            case AnnouncementContract.STATUS_CHECKED_CAN_SAVE:
                super.editText(getString(R.string.live_save))
                        .editColor(ContextCompat.getColor(getContext(), R.color.live_blue))
                        .editClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText editText = (EditText) $.id(R.id.dialog_announcement_et).view();
                                EditText editUrl = (EditText) $.id(R.id.dialog_announcement_url_et).view();
                                if (TextUtils.isEmpty(editText.getText().toString())) {
                                    presenter.saveAnnouncement(editUrl.getText().toString(), editUrl.getText().toString());
                                } else {
                                    presenter.saveAnnouncement(editText.getText().toString(), editUrl.getText().toString());
                                }
                            }
                        });
                break;
            case AnnouncementContract.STATUS_CHECKED_CANNOT_SAVE:
                super.editText(getString(R.string.live_save))
                        .editColor(ContextCompat.getColor(getContext(), R.color.live_blue_half_transparent))
                        .editClick(null);
                break;
            case AnnouncementContract.STATUS_CHECKED_SAVED:
                super.editText(getString(R.string.live_saved))
                        .editColor(ContextCompat.getColor(getContext(), R.color.live_text_color_light))
                        .editClick(null);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EditText editText = (EditText) $.id(R.id.dialog_announcement_et).view();
        EditText editUrl = (EditText) $.id(R.id.dialog_announcement_url_et).view();
        editText.removeTextChangedListener(textWatcher);
        editUrl.removeTextChangedListener(textWatcher);
        textWatcher = null;
        presenter = null;
    }
}
