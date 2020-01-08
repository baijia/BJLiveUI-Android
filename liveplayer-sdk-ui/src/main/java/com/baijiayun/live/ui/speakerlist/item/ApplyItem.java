package com.baijiayun.live.ui.speakerlist.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.speakerlist.SpeakersContract;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.models.imodels.IUserModel;

/**
 * Created by Shubo on 2019-07-25.
 */
public class ApplyItem implements SpeakItem {

    private View view;
    private IUserModel userModel;

    public ApplyItem(ViewGroup itemContainer, IUserModel model, SpeakersContract.Presenter presenter) {
        this.userModel = model;
        Context context = itemContainer.getContext();
        view = LayoutInflater.from(context).inflate(R.layout.item_speak_apply, itemContainer, false);
        QueryPlus q = QueryPlus.with(view);
        q.id(R.id.item_speak_apply_avatar).image(context, model.getAvatar());
        q.id(R.id.item_speak_apply_name).text(model.getName() + context.getString(R.string.live_media_speak_applying));
        q.id(R.id.item_speak_apply_agree).clicked(o -> presenter.agreeSpeakApply(model.getUserId()));
        q.id(R.id.item_speak_apply_disagree).clicked(o -> presenter.disagreeSpeakApply(model.getUserId()));
    }

    @Override
    public String getIdentity() {
        return userModel.getUserId();
    }

    @Override
    public SpeakItemType getItemType() {
        return SpeakItemType.Apply;
    }

    @Override
    public View getView() {
        return view;
    }
}
