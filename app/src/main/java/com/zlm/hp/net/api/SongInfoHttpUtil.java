package com.zlm.hp.net.api;

import android.content.Context;

import com.zlm.hp.net.HttpClientUtils;
import com.zlm.hp.net.entity.SongInfoResult;
import com.zlm.hp.libs.utils.LoggerUtil;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by zhangliangming on 2017/7/30.
 */
public class SongInfoHttpUtil {



    /**
     * 获取歌曲的具体信息
     *
     * @param context
     * @param hash
     * @return
     */
    public static SongInfoResult songInfo(Context context, String hash) {

        try {
            LoggerUtil logger =LoggerUtil.getZhangLogger(context);
//            String url = "http://m.kugou.com/app/i/getSongInfo.php";
            String url = "http://www.kugou.com/yy/index.php";
            Map<String, Object> params = new HashMap<String, Object>();
//            params.put("Cookie",md5Decode(getStringRandom(4)));
//            params.put("cmd", "playInfo");
            params.put("Cookie", "kg_mid="+md5Decode(getStringRandom(4)));
            params.put("r","play/getdata");
            params.put("hash", hash);

            // 获取数据
            String result = HttpClientUtils.httpGetRequest(url, params);
            if (result != null) {
                logger.e("SongInfoResult...result is ok!!!!!!!!!!");

                JSONObject jsonNode = new JSONObject(result);
                int status = jsonNode.getInt("status");
//                String error = jsonNode.getString("error");
                int err_code = jsonNode.getInt("err_code");

                logger.e(String.format("status is %d",status));
//                logger.e("error is"+error);
                logger.e(String.format("errcode is %d",err_code));
                if (status == 1) {

                    SongInfoResult songInfoResult = new SongInfoResult();
                    songInfoResult.setDuration(jsonNode.getInt("timeLength")
                            * 1000 + "");
                    songInfoResult.setExtName(jsonNode.getString("extName"));
                    songInfoResult.setFileSize(jsonNode.getString("fileSize"));
                    songInfoResult.setHash(jsonNode.getString("hash").toLowerCase());
                    songInfoResult.setImgUrl(jsonNode.getString("imgUrl")
                            .replace("{size}", "400"));
                    songInfoResult.setSingerName(jsonNode.getString("singerName"));
                    songInfoResult.setSongName(jsonNode.getString("songName"));
                    songInfoResult.setUrl(jsonNode.getString("url"));
                    logger.e("SongInfoResult...Url is "+jsonNode.getString("url"));

                    return songInfoResult;
                }
            }
            else {
                logger.e("SongInfoResult...result is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 生成随机数字和字母,
     */
    public static String getStringRandom(int length) {

        String val = "";
        Random random = new Random();

        //参数length，表示生成几位随机数
        for(int i = 0; i < length; i++) {

            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            //输出字母还是数字
            if( "char".equalsIgnoreCase(charOrNum) ) {
                //输出是大写字母还是小写字母
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char)(random.nextInt(26) + temp);
            } else if( "num".equalsIgnoreCase(charOrNum) ) {
                val += String.valueOf(random.nextInt(10));
            }
        }
        return val;
    }

    /**
     * 32位MD5加密
     * @param content -- 待加密内容
     * @return
     */
    public static String md5Decode(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException",e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }
        //对生成的16字节数组进行补零操作
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10){
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }
}
