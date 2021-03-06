package com.kilogramm.mattermost.model.entity.post;

import android.util.Log;

import com.kilogramm.mattermost.model.RealmSpecification;
import com.kilogramm.mattermost.model.Specification;
import com.kilogramm.mattermost.model.entity.Posts;
import com.kilogramm.mattermost.model.entity.user.User;

import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Evgeny on 19.09.2016.
 */
public class PostRepository {

    private static final String TAG = "PostRepository";

    public static void add(Post item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(item));
    }

    public static void add(Collection<Post> items) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(items));
    }


    public static void update(Post item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(item));
    }

    public static void remove(Post item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            if (realm.where(Post.class).equalTo("id", item.getId()).findAll().size() != 0) {
                Post post = realm.where(Post.class).equalTo("id", item.getId()).findFirst();
                post.deleteFromRealm();
                RealmResults<Post> comment = realm.where(Post.class)
                        .equalTo("rootId", item.getId()).findAll();
                comment.deleteAllFromRealm();
            }
        });
    }

    public static void remove(Specification specification) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults realmResults = ((RealmSpecification) specification).toRealmResults(realm);
        realm.executeTransaction(realm1 -> realmResults.deleteAllFromRealm());
    }

    public static RealmResults<Post> query(Specification specification) {
        Realm realm = Realm.getDefaultInstance();
        return ((RealmSpecification) specification).toRealmResults(realm);
    }

    public static Post query(String id) {
        Realm realm = Realm.getDefaultInstance();
        //realm.waitForChange();
        return realm.where(Post.class).equalTo("id", id).findFirst();
    }


    public static void removeTempPost(String sendedPostId) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            final Post post = realm.where(Post.class).equalTo("pendingPostId", sendedPostId).findFirst();
            post.deleteFromRealm();
        });
    }

    public static void merge(Collection<Post> posts, Specification specification) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmQuery realmQuery = query(specification).where().notEqualTo("updateAt", Post.NO_UPDATE);
        RealmResults realmResults = query(specification);
        for (Post post : posts) {
            prepareAndUpdatePost(post);
            realmQuery.notEqualTo("id", post.getId());
        }
        realmQuery.findAll().deleteAllFromRealm();
        realm.commitTransaction();
    }

    public static void merge(Post item) {
        Log.d(TAG, "merge: start");
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        try {
            Post post = realm.where(Post.class)
                    .equalTo("id", item.getPendingPostId()).findFirst();
            if (item.getProps() == null || item.getProps().getFrom_webhook() == null) {
                post.setProps(null);
            } else {
                post.setProps(item.getProps());
            }
            Log.d(TAG, "merge() called with: item.id = [" + item.getId() + "]\n     post.id = [" + post.getId() + "}");
            if (!post.getId().equals(item.getId())) {
                post.setId(item.getId());
            }
            post.setCreateAt(item.getCreateAt());
            post.setUpdateAt(item.getUpdateAt());
            post.setDeleteAt(item.getDeleteAt());
            post.setRootId(item.getRootId());
            post.setParentId(item.getParentId());
            post.setOriginalId(item.getOriginalId());
            post.setType(item.getType());
            post.setHashtags(item.getHashtags());
            post.setPendingPostId(item.getPendingPostId());
            post.setFilenames(item.getFilenames());

        } catch (Exception e) {
            e.printStackTrace();
        }
        realm.commitTransaction();
        realm.close();
        Log.d(TAG, "merge: end");
    }

    public static void prepareAndAddPost(Post post) {

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        if (!post.isSystemMessage())
            post.setUser(realm.where(User.class).equalTo("id", post.getUserId()).findFirst());
        else
            post.setUser(new User("System", "System", "System"));
        post.setViewed(true);
        if (post.getProps() == null || post.getProps().getFrom_webhook() == null) {
            post.setProps(null);
        } else {
            /*post.getProps().getAttachments().get(0).setText(
                    Processor.process(post.getProps().getAttachments().get(0).getText(),
                            Configuration.builder().forceExtentedProfile().build()));*/
        }
        //post.setMessage(Processor.process(post.getMessage(), Configuration.builder().forceExtentedProfile().build()));
        realm.insertOrUpdate(post);
        realm.commitTransaction();
        //add(post);
    }


    public static void setUpdateAt(String id, Long updateAt) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        Post post = realm.where(Post.class).equalTo("id", id).findFirst();
        if(post!=null){
            post.setUpdateAt(updateAt);
        }
        realm.insertOrUpdate(post);
        realm.commitTransaction();
        realm.close();
    }

    public static void prepareAndUpdatePost(Post post) {
        Realm realm = Realm.getDefaultInstance();
        if (!post.isSystemMessage())
            post.setUser(realm.where(User.class).equalTo("id", post.getUserId()).findFirst());
        else
            post.setUser(new User("System", "System", "System"));
        post.setViewed(true);

        if (post.getProps() == null || post.getProps().getFrom_webhook() == null) {
            post.setProps(null);
        } else {
            /*post.getProps().getAttachments().get(0).setText(
                    Processor.process(post.getProps().getAttachments().get(0).getText(),
                            Configuration.builder().forceExtentedProfile().build()));*/
        }
        //post.setMessage(Processor.process(post.getMessage(), Configuration.builder().forceExtentedProfile().build()));
        realm.insertOrUpdate(post);
        //update(post);
    }

    public static void prepareAndAdd(Posts posts) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();
        for (Post post : posts.getPosts().values()) {
            if (post.isSystemMessage())
                post.setUser(new User("System", "System", "System"));
            else
                post.setUser(realm.where(User.class).equalTo("id", post.getUserId()).findFirst());
            post.setViewed(true);
            //post.setMessage(Processor.process(post.getMessage(), Configuration.builder().forceExtentedProfile().build()));
            if (post.getProps() == null || post.getProps().getFrom_webhook() == null) {
                post.setProps(null);
            } else {
            }
                /*post.getProps().getAttachments().get(0).setText(
                        Processor.process(post.getProps().getAttachments().get(0).getText(),
                                Configuration.builder().forceExtentedProfile().build()));*/

        }
        realm.insertOrUpdate(posts.getPosts().values());
        realm.commitTransaction();
    }

    public static void updateUpdateAt(String postId, long update) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            Log.i("PRFIX", "updateUpdateAt: TIME: " + System.currentTimeMillis());
            RealmResults<Post> posts = realm1.where(Post.class).equalTo("id", postId).findAll();
            if (posts.size() != 0) {
                Post post = posts.first();
                post.setUpdateAt(update);
            }
        });
    }


    public static void updateUnsentPosts(){
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Post> unsentPosts = realm.where(Post.class).lessThan("updateAt", 0).findAll();
        for(Post post : unsentPosts){
            realm.executeTransaction(realm1 -> post.setCreateAt(System.currentTimeMillis() + 1000000L));
        }
    }


    public static void mergeWithDelete(Post item) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        try {
            Log.d(TAG, "merge: start");
            Post post = realm.where(Post.class)
                    .equalTo("id", item.getPendingPostId()).findFirst();
            post.deleteFromRealm();
            Log.d(TAG, "mergeWithDelete: post delete");
            if (item.isSystemMessage())
                item.setUser(new User("System", "System", "System"));
            else
                item.setUser(realm.where(User.class).equalTo("id", item.getUserId()).findFirst());
            item.setViewed(true);
            if (item.getProps() == null || item.getProps().getFrom_webhook() == null) {
                item.setProps(null);
            }
            realm.insertOrUpdate(item);
            Log.d(TAG, "merge: end");
        } catch (Exception e) {
            e.printStackTrace();
        }
        realm.commitTransaction();
        realm.close();
    }

    public static void mergeSendedPost(Post item) {
        String pendingPostId = item.getPendingPostId();
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        Post post1 = realm.where(Post.class)
                .equalTo("id", pendingPostId)
                .or()
                .equalTo("pendingPostId",pendingPostId)
                .findFirst();
        if(post1!=null){
            post1.deleteFromRealm();
            if (item.isSystemMessage())
                item.setUser(new User("System", "System", "System"));
            else
                item.setUser(realm.where(User.class).equalTo("id", item.getUserId()).findFirst());
            item.setViewed(true);
            if (item.getProps() == null || item.getProps().getFrom_webhook() == null) {
                item.setProps(null);
            }
            realm.insertOrUpdate(item);
        } else {
            if (!item.isSystemMessage())
                item.setUser(realm.where(User.class).equalTo("id", item.getUserId()).findFirst());
            else
                item.setUser(new User("System", "System", "System"));
            item.setViewed(true);
            if (item.getProps() == null || item.getProps().getFrom_webhook() == null) {
                item.setProps(null);
            }
            realm.insertOrUpdate(item);
        }
        realm.commitTransaction();
        realm.close();
        realm = null;
    }
}
