package com.tobot.tobot.control.demand;

import android.content.Context;
import android.util.Log;

import com.tobot.tobot.utils.CommonRequestManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by YF-04 on 2017/11/3.
 */

public class DemandUtils {
    private static final String TAG = "DemandUtils";


    public static final String DANCE_BACKGROUND_MUSIC_Dir="TuubaDanceBackgroundMusic";
    public static final String DANCE_CONFIG_DIR="TuubaDanceConfig";
    public static final String DANCE_CONFIG_FILE_NAME="danceActionConfig";


    private Map<Integer ,String> actionMap;
    private CommonRequestManager manager;
    private Context mContext;

    private static boolean isConfigChange;

    public DemandUtils(Context context){
        this.mContext=context;
        manager=CommonRequestManager.getInstanse(mContext);

    }

    /**
     * 初始化（读取）配置文件：配置文件中包含背景音乐与舞蹈动作的对应关系.
     * eg: 卓依婷-生日歌.mp3 ,131
     * @param fileName :The name of the configuration file
     * @return
     * @throws Exception
     */
    public Map<Integer,String> initActionConfig(File fileName) throws Exception {
        if (fileName == null) {
            throw new Exception("Illegal fileName: fileName is null!");
        }
        if (!fileName.exists()) {
            throw new Exception("Illegal fileName:  fileName is not exist!");
        }
        if (actionMap == null) {
            actionMap = new HashMap<>();
        }
        if (!actionMap.isEmpty()) {
            actionMap.clear();
        }

        FileInputStream is = null;
        BufferedReader br = null;
        String line = "";
        try {
            is = new FileInputStream(fileName);
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp != null && temp.length == 2) {
                    actionMap.put(Integer.valueOf(temp[1].trim()), temp[0].trim());
                } else {
                    throw new Exception("There are some errors in your configuration file:" + DANCE_CONFIG_FILE_NAME);
                }
            }
            br.close();
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return actionMap;
    }

    /**
     * 初始化（读取）配置文件：配置文件中包含背景音乐与舞蹈动作的对应关系;
     * 默认的文件路径：内置sdcard卡中的 TuubaDanceConfig/danceActionConfig
     */
    public Map<Integer, String> initActionConfig()throws Exception{
        Log.d(TAG, "initActionConfig: ");
        String configFileName=DANCE_CONFIG_DIR+ File.separator+DANCE_CONFIG_FILE_NAME;
        File file=manager.getSDcardFile(configFileName);
        Log.d(TAG, "file.getAbsolutePath(): "+file.getAbsolutePath());
        return initActionConfig(file);
    }

    public static synchronized boolean isConfigChange() {
        return isConfigChange;
    }

    public static synchronized void setIsConfigChange(boolean isConfigChange) {
        DemandUtils.isConfigChange = isConfigChange;
    }

    public void download(String url,File targetDir){

    }

}
