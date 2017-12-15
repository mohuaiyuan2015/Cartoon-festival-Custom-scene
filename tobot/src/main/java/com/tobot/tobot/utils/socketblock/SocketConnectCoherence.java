package com.tobot.tobot.utils.socketblock;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.tobot.tobot.MainActivity;
import com.tobot.tobot.control.Demand;
import com.tobot.tobot.control.demand.DemandModel;
import com.tobot.tobot.db.bean.UserDBManager;
import com.tobot.tobot.db.model.User;
import com.tobot.tobot.utils.Transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import static com.tobot.tobot.utils.socketblock.Const.HEART;


/**
 * Created by TAG on 2017/10/9.
 */
public class SocketConnectCoherence {
    private String TAG = "TAG_SocketConnectCoherence";
    private WeakReference<Socket> mSocket;
    private ReadThread mReadThread;
    private long sendTime = 0L;
    private static final long HEART_BEAT_RATE = 3 * 1000;
    public static final String HOST = "39.108.134.20";
    public static final int PORT = 81;
    private static DemandListener mdemandListener;
    private DemandModel model = new DemandModel();

    public SocketConnectCoherence(){
        new InitSocketThread().start();
    }


    // For heart Beat
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {

        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                boolean isSuccess = sendMsg(HEART);//就发送一个心跳包过去 如果发送失败，就重新初始化一个socket
                if (!isSuccess) {
                    Log.i("TAG","心跳包发送失败:");
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                }else {
//                    Log.i("TAG","心跳包发送成功:");
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    public boolean sendMsg(String msg) {
        if (null == mSocket || null == mSocket.get()) {
            return false;
        }
        Socket soc = mSocket.get();
        try {
            if (!soc.isClosed() && !soc.isOutputShutdown()) {
                OutputStream os = soc.getOutputStream();
                os.write(Transform.HexString2Bytes(msg));
                os.flush();
                sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void initSocket() {//初始化Socket
        try {
            Socket so = new Socket(HOST, PORT);
            mSocket = new WeakReference<Socket>(so);
            mReadThread = new ReadThread(so);
            mReadThread.start();
            sendMsg(Joint.setRegister());
            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//初始化成功后，就准备发送心跳包
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void releaseLastSocket(WeakReference<Socket> mSocket) {
        try {
            if (null != mSocket) {
                Socket sk = mSocket.get();
                if (!sk.isClosed()) {
                    sk.close();
                }
                sk = null;
                mSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            initSocket();
        }
    }

    // Thread to read content from Socket
    class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @Override
        public void run() {
            super.run();
            Socket socket = mWeakSocket.get();
            if (null != socket) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024 * 2];
                    int length = 0;
                    while (!socket.isClosed() && !socket.isInputShutdown() && isStart && ((length = is.read(buffer)) != -1))
                        if (length > 0) {
                            String message = new String(Arrays.copyOf(buffer, length),"GB2312");
//                            String message = new String(Arrays.copyOf(buffer, length)).trim();
//                            String message2 = URLEncoder.encode(message, "GB2312");
//                            String  message3 =  new String(buffer,"GB2312");
//                            Log.e("TAG", "SocketConnectTest回应消息......"+message);
                            //收到服务器过来的消息，就通过Broadcast发送出去
                            if (message.equals("[12]")) {//处理心跳回复

                            } else if (message.equals("[10]")){

                            } else if (message.equals("[1100011960B]")) {//注册成功

                            } else if(message.equals("[1100010160E]")){//注册失败
                                sendMsg(Joint.setRegister());
                            } else if (message.substring(2,3).equals("3")) {//拍照 31 33 30 30 30 35 30 30 30 30 31 46 42 39 46
                                sendMsg(Joint.setResponse(Joint.PHOTO,message));
                                MainActivity.mBLocal.camera(Joint.getSpecialRunning(message));
                            } else if (message.substring(2,3).equals("4")) {//点播
                                sendMsg(Joint.setDemandResponse(message));
                                Message Msg = Message.obtain();
                                Msg.what = 4;
                                Msg.obj = message;
                                handler.sendMessage(Msg);
                            } else if (message.substring(2,3).equals("5")) {//角色定义修改通知
                                sendMsg(Joint.setRoleResponse());
                                //还没做
                            } else if (message.substring(2,3).equals("6")) {//出厂设置
                                sendMsg(Joint.setResponse(Joint.RESTORE,message));
                                //还没做
                                UserDBManager.getManager().clear();
                            } else if (message.substring(2,3).equals("8")) {//舞蹈
                                sendMsg(Joint.setResponse(Joint.DANCE,message));
                                Message Msg = Message.obtain();
                                Msg.what = 8;
                                Msg.obj = message;
                                handler.sendMessage(Msg);
                            }
                        }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendData(){
        boolean isSuccess = sendMsg(Joint.setRegister());
        if (isSuccess){
            Log.i("TAG","数据发送成功:");
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String message = (String)msg.obj;
            model.setInitialize();
            switch (msg.what){
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    model.setCategoryId(Integer.parseInt(Joint.getCommaAmong(message,1)));
                    model.setTrack_title(Joint.getCommaAmong(message,2));
                    model.setPlayUrl32(Joint.getPeelVerify(message));
                    Demand.instance(MainActivity.mContext).setResource(model);
//                    mdemandListener.setDemandResource(model);
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 8:
                    model.setCategoryId(88);
                    model.setPlayUrl32(Joint.getPeelVerify(message));
//                    Demand.instance(MainActivity.mContext).setResource(model);
                    mdemandListener.setDemandResource(model);
                    break;
            }
        }
    };

    public static void setDemandListener(DemandListener demandListener) {
        mdemandListener = demandListener;
    }

    public interface DemandListener{
        void setDemandResource(DemandModel demand);
    };
    
}
