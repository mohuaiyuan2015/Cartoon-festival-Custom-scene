package com.tobot.tobot.utils;

import android.util.Log;

import com.tobot.tobot.base.Constants;
import com.tobot.tobot.db.bean.UserDBManager;
import com.turing123.libs.android.resourcemanager.ResourceManager;
import com.turing123.libs.android.resourcemanager.ResourceMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Javen on 2017/8/3.
 */

public class TobotUtils {
    /**
     * 判断是否为空，或者全部空格
     * @return
     */
    public static boolean isEmpty(Object obj) {
        return null == obj || "".equals(obj.toString().trim());
    }

    /**
     * 判断是否空白
     * @return
     */
    public static boolean isBlank(Object obj) {
        return null == obj || "".equals(obj.toString());
    }

    /**
     * 判断是否不为空
     * @return
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断是否不为空
     * @return
     */
    public static boolean isNotEmpty(Object obj1,Object obj2) {
        if (!isEmpty(obj1) && !isEmpty(obj2)){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 判断是否不为空
     * @return
     */
    public static boolean isEqual(Object obj1,Object obj2) {
        if (obj1.equals(obj2) || obj1 == obj2){
            return true;
        }else{
            return false;
        }
    }



    /**
     * 机器人是否首次使用 true:首次/false:非首次
     * @return
     */
    public static boolean isEmploy(){
        String Ultr = null;
        try {
            Ultr = UserDBManager.getManager().getCurrentUser().getUltr();
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (TobotUtils.isEmpty(Ultr)) {
            return true;
        }else {
            return false;
        }
    }

    /**
     * 机器人是否首次使用 true:首次/false:非首次
     * @return
     */
    public static boolean isEmployFack(){
        String UltrFack = null;
        try {
            UltrFack = UserDBManager.getManager().getCurrentUser().getUltrFack();
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (TobotUtils.isEmpty(UltrFack)) {
            return true;
        }else {
            return false;
        }
    }

    /**
     * 机器人联网状态
     * @return
     */
    public static String isEmployAP(){
        try {
            return UserDBManager.getManager().getCurrentUser().getUltrAP();
        } catch (Exception e) {
            // TODO: handle exceptionre
            return "0";
        }
    }


    /**
     * 获取当前日期 格式：yyyy/MM/dd HH:mm:ss
     * @return
     */
    public static String getCurrentlyDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    /**
     * 两个时间相减 格式：yyyy-MM-dd HH:mm:ss
     * @param time1
     * @param time2
     * @return
     */
    public static long DateMinusTime(String time1, String time2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1 = new Date();
        Date d2 = new Date();
        try {
            d1 = dateFormat.parse(time1);
            d2 = dateFormat.parse(time2);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long diff = d2.getTime() - d1.getTime();// 这样得到的差值是微秒级别
        long days = diff / (1000 * 60 * 60 * 24);//24小时
        return days;
    }



    /**
     *读取文本文件中的内容
     * @param strFilePath
     * @return
     */
    public static String ReadTxtFile(String strFilePath) {
        String path = strFilePath;
        String content = ""; //文件内容字符串
        //打开文件
        File file = new File(path);
        //如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isDirectory()) {
            Log.d("TestFile", "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    //分行读取
                    while ((line = buffreader.readLine()) != null) {
                        content += line + "\n";
                    }
                    instream.close();
                }
            } catch (java.io.FileNotFoundException e) {
                Log.d("TestFile", "The File doesn't not exist.");
            } catch (IOException e) {
                Log.d("TestFile", e.getMessage());
            }
        }
        return content;
    }

    /**
     * 取设备ID
     * @param strFilePath
     * @return
     */
    public static String getDeviceId(String matching, String strFilePath){
        String text = ReadTxtFile(strFilePath);
        String regEx = matching+">(.+)<";
        Pattern pat = Pattern.compile(regEx);
        Matcher mat = pat.matcher(text);
        boolean rs = mat.find();
        StringBuffer stringBuffer = new StringBuffer();
        for(int i=1;i<=mat.groupCount();i++){
            Log.e("Javen",".数组........."+i);
            stringBuffer.append(mat.group(i));
        }
        return stringBuffer.toString();
    }


}
