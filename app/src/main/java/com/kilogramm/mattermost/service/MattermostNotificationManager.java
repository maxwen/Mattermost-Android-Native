package com.kilogramm.mattermost.service;

import android.app.Activity;
import android.app.Service;
import android.util.Log;
import android.widget.Toast;

import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.Preference.PreferenceRepository;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.member.MembersRepository;
import com.kilogramm.mattermost.model.entity.post.PostRepository;
import com.kilogramm.mattermost.model.entity.userstatus.AllRemove;
import com.kilogramm.mattermost.model.entity.userstatus.UserStatus;
import com.kilogramm.mattermost.model.entity.userstatus.UserStatusRepository;
import com.kilogramm.mattermost.model.fromnet.ChannelWithMember;
import com.kilogramm.mattermost.model.websocket.WebSocketObj;
import com.kilogramm.mattermost.rxtest.BaseRxPresenter;

import rx.Subscriber;
import rx.schedulers.Schedulers;

import static com.kilogramm.mattermost.rxtest.BaseRxPresenter.CHANNELS_MORE;

/**
 * Created by Evgeny on 23.09.2016.
 */
public class MattermostNotificationManager {

    private static final String TAG = "Websocket";

    private MattermostService service;

    public MattermostNotificationManager(MattermostService service) {
        this.service = service;
    }

    public void handleSocket(WebSocketObj webSocketObj){
        switch (webSocketObj.getEvent()){
            case WebSocketObj.EVENT_CHANNEL_VIEWED:
                requestGetChannel(webSocketObj);
                break;
            case WebSocketObj.EVENT_POST_DELETED:
                PostRepository.remove(webSocketObj.getData().getPost());
                break;
            case WebSocketObj.EVENT_POST_EDITED:
                break;
            case WebSocketObj.EVENT_POSTED:
                if(webSocketObj.getChannelId().equals(MattermostPreference.getInstance().getLastChannelId())){
                    requestUpdateLastViewedAt(webSocketObj.getChannelId());
                } else {
                    requestGetChannel(webSocketObj);
                }
                break;
            case WebSocketObj.EVENT_STATUS_CHANGE:
                Log.d(TAG, "EVENT_STATUS_CHANGE: useid = "+ webSocketObj.getUserId() + "\n" +
                        "status = " + webSocketObj.getData().getStatus());
                UserStatusRepository.add(new UserStatus(webSocketObj.getUserId(), webSocketObj.getData().getStatus()));
                //userRepository.updateUserStatus(webSocketObj.getUserId(), webSocketObj.getData().getStatus());
                break;
            case WebSocketObj.EVENT_TYPING:
                break;
            case WebSocketObj.ALL_USER_STATUS:
                UserStatusRepository.remove(new AllRemove());
                for (String s : webSocketObj.getData().getStatusMap().keySet()) {
                    Log.d(TAG, "EVENT_ALL_USER_STATUS: useid = "+ s + "\n" +
                            "status = " + webSocketObj.getData().getStatusMap().get(s));
                    UserStatusRepository.add(new UserStatus(s,webSocketObj.getData().getStatusMap().get(s)));
                }
                break;
            case WebSocketObj.EVENT_PREFERENCE_CHANGED:
                PreferenceRepository.add(webSocketObj.getData().getPreference());
                break;
        }
    }

    private void requestUpdateLastViewedAt(String channelId) {
        MattermostApp.getSingleton()
                .getMattermostRetrofitService()
                .updatelastViewedAt(MattermostPreference.getInstance().getTeamId(),channelId);
    }

    private void requestGetChannel(WebSocketObj webSocketObj) {
        MattermostApp.getSingleton()
                .getMattermostRetrofitService()
                .getChannel(MattermostPreference.getInstance().getTeamId(),
                        webSocketObj.getChannelId())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<ChannelWithMember>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ChannelWithMember channelWithMember) {
                        ChannelRepository.prepareChannelAndAdd(channelWithMember.getChannel(),
                                MattermostPreference.getInstance().getMyUserId());
                        MembersRepository.add(channelWithMember.getMember());
                    }
                });
    }
}
