package com.baijiayun.live.ui.utils;

import com.baijiahulian.common.utils.StringUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Created by wangkangfei on 17/6/2.
 */

public class JsonObjectUtil {

    public static String getAsString(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).getAsString();
        }
        return "";
    }

    public static int getAsInt(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).getAsInt();
        }
        return -1;
    }

    public static boolean getAsBoolean(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).getAsBoolean();
        }
        return false;
    }

    public static boolean isJsonNull(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).isJsonNull();
        }
        return false;
    }

    public static boolean isJsonObject(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).isJsonObject();
        }
        return false;
    }

    public static boolean isJosnArray(JsonObject jsonObject, String key) {
        if (jsonObject.get(key) != null) {
            return jsonObject.get(key).isJsonArray();
        }
        return false;
    }

    /**
     * 是否是json格式
     */
    public static boolean isGoodJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            new JsonParser().parse(json);
            return true;
        } catch (JsonParseException e) {
            return false;
        }
    }
}
