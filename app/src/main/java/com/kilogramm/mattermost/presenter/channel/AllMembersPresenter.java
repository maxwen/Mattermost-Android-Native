package com.kilogramm.mattermost.presenter.channel;

import android.os.Bundle;

import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.ListPreferences;
import com.kilogramm.mattermost.model.entity.Preference.PreferenceRepository;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.extroInfo.ExtroInfoRepository;
import com.kilogramm.mattermost.model.fromnet.ExtraInfo;
import com.kilogramm.mattermost.network.ServerMethod;
import com.kilogramm.mattermost.rxtest.BaseRxPresenter;
import com.kilogramm.mattermost.view.BaseActivity;
import com.kilogramm.mattermost.view.channel.AllMembersChannelActivity;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmResults;
import io.realm.Sort;
import rx.schedulers.Schedulers;

/**
 * Created by ngers on 01.11.16.
 */

public class AllMembersPresenter extends BaseRxPresenter<AllMembersChannelActivity> {
    private static final int REQUEST_DB_GETUSERS = 1;
    private static final int REQUEST_SAVE = 2;
    private static final int REQUEST_SAVE_PREFERENCES = 3;
    private static final int REQUEST_GET_CHANNEL = 3;

    ExtraInfo mExtraInfo;

    String mId;


    private User mUser;
    ListPreferences listPreferences = new ListPreferences();

    List<Preferences> preferenceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mUser = new User();

        initGetUsers();
        initSavPreferences();
        initSaveRequest();
    }


    public void requestSaveData(Preferences data, String userId) {
        listPreferences.getmSaveData().clear();
        listPreferences.getmSaveData().add(data);
        mUser.setId(userId);
        start(REQUEST_SAVE);
    }

    private void sendShowError(String error) {
        createTemplateObservable(error).subscribe(split(BaseActivity::showErrorText));
    }

    public void initPresenter(String id) {
        this.mId = id;
        start(REQUEST_DB_GETUSERS);
    }

    public void savePreferences(String name) {
        Preferences preferences =
                new Preferences(PreferenceRepository.query(
                        new PreferenceRepository.PreferenceByNameSpecification(name)).first());
        preferences.setValue("true");
        listPreferences.getmSaveData().add(preferences);
        start(REQUEST_SAVE_PREFERENCES);
//        sendSetFragmentChat();
    }

    public RealmResults<User> getMembers(String name) {
        return mExtraInfo.getMembers().where().contains("username", name).findAllSorted("username", Sort.ASCENDING);
    }

    public RealmResults<User> getMembers() {
        return mExtraInfo.getMembers().where().findAllSorted("username", Sort.ASCENDING);
    }

    private void initGetUsers() {
        restartableFirst(REQUEST_DB_GETUSERS,
                () -> ExtroInfoRepository.query(new ExtroInfoRepository.ExtroInfoByIdSpecification(mId)).asObservable(),
                (allMembersActivity, o) -> {
                    this.mExtraInfo = o.first();
                    allMembersActivity.updateDataList(getMembers());
                }, (allMembersActivity, throwable) -> sendShowError(parceError(throwable, SAVE_PREFERENCES)));
    }

    private void initSavPreferences() {
        restartableFirst(REQUEST_SAVE_PREFERENCES, () ->
                       ServerMethod.getInstance()
                               .save(listPreferences.getmSaveData())
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io()),
                (allMembersChannelActivity, aBoolean) -> {
                    PreferenceRepository.update(listPreferences.getmSaveData());
//                    sendSetFragmentChat();
                },
                (allMembersChannelActivity, throwable) ->
                        sendShowError(parceError(throwable, SAVE_PREFERENCES))
        );

    }

    /*private void initGetChannels() {
        restartableFirst(REQUEST_GET_CHANNEL, () ->
                mService.getChannelsTeam(MattermostPreference.getInstance().getTeamId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io()),
                (allMembersChannelActivity, channelsWithMembers) -> {
                   ChannelRepository.add(channelsWithMembers.getChannels());
                    MembersRepository.update(channelsWithMembers.getMembers());
                },
                (allMembersChannelActivity, throwable) ->
                        sendShowError(parceError(throwable, SAVE_PREFERENCES)));
    }*/

    private void initSaveRequest() {
        restartableFirst(REQUEST_SAVE, () ->
                ServerMethod.getInstance()
                        .saveOrCreateDirectChannel(listPreferences.getmSaveData(),
                            MattermostPreference.getInstance().getTeamId(),
                            mUser.getId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                , (generalRxActivity, channel) -> {
                    ChannelRepository.prepareDirectChannelAndAdd(channel, mUser.getId());
                    PreferenceRepository.update(listPreferences.getmSaveData());
                    listPreferences.getmSaveData().clear();
                    MattermostPreference.getInstance().setLastChannelId(channel.getId());
                    sendSetFragmentChat();
                }, (generalRxActivity, throwable) -> sendShowError(parceError(throwable, null)));
    }

    private void sendSetFragmentChat() {
        createTemplateObservable(new Object())
                .subscribe(split((allMembersActivity, o) ->
                        allMembersActivity.startGeneralActivity()));
    }
}

