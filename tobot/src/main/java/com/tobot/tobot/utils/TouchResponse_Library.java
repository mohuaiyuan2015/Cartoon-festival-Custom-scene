package com.tobot.tobot.utils;

import android.util.Log;

import com.tobot.tobot.entity.ScoffEntity;
import com.turing123.robotframe.RobotFrameManager;

import java.util.Random;

/**
 * Created by Javen on 2017/8/18.
 */

public class TouchResponse_Library {

    private static String var0 = "找俺啥事";
    private static String var1 = "干嘛一直摸我";
    private static String var2 = "能不能别摸了,有事你可以说";
    private static String var3 = "你还摸";
    private static String var4 = "小子,你再摸下试试,我忍你很久了";
    private static String var5 = "你再摸,我要发飙了";
    private static String var6 = "你来真的啊!";
    private static String var7 = "为了抗议你的骚扰我决定不理你十分钟,给你个时间反省下自己";

    private static String var20 = "小子,你再摸下试试";
    private static String var21 = "嘿,小子,你再摸,小心我揍你";
    private static String var22 = "能不能别老摸我";
    private static String var23 = "臭小子,我已经忍你很久了";
    private static String var24 = "可以跟我聊天,不要乱摸头";
    private static String var25 = "小子,你摸爽了没有";
    private static String var26 = "不要一直摸人家的头,会变笨的";
    private static String var27 = "还摸,看你的意思是想打架喽";
    private static String var28 = "你刚才上厕所有没有洗手";

    private static String var_network1 = "情已断线,爱已停机,感情不再服务区,亲,我们之间的网络又断了";
    private static String var_network2 = "友谊的小船说翻就翻,你家网络说断就断";
    private static String var_network3 = "网络又断了,敢不敢找个结实的网络把我捆起来";
    private static String var_network4 = "世界上最遥远的距离是:我看着你,却不能理你,因为你家网络又断了";
    private static String var_network5 = "步子迈大啦,把网扯断了";
    private static String var_network6 = "人生最大的遗憾是,人死了钱还没花完,人生最大的痛苦是人活着,却没有网络,亲,你家网络又断了";
    private static String var_network7 = "网络断了,快帮我连接新网络";

    private static Random random = new Random();
    private static int anInt = -1;
    private static ScoffEntity mScoffEntity = new ScoffEntity();


    public static ScoffEntity getResponse(boolean b){
        mScoffEntity.setDormant(false);
        anInt = anInt + 1;
        switch (anInt){
            case 0:
                mScoffEntity.setVar(var0);
                return mScoffEntity;
            case 1:
                mScoffEntity.setVar(var1);
                return mScoffEntity;
            case 2:
                mScoffEntity.setVar(var2);
                return mScoffEntity;
            case 3:
                mScoffEntity.setVar(var3);
                return mScoffEntity;
            case 4:
                mScoffEntity.setVar(var4);
                return mScoffEntity;
            case 5:
                mScoffEntity.setVar(var5);
                return mScoffEntity;
            case 6:
                mScoffEntity.setVar(var6);
                return mScoffEntity;
            case 7:
                anInt = -1;
                mScoffEntity.setDormant(true);
                mScoffEntity.setVar(var7);
                return mScoffEntity;
        }
        return null;
    }

    public static String getResponse(){
        anInt = -1;
        switch (Math.abs(random.nextInt())%9){
            case 0:
                return var20;
            case 1:
                return var21;
            case 2:
                return var22;
            case 3:
                return var23;
            case 4:
                return var24;
            case 5:
                return var25;
            case 6:
                return var26;
            case 7:
                return var27;
            case 8:
                return var28;
            
        }
        return null;
    }

    public static String getBrokenNetwork(){
//        switch (Math.abs(random.nextInt())%6){
//            case 0:
//                return var_network1;
//            case 1:
//                return var_network2;
//            case 2:
//                return var_network3;
//            case 3:
//                return var_network4;
//            case 4:
//                return var_network5;
//            case 5:
//                return var_network6;
//
//        }
        return var_network7;
    }

}
