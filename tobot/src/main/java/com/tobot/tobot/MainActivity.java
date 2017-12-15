package com.tobot.tobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

//import com.qdreamer.qvoice.QModule;
//import com.qdreamer.qvoice.QSession;
import com.google.gson.Gson;
import com.tobot.tobot.Listener.MainScenarioCallback;
import com.tobot.tobot.base.BaseActivity;
import com.tobot.tobot.base.Constants;
import com.tobot.tobot.Listener.ExpressionCallback;
import com.tobot.tobot.Listener.LocalCommandGather;
import com.tobot.tobot.Listener.SimpleFrameCallback;
import com.tobot.tobot.base.UpdateAction;
import com.tobot.tobot.db.bean.UserDBManager;
import com.tobot.tobot.db.model.User;
import com.tobot.tobot.entity.ScoffEntity;
import com.tobot.tobot.function.AssembleFunction;
import com.tobot.tobot.presenter.BRealize.BArmtouch;
import com.tobot.tobot.presenter.BRealize.BBattery;
import com.tobot.tobot.presenter.BRealize.BConnect;
import com.tobot.tobot.presenter.BRealize.BDormant;
import com.tobot.tobot.presenter.BRealize.BLocal;
import com.tobot.tobot.presenter.BRealize.BMonitor;
import com.tobot.tobot.presenter.BRealize.BProtect;
import com.tobot.tobot.presenter.BRealize.BSensor;
import com.tobot.tobot.presenter.ICommon.ISceneV;
import com.tobot.tobot.scene.BaseScene;
import com.tobot.tobot.scene.CustomScenario;

import com.tobot.tobot.presenter.BRealize.BScenario;
import com.tobot.tobot.utils.AppTools;
import com.tobot.tobot.utils.SHA1;
import com.tobot.tobot.utils.Transform;
import com.tobot.tobot.utils.okhttpblock.OkHttpUtils;
import com.tobot.tobot.utils.okhttpblock.callback.StringCallback;
import com.tobot.tobot.utils.socketblock.Joint;
import com.tobot.tobot.utils.TobotUtils;
import com.tobot.tobot.utils.TouchResponse_Library;
import com.tobot.tobot.utils.bluetoothblock.Ble;
import com.tobot.tobot.utils.socketblock.Const;
import com.tobot.tobot.utils.socketblock.NetManager;
import com.tobot.tobot.utils.socketblock.SocketThreadManager;
import com.turing123.robotframe.RobotFrameManager;
import com.turing123.robotframe.RobotFramePreparedListener;
import com.turing123.robotframe.config.SystemConfig;
import com.turing123.robotframe.event.AppEvent;
import com.turing123.robotframe.function.FunctionManager;
import com.turing123.robotframe.function.IInitialCallback;
import com.turing123.robotframe.function.asr.ASRError;
import com.turing123.robotframe.function.asr.IASRFunction;
import com.turing123.robotframe.function.cloud.Cloud;
import com.turing123.robotframe.function.cloud.IAutoCloudCallback;
import com.turing123.robotframe.function.expression.Expression;
import com.turing123.robotframe.function.keyin.KeyInputEvent;
import com.turing123.robotframe.function.motor.Motor;
import com.turing123.robotframe.function.tts.ITTSCallback;
import com.turing123.robotframe.function.tts.TTS;
import com.turing123.robotframe.function.wakeup.VoiceWakeUp;
import com.turing123.robotframe.interceptor.StateBuilder;
import com.turing123.robotframe.multimodal.Behaviors;
import com.turing123.robotframe.multimodal.action.Action;
import com.turing123.robotframe.multimodal.action.BodyActionCode;
import com.turing123.robotframe.multimodal.action.EarActionCode;
import com.turing123.robotframe.multimodal.expression.EmojNames;
import com.turing123.robotframe.multimodal.expression.FacialExpression;
import com.turing123.robotframe.scenario.ScenarioManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;

import static android.R.attr.data;
import static com.turing123.robotframe.function.keyin.KeyInputEvent.KEYCODE_HEAD;
import static com.turing123.robotframe.multimodal.action.Action.PRMTYPE_EXECUTION_TIMES;
import static java.lang.Thread.sleep;

public class MainActivity extends BaseActivity implements ISceneV {
    @BindView(R.id.ed_account)
    EditText account;
    @BindView(R.id.ed_password)
    EditText password;
    @BindView(R.id.btn_conn)
    Button btn_conn;
    @BindView(R.id.tvConnResult)
    public TextView tvConnResult;
    @BindView(R.id.tvASR)
    TextView tvASR;
    @BindView(R.id.im_picture)
    ImageView im_picture;

    public static RobotFrameManager mRobotFrameManager;
    private FunctionManager functionManager;
    private CustomScenario customScenario;
//    private LocalCommandCenter localCommandCenter;
//    private LocalCommand sleepCommand;
    private FacialExpression mFacialExpression;
    private Expression mExpression;
    public Motor motor;
    private Cloud mCloud;
    private TTS tts;
    private BScenario mBScenario;
    private BConnect mBConnect;
    private BMonitor mBMonitor;
    private BDormant mBDormant;
    public static BLocal mBLocal;
    public BBattery mBBattery;
    private BArmtouch mBArmtouch;
    private BProtect mBProtect;
    private BSensor mBSensor;
    private Ble mBle;
    private Timer waitingTimer = new Timer(true);//等待休眠时间
    private Timer activeTimer = new Timer(true);//主动交互时间
    private Timer dormantTimer = new Timer(true);//休眠时间
    private Timer detectionTime = new Timer(true);//异常断网检测时间
    private Timer TimeMachine = new Timer(true);//异常断网语音播报时间
    private boolean isDormant,isWakeup;//休眠,唤醒
    private boolean isNotWakeup = true;//禁止唤醒
    private boolean isInterrupt;//打断
    private boolean isSquagging = true;//自锁
    private boolean isOff_line = true;//ASR检测断网
//    private boolean isConnect;//进入重新联网
    private boolean isInitiativeOff;//判断是否主动断网
    private boolean ACTIVATESIGN;//框架启动标志
    private Bundle packet;
    private long exitTime; // 短时间内是否连续点击返回键

    public static Context mContext;
    private BroadcastReceiver mReceiver;
    private SocketThreadManager manager;

    @Override
    public int getGlobalLayout() {
            return R.layout.activity_main;
    }

    @Override
    public void initial(Bundle savedInstanceState) {

        mContext = this;
        NetManager.instance().init(this);
        manager = SocketThreadManager.sharedInstance();

        //初始化AP联网
        onSetAP();

//        if (!AppTools.netWorkAvailable(MainActivity.this)) {
            //启动框架
            onInitiate();
//        }

//        regBroadcast();

//        try {
//            execCommand("setprop service.adb.tcp.port 5555");
//            execCommand("stop adbd");
//            execCommand("start adbd");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }




    public void regBroadcast() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String value = intent.getStringExtra("response");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        tvASR.setText(value);
                        Log("广播信息:" + value);
                    }
                });
            }
        };
        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(Const.BC);
        registerReceiver(mReceiver, intentToReceiveFilter);
    }



    //联网
    private void onSetAP(){ mBConnect = new BConnect(MainActivity.this); }

    public void onInitiate(){
        //0. 因为各功能的使用都需要携带使用该功能的场景，所以先创建一个场景，如果脱离场景使用，请使用FailOver 类。
        customScenario = new CustomScenario(MainActivity.this);
        //1. 设置对话模式为自动对话，主场景将维护对话的输入和输出。
        startRobotFramework(SystemConfig.CHAT_MODE_AUTO);
    }

    void startRobotFramework(int mode) {
        // 取得框架实例
        mRobotFrameManager = RobotFrameManager.getInstance(MainActivity.this);
        //设置apikey
        mRobotFrameManager.setApiKeyAndSecret(Constants.APIKEY, Constants.SERVICE);
        // 设置框架聊天模式
        mRobotFrameManager.setChatMode(mode);
        // 设置状态机工作模式。查看API Ref以了解更多关于框架工作模式的信息
        int state = new StateBuilder(StateBuilder.DefaultMode).build();
        // prepare（）这个方法必须在你做任何事情之前被调用
        mRobotFrameManager.prepare(state, new RobotFramePreparedListener() {

            @Override
            public void onPrepared() {
                // 激活
                mRobotFrameManager.start();
                // 可选的控制场景
                // mRobotFrameManager.toLostScenario();
                // 回到默认场景
                // mRobotFrameManager.backMainScenario();
                mainHandler.sendEmptyMessage(Constants.START_SUCESS_MSG);
            }

            @Override
            public void onError(String errorMsg) {
                // error occurred, check errorMsg and have all error fixed
                Message message = Message.obtain();
                message.what = Constants.START_ERROR_MSG;
                message.obj = errorMsg;
                mainHandler.sendMessage(message);
            }
        });
    }


    // TTS的使用
    private void onTTS(){
        tts = new TTS(MainActivity.this, new BaseScene(MainActivity.this,"os.sys.chat"));
    }

    //初始化功能
    private void onFunction(){
        functionManager = new FunctionManager(MainActivity.this);
        motor = new Motor(this, new CustomScenario(this));
        mCloud = new Cloud(MainActivity.this,new MainScenarioCallback());
        mFacialExpression = new FacialExpression();
        mFacialExpression.displayMode = FacialExpression.DISPLAY_MODE_PROTOCOL_PREDEFINED;
        mFacialExpression.executeMode = Action.MODE_COVER;
        mFacialExpression.eyeParams.put(PRMTYPE_EXECUTION_TIMES, 1);
        mExpression= new Expression(MainActivity.this,new BaseScene(MainActivity.this,"os.sys.chat"));

    }

    private void onAssemble(){
        //1. 创建Assemble Function 实例。
        final AssembleFunction assembleFunction = new AssembleFunction(MainActivity.this);
        //2. 初始化
        assembleFunction.init(new IInitialCallback() {
            @Override
            public void onSuccess() {
                //3. 初始化成功后将assemble function加入RobotFrame.
                //3.1 获取Function 的管理类
                //3.2 调用addFunction, 将assembleFunction加入系统
                functionManager.addFunction(assembleFunction);
            }

            @Override
            public void onError(String s) {

            }
        });
    }

    //进入次场景
    private void  onMinorscene(){
        mBScenario = new BScenario(MainActivity.this);
        mBScenario.inScene();
    }

    //监听
    private void onNotification() {
        mBMonitor = new BMonitor(MainActivity.this);
    }

    //休眠
    private void onDormant(){
        mBDormant = new BDormant(MainActivity.this);
    }

    //本地命令
    private void onLocal() { mBLocal = new BLocal(MainActivity.this);  }

    //电量检测
    private void onBattery(){
        mBBattery = new BBattery(MainActivity.this);
    }

    //手臂触摸
    private void onBArmtouch(){
        mBArmtouch = new BArmtouch(MainActivity.this);
    }

    //自我保护机制
    private void onBProtect(){
        mBProtect = new BProtect(MainActivity.this);
    }

    //传感器监听
    private void onBSensor(){
        mBSensor = new BSensor(MainActivity.this);
    }

    //启动机器人蓝牙
    private void onBle(){ mBle = new Ble(MainActivity.this,mRobotFrameManager); }


    // 唤醒功能
    private void onRouse() {
        VoiceWakeUp mVoiceWakeUp = new VoiceWakeUp(MainActivity.this,customScenario);
        mVoiceWakeUp.configWakeUp("/sdcard/.TuringResource/system/WakeUp.bin");
//        mVoiceWakeUp.configWakeUp("assets/WakeUp.bin");
    }


    //ASR提醒音
    private void hint(){
        //1. 获取ScenarioManager.
        ScenarioManager scenarioManager = new ScenarioManager(MainActivity.this);
        //2. 设置开关，true 为开， false 为关。
        scenarioManager.switchDefaultChatAsrPrompt(true, false);
    }


   private void manifestation(){
//        if (TobotUtils.isEmployFack()){
//            //首次使用提示语,动作等
//        }
       if (AppTools.netWorkAvailable(this) && !isInitiativeOff) {//自动联网成功
           tts.speak("小主人有没有想我呀,一起来玩吧");
           mFacialExpression.emoj = EmojNames.HAPPY;
           mExpression.showExpression(mFacialExpression, new ExpressionCallback());
           motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_17,PRMTYPE_EXECUTION_TIMES,1),new SimpleFrameCallback());
           motor.doAction(Action.buildEarAction(EarActionCode.EAR_MOTIONCODE_1, 80, 0));
       }
   }


    Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.START_ERROR_MSG://框架加载失败
                    mBConnect.isLoad(false);
                    mBConnect.shunt();
                    String error = (String) msg.obj;
                    Log("start error ⊙﹏⊙b\n" + error);
                    break;
                case Constants.START_SUCESS_MSG :
                    Log("框架加载成功..........");
                    mBConnect.isLoad(true);
                    isDormant = true;
                    //运行TTS
                    onTTS();
                    //初始化功能
                    onFunction();
                    //调度
//                    onAssemble();
                    //进入次场景
                    onMinorscene();
                    //通知
                    onNotification();
                    //手臂触摸
                    onBArmtouch();
                    //提示音
//                    hint();
                    //休眠
                    onDormant();
                    //唤醒
                    onRouse();
                    //本地命令
                    onLocal();
                    //电量
                    onBattery();
                    //自我保护
                    onBProtect();
                    //注册监听器
                    onBSensor();
                    //启动标志
                    ACTIVATESIGN = true;
                    manifestation();
                    //蓝牙
                    onBle();


                    break;
                case Constants.NOTIFICATION_MSG:
                    packet = (Bundle)msg.obj;
                    try{
                        switch (packet.getString("action")) {
                            case "tts.status":
                                if (packet.getInt("arg1",0) == 0){
                                    isInterrupt = true;
                                    try {
                                        activeTimer.cancel();
                                        activeTimer = new Timer();
                                        sleep(1000);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                hint();
                                }
                                break;
                            case "connection.status":
                                if (packet.getInt("arg1") == 1 && !isInitiativeOff) {//非主动
                                    detectionTime.schedule(new DetectionTimerTask(),4000,4000);//4秒钟
                                    Log("断网了...................");
                                }
                                break;
                            case "asr.status":
                                isInterrupt = false;
                                String asrContent = packet.getString("arg2");
                                if(packet.getInt("arg1") == 4){
                                    //-------------------------------
                                    if(asrContent.contains("没有检查到网络")) {
                                        if (isOff_line) {
                                            Log("没有检查到网络");
                                            isOff_line = false;
                                            detectionTime.schedule(new DetectionTimerTask(),10000,10000);//10秒钟
                                        }
                                    }
//                                    if(asrContent.contains("没有检查到网络")) {
//                                        if (!hintConnect) {
//                                            Log("没有检查到网络");
//                                            detectionTime.schedule(new DetectionTimerTask(),4000,4000);//4秒钟
//                                        }
//                                    }
                                    //--------------------------------
                                }else if(packet.getInt("arg1") == 3 && asrContent != null){// packet.getString("arg2") != null  //收到对话
                                    if (!isSquagging){
                                        //等待睡眠
                                        waitingTimer.cancel();
                                        waitingTimer = new Timer();
                                        //等待主动交互
                                        activeTimer.cancel();
                                        activeTimer = new Timer();
                                    }
                                    if(hintConnect){//断网收到语音提示
                                        tts.speak(getResources().getString(R.string.Hint_Break), ittsCallback);
                                    }
                                    isSquagging = true;
                                    Log("结束倒计时");
                                }else if(packet.getInt("arg1") != 3 && asrContent != null){//无对话
                                    if (isSquagging){//自锁
                                        Log("开始倒计时");
                                        isSquagging = false;
                                        waitingTimer.schedule(new DormantTimerTask(),300000000);//5分钟
//                                        activeTimer.schedule(new ActiveTimerTask(),20000,1000);//主动交互请求
                                    }
                                }
                                break;
                            case "robot.state"://状态机
                                if(packet.getInt("arg1") == 5){

                                }else if(packet.getInt("arg1") == 4){

                                }else if(packet.getInt("arg1") == 3){

                                    motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_9,PRMTYPE_EXECUTION_TIMES,1),new SimpleFrameCallback());
                                }
                                break;
                        }
                    }catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.AWAIT_DORMANT ://自动休眠
                    if (isDormant) {
                        isDormant = false;
                        isWakeup = true;
                        motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_8, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
                        mRobotFrameManager.sleep();
                        new LocalCommandGather().onComplete();
                    }
                    break;
                case Constants.AWAIT_AWAKEN ://等待唤醒
                    mRobotFrameManager.wakeup();
                    isNotWakeup = true;
                    tts.speak(getResources().getString(R.string.Mend_Error), ittsCallback);
                    break;
                case Constants.AWAIT_ACTIVE ://主动交互
                    mCloud.requestActiveTalk(new IAutoCloudCallback() {
                        @Override
                        public void onResult(Behaviors behaviors) {
                            Log.i("Javen","主动交互请求成功..."+behaviors);
                        }

                        @Override
                        public void onError(String s) {
                            Log.i("Javen","主动交互请求失败"+s);
                        }
                    });
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    };




    ScoffEntity mScoffEntity;
    boolean isFeelHead = true;//摸头启动ap联网
    @Override
    public void isKeyDown(int keyCode, KeyEvent event) {
        Log("触摸事件............."+keyCode+"..."+event);
        if (ACTIVATESIGN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (isWakeup && isNotWakeup) {
                        Log("触摸--唤醒");
                        isDormant = true;
                        isWakeup = false;
                        mRobotFrameManager.wakeup();
                    } else if (isInterrupt || mScenario.equals("os.sys.song")) {
                        Log("触摸--打断"+mScenario);
                        KeyInputEvent mKeyInputEvent = new KeyInputEvent(keyCode, KEYCODE_HEAD);
                        mRobotFrameManager.interrupt(SystemConfig.INTERRUPT_TYPE_TOUCH, null);
                        isInterrupt = false;
                    } else {
                        Log("触摸--调侃聊天");
                        try {
                            long l = (System.currentTimeMillis() - exitTime);
                            if (l < 4000) {//连续点击
                                Log("触摸--连续点击......");
                                onBle();
//                                mScoffEntity = TouchResponse_Library.getResponse(true);
//                                tts.speak(mScoffEntity.getVar(), ittsCallback);
                            } else {
                                Log("触摸--单击......");
                                exitTime = System.currentTimeMillis();
                                tts.speak(TouchResponse_Library.getResponse(), ittsCallback);
                            }
                        } catch (Exception e) {
                        }
                    }
                    break;
                case KeyEvent.FLAG_LONG_PRESS:
                    Log("触摸--进入长按事件");
                    if (isFeelHead) {
                        isInitiativeOff = true;//主动断网
                        mBConnect.shunt();//启动ap联网
                        tts.speak("联网模式已启动,请重新帮我联网或长摸头三秒解除联网",ittsCallback);
                        mFacialExpression.emoj = EmojNames.EXPECT;
                        mExpression.showExpression(mFacialExpression, new ExpressionCallback());
                        motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_95, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
                        isFeelHead = false;
                    } else if (isFeelHead == false) {
                        isInitiativeOff = true;//关掉主动断网
                        mBConnect.shut();//关闭ap联网
                        tts.speak("联网模式已解除",ittsCallback);
                        mFacialExpression.emoj = EmojNames.DEPRESSED;
                        mExpression.showExpression(mFacialExpression, new ExpressionCallback());
                        motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_STAND_STILL, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
                        isFeelHead = true;
                    }
                    break;

                default:
                    break;
            }
        }else{
            mBConnect.shuntVoice();
        }

    }


    ITTSCallback ittsCallback = new ITTSCallback() {

        @Override
        public void onStart(String s) {
        Log("开始语音播报"+s);
        }

        @Override
        public void onPaused() { }

        @Override
        public void onResumed() { }

        @Override
        public void onCompleted() {
            Log("朗读完成...");
//            if(isConnect){
//                Log("异常断网进入重连");
//                isConnect = false;
//                mBConnect.shunt();//重新联网 -- 需要考虑是否要直接启动还是摸头三秒启动
//            }
            try {
                if (mScoffEntity.getDormant()) {
                    Log("进入自主休眠10分钟" + isInterrupt);
                    //注意： 若要唤醒机器人，可调用wakeup,或者使用语言唤醒词唤醒。
                    motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_8, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
                    mRobotFrameManager.sleep();
                    //5.2 命令执行完成后需明确告诉框架，命令处理结束，否则无法继续进行主对话流程。
                    new LocalCommandGather().onComplete();
                    dormantTimer.schedule(new AwakenTimerTask(), 600000);//休眠10分钟
                    isNotWakeup = false;//禁止打断
                }
            } catch (NullPointerException e) {

            }
        }

        @Override
        public void onError(String s) {
        Log("TTS错误"+s);
        }
    };



//-------------------------------------------------------------------------------------------------------------------------------------------------------------------



    @Override
    public Object setInfluence() {
        return mRobotFrameManager;
    }

    @Override
    public void getResult(Object result) {
        mainHandler.sendMessage((Message)result);
    }

    @Override
    public void getInitiativeOff(boolean initiative) {
        this.isInitiativeOff = initiative;
    }

    @Override
    public void getFeelHead(boolean feel) {
        isFeelHead = feel;
    }

    @Override
    public void getConnectFailure(boolean failure) {
        if (failure){
            mFacialExpression.emoj = EmojNames.DEPRESSED;
            mExpression.showExpression(mFacialExpression, new ExpressionCallback());
            motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_45, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
        }
    }

    @Override
    public void getDormant(boolean dormant) {
        Log("休眠"+dormant);
        isDormant = dormant;//自动休眠
        isWakeup = true;//允许唤醒
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log("动作下发时间..1...:"+dateFormat.format(new Date()));
        motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_8, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
    }

    private String mScenario = "stop";
    @Override
    public void getScenario(String scenario) {
        mScenario = scenario;
        mBArmtouch.getScenario(scenario);
    }

    @Override
    public void getSongScenario(Object song) {
        mBArmtouch.getSongScenario(song);
    }




//-------------------------------------------------------------------------------------------------------------------------------------------------------------------



    @OnClick(R.id.btn_conn)
    public void  send(){
        //启动ap联网
        mBConnect.shunt();
    }

    @OnClick(R.id.btn_close)
    public void close(){
        //关闭ap联网
        Log("关闭AP联网");
        mBConnect.shut();
    }

    @OnClick(R.id.btn_shutdown)
    public void  shutdown(){
       //下发动作
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Log("动作下发时间..1...:"+dateFormat.format(new Date()));
//        motor.doAction(Action.buildEarAction(EarActionCode.EAR_MOTIONCODE_0,PRMTYPE_EXECUTION_TIMES,1),new SimpleFrameCallback());

        //发送注册
//        manager.sendMsg(Transform.HexString2Bytes(Joint.setRegister()));

    }



//-------------------------------------------------------------------------------------------------------------------------------------------------------------------




    private class DormantTimerTask extends TimerTask {
        public void run() {
            Message message = new Message();
            message.what = Constants.AWAIT_DORMANT;
            mainHandler.sendMessage(message);
        }
    }
    private class AwakenTimerTask extends TimerTask {
        public void run() {
            Message message = new Message();
            message.what = Constants.AWAIT_AWAKEN;
            mainHandler.sendMessage(message);
        }
    }
    private class ActiveTimerTask extends TimerTask {
        public void run() {
            Message message = new Message();
            message.what = Constants.AWAIT_ACTIVE;
            mainHandler.sendMessage(message);
        }
    }

    private boolean hintConnect;//断网提示连接--接下来看下与isOff_line合并

    private class DetectionTimerTask extends TimerTask {
        public void run() {
            Log("离线五秒检测到断网...............");
            if (!AppTools.netWorkAvailable(MainActivity.this) && !hintConnect) {
                Log("离线五秒检测到断网进入语音提示...............");
                functionManager.choiceFunctionProcessor(AppEvent.FUNC_TYPE_ASR, IASRFunction.DEFAULT_ASR_PROCESSOR_OFFLINE);
//                isConnect = true;
                hintConnect = true;
                mFacialExpression.emoj = EmojNames.DEPRESSED;
                mExpression.showExpression(mFacialExpression, new ExpressionCallback());
                motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_80, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
//                tts.speak(getResources().getString(R.string.Connection_Break), ittsCallback);
                detectionTime.schedule(new TimeMachineTimerTask(),0,30000);
//                TimeMachine.schedule(new TimeMachineTimerTask(),0,30000);
            }else if (AppTools.netWorkAvailable(MainActivity.this)){
                Log("检测到异常断网重新连接...............");
                functionManager.resetFunction(AppEvent.FUNC_TYPE_ASR);
                hintConnect = false;
                isOff_line = true;
                mFacialExpression.emoj = EmojNames.TIRED;
                mExpression.showExpression(mFacialExpression, new ExpressionCallback());
                motor.doAction(Action.buildBodyAction(BodyActionCode.ACTION_45, PRMTYPE_EXECUTION_TIMES, 1), new SimpleFrameCallback());
                tts.speak("网络已恢复连接,不过你给我连的这是什么破网络");
                detectionTime.cancel();
                detectionTime = new Timer();
            }
        }
    }

    private class TimeMachineTimerTask extends TimerTask {
        public void run() {
            if (hintConnect)
                tts.speak(TouchResponse_Library.getBrokenNetwork(), ittsCallback);
        }
    }


//-------------------------------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onStart() {
        super.onStart();
//        eliminate();
    }

    private void eliminate() {
        try {
                String time1 = UserDBManager.getManager().getCurrentUser().getRequestTime();
                String time2 = TobotUtils.getCurrentlyDate();
                long date = TobotUtils.DateMinusTime(time1, time2);
                Log("date.............." + date);
                if (date > 1) {
                    new UpdateAction(MainActivity.this);
                }
        } catch (NullPointerException e) {
            // TODO: handle exception
            User user = new User();
            user.setRequestTime(TobotUtils.getCurrentlyDate());
            UserDBManager.getManager().insertOrUpdate(user);
        } catch (IllegalArgumentException e){
            new UpdateAction(MainActivity.this);
        }
    }

//        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        Log(tm.getDeviceId());
//        Log("系统版本号"+android.os.Build.VERSION.RELEASE);



//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------



//    public void execCommand(String command) throws IOException {
//        // start the ls command running
//        //String[] args = new String[]{"sh", "-c", command};
//        Runtime runtime = Runtime.getRuntime();
//        Process proc = runtime.exec(command);
//        InputStream inputstream = proc.getInputStream();
//        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
//        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
//        String line = "";
//        StringBuilder sb = new StringBuilder(line);
//        while ((line = bufferedreader.readLine()) != null) {
//            sb.append(line);
//            sb.append('\n');
//        }
//        Log.d(TAG, "return:" + sb.toString());
//        try {
//            if (proc.waitFor() != 0) {
//                Log.d(TAG,"exit value = " + proc.exitValue());
//            }
//        } catch (InterruptedException e) {
//            System.err.println(e);
//        }
//    }

}


