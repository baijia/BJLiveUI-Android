package com.baijiayun.live.ui.utils;

import android.content.Context;
import android.util.Log;

import com.baijiayun.livecore.utils.DisplayUtils;
import com.baijiayun.livecore.utils.LPBJUrl;

import java.util.HashMap;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * Created by Shubo on 2017/4/6.
 */

public class AliCloudImageUtil {

    /**
     * 200*200像素阿里云图片裁剪规则 (聊天图片)
     *
     * @param url
     * @return
     */
    public static final String SCALED_MFIT = "m_mfit";
    public static final String SCALED_LFIT = "m_lfit";
    public static final String SCALED_FILL = "m_fill";
    public static final String SCALED_FIXED = "m_fixed";

    private static String _getScaledUrl(String url, String model, int width, int height) {
        String urlSplit = url.split("\\?")[0];
        if (urlSplit.endsWith(".jpg") || urlSplit.endsWith(".jpeg") || urlSplit.endsWith(".png") || urlSplit.endsWith(".webp")
                || urlSplit.endsWith(".bmp") || urlSplit.endsWith(".gif")) {
            if (!url.contains("?")) {
                url += "?x-oss-process=image/resize," + model + ",h_" + String.valueOf(height) + ",w_" + String.valueOf(width) + "/format" + imageUrlSuffix();
                return url;
            }
            if (url.contains("x-oss-process")) {
                HashMap<String, String> hashMap = (LPBJUrl.parse(url)).getParameters();
                return url.replace(hashMap.get("x-oss-process"), "image/resize," + model + ",h_" + String.valueOf(height) + ",w_" + String.valueOf(width)
                        + "/format" + imageUrlSuffix());
            } else {
                StringBuilder stringBuilder = new StringBuilder(url);
                stringBuilder.insert(url.indexOf("?") + 1, "x-oss-process=image/resize," + model + ",h_" + String.valueOf(height) + ",w_" + String.valueOf(width)
                        + "/format" + imageUrlSuffix() + "&");
                return stringBuilder.toString();
            }
        }
        return url;
    }

    public static String getScaledUrl(String url, String model, int width, int height) {
        switch (model) {
            case SCALED_MFIT:
                return _getScaledUrl(url, SCALED_MFIT, width, height);

            case SCALED_LFIT:
                return _getScaledUrl(url, SCALED_LFIT, width, height);

            case SCALED_FILL:
                return _getScaledUrl(url, SCALED_FILL, width, height);

            case SCALED_FIXED:
                return _getScaledUrl(url, SCALED_FIXED, width, height);

            default:
                return url;
        }
    }

    /**
     * 正方形阿里云图片裁剪规则
     *
     * @param url
     * @return
     */
    public static String getRectScaledUrl(Context context, String url, int dp) {
        int px = DisplayUtils.dip2px(context, dp);
        String urlSplit = url.split("\\?")[0];
        if (urlSplit.endsWith(".jpg") || urlSplit.endsWith(".jpeg") || urlSplit.endsWith(".png") || urlSplit.endsWith(".webp")
                || urlSplit.endsWith(".bmp") || urlSplit.endsWith(".gif")) {
            if (!url.contains("?")) {
                url += "?x-oss-process=image/crop,w_" + px + ",h_" + px + "/format" + imageUrlSuffix();
                return url;
            }
            if (url.contains("x-oss-process")) {
                HashMap<String, String> hashMap = (LPBJUrl.parse(url)).getParameters();
                return url.replace(hashMap.get("x-oss-process"), "image/crop,w_" + px + ",h_" + px + "/format" + imageUrlSuffix());
            } else {
                StringBuilder stringBuilder = new StringBuilder(url);
                stringBuilder.insert(url.indexOf("?") + 1, "x-oss-process=image/crop,w_" + px + ",h_" + px + "/format" + imageUrlSuffix() + "&");
                return stringBuilder.toString();
            }
        }
        return url;
    }

    /**
     * 屏幕长宽阿里云图片裁剪规则
     *
     * @param context
     * @param url
     * @return
     */
    public static String getScreenScaledUrl(Context context, String url) {
        String urlSplit = url.split("\\?")[0];
        if (urlSplit.endsWith(".jpg") || urlSplit.endsWith(".jpeg") || urlSplit.endsWith(".png") || urlSplit.endsWith(".webp")
                || urlSplit.endsWith(".bmp") || urlSplit.endsWith(".gif")) {
            if (!url.contains("?")) {
                url += "?x-oss-process=image/crop," + "w_" + DisplayUtils.getScreenWidthPixels(context) + ",h_" + DisplayUtils.getScreenHeightPixels(context) +
                        "/format" + imageUrlSuffix();
                return url;
            }
            if (url.contains("x-oss-process")) {
                HashMap<String, String> hashMap = (LPBJUrl.parse(url)).getParameters();
                return url.replace(hashMap.get("x-oss-process"), "image/crop," + "w_" + DisplayUtils.getScreenWidthPixels(context) + ",h_" + DisplayUtils.getScreenHeightPixels(context) +
                        "/format" + imageUrlSuffix());
            } else {
                StringBuilder stringBuilder = new StringBuilder(url);
                stringBuilder.insert(url.indexOf("?") + 1, "x-oss-process=image/crop," + "w_" + DisplayUtils.getScreenWidthPixels(context) + ",h_" + DisplayUtils.getScreenHeightPixels(context) +
                        "/format" + imageUrlSuffix() + "&");
                return stringBuilder.toString();
            }
        }
        return url;
    }

    /**
     * 阿里云圆形图片规则
     *
     * @param url
     * @param radius
     * @return
     */
    public static String getRoundedAvatarUrl(String url, int radius) {
        String urlSplit = url.split("\\?")[0];
        if (urlSplit.endsWith(".jpg") || urlSplit.endsWith(".jpeg") || urlSplit.endsWith(".png") || urlSplit.endsWith(".webp")
                || urlSplit.endsWith(".bmp") || urlSplit.endsWith(".gif")) {
            if (!url.contains("?")) {
                url += "?x-oss-process=image/circle,r_" + String.valueOf(radius) + "/format" + imageUrlSuffix();
                return url;
            }
            if (url.contains("x-oss-process")) {
                HashMap<String, String> hashMap = (LPBJUrl.parse(url)).getParameters();
//                Log.e("debugRound1: ", url.replace(hashMap.get("x-oss-process"), "image/circle,r_" + String.valueOf(radius) + "/format" + imageUrlSuffix()));
                return url.replace(hashMap.get("x-oss-process"), "image/circle,r_" + String.valueOf(radius) + "/format" + imageUrlSuffix());
            } else {
                StringBuilder stringBuilder = new StringBuilder(url);
                stringBuilder.insert(url.indexOf("?") + 1, "x-oss-process=image/circle,r_" + String.valueOf(radius) + "/format" + imageUrlSuffix() + "&");

                return stringBuilder.toString();
            }
        }
        return url;
    }

    private static String imageUrlSuffix() {
        if (SDK_INT >= ICE_CREAM_SANDWICH) {
            return ",webp";
        } else {
            return ",png";
        }
    }
}
