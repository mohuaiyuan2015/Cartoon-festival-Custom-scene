package com.tobot.tobot.control;

import android.content.Context;

import com.tobot.tobot.MainActivity;
import com.tobot.tobot.control.demand.DemandFactory;
import com.tobot.tobot.control.demand.DemandModel;
import com.tobot.tobot.utils.TobotUtils;
import com.tobot.tobot.utils.socketblock.SocketConnectCoherence;

/**
 * Created by Javen on 2017/10/19.
 */
public class Demand {
    private static final String TAG = "Javen_MusicDemand";
    private static Demand demand;
    private static Context context;

    public Demand(Context context){
        this.context = context;
    }

    public static synchronized Demand instance(Context context) {
        if (demand == null) {
            demand = new Demand(context);
            setResource();
        }
        return demand;
    }

    public static void setResource(){
        SocketConnectCoherence.setDemandListener(new SocketConnectCoherence.DemandListener() {
            @Override
            public void setDemandResource(DemandModel demand) {
                //功能实现
                DemandFactory demandFactory = DemandFactory.getInstance(context);
                try {
                    if (TobotUtils.isNotEmpty(MainActivity.mRobotFrameManager)){
                        MainActivity.mRobotFrameManager.toLostScenario();
                    }
                    demandFactory.demands(demand);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setResource(DemandModel demandModel){
        DemandFactory demandFactory=DemandFactory.getInstance(context);
        try {
            if (TobotUtils.isNotEmpty(MainActivity.mRobotFrameManager)){
                MainActivity.mRobotFrameManager.toLostScenario();
            }
            demandFactory.demands(demandModel);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
