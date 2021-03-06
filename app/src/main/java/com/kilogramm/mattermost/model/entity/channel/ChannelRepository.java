package com.kilogramm.mattermost.model.entity.channel;

import android.support.annotation.NonNull;
import android.util.Log;

import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.RealmSpecification;
import com.kilogramm.mattermost.model.Specification;
import com.kilogramm.mattermost.model.entity.Preference.PreferenceRepository;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.user.User;

import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Evgeny on 22.09.2016.
 */
public class ChannelRepository {

    private static final int LOW_DASH_COUNT = 2;
    private static final String TAG = "CHANNEL_REPOSITORY";

    public static void add(Channel channel) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.insertOrUpdate(channel));
    }

    public static void add(Collection<Channel> items) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.insertOrUpdate(items));
    }

    public static void update(Channel item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(item));
    }

    public static void remove(Channel item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            Channel post = realm1.where(Channel.class).equalTo("id", item.getId()).findFirst();
            post.deleteFromRealm();
        });
    }

    public static void prepareChannelAndAdd(Collection<Channel> items, String myId) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            for (Channel channel : items) {
                String userId;
                if (channel.getType().equals(Channel.DIRECT)) {
                    userId = getUserId(myId, channel);
                    channel.setUser(realm1.where(User.class).equalTo("id", userId).findFirst());
                    if (channel.getUser() != null)
                        channel.setUsername(channel.getUser().getUsername());
                }
            }
            realm1.copyToRealmOrUpdate(items);

            RealmQuery<Channel> channelRealmQuery = realm.where(Channel.class);
            for (Channel channel : items)
                channelRealmQuery.notEqualTo("id", channel.getId());
            if (channelRealmQuery.findAll().size() > 0)
                channelRealmQuery.findAll().deleteAllFromRealm();
        });
    }

    public static void prepareChannelAndAdd(Channel item, String myId) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            String userId;
            if (item.getType().equals(Channel.DIRECT)) {
                userId = getUserId(myId, item);
                item.setUser(realm1.where(User.class).equalTo("id", userId).findFirst());
                item.setUsername(item.getUser().getUsername());
            }
            realm1.copyToRealmOrUpdate(item);
        });
    }

    @NonNull
    private static String getUserId(String myId, Channel channel) {
        Log.d(TAG, channel.getId());
        String userId;
        if (channel.getName().startsWith(myId)) {
            userId = channel.getName().substring(myId.length() + LOW_DASH_COUNT);
        } else {
            userId = channel.getName().substring(0, channel.getName().length() - myId.length() - LOW_DASH_COUNT);
        }
        return userId;
    }

    public static void prepareDirectChannelAndAdd(Channel channel, String userId) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            if (channel.getType().equals(Channel.DIRECT)) {
                channel.setUser(realm1.where(User.class).equalTo("id", userId).findFirst());
                channel.setUsername(channel.getUser().getUsername());
            }
            realm1.copyToRealmOrUpdate(channel);
            realm1.close();
        });
    }

    public static void prepareDirectAndChannelAdd(Collection<Channel> channels) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            for (Channel channel : channels) {
                if (channel.getType().equals(Channel.DIRECT)) {
                    String name = channel.getName()
                            .replace(MattermostPreference.getInstance().getMyUserId(), "");
                    name = name.replace("__", "");
                    channel.setUser(realm1.where(User.class)
                            .equalTo("id", name)
                            .findFirst());
                    if(channel.getUser() != null) channel.setUsername(channel.getUser().getUsername());
                }
                realm.insertOrUpdate(channel);
            }
        });
    }

    public static void remove(Specification specification) {
        Realm realm = Realm.getDefaultInstance();
        final RealmResults realmResults = ((RealmSpecification) specification).toRealmResults(realm);
        realm.executeTransaction(realm1 -> realmResults.deleteAllFromRealm());
    }

    public static RealmResults<Channel> query(Specification specification) {
        Realm realm = Realm.getDefaultInstance();
        return ((RealmSpecification) specification).toRealmResults(realm);
    }

    public static Boolean isExistChannelDirect(String userId, String myUserId) {
        Boolean isExist = false;
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        String id = userId + "__" + myUserId;
        String revertId = myUserId + "__" + userId;
        long count = realm.where(Channel.class)
                .equalTo("name", id)
                .or()
                .equalTo("name", revertId)
                .count();
        isExist = count!=0;
        realm.commitTransaction();
        realm.close();
        return isExist;
    }

    // region Specification
    public static class ChannelByIdSpecification implements RealmSpecification {
        private final String id;

        public ChannelByIdSpecification(String id) {
            this.id = id;
        }

        @Override
        public RealmResults<Channel> toRealmResults(Realm realm) {
            return realm.where(Channel.class)
                    .equalTo("id", id)
                    .findAll();
        }
    }

    public static class ChannelByTeamIdSpecification implements RealmSpecification {
        private final String teamId;

        public ChannelByTeamIdSpecification(String teamId) {
            this.teamId = teamId;
        }

        @Override
        public RealmResults<Channel> toRealmResults(Realm realm) {
            return realm.where(Channel.class)
                    .equalTo("team_id", teamId)
                    .findAll();
        }
    }

    public static class ChannelByTypeSpecification implements RealmSpecification {

        private final String type;

        public ChannelByTypeSpecification(String type) {
            this.type = type;
        }

        @Override
        public RealmResults<Channel> toRealmResults(Realm realm) {
            if (type.equals(Channel.OPEN) || type.equals(Channel.PRIVATE)) {
                return realm.where(Channel.class)
                        .equalTo("type", type)
                        .findAllSorted("displayName", Sort.ASCENDING);
            } else {
                return realm.where(Channel.class)
                        .equalTo("type", type)
                        .findAllSorted("username", Sort.ASCENDING);
            }
        }
    }

    public static class ChannelAllSpecification implements RealmSpecification {
        @Override
        public RealmResults toRealmResults(Realm realm) {
            return realm.where(Channel.class).findAllSorted("username", Sort.ASCENDING);
        }
    }

    public static class ChannelDirectByIdSpecification implements RealmSpecification {

        private String userId;

        public ChannelDirectByIdSpecification(String userId) {
            this.userId = userId;
        }

        @Override
        public RealmResults toRealmResults(Realm realm) {
            String myId = MattermostPreference.getInstance().getMyUserId();
            return realm.where(Channel.class)
                    .equalTo("name", myId + "__" + userId)
                    .or()
                    .equalTo("name", userId + "__" + myId)
                    .findAll();
        }
    }

    public static class ChannelListDirectMenu implements RealmSpecification {

        @Override
        public RealmResults toRealmResults(Realm realm) {
            RealmResults<Preferences> mPreferences = PreferenceRepository.query(new PreferenceRepository.ListDirectMenu());
            RealmQuery<Channel> realmQuery = RealmQuery.createQuery(Realm.getDefaultInstance(), Channel.class);
            for (int i = 0; i < mPreferences.size(); i++) {
                String name = String.format("%s__%s", mPreferences.get(i).getName(), mPreferences.get(i).getUser_id());
                String revertName = String.format("%s__%s", mPreferences.get(i).getUser_id(), mPreferences.get(i).getName());
                if(i==mPreferences.size()-1){
                    realmQuery.equalTo("name", name).or().equalTo("name", revertName);
                } else {
                    realmQuery.equalTo("name", name).or().equalTo("name", revertName).or();
                }
            }
            return realmQuery.findAllSorted("username", Sort.ASCENDING);
        }
    }
    //endregion
}

