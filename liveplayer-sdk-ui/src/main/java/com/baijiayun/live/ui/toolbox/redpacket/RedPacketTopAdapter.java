package com.baijiayun.live.ui.toolbox.redpacket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baijiayun.live.ui.R;

/**
 * 红包排名
 * @author panzq
 * 20180515
 */
public class RedPacketTopAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private RedPacketTopModel[] mList;
    private Context mContext;

    public RedPacketTopAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        mContext = context;
    }

    public void setDate(RedPacketTopModel[] list) {
        if (list == null)
            return;

        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mList == null)
            return 0;
        return mList.length;
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < getCount())
            return mList[position];
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TopHolder topHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_jingx_red_packet_top, parent,false);
            topHolder = new TopHolder();

            topHolder.mLlItemJingxiRedPacketTop = convertView.findViewById(R.id.ll_item_jingxi_red_packet_top);
            topHolder.mTvJingxiRpIcon = convertView.findViewById(R.id.tv_jingxi_rp_icon);
            topHolder.mTvJingxiRpTopName = convertView.findViewById(R.id.tv_jingxi_rp_top_name);
            topHolder.mTvJingxiRpTopCoin = convertView.findViewById(R.id.tv_jingxi_rp_top_coin);
            convertView.setTag(topHolder);
        } else {
            topHolder = (TopHolder) convertView.getTag();
        }

        RedPacketTopModel model = mList[position];

        topHolder.mTvJingxiRpIcon.setText("");
        topHolder.mTvJingxiRpTopName.setTextColor(mContext.getResources().getColor(R.color.color_FF6059));
        if (model.rank_id == 1) {
            topHolder.mTvJingxiRpIcon.setBackground(
                    mContext.getResources().getDrawable(R.drawable.iv_lp_ui_red_top_1));
        } else if (model.rank_id == 2) {
            topHolder.mTvJingxiRpIcon.setBackground(
                    mContext.getResources().getDrawable(R.drawable.iv_lp_ui_red_top_2));
        } else if (model.rank_id == 3) {
            topHolder.mTvJingxiRpIcon.setBackground(
                    mContext.getResources().getDrawable(R.drawable.iv_lp_ui_red_top_3));
        } else {
            topHolder.mTvJingxiRpIcon.setBackground(null);
            topHolder.mTvJingxiRpIcon.setText("" + model.rank_id);
            topHolder.mTvJingxiRpTopName.setTextColor(mContext.getResources().getColor(R.color.color_00000000));
        }

        topHolder.mTvJingxiRpTopName.setText(model.user_name);
        topHolder.mTvJingxiRpTopCoin.setText("" + model.coin);
        return convertView;
    }

    class TopHolder {
        private LinearLayout mLlItemJingxiRedPacketTop;
        private TextView mTvJingxiRpIcon;
        private TextView mTvJingxiRpTopName;
        private TextView mTvJingxiRpTopCoin;
    }
}
