package com.tobot.tobot.scene;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tobot.tobot.MainActivity;
import com.tobot.tobot.base.Constants;
import com.tobot.tobot.entity.DetailsEntity;
import com.tobot.tobot.entity.SongEntity;
import com.tobot.tobot.presenter.ICommon.ISceneV;
import com.tobot.tobot.utils.CommonRequestManager;
import com.tobot.tobot.utils.TobotUtils;
import com.turing123.robotframe.function.tts.TTS;
import com.turing123.robotframe.multimodal.Behavior;
import com.turing123.robotframe.scenario.IScenario;
import com.turing123.robotframe.scenario.ScenarioManager;
import com.turing123.robotframe.scenario.ScenarioRuntimeConfig;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.AlbumList;
import com.ximalaya.ting.android.opensdk.model.track.SearchTrackList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;

import java.io.IOException;
import java.lang.reflect.Type;
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
 * Created by Javen on 2017/8/3.
 */

public class SongScenario implements IScenario {

    private static final String TAG = "SongScenario";
    private static String APPKEY = "os.sys.song";
    private Context mContext;
    private MediaPlayer mediaPlayer;
    private String interrupt;
    private boolean createState;
    private DetailsEntity details;
    private SongEntity songEntity;
    private TTS tts;
    private ScenarioManager scenarioManager;


    /**
     * mohuaiyuan : 歌曲名称
     */
    private String songName;
    private String singer;
    private CommonRequestManager manager;

    private Map<String, String> specificParams;

    private List<Album>albums=new ArrayList<>();
    private List<Track> tracks=new ArrayList<>();
    private int categoryId;
    private int calcDimension;
    private int successCount;

    private long albumId;


    private MyHandler myHandler;

    private ISceneV mISceneV;

    private static int defaultPageCount=50;

    int position=-1;

//    public SongScenario(Context context){
//        Log.d(TAG, "SongScenario: ");
//        this.mContext = context;
//        myHandler=new MyHandler();
//        specificParams=new HashMap<>();
//        initXimalaya();
//    }

    public SongScenario(ISceneV mISceneV){
        Log.d(TAG, "SongScenario: ");
        this.mContext = (Context)mISceneV;
        this.mISceneV = mISceneV;

        myHandler=new MyHandler();
        specificParams=new HashMap<>();
        initXimalaya();
		
        tts = new TTS(mContext ,new BaseScene(mContext,"os.sys.chat"));
        scenarioManager = new ScenarioManager(mContext);

    }

    /**
     * 初始化 喜马拉雅 环境
     */
    private void initXimalaya() {
        manager=CommonRequestManager.getInstanse(mContext);
        manager.initXimalaya();
    }

    private void initListener() {
        Log.d(TAG, "initListener: ");
        manager.setSearchTrackListIDataCallBack(new CommonRequestManager.SearchTrackListIDataCallBack() {
            @Override
            public void onSuccess(List<Track> trackResultList) {
                Log.d(TAG, "manager.setSearchTrackListIDataCallBack onSuccess(List<Track> tracks): ");

                //mohuaiyuan 仅仅用于测试
//                if (!tracks.isEmpty()){
//                    tracks.clear();
//                }

                int trackSize=tracks.size();
                try {
                    if (trackSize>0){

                        initTrack();

                        Message message = new Message();
                        message.what = TO_EXECUTE_SONG;
                        myHandler.sendMessage(message);

                    }else {

                        //默认的歌曲
                        executeSong();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception e.getMessage(): " + e.getMessage());
                    return;
                }


            }

            @Override
            public void onSuccess(SearchTrackList searchTrackList) {
                Log.d(TAG, "manager.setSearchTrackListIDataCallBack onSuccess(SearchTrackList searchTrackList): ");

                List<Track> tempTrack = searchTrackList.getTracks();
                List<Track> resultTrack =musicFilter(tempTrack);
                if (resultTrack==null){
                    Log.d(TAG, "resultTrack==null: ");
                }else {
                    Log.d(TAG, "resultTrack.size(): " + resultTrack.size());
                }
                tracks.addAll(resultTrack);

                //tracks.addAll(searchTrackList.getTracks());

            }

            @Override
            public void onError(int code, String message) {
                Log.d(TAG, "manager.setSearchTrackListIDataCallBack onError(int code, String message): ");
                Log.d(TAG, "code = [" + code + "], message = [" + message + "]");
            }
        });

        manager.setTrackListIDataCallBack(new CommonRequestManager.TrackListIDataCallBack() {
            @Override
            public void onSuccess(List<Track> trackResultList) {
                Log.d(TAG, "manager.setTrackListIDataCallBack onSuccess(List<Track> tracks): ");

                //mohuaiyuan 仅仅用于测试
//                if (!tracks.isEmpty()){
//                    tracks.clear();
//                }

                int trackSize=tracks.size();
                try {
                    if (trackSize>0) {
                        Random random=new Random();
                        position = random.nextInt(tracks.size());
                        Log.d(TAG, "tracks.size(): " + trackSize);
                        initTrack(position);

                        Log.d(TAG, "position: " + position);
                        Log.d(TAG, "track title: " + tracks.get(position).getTrackTitle());
                        Log.d(TAG, "duration: " + tracks.get(position).getDuration());

                        Message message = new Message();
                        message.what = TO_EXECUTE_SONG;
                        myHandler.sendMessage(message);

                    }else {
                        //当前的专辑没有歌曲
//                        beyond 真的爱你
//                        String playUrl = "http://fdfs.xmcdn.com/group9/M04/22/52/wKgDZlboEhbAZLxFABEBSwrVj1Y718.mp3";
//                        initTrack(playUrl);
                        //唱默认的歌曲
                        executeSong();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception e: "+e.getMessage() );
                }

            }

            @Override
            public void onSuccess(TrackList trackList) {
                Log.d(TAG, "manager.setTrackListIDataCallBack onSuccess(SearchTrackList searchTrackList): ");
                //过滤掉时间小于1分钟和大于6分钟的歌曲
//                List<Track> tempTrack = trackList.getTracks();
//                List<Track> resultTrack =musicFilter(tempTrack);
//                if (resultTrack==null){
//                    Log.d(TAG, "resultTrack==null: ");
//                }else {
//                    Log.d(TAG, "resultTrack.size(): " + resultTrack.size());
//                }
//                tracks.addAll(resultTrack);

                tracks.addAll(trackList.getTracks());
            }

            @Override
            public void onError(int code, String message) {
                Log.d(TAG, "manager.setTrackListIDataCallBack onError(int code, String message): ");
                Log.d(TAG, "code = [" + code + "], message = [" + message + "]");
            }
        });

    }

    /**
     * 音乐过滤器：对音乐进行过滤，过滤掉时间小于1分钟和大于6分钟的歌曲
     * @param songList
     * @return
     */
    private List<Track> musicFilter(List<Track> songList){
        Log.d(TAG, "musicFilter: ");

        if (songList==null || songList.isEmpty()){
            return null;
        }

        List<Track> resultTrack = new ArrayList<Track>();
        Log.d(TAG, "songList.size(): " + songList.size());
        Iterator iterator = songList.iterator();
        while (iterator.hasNext()) {
            Track track = (Track) iterator.next();
            int duration = track.getDuration();
//            Log.d(TAG, "duration: "+duration);
            //mohuaiyuan 过滤掉  时间小于1分钟和大于 6分钟的歌曲
            if (duration > 60 && duration < 360) {
                resultTrack.add(track);
            }

        }

        return  resultTrack;


    }

    @Override
    public void onScenarioLoad() {
        Log.d(TAG, "加载场景: ");
        Log.i("Javen","加载场景");
    }

    @Override
    public void onScenarioUnload() {
        Log.d(TAG, "卸载场景: ");
        mISceneV.getScenario("os.sys.song_stop");
        Log.i("Javen","卸载场景");

    }

    @Override
    public boolean onStart() {
        Log.d(TAG, "开始: ");
        Log.i("Javen","开始");
        return true;
    }

    @Override
    public boolean onExit() {
		Log.d(TAG, "onExit: ");
        Log.d(TAG, "退出音乐场景: ");
        mISceneV.getScenario("os.sys.song_stop");
        if (mediaPlayer!=null &&mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        mediaPlayer = null;

//        manager.mediaPlayonExit(mediaPlayer);

//        manager.backMainScenario();

        // 调用quitCurrentScenario 退出当前场景，恢复NLP处理。
         scenarioManager.quitCurrentScenario();
        return true;
    }


    private static final int TO_EXECUTE_SONG=23;
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TO_EXECUTE_SONG:
                    executeSong();

                    break;
                default:
            }
        }
    }

    @Override
    public boolean onTransmitData(Behavior behavior) {
        if (behavior.results != null) {
            Log.i("Javen","进入唱歌场景.......");
            Log.d(TAG, "进入唱歌场景....... ");

            initListener();
            //用于跟踪代码
            manager.setTAG(TAG);
			
            Log.i("Javen","进入唱歌场景......."+behavior.toString());
            mISceneV.getScenario("os.sys.song");

            Behavior.IntentInfo intent = behavior.intent;
            JsonObject parameters = intent.getParameters();
            //TODO  mohuaiyuan operateState 作用未知
//            String operateState=intent.getOperateState();
//            Log.d(TAG, "operateState: "+operateState);
            songEntity = new Gson().fromJson(parameters, SongEntity.class);

            //TODO mohuaiyuan 201708 根据歌名 搜索歌曲播放资源(playUrl)
            if (songEntity==null){
                Log.e(TAG, "songEntity==null: " );
                return false;
            }
            songName=songEntity.getSong();
            singer=songEntity.getSinger();

            Log.d(TAG, "songName: "+songName);

            //mohuaiyuan 先用图灵的资源 20170922
            executeSong();

//            try {
//                //无歌名，无歌手名 则播放任意的一首歌
//                if ((songName == null || songName.length() < 1) && (singer == null || singer.length() < 1)) {
//                    searchSongByName();
//                } else {
//                    //无歌名，有歌手名，则用歌手名搜索歌曲
//                    if ((songName == null || songName.length() < 1) && (singer != null && singer.length() > 0)) {
//                        songName = singer;
//                    }
//                    //根据歌曲名称搜索歌曲
//                    searchSongByName(songName);
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            }


        }
        return true;
    }


    public void searchSongByName(String songName) throws Exception{
        Log.d(TAG, "searchSongByName(String songName) ");

        if(songName==null || songName.length()<1){
            Log.e(TAG, "songName==null || songName.length()<1: " );
            return ;
        }
        if(songName!=null){
            Log.d(TAG, "songName: "+songName);
        }

        if(!tracks.isEmpty()){
            tracks.clear();
        }

        //声音类别：2-音乐
        categoryId=2;
        //排序条件：2-最新，3-最多播放，4-最相关（默认）
        calcDimension=4;

        manager.searchVoice(songName,categoryId,calcDimension);

    }

    public void searchSongByName() throws Exception{
        Log.d(TAG, "searchSongByName(): ");
        if(!tracks.isEmpty()){
            tracks.clear();
        }
        manager.getVoiceList(true,2,1,"");

    }


    private void initTrack() throws Exception{
        Log.d(TAG, "initTrack(): ");

        //mohuaiyuan 不进行排序 效果会好一点，进行排序并没有明显的效果 20170913
        //mohuaiyuan 按歌曲播放次数 降序排列
//        Collections.sort(tracks, new Comparator<Track>() {
//            @Override
//            public int compare(Track o1, Track o2) {
//                if (o1.getPlayCount()>o2.getPlayCount()){
//                    return -1;
//                }else if (o1.getPlayCount()==o2.getPlayCount()){
//                    return 0;
//                }else {
//                    return 1;
//                }
//            }
//        });


        position=-1;
        //TODO mohuaiyuan 在这里添加 筛选歌曲
        for(int i=0;i<tracks.size();i++){
            if (tracks.get(i).getTrackTitle().toLowerCase().contains(songName.toLowerCase())){
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

        initTrack(position);

    }


    private void initTrack(int position) throws Exception{
        Log.d(TAG, "initTrack(int position): ");
        String playUrl=null;
        try {
            playUrl=tracks.get(position).getPlayUrl32();
            if(playUrl==null || playUrl.length()<1){
                playUrl=tracks.get(position).getPlayUrl64();
            }
            if(playUrl==null || playUrl.length()<1){
                playUrl=tracks.get(position).getPlayUrl24M4a();
            }
            if(playUrl==null || playUrl.length()<1){
                playUrl=tracks.get(position).getPlayUrl64M4a();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onSuccess: "+e.getMessage() );
        }

        if (playUrl==null){
            throw new Exception("init Track error ,all of the playUrl is null! ");
//            Log.e(TAG, "init Track error ,all of the playUrl is null! " );

        }
        initTrack(playUrl);


    }

    private void initTrack(String playUrl)throws  Exception{
        Log.d(TAG, "initTrack(String playUrl): ");
        if (playUrl==null || playUrl.isEmpty()){
            throw new Exception("init Track error: playUrl==null || playUrl.isEmpty() ");
        }
        DetailsEntity detailsEntity=new DetailsEntity();
        detailsEntity.setTrack_url(playUrl);

        List<DetailsEntity> playList=new ArrayList<DetailsEntity>();
        playList.add(detailsEntity);
        songEntity.setPlayList(playList);
    }

    @Override
    public boolean onUserInterrupted(int i, Bundle bundle) {
        Log.d(TAG, "onUserInterrupted: ");
        try {
            if (bundle != null) {
                Log.i("Javen", bundle.toString() + "..." + i);
                interrupt = bundle.getString("interrupt_extra_voice_cmd");
            }

            if (TobotUtils.isNotEmpty(interrupt) && i == 1) {
                try {
                    if (interrupt.contains("暂停")) {
                        Log.i("Javen", "暂停");
                        Log.d(TAG, "暂停: ");
                        mISceneV.getScenario("os.sys.song_stop");
                        mediaPlayer.pause();
                    }
                    if (interrupt.contains("不想听了") || interrupt.contains("好了") || interrupt.contains("可以了")) {
                        Log.i("Javen", "不想听了");
                        Log.d(TAG, "不想听了: ");
                        mISceneV.getScenario("os.sys.song_stop");
                        mediaPlayer.stop();
                        //退出音乐场景
//                    onExit();
//                    manager.backMainScenario();
                        scenarioManager.quitCurrentScenario();

                    }
                    if (interrupt.contains("继续") && !mediaPlayer.isPlaying()) {
                        Log.i("Javen", "继续");
                        Log.d(TAG, "继续: ");
                        mISceneV.getScenario("os.sys.song");
                        mediaPlayer.start();
                    }
                    if (/*interrupt.contains("图巴") || */interrupt.contains("你好小图")) {
                        Log.i("Javen", "图巴,小图,退出场景");
                        Log.d(TAG, "图巴,小图,退出场景: ");
                        Log.d(TAG, "图巴 :关键词------这里调用onexit方法了 ");
                        this.onExit();

                        mISceneV.getScenario("os.sys.song_stop");
//                    manager.backMainScenario();
//                        scenarioManager.quitCurrentScenario();
                    }

                    //mohuaiyuan 暂时不用
                    if (interrupt.contains("退出")) {
                        Log.d(TAG, "退出: ");
                        Log.d(TAG, "退出 ：关键词------这里调用onexit方法了 ");
                        onExit();
//                    manager.backMainScenario();
//                        scenarioManager.quitCurrentScenario();
                    }
                    //mohuaiyuan 什么也不做 20170914
                    if (interrupt.contains("推出")) {
                        Log.d(TAG, "推出: ");

                    }


                } catch (IllegalStateException e) {

                }
            } else if (TobotUtils.isNotEmpty(bundle.getString("interrupt_extra_touch_keyEvent")) && i == 2) {
                Log.i("Javen", "进入打断处理.................");
                Log.d(TAG, "进入打断处理.................");
                this.onExit();
            }
        } catch (NullPointerException e) {
            if (TobotUtils.isEmpty(bundle) && i == 2) {
                Log.i("Javen", "进入打断处理......2...........");
                Log.d(TAG, "进入打断处理......2...........");
                this.onExit();
            }
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
//        scenarioRuntimeConfig.addInterruptCmd("快进");
//        scenarioRuntimeConfig.addInterruptCmd("前进");
        //scenarioRuntimeConfig.addInterruptCmd("图巴");
        //scenarioRuntimeConfig.addInterruptCmd("小图");
        scenarioRuntimeConfig.addInterruptCmd("退出");
        scenarioRuntimeConfig.addInterruptCmd("推出");
        scenarioRuntimeConfig.addInterruptCmd("你好小图");
//        scenarioRuntimeConfig.addInterruptCmd("退出");
        return scenarioRuntimeConfig;
    }

    private void executeSong(){
        Log.d(TAG, "executeSong(): ");

        boolean isExecuteSuccess=true;
        try {
            Log.d(TAG, "准备。。播放喜马拉雅的歌曲: ");
            if (TobotUtils.isNotEmpty(songEntity.getPlayList().get(0))) {
                details = songEntity.getPlayList().get(0);
                Log.d(TAG, "播放喜马拉雅的歌曲: ");
                Log.d(TAG, "歌曲playUrl: "+details.getTrack_url());

                executeSong(details.getTrack_url());
            }
        } catch (Exception e) {
            e.printStackTrace();
            isExecuteSuccess=false;
            Log.d(TAG, "Exception e.getMessage(): "+e.getMessage());
            Log.d(TAG, "Exception e.getCause(): "+e.getCause());
        }

        //mohuaiyuan 先用图灵的资源 20170922
//        if (!isExecuteSuccess){
            try {
                Log.d(TAG, "准备。。播放图灵默认的歌曲: ");
                if (TobotUtils.isNotEmpty(songEntity.getUrl())) {
                    Log.i("Javen","歌曲rul..."+songEntity.getUrl());
                    Log.d(TAG, "播放图灵默认的歌曲: ");
                    Log.d(TAG, "歌曲playUrl...: "+songEntity.getUrl());
                    executeSong(songEntity.getUrl());
                }else {
                    //退出当前场景
                    Log.d(TAG, "退出当前场景46475: ");
//                manager.backMainScenario();
                    //mohuaiyuan 20170925 调用onexit方法
                    tts.speak("糟糕，这首歌唱不出来了，小主人换其他的歌曲吧！");
                    onExit();
//                    scenarioManager.quitCurrentScenario();
                }
            } catch (Exception e) {
                e.printStackTrace();

                Log.d(TAG, "Exception e.getMessage(): "+e.getMessage());
                Log.d(TAG, "Exception e.getCause(): "+e.getCause());
                //退出当前场景
                Log.d(TAG, "退出当前场景9395: ");
//                manager.backMainScenario();
                //mohuaiyuan 20170925 调用onexit方法
                tts.speak("糟糕，这首歌唱不出来了，小主人换其他的歌曲吧！");
                onExit();
//                scenarioManager.quitCurrentScenario();

            }
//        }

    }



    public void executeSong(String url) throws IOException {
        Log.d(TAG, "executeSong(String url): ");
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
                //mohuaiyuan 注释：不要退出场景
//                退出音乐场景
//                onExit();
//                mp.release();//释放音频资源
                mISceneV.getScenario("os.sys.song_stop");
//                manager.backMainScenario();
                scenarioManager.quitCurrentScenario();
//                Log.i("Javen", "资源已经被释放了");


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
     * 播放当前的歌曲
     */
    private void replaySong() {
        Log.d(TAG, "replaySong: ");
        Message message = new Message();
        message.what = TO_EXECUTE_SONG;
        myHandler.sendMessage(message);
    }

    /**
     * 播放下一首歌曲
     */
    private void playNextSong(){
        Log.d(TAG, "nextSong: ");

        int size=tracks.size();
        if (size==1){

        }else {
            position=position+1;
            if (position>=size){
                position-=size;
            }
        }
        try {
            initTrack(position);

            Log.d(TAG, "position: "+position);
            Log.d(TAG, "track title: "+tracks.get(position).getTrackTitle());
            Log.d(TAG, "duration: "+tracks.get(position).getDuration());
        } catch (Exception e) {
            e.printStackTrace();

            Log.e(TAG, "Exception e.getMessage(): " + e.getMessage());
            return;
        }

        Message message = new Message();
        message.what = TO_EXECUTE_SONG;
        myHandler.sendMessage(message);

    }

}
