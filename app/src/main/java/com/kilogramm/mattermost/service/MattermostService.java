package com.kilogramm.mattermost.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.kilogramm.mattermost.model.websocket.WebSocketObj;
import com.kilogramm.mattermost.service.websocket.WebSocketManager;


/**
 * Created by kraftu on 16.09.16.
 */
public class MattermostService extends Service implements WebSocketManager.WebSocketMessage {

    // TODO любопыно, а почему ru.com. ?)
    public static final String SERVICE_ACTION_START_WEB_SOCKET = "ru.com.kilogramm.mattermost.SERVICE_ACTION_START_WEB_SOCKET";
    public static final String UPDATE_USER_STATUS = "ru.com.kilogramm.mattermost.SERVICE_ACTION_START_WEB_SOCKET.UPDATE_USER_STATUS";
    public static final String USER_TYPING = "USER_TYPING";
    public static final String START_LOAD_USER_DATA = "START_LOAD_USER_DATA";
    public static final String CANCEL_LOAD_USER_DATA = "CANCEL_LOAD_USER_DATA";
    public static final String CANCEL_ALL_LOAD_USER_DATA = "CANCEL_ALL_LOAD_USER_DATA";
    public static final String USER_ID = "USER_ID";

    public static final String CHANNEL_ID = "sCHANNEL_ID";
    public static final String BROADCAST_MESSAGE = "broadcast_message";

    private static final String TAG = "MattermostService";

    private WebSocketManager mWebSocketManager;
    private LoadUserService mLoadUserService;
    private ManagerBroadcast managerBroadcast;
    private MattermostNotificationManager mattermostNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mWebSocketManager = new WebSocketManager(this);
        managerBroadcast = new ManagerBroadcast(this);
        mLoadUserService = new LoadUserService();
        mattermostNotificationManager = new MattermostNotificationManager(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWebSocketManager != null) mWebSocketManager.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_REDELIVER_INTENT;
        String action = intent.getAction();
        if (action == null) return START_REDELIVER_INTENT;
        switch (action) {
            case SERVICE_ACTION_START_WEB_SOCKET:
                mWebSocketManager.start();
                break;
            case UPDATE_USER_STATUS:
                mWebSocketManager.updateUserStatusNow();
                break;
            case USER_TYPING:
                mWebSocketManager.sendUserTyping(intent.getStringExtra(CHANNEL_ID));
                break;
            case START_LOAD_USER_DATA:
                mLoadUserService.startLoadUser(intent.getStringExtra(USER_ID));
                break;
            case CANCEL_LOAD_USER_DATA:
                mLoadUserService.cancelLoadUser(intent.getStringExtra(USER_ID));
                break;
            case CANCEL_ALL_LOAD_USER_DATA:
                break;
        }
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void receiveMessage(String message) {
        WebSocketObj sendedMessage = managerBroadcast.parseMessage(message);
        mattermostNotificationManager.handleSocket(sendedMessage);
        if (sendedMessage != null) {
            Intent intent = new Intent(sendedMessage.getEvent());
            intent.putExtra(BROADCAST_MESSAGE, sendedMessage);
            sendBroadcast(intent);
        }
    }

    public static class Helper {

        private Context mContext;

        public Helper(Context mContext) {
            this.mContext = mContext;
        }

        public static Helper create(Context c) {
            return new Helper(c);
        }

        public Helper startService() {
            Intent intent = new Intent(mContext, MattermostService.class);
            mContext.startService(intent);
            return this;
        }

        public Helper stopService() {
            Intent intent = new Intent(mContext, MattermostService.class);
            mContext.stopService(intent);
            return this;
        }

        public Helper startWebSocket() {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.setAction(SERVICE_ACTION_START_WEB_SOCKET);
            mContext.startService(intent);
            return this;
        }

        public Helper updateUserStatusNow() {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.setAction(UPDATE_USER_STATUS);
            mContext.startService(intent);
            return this;
        }

        public Helper sendUserTyping(String channelId) {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.putExtra(CHANNEL_ID, channelId);
            intent.setAction(USER_TYPING);
            mContext.startService(intent);
            return this;
        }

        public Helper startLoadUser(String userId) {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.putExtra(USER_ID, userId);
            intent.setAction(START_LOAD_USER_DATA);
            mContext.startService(intent);
            return this;
        }

        public Helper cancelLoadUser(String userId) {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.putExtra(USER_ID, userId);
            intent.setAction(CANCEL_LOAD_USER_DATA);
            mContext.startService(intent);
            return this;
        }

        public Helper cancelAllLoadUser(String userId) {
            Intent intent = new Intent(mContext, MattermostService.class);
            intent.putExtra(USER_ID, userId);
            intent.setAction(CANCEL_ALL_LOAD_USER_DATA);
            mContext.startService(intent);
            return this;
        }
    }
}
