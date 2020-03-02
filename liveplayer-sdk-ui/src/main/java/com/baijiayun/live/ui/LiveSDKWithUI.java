package com.baijiayun.live.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.activity.LiveRoomActivity;
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity;
import com.baijiayun.live.ui.utils.LPShareModel;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;

import java.util.ArrayList;
import java.util.Map;

/**
 * 入口类
 * Created by Shubo on 2017/2/13.
 */

public class LiveSDKWithUI {

    /**
     * 通过参加码进入房间
     *
     * @param context
     * @param code     参加码
     * @param name     昵称
     * @param listener 出错回调
     */
    public static void enterRoom(@NonNull Context context, @NonNull String code, @NonNull String name,
                                 @NonNull LiveSDKEnterRoomListener listener) {
        enterRoom(context, code, name, null, listener);
    }

    /**
     * 通过参加码进入房间
     *
     * @param context
     * @param code     参加码
     * @param name     昵称
     * @param avatar
     * @param listener 出错回调
     */
    public static void enterRoom(@NonNull Context context, @NonNull String code, @NonNull String name,
                                 @Nullable String avatar, @NonNull LiveSDKEnterRoomListener listener) {
        if (TextUtils.isEmpty(name)) {
            listener.onError("name is empty");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            listener.onError("code is empty");
            return;
        }
        addDefaultLoginConflictCallback();

        Intent intent = new Intent(context, LiveRoomTripleActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("code", code);
        if (!TextUtils.isEmpty(avatar)) {
            intent.putExtra("avatar", avatar);
        }
        context.startActivity(intent);
    }

    /**
     * @param context
     * @param roomId   房间号
     * @param sign     签名
     * @param model    用户model (包含昵称、头像、角色等)
     * @param listener 出错回调
     */
    public static void enterRoom(@NonNull Context context, long roomId,
                                 @NonNull String sign, @NonNull LiveRoomUserModel model, @NonNull LiveSDKEnterRoomListener listener) {
        if (roomId <= 0) {
            listener.onError("room id =" + roomId);
            return;
        }
        if (TextUtils.isEmpty(sign)) {
            listener.onError("sign =" + sign);
            return;
        }
        addDefaultLoginConflictCallback();
        Intent intent = new Intent(context, LiveRoomTripleActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("sign", sign);
        intent.putExtra("user", model);
        context.startActivity(intent);
    }

    private static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public interface LiveSDKEnterRoomListener {
        void onError(String msg);
    }

    public static void setRoomExitListener(LPRoomExitListener listener) {
        LiveRoomBaseActivity.setRoomExitListener(listener);
    }

    public static void setShareListener(LPShareListener listener) {
        LiveRoomBaseActivity.setShareListener(listener);
    }

    public static void setEnterRoomConflictListener(RoomEnterConflictListener listener) {
        LiveRoomBaseActivity.setEnterRoomConflictListener(listener);
    }

    public static void setRoomLifeCycleListener(LPRoomResumeListener listener) {
        LiveRoomBaseActivity.setRoomLifeCycleListener(listener);
    }

    public static void disableSpeakQueuePlaceholder() {
        LiveRoomBaseActivity.disableSpeakQueuePlaceholder();
    }

    /**
     * 跑马灯字段
     */
    public static void setLiveRoomMarqueeTape(String str) {
        LiveRoomBaseActivity.setLiveRoomMarqueeTape(str);
    }

    public static void setLiveRoomMarqueeTape(String str, int interval) {
        LiveRoomBaseActivity.setLiveRoomMarqueeTape(str, interval);
    }

    public interface LPRoomResumeListener {
        void onResume(Context context, LPRoomChangeRoomListener listener);
    }

    public interface LPRoomChangeRoomListener {
        void changeRoom(String code, String nickName);
    }

    public interface RoomEnterConflictListener {
        void onConflict(Context context, LPConstants.LPEndType endType, LPRoomExitCallback callback);
    }

    public interface LPRoomExitListener {
        void onRoomExit(Context context, LPRoomExitCallback callback);
    }

    public interface LPRoomExitCallback {
        void exit();

        void cancel();
    }

    public interface LPShareListener {
        void onShareClicked(Context context, int type);

        ArrayList<? extends LPShareModel> setShareList();

        void getShareData(Context context, long roomId);
    }

    public static class LiveRoomUserModel implements IUserModel {

        String userName;
        String userAvatar;
        String userNumber;
        LPConstants.LPUserType userType;
        int groupID = -1;

        public LiveRoomUserModel(@NonNull String userName, @Nullable String userAvatar,
                                 @Nullable String userNumber, @NonNull LPConstants.LPUserType userType) {
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.userNumber = userNumber;
            this.userType = userType;
        }

        public LiveRoomUserModel(@NonNull String userName, @Nullable String userAvatar,
                                 @Nullable String userNumber, @NonNull LPConstants.LPUserType userType,
                                 int groupID) {
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.userNumber = userNumber;
            this.userType = userType;
            this.groupID = groupID;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public String getNumber() {
            return userNumber;
        }

        @Override
        public LPConstants.LPUserType getType() {
            return userType;
        }

        @Override
        public String getName() {
            return userName;
        }

        @Override
        public String getAvatar() {
            return userAvatar;
        }

        @Override
        public int getGroup() {
            return groupID;
        }

        @Override
        public LPConstants.LPEndType getEndType() {
            return LPConstants.LPEndType.Android;
        }

        @Override
        public Map<String, Object> getWebRTCInfo() {
            return null;
        }
    }

    /**
     * 登录冲突默认弹框提示
     */
    private static void addDefaultLoginConflictCallback(){
        LiveSDKWithUI.setEnterRoomConflictListener((context12, endType, callback) -> {
            String message;
            switch (endType) {
                case iOS:
                    message = "iOS";
                    break;
                case PC_H5:
                    message = "PC网页";
                    break;
                case PC_HTML:
                    message = "PC网页";
                    break;
                case PC_Client:
                    message = "PC客户";
                    break;
                case PC_MAC_Client:
                    message = "MAC客户";
                    break;
                case Android:
                    message = "Android";
                    break;
                default:
                    message = endType.name();
                    break;
            }
            new MaterialDialog.Builder(context12)
                    .content("您的账号已在" + message + "端登录")
                    .positiveText(R.string.confirm)
                    .canceledOnTouchOutside(false)
                    .onPositive((dialog, which) -> callback.exit())
                    .show();
        });
    }
}
