package com.baijiayun.live.ui.utils;

import android.content.Context;
import android.content.res.Resources;

public class CommonUtil {
    public static int getId(Context paramContext, String paramString) {
        try {
            return paramContext.getResources().getIdentifier(paramString,"id", paramContext.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getDimenById(Context paramContext, int dimenId){
        try {
            return (int)paramContext.getResources().getDimension(dimenId);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }
}