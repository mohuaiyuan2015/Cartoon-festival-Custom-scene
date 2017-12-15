package com.tobot.tobot.control.demand;

/**
 * Created by YF-04 on 2017/10/9.
 */

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 点播功能 的工厂类
 */
public class DemandFactory {
    private static final String TAG = "DemandFactory";

    /**
     * 音乐
     */
    public static final int DEMAND_MUSIC=2;
    /**
     * 故事
     */
    public static final int DEMAND_STORY=6;
    /**
     * 国学
     */
    public static final int DEMAND_SINOLOGY=40;
    /**
     * 舞蹈
     */
    public static final int DEMAND_DANCE=88;

    private static DemandFactory demandFactory=new DemandFactory();

    private DemandBehavior demandBehavior;

    private  AnalyzeTrackModel analyzeTrackModel;
    private  DemandModel demandModel;

    private AnalyzeDanceItemChain analyzeDanceItemChain;
    private DanceItemChain danceItemChain;

    private  int categoryId;

    private  static Context mContext;
    private String json;

    private DemandTools demandTools;

    private DemandFactory(){

    }

    public static DemandFactory getInstance(Context context){
        mContext=context;
        return demandFactory;
    }

    private void initDemandBehavior(int categoryId) {

        switch (categoryId){
            case DEMAND_MUSIC:

            case DEMAND_STORY:

            case DEMAND_SINOLOGY:
                demandBehavior=new DemandMusic(mContext,demandModel);
                break;

            case DEMAND_DANCE:
                demandBehavior=new DemandDance(mContext,demandModel);
                break;

            default:
                demandBehavior=null;
                break;
        }

    }

    public void demands(String jsonString)throws Exception {

        if (jsonString==null || jsonString.length()<1 || jsonString.trim().length()<1){
           throw new Exception("Json format is  illegal!");
        }
        json=jsonString.trim();
        try {
            analyzeTrackModel=new AnalyzeTrackModel(json);
            analyzeTrackModel.toModel();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Json format is  illegal!");
        }
        demandModel=analyzeTrackModel.getDemandModel();
        categoryId=demandModel.getCategoryId();

        demands();
    }


    public void demands(DemandModel demandModel)throws Exception {
        if (demandModel==null){
            throw new Exception("demandModel==null ,demandModel is illegal!");
        }
        this.demandModel=demandModel;
        categoryId=demandModel.getCategoryId();

        demands();
    }

    private void demands()throws Exception {
        initDemandBehavior(categoryId);

        if (demandBehavior==null){
            throw new Exception("Init DemandFactory error:please  check the data your provided");
        }
        demandBehavior.executeDemand();
    }

    /**
     * 下载 舞蹈及其相关的资源
     * @param data: json 格式的 用于下载舞蹈及其相关资源 的列表
     * @throws Exception
     */
    public void downloadDanceResource(String data)throws Exception{
        //TODO 解析 json
        initDanceItemChain(data);

        //TODO 下载
        downloadDanceResource();

        //TODO 修改配置文件   考虑 配置文件是否修改的标记 使用sharedperforence 存储

    }

    private void downloadDanceResource() throws Exception {
        Log.d(TAG, "downloadDanceResource: ");
        demandTools=new DemandTools(mContext);
        List<DanceItem>list=danceItemChain.getData();
        for (int i=0;i<list.size();i++){

            DanceItem danceItem=list.get(i);
            MyCallback myCallback=new MyCallback(danceItem);
            demandTools.download(danceItem.getData(),myCallback);
        }
    }

    private  void initDanceItemChain(String data)throws Exception{
        Log.d(TAG, "initDanceItemChain: ");
        if (data==null || data.length()<1 || data.trim().length()<1){
            throw new Exception("Json format is  illegal!");
        }
        try {
            analyzeDanceItemChain=new AnalyzeDanceItemChain(data);
            analyzeDanceItemChain.toModel();
        } catch (Exception e) {
            Log.e(TAG,"Json format is  illegal!" );
            e.printStackTrace();
            throw new Exception("Json format is  illegal!");
        }
        danceItemChain=analyzeDanceItemChain.getDanceItemChain();
    }

    /**
     * 下载 动作及其相关的资源
     * @param data:json 格式的  用于下载动作及其相关资源 的列表
     * @throws Exception
     */
    public void downloadActionResource(String data)throws Exception{

        //TODO 解析 json

        //TODO 下载

        //TODO 修改配置文件

    }

    class MyCallback implements Callback{

        private DanceItem mDanceItem;
        public MyCallback(DanceItem danceItem){
            this.mDanceItem=danceItem;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "下载失败。。。 " );
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            InputStream is = null;
            byte[] buf = new byte[2048];
            int len = 0;
            FileOutputStream fos = null;
            String SDPath = Environment.getExternalStorageDirectory().getPath()+ File.separator+DemandUtils.DANCE_BACKGROUND_MUSIC_Dir;
            Log.d(TAG, "SDPath: "+SDPath);
            try {
                is = response.body().byteStream();
                long total = response.body().contentLength();
                File file = new File(SDPath, mDanceItem.getName());
                fos = new FileOutputStream(file);
                long sum = 0;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    sum += len;
                    int progress = (int) (sum * 1.0f / total * 100);//下载进度的百分比
                    Log.d(TAG, "progress=" + progress);

                }
                fos.flush();
                Log.d(TAG, "文件下载成功");
            } catch (Exception e) {
                Log.e(TAG, "文件下载失败");
            } finally {
                try {
                    if (is != null){
                        is.close();
                    }
                } catch (IOException e) {
                }
                try {
                    if (fos != null){
                        fos.close();
                    }
                } catch (IOException e) {
                }
            }
        }
    }


}
