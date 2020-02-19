package com.zlm.hp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.util.JsonParser;
import com.zlm.hp.application.HPApplication;
import com.zlm.hp.libs.utils.LoggerUtil;
import com.zlm.hp.receiver.AudioBroadcastReceiver;
import com.zlm.hp.receiver.FragmentReceiver;
import com.zlm.hp.receiver.VoiceHelperReceiver;
import com.zlm.hp.ui.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 语音助手服务
 *
 *
 */

public class VoiceHelperService extends Service {


    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults ;

    private StringBuffer buffer ;

    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private String resultType = "json";

    private static String TAG = VoiceHelperService.class.getSimpleName();

    private VoiceHelperReceiver mVoiceHelperReceiver;

    private HPApplication mHPApplication;

    private LoggerUtil logger;

    private Thread mVoiceThread;

    private StringBuffer mResultBuffer;

    private VoiceHelperReceiver.VoiceHelperReceiverListener mVoiceHelperReceiverListener =
            new VoiceHelperReceiver.VoiceHelperReceiverListener() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if(action.equals(VoiceHelperReceiver.ACTION_VOICEHELPERSTART))
                    {
                        doVoiceReceive(context,intent);
                    }

                }
            };


    public VoiceHelperService() {
    }


    @Override
    public void onCreate(){
        super.onCreate();
        mHPApplication = HPApplication.getInstance();
        logger = LoggerUtil.getZhangLogger(getApplicationContext());

        //注册语音助手广播
        mVoiceHelperReceiver = new VoiceHelperReceiver(getApplicationContext(),mHPApplication);
        mVoiceHelperReceiver.setVoiceHelperReceiverListener(mVoiceHelperReceiverListener);
        mVoiceHelperReceiver.registerReceiver(getApplicationContext());

        mIat = SpeechRecognizer.createRecognizer(VoiceHelperService.this, mInitListener);
        mIatDialog = new RecognizerDialog(VoiceHelperService.this, mInitListener);
        mIatResults = new LinkedHashMap<String, String>();
        buffer = new StringBuffer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class VoiceRunable implements Runnable{

        @Override
        public void run(){

            buffer.setLength(0);
            mIatResults.clear();
            // 设置参数
            setParam();
            mIat.startListening(mRecognizerListener);

        }
    }

    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        // 设置语言区域
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS,"4000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                logger.e("初始化失败，错误码：" + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };



    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        mResultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            mResultBuffer.append(mIatResults.get(key));
        }



    }

    private void doVoiceReceive(Context context, Intent intent){
        releaseVoiceHelper();

        if(mVoiceThread == null){
            mVoiceThread = new Thread(new VoiceRunable());
            mVoiceThread.start();
        }

    }

    public void voiceTranslateToAction(String result) {

        Intent showIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPERSHOW);
        showIntent.putExtra("showText",mResultBuffer.toString());
        showIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(showIntent);

        if(result.contains(getString(R.string.action_key_play))){
            mHPApplication.sendMessageToPlay();
        }
        if(result.contains(getString(R.string.action_key_pause))){
            mHPApplication.sendMessageToPause();
        }
        if(result.contains((getString(R.string.action_key_next)))){
            mHPApplication.sendMessageToNext();
        }
        if(result.contains((getString(R.string.action_key_previous)))){
            mHPApplication.sendMessageToPre();
        }

        if(result.contains(getString(R.string.action_key_like_open))){
            Intent voiceMessageShowIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_OPEN_LIKE);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);
        }
        if(result.contains(getString(R.string.action_key_local_open))){
            Intent voiceMessageShowIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_OPEN_LOCAL);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);
        }
        if(result.contains(getString(R.string.action_key_download_open))){
            Intent voiceMessageShowIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_OPEN_DOWNLOAD);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);
        }
        if(result.contains(getString(R.string.action_key_recent_open))){
            Intent voiceMessageShowIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_OPEN_RECENT);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);
        }
        if(result.contains(getString(R.string.action_key_lrcActivity_open))){
            Intent voiceMessageShowIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_OPEN_LRCACTIVITY);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);
        }

        if(result.contains(getString(R.string.action_key_back))){
            Intent voiceMessageShowIntent = new Intent(FragmentReceiver.ACTION_CLOSEDFRAGMENT);
            voiceMessageShowIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageShowIntent);

            Intent voiceMessageIntent = new Intent(VoiceHelperReceiver.ACTION_VOICEHELPER_BACK);
            voiceMessageIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(voiceMessageIntent);

        }

    }

    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            logger.e("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。

            logger.e(error.getPlainDescription(true));

        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            logger.e("结束说话");

            voiceTranslateToAction(mResultBuffer.toString());
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if (resultType.equals("json")) {

                printResult(results);

            }else if(resultType.equals("plain")) {
                buffer.append(results.getResultString());
            }

        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            logger.e("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    public void releaseVoiceHelper(){
        if(mVoiceThread != null){
            mVoiceThread = null;
        }
        System.gc();
    }
}
