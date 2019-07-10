package com.baijiayun.live.ui.utils;

/**
 * Created by Shubo on 2017/5/4.
 */

public class ChatImageUtil {

    /**
     * @param size
     * @param maxStandard
     * @param minStandard 1、等比显示、最小缩放比例；
     *                    2、无法同时满足最大、最小尺寸限制时，最小尺寸优先，另一方向裁切、居中；
     */
    public static void calculateImageSize(int[] size, int maxStandard, int minStandard) {
        int width = size[0];
        int height = size[1];
        if (width > maxStandard || height > maxStandard) {
            if (width / (float) maxStandard > height / (float) maxStandard) {
                size[0] = maxStandard;
                size[1] = height * maxStandard / width;
            } else {
                size[0] = width * maxStandard / height;
                size[1] = maxStandard;
            }
            return;
        }
        if (width < minStandard || height < minStandard) {
            if (width / (float) minStandard > height / (float) minStandard) {
                width = (int) (minStandard / (float) height * width);
                height = minStandard;
            } else {
                height = (int) (minStandard / (float) width * height);
                width = minStandard;
            }
            if (width > maxStandard || height > maxStandard) {
                if (width / (float) maxStandard > height / (float) maxStandard) {
                    size[0] = maxStandard;
                    size[1] = height * maxStandard / width;
                } else {
                    size[0] = width * maxStandard / height;
                    size[1] = maxStandard;
                }
            } else {
                size[0] = width;
                size[1] = height;
            }
        } else {
            size[0] = width;
            size[1] = height;
        }
    }
}
