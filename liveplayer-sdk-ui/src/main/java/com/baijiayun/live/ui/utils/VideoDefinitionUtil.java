package com.baijiayun.live.ui.utils;

import com.baijiayun.livecore.context.LPConstants;

/**
 * Created by Shubo on 2018/6/21.
 */
public class VideoDefinitionUtil {

    public static String getVideoDefinitionLabelFromType(LPConstants.VideoDefinition definition) {
        switch (definition) {
            case _1080P:
                return "1080P";
            case _720P:
                return "720P";
            case Super:
                return "超清";
            case High:
                return "高清";
            case Std:
                return "标清";
            case Low:
                return "流畅";
            default:
                return "";
        }
    }

    public static LPConstants.VideoDefinition getVideoDefinitionTypeFromLabel(String label) {
        switch (label) {
            case "1080P":
                return LPConstants.VideoDefinition._1080P;
            case "720P":
                return LPConstants.VideoDefinition._720P;
            case "超清":
                return LPConstants.VideoDefinition.Super;
            case "高清":
                return LPConstants.VideoDefinition.High;
            case "标清":
                return LPConstants.VideoDefinition.Std;
            case "流畅":
                return LPConstants.VideoDefinition.Low;
        }
        return null;
    }
}
