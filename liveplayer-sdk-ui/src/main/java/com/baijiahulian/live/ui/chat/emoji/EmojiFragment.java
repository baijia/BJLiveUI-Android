package com.baijiahulian.live.ui.chat.emoji;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.baijiahulian.live.ui.R;
import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.live.ui.base.BaseFragment;
import com.baijiahulian.live.ui.utils.DisplayUtils;
import com.baijiahulian.livecore.models.imodels.IExpressionModel;
import com.squareup.picasso.Picasso;

import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;

/**
 * Created by Shubo on 2017/5/6.
 */

public class EmojiFragment extends BaseFragment implements EmojiContract.View {

    private EmojiSelectCallBack callBack;
    private EmojiContract.Presenter presenter;
    private ViewPager viewPager;
    private EmojiPagerAdapter pagerAdapter;
    private int gridPadding;
    private int spanCount = 8;
    private int rouCount = 4;

    public static EmojiFragment newInstance() {

        Bundle args = new Bundle();
        EmojiFragment fragment = new EmojiFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        gridPadding = DisplayUtils.dip2px(getContext(), 10);
        adjustItemCount();
        presenter = new EmojiPresenter(this);
        presenter.setRouter((LiveRoomRouterListener) getActivity());
        setPresenter(presenter);
        viewPager = (ViewPager) $.id(R.id.fragment_emoji_container_viewpager).view();
        pagerAdapter = new EmojiPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == SCROLL_STATE_IDLE){
                    presenter.onPageSelected(viewPager.getCurrentItem());
                }
            }
        });
    }

    private void adjustItemCount() {
        int screenWidth = DisplayUtils.getScreenWidthPixels(getContext());
        spanCount = (screenWidth - 2 * gridPadding) / DisplayUtils.dip2px(getContext(), 40);
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_emoji;
    }

    public void setCallBack(EmojiSelectCallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void setPresenter(EmojiContract.Presenter presenter) {
        super.setBasePresenter(presenter);
    }

    @Override
    public int getSpanCount() {
        return spanCount;
    }

    @Override
    public int getRowCount() {
        return rouCount;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustItemCount();
        presenter.onSizeChanged();
        viewPager.setAdapter(new EmojiPagerAdapter());
        viewPager.setCurrentItem(presenter.getPageOfCurrentFirstItem());
    }

    private class EmojiPagerAdapter extends PagerAdapter {

        private View[] viewList;

        EmojiPagerAdapter() {
            viewList = new View[getCount()];
        }

        @Override
        public int getCount() {
            return presenter.getPageCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (viewList.length <= position || viewList[position] == null) {
                GridView gridView = new GridView(getContext());
                gridView.setPadding(gridPadding, 0, gridPadding, 0);
                gridView.setNumColumns(spanCount);
                gridView.setAdapter(new EmojiAdapter(position));
                viewList[position] = gridView;
            }
            container.addView(viewList[position]);
            return viewList[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(viewList[position]);
        }

        @Override
        public void startUpdate(ViewGroup container) {
            super.startUpdate(container);
        }
    }

    private class EmojiAdapter extends BaseAdapter {

        private int page;

        EmojiAdapter(int page) {
            this.page = page;
        }

        @Override
        public int getCount() {
            return presenter.getCount(page);
        }

        @Override
        public IExpressionModel getItem(int position) {
            return presenter.getItem(page, position);
        }

        @Override
        public long getItemId(int position) {
            return page * spanCount * rouCount + position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate(R.layout.bj_live_item_emoji, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.bj_live_item_emoji_iv);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final IExpressionModel expressionModel = getItem(position);
            Picasso.with(getContext()).load(expressionModel.getUrl()).into(holder.imageView);
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callBack != null)
                        callBack.onEmojiSelected(expressionModel);
                }
            });
            return convertView;
        }
    }

    private class ViewHolder {
        ImageView imageView;
    }
}
