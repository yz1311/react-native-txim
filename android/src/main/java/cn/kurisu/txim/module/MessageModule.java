package cn.kurisu.txim.module;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import cn.kurisu.txim.constants.IMEventNameConstant;
import cn.kurisu.txim.listener.MessageEventListener;
import cn.kurisu.txim.utils.messageUtils.MessageInfo;
import cn.kurisu.txim.utils.messageUtils.MessageInfoUtil;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMValueCallBack;
import com.tencent.imsdk.log.QLog;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Kurisu
 */
public class MessageModule extends BaseModule {
    TIMConversation conversation;

    public MessageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Nonnull
    @Override
    public String getName() {
        return "IMMessageModule";
    }


    @ReactMethod
    public void getConversation(int type, String peer) {
        final WritableMap map = Arguments.createMap();
        try {
            conversation = TIMManager.getInstance().getConversation(
                    TIMConversationType.values()[type],
                    peer);
            map.putInt("code", 0);
            map.putString("msg", "");
            sendEvent(IMEventNameConstant.CONVERSATION_STATUS, map);
        } catch (Exception e) {
            map.putInt("code", -1);
            map.putString("msg", e.getMessage());
            sendEvent(IMEventNameConstant.CONVERSATION_STATUS, map);
        }
    }

    @ReactMethod
    public void sendMessage(Integer type, String content,
                            String imgPath, Integer width, Integer height, Integer duration, //视频消息参数
                            Boolean compressed,  //图片消息
                            Double latitude, Double longitude //地理位置
    ) {
        final WritableMap map = Arguments.createMap();
        if (conversation == null) {
            map.putInt("code", -1);
            map.putString("msg", "当前会话已被销毁，请重新获取");
            sendEvent(IMEventNameConstant.SEND_STATUS, map);
        } else {
            MessageInfo info = newMeassage(type, content, imgPath, width, height, duration, compressed, latitude, longitude);
            if (info == null) {
                map.putInt("code", -1);
                map.putString("msg", "发送失败请重试");
                sendEvent(IMEventNameConstant.SEND_STATUS, map);
                return;
            }
            conversation.sendMessage(info.getTIMMessage(), new TIMValueCallBack<TIMMessage>() {//发送消息回调
                @Override
                public void onError(int code, String desc) {//发送消息失败
                    //错误码 code 含义请参见错误码表
                    QLog.e("发送消息", "send message failed. code: " + code + " errmsg: " + desc);
                    map.putInt("code", code);
                    map.putString("msg", desc);
                    sendEvent(IMEventNameConstant.SEND_STATUS, map);
                }

                @Override
                public void onSuccess(TIMMessage msg) {//发送消息成功
                    QLog.i("发送消息", "SendMsg ok");
                    map.putInt("code", 0);
                    map.putString("msg", "SendMsg ok");
                    sendEvent(IMEventNameConstant.SEND_STATUS, map);
                    WritableArray array = Arguments.createArray();
                    array.pushMap(MessageEventListener.messageAnalysis(info));
                    sendEvent(IMEventNameConstant.ON_NEW_MESSAGE, array);
                }
            });

        }

    }

    /**
     * 注销会话
     */
    @ReactMethod
    public void destroyConversation() {
        if (conversation != null) {
            this.conversation = null;
        }
    }

    private MessageInfo newMeassage(@Nonnull Integer type, @Nonnull String content,
                                    @Nullable String imgPath, @Nullable Integer width, @Nullable Integer height, @Nullable Integer duration, //视频消息参数
                                    Boolean compressed,  //图片消息
                                    Double latitude, Double longitude //地理位置
    ) {
        //构造一条消息
        MessageInfo messageInfo = null;
        switch (type) {
            case MessageInfo.MSG_TYPE_TEXT:
                messageInfo = MessageInfoUtil.buildTextMessage(content);
                break;
            case MessageInfo.MSG_TYPE_IMAGE:
                Uri uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + content));
                messageInfo = MessageInfoUtil.buildImageMessage(uri, compressed, false);
                break;
            case MessageInfo.MSG_TYPE_AUDIO:
                messageInfo = MessageInfoUtil.buildAudioMessage(Environment.getExternalStorageDirectory() + content, duration);
                break;
            case MessageInfo.MSG_TYPE_VIDEO:
                messageInfo = MessageInfoUtil.buildVideoMessage(Environment.getExternalStorageDirectory() + imgPath, Environment.getExternalStorageDirectory() + content, width, height, duration);
                break;
            case MessageInfo.MSG_TYPE_FILE:
                Uri fileUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + content));
                messageInfo = MessageInfoUtil.buildFileMessage(fileUri);
                break;
            case MessageInfo.MSG_TYPE_LOCATION:
                messageInfo = MessageInfoUtil.buildLocationMessage(content, latitude, longitude);
                break;
            case MessageInfo.MSG_TYPE_CUSTOM_FACE:
                //TODO 表情待定
                break;
            default:
                break;
        }

        return messageInfo;
    }
}