package com.tobot.tobot.control;

import android.content.Context;

import com.tobot.tobot.MainActivity;
import com.tobot.tobot.base.UpdateAction;
import com.tobot.tobot.control.demand.DemandFactory;
import com.tobot.tobot.control.demand.DemandModel;
import com.tobot.tobot.utils.TobotUtils;
import com.tobot.tobot.utils.socketblock.SocketConnectCoherence;

/**
 * Created by Javen on 2017/10/23.
 */

public class SaveAction {

    private static SaveAction saveAction;
    private static Context context;

    public SaveAction(Context context){
        this.context = context;
    }

    public static synchronized SaveAction instance(Context context) {
        if (saveAction == null) {
            saveAction = new SaveAction(context);
            setResource();
        }
        return saveAction;
    }

    public static void setResource(){
//        UpdateAction.setSaveListener(new UpdateAction.SaveListener() {
//            @Override
//            public void setSaveResource(String demand) {
//                //功能实现
//                DemandFactory demandFactory = DemandFactory.getInstance(context);
//                try {
//                    demandFactory.downloadDanceResource(demand);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }



    public void setResource(String demand){
//        DemandFactory demandFactory = DemandFactory.getInstance(context);
//        try {
//            demandFactory.downloadDanceResource(demand);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


}
