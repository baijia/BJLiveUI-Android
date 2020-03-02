package com.baijiayun.live.ui.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.baijiayun.live.ui.utils.Precondition;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.utils.ToastCompat;

/**
 * Created by Shubo on 2017/2/13.
 */

public abstract class BaseFragment extends Fragment {

    private final String TAG = BaseFragment.class.getCanonicalName();
    protected View view;
    protected QueryPlus $;
    private BasePresenter basePresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int res = getLayoutId();

        if (res != 0) {
            view = inflater.inflate(res, container, false);
        } else {
            view = getContentView();
        }
        Precondition.checkNotNull(view);
        $ = QueryPlus.with(view);
        init(savedInstanceState);
        if (basePresenter != null)
            basePresenter.subscribe();
        return view;
    }

    public abstract int getLayoutId();

    protected void init(Bundle savedInstanceState) {

    }

    protected View getContentView() {
        return null;
    }

    public void setBasePresenter(BasePresenter presenter) {
        basePresenter = presenter;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    protected void showToast(String msg) {
        if(getActivity() == null || getActivity().isDestroyed()){
            return;
        }
        ToastCompat.showToastCenter(getContext(),msg,Toast.LENGTH_SHORT);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
//        hideShowLifeCycle(hidden);
    }

    protected void hideShowLifeCycle(boolean hidden) {
        if (hidden) onPause();
        else onResume();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (basePresenter != null) {
            basePresenter.unSubscribe();
            basePresenter.destroy();
            basePresenter = null;
        }
        $ = null;
    }

    @Override
    public void onDetach(){
        Log.d(TAG, "onDetach");
        super.onDetach();
    }
}
