package com.tobot.tobot.presenter.ICommon;

import android.net.Uri;

/**
 * Created by Javen on 2017/8/8.
 */

public interface ISceneV {
    public Object setInfluence();//BDormant
    public void getResult(Object result);//公用
    public void getDormant(boolean dormant);//BDormant
    public void getInitiativeOff(boolean initiative);//BConnect--主动联网
    public void getFeelHead(boolean feel);//BConnect--摸头
    public void getConnectFailure(boolean failure);//BConnect--联网失败回调
    public void getScenario(String scenario);//BArmtouch
    public void getSongScenario(Object song);//BArmtouch 废弃
//    public void getImgpath(Uri path);//废弃
}
