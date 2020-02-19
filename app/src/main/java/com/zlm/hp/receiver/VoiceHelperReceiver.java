package com.zlm.hp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import com.zlm.hp.application.HPApplication;
import com.zlm.hp.libs.utils.LoggerUtil;

import java.util.Date;

/**
 * 语音助手广播监听
 * Created by shixinbin on 2020/2/14.
 */

public class VoiceHelperReceiver {

    /**
     *
     */
    private LoggerUtil logger;
    /**
     * 是否注册成功
     */
    private boolean isRegisterSuccess = false;
    private Context mContext;
    private HPApplication mHPApplication;

    /**
     * 注册成功广播
     */
    private String ACTION_VOICEHELPERSUCCESS = "com.zlm.hp.voicehelper.success_" + new Date().getTime();

    //
    public static final String ACTION_VOICEHELPERSTART = "com.zlm.hp.voice_helper.start";
    public static final String ACTION_VOICEHELPERSHOW = "com.zlm.hp.voice_helper.show";


    //
    public static final String ACTION_VOICEHELPER_OPEN_LOCAL = "com.zlm.hp.voice_helper.open_local";
    public static final String ACTION_VOICEHELPER_OPEN_LIKE = "com.zlm.hp.voice_helper.open_like";
    public static final String ACTION_VOICEHELPER_OPEN_DOWNLOAD = "com.zlm.hp.voice_helper.open_download";
    public static final String ACTION_VOICEHELPER_OPEN_RECENT = "com.zlm.hp.voice_helper.open_recent";
    public static final String ACTION_VOICEHELPER_OPEN_LRCACTIVITY = "com.zlm.hp.voice_helper.open_lrcActivity";
    public static final String ACTION_VOICEHELPER_BACK = "com.zlm.hp.voice_helper.back";

    private BroadcastReceiver mVoiceHelperBroadcastReceiver;
    private IntentFilter mVoiceHelperIntentFilter;
    private VoiceHelperReceiverListener mVoiceHelperReceiverListener;

    public VoiceHelperReceiver(Context context, HPApplication hPApplication) {
        this.mHPApplication = hPApplication;
        this.mContext = context;
        logger = LoggerUtil.getZhangLogger(context);

        //
        mVoiceHelperIntentFilter = new IntentFilter();
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPERSUCCESS);

        //
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPERSTART);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPERSHOW);

        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_OPEN_DOWNLOAD);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_OPEN_LIKE);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_OPEN_LOCAL);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_OPEN_LRCACTIVITY);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_OPEN_RECENT);
        mVoiceHelperIntentFilter.addAction(ACTION_VOICEHELPER_BACK);
    }


    /**
     *
     */
    private Handler mVoiceHelperHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (mVoiceHelperReceiverListener != null) {
                Intent intent = (Intent) msg.obj;

                if (intent.getAction().equals(ACTION_VOICEHELPERSUCCESS)) {
                    isRegisterSuccess = true;
                } else {
                    mVoiceHelperReceiverListener.onReceive(mContext, intent);
                }
            }
        }
    };

    /**
     * 注册广播
     *
     * @param context
     */
    public void registerReceiver(Context context) {
        if (mVoiceHelperBroadcastReceiver == null) {
            //
            mVoiceHelperBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    Message msg = new Message();
                    msg.obj = intent;
                    mVoiceHelperHandler.sendMessage(msg);


                }
            };

            mContext.registerReceiver(mVoiceHelperBroadcastReceiver, mVoiceHelperIntentFilter);
            //发送注册成功广播
            Intent successIntent = new Intent(ACTION_VOICEHELPERSUCCESS);
            successIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            mContext.sendBroadcast(successIntent);

        }
    }

    /**
     * 取消注册广播
     */
    public void unregisterReceiver(Context context) {
        if (mVoiceHelperBroadcastReceiver != null && isRegisterSuccess) {

            mContext.unregisterReceiver(mVoiceHelperBroadcastReceiver);

        }

    }


    ///////////////////////////////////
    public interface VoiceHelperReceiverListener {
        void onReceive(Context context, Intent intent);
    }

    public void setVoiceHelperReceiverListener(VoiceHelperReceiverListener mVoiceHelperReceiverListener) {
        this.mVoiceHelperReceiverListener = mVoiceHelperReceiverListener;
    }
}
