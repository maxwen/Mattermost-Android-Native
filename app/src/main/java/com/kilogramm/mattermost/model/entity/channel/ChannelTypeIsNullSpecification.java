package com.kilogramm.mattermost.model.entity.channel;

import com.kilogramm.mattermost.model.RealmSpecification;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Evgeny on 22.09.2016.
 */
public class ChannelTypeIsNullSpecification implements RealmSpecification {
    @Override
    public RealmResults toRealmResults(Realm realm) {
        return realm.where(Channel.class)
                .isNull("type")
                .findAll();
    }
}
