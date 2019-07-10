package com.baijiayun.live.ui.utils;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;


public class FileUtil {

    /**
     * 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static String copyFile(String oldPath, String newPath) {
        String newFileName = "/bjy_live_player_log.txt";
        if (TextUtils.isEmpty(oldPath) || TextUtils.isEmpty(newPath)) return "文件路径错误";
        newPath += newFileName;

        File dest= new File(newPath);
        if (dest!= null && dest.exists()) {
            dest.delete(); // delete file
        }
        try {
            dest.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            srcChannel = new FileInputStream(oldPath).getChannel();
            dstChannel = new FileOutputStream(dest).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "文件未找到";
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        try {
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "拷贝完成";


    }

    public static String getSDPath(){
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            return  Environment.getExternalStorageDirectory().getPath();//获取跟目录
        }else
            return null;
    }

}
