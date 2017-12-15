package com.tobot.tobot.scene;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tobot.tobot.MainActivity;

import com.tobot.tobot.Listener.SimpleFrameCallback;
import com.tobot.tobot.entity.DetailsEntity;
import com.tobot.tobot.entity.SongEntity;
import com.tobot.tobot.presenter.ICommon.ISceneV;
import com.tobot.tobot.utils.CommonRequestManager;
import com.tobot.tobot.utils.TobotUtils;
import com.turing123.robotframe.function.motor.Motor;
import com.turing123.robotframe.function.tts.TTS;
import com.turing123.robotframe.multimodal.Behavior;
import com.turing123.robotframe.multimodal.action.Action;
import com.turing123.robotframe.multimodal.action.BodyActionCode;
import com.turing123.robotframe.scenario.IScenario;
import com.turing123.robotframe.scenario.ScenarioManager;
import com.turing123.robotframe.scenario.ScenarioRuntimeConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Javen on 2017/8/31.
 */

public class DanceScenario implements IScenario{
    private static final String TAG = "DanceScenario";
    private static String APPKEY = "os.sys.dance";

    public static final String DANCE_BACKGROUND_MUSIC_Dir="TuubaDanceBackgroundMusic";
    public static final String DANCE_CONFIG_DIR="TuubaDanceConfig";
    public static final String DANCE_CONFIG_FILE_NAME="danceActionConfig";


    private Context mContext;
    private ISceneV mISceneV;
    private MediaPlayer mediaPlayer;
    private String interrupt;
    private boolean createState;
    private DetailsEntity details;
    private SongEntity songEntity;

    /**
     * mohuaiyuan : 歌曲名称
     */
    private String songName;
    private String singer;

    private CommonRequestManager manager;

    private int position;

    private List<String> musicNames;
    /**
     * 存放机器人舞蹈动作和背景音乐的对应关系
     */
    Map<Integer,String>actionMap;

    //mohuaiyuan 仅仅用于测试
//    public DanceScenario(Context context){
//        this.mContext=context;
//
//        manager=CommonRequestManager.getInstanse(mContext);
//        manager.setTAG(TAG);
//    }
    private TTS tts;
    private Motor motor;
    private ScenarioManager scenarioManager;

    public DanceScenario(ISceneV mISceneV){
        Log.d(TAG, "DanceScenario: ");
        this.mContext = (Context)mISceneV;
        this.mISceneV = mISceneV;

        manager=CommonRequestManager.getInstanse(mContext);
        actionMap=new HashMap<>();
        manager.setTAG(TAG);


        scenarioManager = new ScenarioManager(mContext);
    }

    @Override
    public void onScenarioLoad() {
    }

    @Override
    public void onScenarioUnload() {
    }

    @Override
    public boolean onStart() {
        motor = new Motor(mContext, new CustomScenario(mContext));
        tts = new TTS(mContext,new BaseScene(mContext,"os.sys.chat"));
        return true;
    }

    @Override
    public boolean onExit() {
        Log.d(TAG, "onExit: ");
        Log.d(TAG, "退出舞蹈场景 ");

        try {
            if (mediaPlayer!=null &&mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.release();//释放资源
            }
            mediaPlayer = null;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
		
        //scenarioManager.quitCurrentScenario();

//        manager.mediaPlayonExit(mediaPlayer);

        return true;
    }


    @Override
    public boolean onTransmitData(Behavior behavior) {
        Log.d(TAG, "onTransmitData: ");
        if (behavior.results != null) {
            Log.i("Javen","进入跳舞场景.......");
            Log.d(TAG, "进入跳舞场景.......: ");
//            mISceneV.getScenario("os.sys.song");

            Behavior.IntentInfo intent = behavior.intent;
            JsonObject parameters = intent.getParameters();
            songEntity = new Gson().fromJson(parameters, SongEntity.class);
            songName=songEntity.getSong();
            singer=songEntity.getSinger();
            Log.d(TAG, "songName: "+songName);
            Log.d(TAG, "singer: "+singer);


//            initBackgroundMusic();

            try {
                initBackgroundMusic(DANCE_BACKGROUND_MUSIC_Dir);

                initActionConfig();
                Log.d(TAG, "actionMap: "+actionMap.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }

//            //mohuaiyuan 暂时注释 20170926 仅仅用于测试
//            onExit();
            try {
                if (songName==null || songName.length()<1){
                    executeSong();
                }else {
                    prepareExecuteSong(songName);
                    //mohuaiyuan 仅仅用于测试 20170926
//                    prepareExecuteSong("我是一个机器人");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return true;
    }

    private void prepareExecuteSong(String songName) throws Exception{
        Log.d(TAG, "prepareExecuteSong: ");
        if (songName==null || songName.length()<1){
            throw new Exception("Illegal  parameter");
        }
        boolean isContains=false;
        for (int i=0;i<musicNames.size();i++){
            String temp=musicNames.get(i);
            if (temp.toLowerCase().contains(songName) ){
                position=i;
                isContains=true;
                break;
            }
        }
        if (isContains){
            executeSong(position);
        }else {
            executeSong(0);
        }

    }


    @Override
    public boolean onUserInterrupted(int i, Bundle bundle) {
        Log.d(TAG, "onUserInterrupted: ");

        Log.d(TAG, "bundle!=null: " + (bundle != null));
        try {
            if (bundle != null) {
                Log.i("Javen", bundle.toString() + "..." + i);
                interrupt = bundle.getString("interrupt_extra_voice_cmd");
            }

            if (TobotUtils.isNotEmpty(interrupt) && i == 1) {
                try {
                    if (interrupt.contains("暂停")) {
                        Log.d(TAG, "暂停: ");
//                    Log.i("Javen", "暂停");
//                    mediaPlayer.pause();
                    }
                    if (interrupt.contains("不想听了") || interrupt.contains("好了") || interrupt.contains("可以了")) {
                        Log.d(TAG, "不想听了 or 好了  or 可以了: ");
//                    Log.i("Javen", "不想听了");
//                    mediaPlayer.stop();
                    }
                    if (interrupt.contains("继续") && !mediaPlayer.isPlaying()) {
                        Log.d(TAG, "继续: ");
//                    Log.i("Javen", "继续");
//                    mediaPlayer.start();
                    }
                    //mohuaiyuan 仅仅用于测试 20170912
                    if (interrupt.contains("退出") || interrupt.contains("推出")) {
                        Log.d(TAG, "退出: ");
                        beforeExit();
//                    onExit();
                    }

//                //mohuaiyuan 测试 跳下一个舞蹈 ，仅仅用于测试 20170912
//                if (interrupt.contains("再来一个") || interrupt.contains("再跳一个")|| interrupt.contains("跳支舞") ||interrupt.contains("下一个")){
//                    Log.d(TAG, "跳下一个舞蹈: ");
//                    playNextBackgroundMusic();
//                }

//                if (interrupt.contains("快进") || interrupt.contains("前进")) {
//                    int percentage = Integer.parseInt(details.getDuration());
//                    int speed = percentage / 100;
//                    Pattern pattern = Pattern.compile("[^0-9]");
//                    Matcher isNum = pattern.matcher(interrupt);
//                    Log.i("Javen","运动代号..."+ isNum.replaceAll("").trim());
//
////                Pattern pattern = Pattern.compile("[0-9]*");
////                Log.i("Javen", interrupt.substring(2, interrupt.length() - 1));
////                Matcher isNum = pattern.matcher(interrupt.substring(2, interrupt.length() - 1));
////                Log.i("Javen", isNum.matches() + "11");
//                    if (isNum.matches()) {
//                        Log.i("Javen", "快进" + Integer.parseInt(isNum.replaceAll("").trim()) * speed);
//                        mediaPlayer.seekTo(Integer.parseInt(isNum.replaceAll("").trim()) * speed);
//                    }
//                }
                } catch (IllegalStateException e) {


                }
            } else if (TobotUtils.isNotEmpty(bundle.getString("interrupt_extra_touch_keyEvent")) && i == 2) {
                Log.i("Javen", "头部触摸");
            }
        } catch (Exception e) {

        }
        return true;
    }

    private void beforeExit() {
        Log.d(TAG, "beforeExit: ");
        try{
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//                mediaPlayer.stop();
//                mediaPlayer.release();
//                mediaPlayer = null;
            } else {
                Log.d(TAG, "mediaPlayer!=null: " + (mediaPlayer != null));
                onExit();
            }
        }catch (Exception e){

        }

    }

    @Override
    public String getScenarioAppKey() {
        return APPKEY;
    }

    @Override
    public ScenarioRuntimeConfig configScenarioRuntime(ScenarioRuntimeConfig scenarioRuntimeConfig) {
        scenarioRuntimeConfig.allowDefaultChat = false;
        scenarioRuntimeConfig.interruptMatchMode = scenarioRuntimeConfig.INTERRUPT_CMD_MATCH_MODE_FUZZY;
        //为场景添加打断语，asr 识别到打断语时将产生打断事件，回调到场景的onUserInterrupted() 方法。
        scenarioRuntimeConfig.addInterruptCmd("暂停");
        scenarioRuntimeConfig.addInterruptCmd("继续");
        scenarioRuntimeConfig.addInterruptCmd("不想听了");
        scenarioRuntimeConfig.addInterruptCmd("好了");
        scenarioRuntimeConfig.addInterruptCmd("可以了");
        scenarioRuntimeConfig.addInterruptCmd("快进");
        scenarioRuntimeConfig.addInterruptCmd("前进");

//        //mohuaiyuan 仅仅用于测试 20170912
        scenarioRuntimeConfig.addInterruptCmd("退出");
        scenarioRuntimeConfig.addInterruptCmd("推出");
//
//        //mohuaiyuan 测试 跳下一个舞蹈,仅仅用于测试 20170912
//        scenarioRuntimeConfig.addInterruptCmd("再来一个");
//        scenarioRuntimeConfig.addInterruptCmd("再跳一个");
//        scenarioRuntimeConfig.addInterruptCmd("跳支舞");
//        scenarioRuntimeConfig.addInterruptCmd("下一个");


        return scenarioRuntimeConfig;
    }


    /**
     * 初始化背景音乐,获取所有背景音乐的文件路径
     */
    private void initBackgroundMusic() {
        //读取assects 音乐文件 文件名
        String[] fileNames=null;

        try {
            fileNames=mContext.getResources().getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int length=0;length<fileNames.length;length++){
            Log.d(TAG, "fileName: "+fileNames[length]);
        }
        Log.d(TAG, "----------------------------------------: ");

        if (musicNames==null){
            musicNames=new LinkedList<>();
        }
        if (!musicNames.isEmpty()){
            musicNames.clear();
        }


        for (int i=0;i<fileNames.length;i++){
            if (isMusicName(fileNames[i])){
                musicNames.add(fileNames[i]);
            }
        }
        for (int index=0;index<musicNames.size();index++){
            Log.d(TAG, "musicName: "+musicNames.get(index));
        }

    }

    /**
     * 初始化背景音乐：获取所有背景音乐的文件路径
     */
    private void initBackgroundMusic(String musicDir) throws Exception {
        Log.d(TAG, "initBackgroundMusic(String fileName): ");
        //如果手机插入了SD卡，而且应用程序具有访问SD卡的权限
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File musicFile = null;
            musicFile = manager.getSDcardFile(musicDir);
            if(musicFile==null){
                new Exception("There is not DanceBackgroundMusic dir !");
            }

//            String[] fileNames = null;
            File [] files=null;
            boolean exists=musicFile.exists();
            Log.d(TAG, "exists: "+exists);
            if (exists) {
                Log.d(TAG, "musicFile: " + musicFile.getAbsolutePath());
//                fileNames = musicFile.list();
                files=musicFile.listFiles();
            } else {
                new Exception("There is not DanceBackgroundMusic dir !");
            }

//            for (int i=0;i<files.length;i++){
//                Log.d(TAG, "files[i]: "+files[i].getAbsolutePath());
//            }

            if (musicNames == null) {
                musicNames = new LinkedList<>();
            }

            if (!musicNames.isEmpty()){
                musicNames.clear();
            }

            for (int i = 0; i < files.length; i++) {
                musicNames.add(files[i].getAbsolutePath());
            }

            for (int index = 0; index < musicNames.size(); index++) {
                Log.d(TAG, "musicName: " + musicNames.get(index));
            }


        }else {
            new Exception("Ther is not SD card or permission denied");
        }

    }

    /**
     * 初始化（读取）配置文件：配置文件中包含背景音乐与舞蹈动作的对应关系
     */
    private void initActionConfig()throws Exception{
        Log.d(TAG, "initActionConfig: ");
        String configFileName=DANCE_CONFIG_DIR+File.separator+DANCE_CONFIG_FILE_NAME;
        File file=manager.getSDcardFile(configFileName);

        if (file==null){
            throw  new Exception("illegal null file!");
        }

        FileInputStream is=null;
        BufferedReader br=null;
        String line="";
        try {
            is=new FileInputStream(file);
            br=new BufferedReader(new InputStreamReader(is));
            while ((line=br.readLine())!=null){
                String [] temp=line.split(",");
                if (temp!=null && temp.length==2){
                    actionMap.put(Integer.valueOf(temp[1].trim()),temp[0].trim());
                }else {
                    Log.e(TAG, "There are some errors in your configuration file:"+ DANCE_CONFIG_FILE_NAME);
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
                if (br!=null){
                    br.close();
                }
                if (is!=null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    public void executeSong()throws Exception{
        Log.d(TAG, "executeSong(): ");

        Random random=new Random();
        position=random.nextInt(musicNames.size());
        Log.d(TAG, "position: "+position);
        executeSong(position);

    }

    private void executeSong(int position)throws Exception{
        Log.d(TAG, "executeSong(int position): ");
        if (position>=musicNames.size() || position<0){
            Log.e(TAG, "illegal position: " );
            throw new Exception("Illegal position to get musicName!" );
        }

        String musicName=musicNames.get(position);
        Log.d(TAG, "musicName: "+musicName);

        try {
            executeSong(musicName);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "executeSong e: "+e.getMessage() );
        }


    }

    private void executeSong(String fileName) throws IOException {
        Log.d(TAG, "executeSong(String fileName): ");

        if (TobotUtils.isEmpty(mediaPlayer)) {
            mediaPlayer = manager.createNetMp3(fileName);
            createState = true;
        } else{
            mediaPlayer.release();//释放音频资源
            mediaPlayer = manager.createNetMp3(fileName);
            createState = true;
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "mediaPlayer==null: "+(mediaPlayer==null));
        //当播放完音频资源时，会触发onCompletion事件，可以在该事件中释放音频资源，
        //以便其他应用程序可以使用该资源:
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "mediaPlayer.setOnCompletionListener onCompletion: ");

                //mohuaiyuan  20170915 暂时注释
//                mp.release();//释放音频资源
//                Log.i("Javen", "资源已经被释放了");
//                Log.d(TAG, "资源已经被释放了: ");

                //mohuaiyuan 释放资源
                onExit();

               // manager.backMainScenario();
				scenarioManager.quitCurrentScenario();


            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "mediaPlayer.setOnErrorListener : ");
                Log.d(TAG, "what: "+what);
                Log.d(TAG, "extra: "+extra);
                return false;
            }
        });
        //在播放音频资源之前，必须调用Prepare方法完成些准备工作
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "mediaPlayer.setOnPreparedListener onPrepared: ");

                //机器人开始跳舞动作
                int actionCode=getActionCode();
                Log.d(TAG, "actionCode: "+actionCode);
//                tts.speak("好的,我要开始跳了");
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                sendBodyAction(actionCode);

            }
        });
//        if (createState) {
//            mediaPlayer.prepare();
//        }
        //开始播放音频
//        mediaPlayer.start();
    }

    /**
     * 播放当前舞蹈的背景音乐
     */
//    private void replayBackgroundMusic(){
//        Log.d(TAG, "replayBackgroundMusic: ");
//        try {
//            executeSong(position);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    /**
     * 播放下一个舞蹈的背景音乐
     */
//    private void playNextBackgroundMusic(){
//        Log.d(TAG, "playNextBackgroundMusic: ");
//        int musicSize=musicNames.size();
//        if (musicSize<1){
//            Log.e(TAG, "(musicSize<1 " );
//            return;
//        }
//        if (musicSize==1){
//
//        }else {
//            position+=1;
//            if (position>=musicSize){
//                position-=musicSize;
//            }
//        }
//
//        String musicName=musicNames.get(position);
//        Log.d(TAG, "musicName: "+musicName);
//
//        try {
//            executeSong(musicName);
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "executeSong e: "+e.getMessage() );
//        }
//
//    }

    private boolean isMusicName(String fileName) {
        boolean isMusic = false;
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
            isMusic = true;
        }
        return isMusic;
    }


    /**
     *
     * @param bodyActionCode:BodyActionCode.ACTION_6
     */
    private void sendBodyAction(int bodyActionCode ){
        Log.d(TAG, "sendBodyAction: ");
        motor.doAction(Action.buildBodyAction(bodyActionCode,Action.PRMTYPE_EXECUTION_TIMES,1),new SimpleFrameCallback(){
            @Override
            public void onStarted() {
                super.onStarted();
                Log.d(TAG, "onStarted: ");
                try{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            try {
//                                Thread.sleep(1700);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                            //播放背景音乐
                            mediaPlayer.start();
                        }
                    }).start();
                }catch (Exception e){

                }
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(TAG, "onStopped: ");
            }

            @Override
            public void onPaused() {
                super.onPaused();
                Log.d(TAG, "onPaused: ");
            }

            @Override
            public void onResumed() {
                super.onResumed();
                Log.d(TAG, "onResumed: ");
            }

            @Override
            public void onInterrupted() {
                super.onInterrupted();
                Log.d(TAG, "onInterrupted: ");
            }

            @Override
            public void onCompleted() {
                super.onCompleted();
                Log.d(TAG, "onCompleted: ");
            }

            @Override
            public void onError(String s) {
                super.onError(s);
                Log.d(TAG, "onError: "+s);
            }
        });
    }

    private int getActionCode(){
        Log.d(TAG, "getActionCode: ");
        int actonCode=-1;
        Iterator<Integer>iterator= actionMap.keySet().iterator();
        while (iterator.hasNext()){
            int key=iterator.next();
//            Log.d(TAG, "------------------------: ");
//            Log.d(TAG, "key: "+key);
            String musicName=musicNames.get(position).trim();
            String action=actionMap.get(key).trim();
//            Log.d(TAG, "musicName: "+musicName);
//            Log.d(TAG, "action: "+action);
//            Log.d(TAG, "musicName.contains(action): "+musicName.contains(action));
            if (musicName.contains(action)){
                actonCode=key;
                break;
            }
        }
        if (actonCode==-1){
            return -1;
        }
        return actonCode;
    }


}
