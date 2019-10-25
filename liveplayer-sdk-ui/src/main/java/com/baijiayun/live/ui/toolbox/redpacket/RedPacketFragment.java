package com.baijiayun.live.ui.toolbox.redpacket;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ListView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.toolbox.redpacket.widget.MoveModel;
import com.baijiayun.live.ui.toolbox.redpacket.widget.TranslateSurfaceView;
import com.baijiayun.livecore.models.LPRedPacketModel;


/**
 * 鲸溪定制UI-红包雨效果
 * @author panzq
 * @date    20190514
 */
public class RedPacketFragment extends BaseFragment implements RedPacketContract.View{

    private RedPacketContract.Presenter mPresenter;

    private TranslateSurfaceView mTsf;
    private ListView mLvJignxiTop;;
    private RedPacketTopAdapter mTopAdapter;
    private int mCurrStateType;

    private int count = 0;

    private LPRedPacketModel mLPRedPacketModel;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_red_packet;
    }


    @Override
    public void setPresenter(RedPacketContract.Presenter presenter) {
        this.mPresenter = presenter;
        setBasePresenter(presenter);
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        mTsf = (TranslateSurfaceView) $.id(R.id.tsf_red_packet).view();
        mLvJignxiTop = (ListView) $.id(R.id.lv_fragment_top).view();
        mTopAdapter = new RedPacketTopAdapter(getContext());
        mLvJignxiTop.setAdapter(mTopAdapter);

        $.id(R.id.rl_red_packet).view().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //屏蔽点击传递其他View
            }
        });

        $.id(R.id.btn_reb_phb_close).view().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPresenter.exit();
//                mPresenter.updateRedPacket();
            }
        });
        //没抢到
        $.id(R.id.tv_red_not_next).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchState(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);
            }
        });
        //抢到了
        $.id(R.id.tv_red_have_button).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchState(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);
            }
        });

    }

    @Override
    public void upDateRedPacketTime(long timeStart) {

        $.id(R.id.tv_red_packet_time_start).text("" + timeStart);
    }


    int index = 0;
    @Override
    public void switchRedPacketRankingList(RedPacketTopModel[] list) {

        switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);

//        RedPacketTopModel[] test = new RedPacketTopModel[5];
//        RedPacketTopModel model = new RedPacketTopModel();
//        model.rank_id = 1;
//        model.user_name = "百家赵";
//        model.coin = 300 + (index * 5);
//        test[0] = model;
//
//        model = new RedPacketTopModel();
//        model.rank_id = 2;
//        model.user_name = "百家钱";
//        model.coin = 240 + + (index * 5);
//        test[1] = model;
//
//        model = new RedPacketTopModel();
//        model.rank_id = 3;
//        model.user_name = "百家孙";
//        model.coin = 210 + + (index * 5);
//        test[2] = model;
//
//        model = new RedPacketTopModel();
//        model.rank_id = 4;
//        model.user_name = "百家李";
//        model.coin = 180 + + (index * 5);
//        test[3] = model;
//
//        model = new RedPacketTopModel();
//        model.rank_id = 5;
//        model.user_name = "百家周";
//        model.coin = 99 + + (index * 5);
//        test[4] = model;
//        index++;

        if(list == null || list.length == 0) {
            //显示表情
            $.id(R.id.rl_lp_ui_redpack_none).visibility(View.VISIBLE);
            $.id(R.id.lv_fragment_top).visibility(View.INVISIBLE);
        } else {
            $.id(R.id.rl_lp_ui_redpack_none).visibility(View.GONE);
            $.id(R.id.lv_fragment_top).visibility(View.VISIBLE);
        }

        mTopAdapter.setDate(list);
    }

    @Override
    public void switchRedPacketStart(int type) {

        mCurrStateType = type;

        if (type == RedPacketContract.TYPE_REDPACKET_START_COUNTDOWN) {

            $.id(R.id.rl_red_packet).visibility(View.VISIBLE);

            $.id(R.id.rl_fragment_redpacket_time).visibility(View.VISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.INVISIBLE);
            $.id(R.id.rl_not_red).visibility(View.INVISIBLE);
            $.id(R.id.rl_red_rob).visibility(View.INVISIBLE);

        } else if (type == RedPacketContract.TYPE_REDPACKET_RUNNING) {
            $.id(R.id.rl_fragment_redpacket_time).visibility(View.INVISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.VISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.INVISIBLE);
            $.id(R.id.rl_red_rob).visibility(View.INVISIBLE);

            count = 0;
            mTsf.setOnClickRedPacketListenert(new TranslateSurfaceView.OnClickRedPacketListener() {
                @Override
                public void onClick(MoveModel model) {
                    //点击请求
                    mPresenter.robRedPacket(model);
                }
            });
            mTsf.setVisibility(View.VISIBLE);
            mTsf.start();
        } else if (type == RedPacketContract.TYPE_REDPACKET_RANKING_LIST) {

            mTsf.pause();
            mTsf.setVisibility(View.INVISIBLE);

            $.id(R.id.rl_not_red).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_time).visibility(View.INVISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.VISIBLE);
            $.id(R.id.rl_red_rob).visibility(View.INVISIBLE);
        } else if (type == RedPacketContract.TYPE_REDPACKET_EXIT) {

            $.id(R.id.rl_fragment_redpacket_time).visibility(View.INVISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.INVISIBLE);

            $.id(R.id.rl_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_red_rob).visibility(View.INVISIBLE);
        } else if (type == RedPacketContract.TYE_REDPACKET_NOT_ROB) {
            //一个没抢到
            $.id(R.id.rl_fragment_redpacket_time).visibility(View.INVISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.INVISIBLE);
            $.id(R.id.rl_red_rob).visibility(View.INVISIBLE);

            ScaleAnimation animation = new ScaleAnimation(0.4f,1f,0.4f,1f,Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(800);
            $.id(R.id.rl_not_red).view().setAnimation(animation);
            animation.startNow();
            $.id(R.id.rl_not_red).visibility(View.VISIBLE);

        } else if (type == RedPacketContract.TYPE_REDPACKET_ROB) {
            //显示抢到的积分
            $.id(R.id.rl_fragment_redpacket_time).visibility(View.INVISIBLE);
            $.id(R.id.tsf_red_packet).visibility(View.INVISIBLE);
            $.id(R.id.rl_fragment_redpacket_phb).visibility(View.INVISIBLE);
            $.id(R.id.rl_not_red).visibility(View.INVISIBLE);

            ScaleAnimation animation = new ScaleAnimation(0.4f,1f,0.4f,1f,Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(800);
            $.id(R.id.rl_red_rob).view().setAnimation(animation);
            animation.startNow();
            $.id(R.id.rl_red_rob).visibility(View.VISIBLE);

//            RotateAnimation rotate =
//                    new RotateAnimation(0, 360 * 10000,
//                            Animation.RELATIVE_TO_SELF,0.5f,
//                            Animation.RELATIVE_TO_SELF,0.5f);
////                        rotate.setRepeatCount(-1);
//            rotate.setDuration(3000 * 10000);
//            LinearInterpolator lir = new LinearInterpolator();
//            rotate.setInterpolator(lir);
//            rotate.setRepeatCount(Animation.INFINITE);
//            rotate.setRepeatMode(Animation.RESTART);
//            $.id(R.id.iv_red_le).view().startAnimation(rotate);

            $.id(R.id.tv_red_rob_credit).text("" + mPresenter.getScoreAmount());
        }
    }

    @Override
    public int getCurrStateType() {
        return mCurrStateType;
    }

    @Override
    public void showRedPacketScoreAmount(int mScoreAmount) {
    }

    @Override
    public void setRobEnable(boolean robEnable) {
        if (mTsf == null)
            return;
        mTsf.setRobEnable(robEnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTsf != null) {
            mTsf.pause();
            mTsf.destory();
            mTsf = null;
        }
    }
}
