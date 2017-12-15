package com.tobot.tobot.scene;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tobot.tobot.entity.DetailsEntity;
import com.tobot.tobot.entity.SongEntity;
import com.tobot.tobot.entity.StoryEntity;
import com.tobot.tobot.presenter.ICommon.ISceneV;
import com.tobot.tobot.utils.CommonRequestManager;
import com.tobot.tobot.utils.TobotUtils;
import com.turing123.robotframe.multimodal.Behavior;
import com.turing123.robotframe.scenario.IScenario;
import com.turing123.robotframe.scenario.ScenarioRuntimeConfig;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.track.SearchTrackList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by YF-03 on 2017/8/31.
 */

public class StoryScenario implements IScenario{
    private static final String TAG = "StoryScenario";
    private static String APPKEY = "os.sys.story";
    private Context mContext;
    private ISceneV mISceneV;
    private MediaPlayer mediaPlayer;
    private String interrupt;
    private boolean createState;
    private DetailsEntity details;
    private StoryEntity storyEntity;

    /**
     * mohuaiyuan : 故事名称
     */
    private String storyName;
    private CommonRequestManager manager;

    private Map<String, String> specificParams;
    private List<Track> tracks=new ArrayList<>();
    private int categoryId;
    private int calcDimension;
    private int successCount;


    private MyHandler myHandler;

    int position=-1;

//    public StoryScenario(Context context){
//        Log.d(TAG, "StoryScenario: ");
//        this.mContext=context;
//        myHandler=new MyHandler();
//        specificParams=new HashMap<>();
//        initXimalaya();
//        initListener();
//    }


    public StoryScenario(ISceneV mISceneV){
        Log.d(TAG, "StoryScenario: ");
        this.mContext = (Context)mISceneV;
        this.mISceneV = mISceneV;

        myHandler=new MyHandler();
        specificParams=new HashMap<>();
        initXimalaya();


    }

    /**
     * 初始化 喜马拉雅 环境
     */
    public void initXimalaya() {
        Log.d(TAG, "initXimalaya: ");
        manager= CommonRequestManager.getInstanse(mContext);
        manager.initXimalaya();

    }

    private void initListener() {
        Log.d(TAG, "initListener: ");
        manager.setSearchTrackListIDataCallBack(new CommonRequestManager.SearchTrackListIDataCallBack() {
            @Override
            public void onSuccess(List<Track> tracks) {
                Log.d(TAG, "manager.setSearchTrackListIDataCallBack onSuccess(List<Track> tracks): ");
                Log.d(TAG, "tracks.size(): "+tracks.size());
                try {
                    initVoice();
                } catch (Exception e) {
                    e.printStackTrace();

                    Log.e(TAG, "Exception e.getMessage(): "+e.getMessage());
                    return;
                }

                Message message = new Message();
                message.what = TO_EXECUTE_STORY;
                myHandler.sendMessage(message);

            }

            @Override
            public void onSuccess(SearchTrackList searchTrackList) {
                Log.d(TAG, "manager.setSearchTrackListIDataCallBack onSuccess(SearchTrackList searchTrackList): ");
                List<Track> tempTrack = searchTrackList.getTracks();
                List<Track> resultTrack = new ArrayList<Track>();
                Log.d(TAG, "tempTrack.size(): " + tempTrack.size());
                Iterator iterator = tempTrack.iterator();
                while (iterator.hasNext()) {
                    Track track = (Track) iterator.next();
                    int duration = track.getDuration();
                    //mohuaiyuan 过滤掉  时间小于1 分钟 的故事
                    if (duration > 60) {
                        resultTrack.add(track);
                    }

                }
                Log.d(TAG, "resultTrack.size(): " + resultTrack.size());
                tracks.addAll(resultTrack);

//                tracks.addAll(searchTrackList.getTracks());

            }

            @Override
            public void onError(int code, String message) {
                Log.d(TAG, " manager.setSearchTrackListIDataCallBack  onError: ");
                Log.d(TAG, "code = [" + code + "], message = [" + message + "]");

            }
        });

        manager.setTrackListIDataCallBack(new CommonRequestManager.TrackListIDataCallBack() {
            @Override
            public void onSuccess(List<Track> tracks) {
                Log.d(TAG, " manager.setTrackListIDataCallBack  onSuccess(List<Track> tracks): ");
                Log.d(TAG, "tracks.size(): "+tracks.size());

                Random random=new Random();
                position=random.nextInt(tracks.size());

                try {
                    initVoice(position);

                    Log.d(TAG, "position: "+position);
                    Log.d(TAG, "track title: "+tracks.get(position).getTrackTitle());
                    Log.d(TAG, "duration: "+tracks.get(position).getDuration());

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception e: "+e.getMessage() );
                }
                Message message = new Message();
                message.what = TO_EXECUTE_STORY;
                myHandler.sendMessage(message);

            }

            @Override
            public void onSuccess(TrackList trackList) {
                Log.d(TAG, "manager.setTrackListIDataCallBack  onSuccess(TrackList trackList): ");
                tracks.addAll(trackList.getTracks());
            }

            @Override
            public void onError(int code, String message) {
                Log.d(TAG, "manager.setTrackListIDataCallBack  onError: ");
                Log.d(TAG, "code = [" + code + "], message = [" + message + "]");
            }
        });


    }


    @Override
    public void onScenarioLoad() {
    }

    @Override
    public void onScenarioUnload() {
    }

    @Override
    public boolean onStart() {
        return true;
    }

    @Override
    public boolean onExit() {
        Log.d(TAG, "onExit: ");
        Log.d(TAG, "退出故事场景 ");

        manager.mediaPlayonExit(mediaPlayer);
        return true;
    }

    private static final int TO_EXECUTE_STORY=23;
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TO_EXECUTE_STORY:
                    executeVoice();

                    break;
                default:
            }
        }
    }

    @Override
    public boolean onTransmitData(Behavior behavior) {
        Log.d(TAG, "onTransmitData: ");
        if (behavior.results != null) {
            Log.i("Javen","进入故事场景.......");
            Log.d(TAG, "进入故事场景....... ");

            initListener();
            //用于跟踪代码
            manager.setTAG(TAG);


//            mISceneV.getScenario("os.sys.song");
            Behavior.IntentInfo intent = behavior.intent;
            JsonObject parameters = intent.getParameters();
//            songEntity = new Gson().fromJson(parameters, SongEntity.class);
            storyEntity = new Gson().fromJson(parameters, StoryEntity.class);
            Log.d(TAG, "storyEntity: "+storyEntity);

            //TODO mohuaiyuan 201708 根据故事名 搜索故事播放资源(playUrl)
            if (storyEntity==null){
                Log.e(TAG, "songEntity==null: " );
                return false;
            }
            storyName=storyEntity.getStory();

            if(!tracks.isEmpty()){
                tracks.clear();
            }
            Log.d(TAG, "storyName: "+storyName);
            try {
                if (storyName==null || storyName.length()<1){
                    searchVoiceByName();
                }else {
                    searchVoiceByName(storyName);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception e: "+e.getMessage() );
                return false;
            }


        }
        return true;
    }


    public void searchVoiceByName(String voiceName) throws Exception{
        Log.d(TAG, "searchVoiceByName(String voiceName): ");

        if(voiceName==null || voiceName.length()<1){
            Log.e(TAG, "voiceName==null || voiceName.length()<1: " );
            return ;
        }
//        Log.d(TAG, "voiceName: "+voiceName);

        //分类ID，不填或者为0检索全库
        categoryId=0;
        //排序条件：2-最新，3-最多播放，4-最相关（默认）
        calcDimension=4;

        manager.searchVoice(voiceName,categoryId,calcDimension);

    }

    public void  searchVoiceByName()throws Exception{
        Log.d(TAG, "searchVoiceByName(): ");
        if(!tracks.isEmpty()){
            tracks.clear();
        }
        manager.getVoiceList(true,6,1,"故事");
    }

    private void initVoice() throws Exception{
        Log.d(TAG, "initVoice() : ");


//        //mohuaiyuan 按播放次数 降序排列
        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track o1, Track o2) {
                if (o1.getPlayCount()>o2.getPlayCount()){
                    return -1;
                }else if (o1.getPlayCount()==o2.getPlayCount()){
                    return 0;
                }else {
                    return 1;
                }
            }
        });



        //TODO mohuaiyuan 在这里添加 筛选故事
        for(int i=0;i<tracks.size();i++){
            if (tracks.get(i).getTrackTitle().toLowerCase().contains(storyName.toLowerCase())){
                position=i;
                break;
            }
        }

        if (position==-1){
            position=0;
        }

        Log.d(TAG, "position: "+position);
        Log.d(TAG, "track title: "+tracks.get(position).getTrackTitle());
        Log.d(TAG, "duration: "+tracks.get(position).getDuration());

        initVoice(position);

    }

    private void initVoice(int position) throws Exception{
        Log.d(TAG, "initVoice(int position): ");

        String playUrl="";
        try {
            playUrl=tracks.get(position).getPlayUrl32();
            if(playUrl==null){
                playUrl=tracks.get(position).getPlayUrl64();
            }
            if(playUrl==null){
                playUrl=tracks.get(position).getPlayUrl24M4a();
            }
            if(playUrl==null){
                playUrl=tracks.get(position).getPlayUrl64M4a();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onSuccess: "+e.getMessage() );
        }

        if (playUrl==null){
            throw new Exception("init voice error ,all of the playUrl is null! ");
//            Log.e(TAG, "init Track error ,all of the playUrl is null! " );

        }

        storyEntity.setUrl(playUrl);
    }


    @Override
    public boolean onUserInterrupted(int i, Bundle bundle) {
        try{
            Log.i("Javen", bundle.toString() + "..." + i);
            Log.d(TAG, "onUserInterrupted: ");
            interrupt = bundle.getString("interrupt_extra_voice_cmd");
            if (TobotUtils.isNotEmpty(interrupt) && i == 1) {
                try {
                    if (interrupt.contains("暂停")) {
                        Log.i("Javen", "暂停");
                        Log.d(TAG, "暂停: ");
                        mediaPlayer.pause();
                    }
                    if (interrupt.contains("不想听了") || interrupt.contains("好了") || interrupt.contains("可以了")) {
                        Log.i("Javen", "不想听了");
                        Log.d(TAG, "不想听了 or 好了 or 可以了: ");
                        mediaPlayer.stop();
                        //mohuaiyuan 注释：不要退出场景
//                    //退出当前场景
//                    onExit();
                    }
                    if (interrupt.contains("继续") && !mediaPlayer.isPlaying()) {
                        Log.i("Javen", "继续");
                        Log.d(TAG, "继续: ");
                        mediaPlayer.start();
                    }
                    //mohuaiyuan 暂时不用
                    if (interrupt.contains("退出")) {
                        Log.d(TAG, "退出: ");
                        onExit();
                    }
                    //mohuaiyuan 什么也不做 20170914
                    if (interrupt.contains("推出")){
                        Log.d(TAG, "推出: ");

                    }
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
        }catch(Exception e){

        }
        return true;
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
        scenarioRuntimeConfig.addInterruptCmd("退出");
        scenarioRuntimeConfig.addInterruptCmd("推出");
        return scenarioRuntimeConfig;
    }



    private void executeVoice(){
        Log.d(TAG, "executeVoice(): ");

        try {
            String url=storyEntity.getUrl();
            Log.d(TAG, "url: "+url);
            executeSong(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void executeSong(String url) throws IOException {
        if (TobotUtils.isEmpty(mediaPlayer)) {
            mediaPlayer = manager.createNetMp3(url);
            createState = true;
        } else{
            mediaPlayer.release();//释放音频资源
            mediaPlayer = manager.createNetMp3(url);
            createState = true;
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "mediaPlayer==null: "+(mediaPlayer==null));

        //当播放完音频资源时，会触发onCompletion事件，可以在该事件中释放音频资源，
        //以便其他应用程序可以使用该资源:
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "mediaPlayer onCompletion: ");
                //mohuaiyuan 注释 ：不要退出场景
//                //退出故事场景
                onExit();
//                mp.release();//释放音频资源
//                Log.i("Javen", "资源已经被释放了");
                //重新播放当前的故事
//                replayStory();
//                //播放下一个故事
//                playNextStory();


            }
        });
        //在播放音频资源之前，必须调用Prepare方法完成些准备工作
        if (createState) {
            mediaPlayer.prepare();
        }
        //开始播放音频
        mediaPlayer.start();
    }

    /**
     * 播放当前的故事
     */
    private void replayStory() {
        Log.d(TAG, "replayStory: ");
        Message message = new Message();
        message.what = TO_EXECUTE_STORY;
        myHandler.sendMessage(message);
    }

    /**
     * 播放下一个故事
     */
    private void playNextStory(){
        Log.d(TAG, "playNextStory: ");

        int size=tracks.size();
        if (size==1){

        }else {
            position=position+1;
            if (position>=size){
                position-=size;
            }
        }
        try {
            initVoice(position);

            Log.d(TAG, "position: "+position);
            Log.d(TAG, "track title: "+tracks.get(position).getTrackTitle());
            Log.d(TAG, "duration: "+tracks.get(position).getDuration());
        } catch (Exception e) {
            e.printStackTrace();

            Log.e(TAG, "Exception e.getMessage(): " + e.getMessage());
            return;
        }

        Message message = new Message();
        message.what = TO_EXECUTE_STORY;
        myHandler.sendMessage(message);

    }

    public void setStoryName(String storyName) {
        this.storyName = storyName;
    }
}
