package com.kilogramm.mattermost.rxtest.left_menu;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.FragmentLeftMenuBinding;
import com.kilogramm.mattermost.model.UserMember;
import com.kilogramm.mattermost.model.entity.Preference.PreferenceRepository;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.channel.Channel;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository.ChannelDirectByIdSpecification;
import com.kilogramm.mattermost.model.entity.member.Member;
import com.kilogramm.mattermost.model.entity.member.MemberAll;
import com.kilogramm.mattermost.model.entity.member.MembersRepository;
import com.kilogramm.mattermost.model.entity.team.Team;
import com.kilogramm.mattermost.model.entity.team.TeamRepository;
import com.kilogramm.mattermost.model.entity.usermember.UserMemberRepository;
import com.kilogramm.mattermost.model.entity.userstatus.UserStatus;
import com.kilogramm.mattermost.model.entity.userstatus.UserStatusRepository;
import com.kilogramm.mattermost.rxtest.left_menu.adapters.AdapterDirectMenuLeft;
import com.kilogramm.mattermost.rxtest.left_menu.adapters.ChannelListAdapter;
import com.kilogramm.mattermost.rxtest.left_menu.adapters.PrivateListAdapter;
import com.kilogramm.mattermost.view.addchat.AddExistingChannelsActivity;
import com.kilogramm.mattermost.view.createChannelGroup.CreateNewChannelActivity;
import com.kilogramm.mattermost.view.createChannelGroup.CreateNewGroupActivity;
import com.kilogramm.mattermost.view.direct.WholeDirectListActivity;
import com.kilogramm.mattermost.view.fragments.BaseFragment;

import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import nucleus.factory.RequiresPresenter;

import static android.app.Activity.RESULT_OK;
import static com.kilogramm.mattermost.model.entity.channel.Channel.DIRECT;
import static com.kilogramm.mattermost.model.entity.channel.Channel.OPEN;
import static com.kilogramm.mattermost.model.entity.channel.Channel.PRIVATE;


@RequiresPresenter(LeftMenuRxPresenter.class)
public class LeftMenuRxFragment extends BaseFragment<LeftMenuRxPresenter> implements OnLeftMenuClickListener,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "LeftMenuRxFragment";

    private static final int NOT_SELECTED = -1;

    public static final int REQUEST_CREATE_CHANNEL = 97;
    public static final int REQUEST_CREATE_GROUP = 96;
    public static final int REQUEST_JOIN_CHANNEL = 98;
    public static final int REQUEST_JOIN_DIRECT = 99;

    private FragmentLeftMenuBinding mBinding;

    private ChannelListAdapter mChannelListAdapter;
    private PrivateListAdapter mPrivateListAdapter;
    private AdapterDirectMenuLeft mAdapterDirectMenuLeft;

    private OnChannelChangeListener mListener;

    private RealmResults<Member> mMembers;
    private RealmResults<Preferences> mPreferences;
    private RealmResults<UserMember> mUserMembers;
    private RealmResults<UserStatus> mUserStatuses;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_left_menu, container, false);
        View view = mBinding.getRoot();
        mPreferences = PreferenceRepository.query(new PreferenceRepository.ListDirectMenu());

        mPreferences.addChangeListener(element -> {
            Log.d(TAG, "onCreateView: prefChange");
            onRefresh();
        });
        mMembers = MembersRepository.query(new MemberAll());
        mMembers.addChangeListener(element -> {
            if (mChannelListAdapter != null) {
                mChannelListAdapter.notifyDataSetChanged();
            }
            if (mAdapterDirectMenuLeft != null) {
                mAdapterDirectMenuLeft.invalidateMember();
            }
            if (mPrivateListAdapter != null) {
                mPrivateListAdapter.notifyDataSetChanged();
            }
        });
        mUserMembers = UserMemberRepository.query(new UserMemberRepository.UserMemberAllSpecification());
        mUserMembers.addChangeListener(element -> mAdapterDirectMenuLeft.update());

        mUserStatuses = UserStatusRepository.query(new UserStatusRepository.UserStatusAllSpecification());
        mUserStatuses.addChangeListener(element -> {
            mAdapterDirectMenuLeft.invalidateStatus();
        });
        mBinding.leftSwipeRefresh.setOnRefreshListener(this);

        initView();
        showFirstLoadingMenu();
        getPresenter().requestInit(Stream.of(mPreferences)
                .filter(value -> Objects.equals(value.getValue(), "true"))
                .collect(Collectors.toList()));
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMembers.removeChangeListeners();
        mPreferences.removeChangeListeners();
        mUserMembers.removeChangeListeners();
        mUserStatuses.removeChangeListeners();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CREATE_CHANNEL) {
                handleRequestCreateChannel(data);
            }
            if (requestCode == REQUEST_CREATE_GROUP) {
                handleRequestCreateGroup(data);
            }
            if (requestCode == REQUEST_JOIN_DIRECT) {
                handleRequestJoinDirect(data);
            }
            if (requestCode == REQUEST_JOIN_CHANNEL) {
                handleRequestJoinChannel(data);
            }
        }
    }

    @Override
    public void onChannelClick(String itemId, String name, String type) {
        removeSelection(type);
        sendOnChange(itemId, name, type);
        MattermostPreference.getInstance().setLastChannelId(itemId);
    }

    @Override
    public void onCreateChannelClick(View view) {
        switch (view.getId()) {
            case R.id.addChannel:
                CreateNewChannelActivity.startActivityForResult(this, REQUEST_CREATE_CHANNEL);
                break;
            case R.id.addGroup:
                CreateNewGroupActivity.startActivityForResult(this, REQUEST_CREATE_GROUP);
                break;
        }
    }

    @Override
    public void onRefresh() {
        getPresenter().requestUpdate();
    }

    public void setSelectItemMenu(String id, String typeChannel) {
        switch (typeChannel) {
            case Channel.OPEN:
                mChannelListAdapter.setSelectedItem(mChannelListAdapter.getPositionById(id));
                break;
            case Channel.PRIVATE:
                mPrivateListAdapter.setSelectedItem(mPrivateListAdapter.getPositionById(id));
                break;
            case Channel.DIRECT:
                mAdapterDirectMenuLeft.setSelectedItem(mAdapterDirectMenuLeft.getPositionById(id));
                break;
        }
    }

    public void initView() {
        initTeamHeader();
        initChannelList();
        initPrivateList();
        initDirectList();
    }
    public void setRefreshAnimation(boolean isVisible) {
        mBinding.leftSwipeRefresh.setRefreshing(isVisible);
    }

    public RealmResults<Channel> getDirectChannelData() {
        return ChannelRepository.query(new ChannelRepository.ChannelListDirectMenu());
    }

    public void selectLastChannel() {
        RealmResults<Channel> channels = ChannelRepository.query(
                new ChannelRepository.ChannelByIdSpecification(MattermostPreference.getInstance().getLastChannelId()));
        if (channels.size() > 0) {
            Channel channel = channels.first();
            setSelectItemMenu(channel.getId(), channel.getType());
        }
    }

    public void setOnChannelChangeListener(OnChannelChangeListener listener) {
        this.mListener = listener;
    }

    public void invalidateDirect() {
        if( getView()!=null) getView().postDelayed(() -> {
            mAdapterDirectMenuLeft.update();
            selectLastChannel();
            mBinding.frDirect.recView.invalidate();
        },200);

    }

    private void handleRequestJoinChannel(Intent data) {
        onChannelClick(data.getStringExtra(AddExistingChannelsActivity.sCHANNEL_ID),
                data.getStringExtra(AddExistingChannelsActivity.sCHANNEL_NAME),
                data.getStringExtra(AddExistingChannelsActivity.sTYPE));
        setSelectItemMenu(data.getStringExtra(AddExistingChannelsActivity.sCHANNEL_ID),
                data.getStringExtra(AddExistingChannelsActivity.sTYPE));
    }

    private void handleRequestJoinDirect(Intent data) {
        String userTalkToId = data.getStringExtra(WholeDirectListActivity.mUSER_ID);
        Preferences saveData = new Preferences(userTalkToId,
                MattermostPreference.getInstance().getMyUserId(),
                true,
                "direct_channel_show");
        RealmResults<Channel> channels = ChannelRepository.query(new ChannelDirectByIdSpecification(userTalkToId));
        if (channels.size() == 0) {
            getPresenter().requestSaveData(saveData, userTalkToId);
        } else {
            onChannelClick(channels.get(0).getId(),
                    channels.get(0).getUsername(),
                    channels.get(0).getType());
            setSelectItemMenu(data.getStringExtra(AddExistingChannelsActivity.sCHANNEL_ID),
                    data.getStringExtra(AddExistingChannelsActivity.sTYPE));
        }
    }

    private void handleRequestCreateGroup(Intent data) {
        mPrivateListAdapter.setSelectedItem(
                mPrivateListAdapter.getPositionById(data.getStringExtra(CreateNewGroupActivity.sCREATED_GROUP_ID)));
        onChannelClick(data.getStringExtra(CreateNewGroupActivity.sCREATED_GROUP_ID),
                data.getStringExtra(CreateNewGroupActivity.sGROUP_NAME),
                data.getStringExtra(CreateNewGroupActivity.sTYPE));
    }

    private void handleRequestCreateChannel(Intent data) {
        mChannelListAdapter.setSelectedItem(
                mChannelListAdapter.getPositionById(data.getStringExtra(CreateNewChannelActivity.sCREATED_CHANNEL_ID)));
        onChannelClick(data.getStringExtra(CreateNewChannelActivity.sCREATED_CHANNEL_ID),
                data.getStringExtra(CreateNewChannelActivity.CHANNEL_NAME),
                data.getStringExtra(CreateNewChannelActivity.sTYPE));
    }


    private void removeSelection(String type) {
        switch (type) {
            case OPEN:
                mAdapterDirectMenuLeft.setSelectedItem(NOT_SELECTED);
                mPrivateListAdapter.setSelectedItem(NOT_SELECTED);
                break;
            case PRIVATE:
                mChannelListAdapter.setSelectedItem(NOT_SELECTED);
                mAdapterDirectMenuLeft.setSelectedItem(NOT_SELECTED);
                break;
            case DIRECT:
                mChannelListAdapter.setSelectedItem(NOT_SELECTED);
                mPrivateListAdapter.setSelectedItem(NOT_SELECTED);
                break;
        }
    }

    private void sendOnChange(String itemId, String name, String type) {
        if (this.mListener != null) {
            this.mListener.onChange(itemId, name, type);
        }
    }


    private void initTeamHeader() {
        RealmResults<Team> teams = TeamRepository.query();
        for (Team item : teams) {
            if (item.getId().equals(MattermostPreference.getInstance().getTeamId())) {
                mBinding.leftMenuHeader.teamHeaderText.setText(item.getDisplayName());
            }
        }
    }

    private void initChannelList() {
        RealmResults<Channel> channels = ChannelRepository.query(new ChannelRepository.ChannelByTypeSpecification("O"));
        mBinding.frChannel.recView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.frChannel.recView.setNestedScrollingEnabled(false);
        mBinding.frChannel.addChannel.setOnClickListener(this::onCreateChannelClick);
        mBinding.frChannel.btnMoreChannel.setOnClickListener(this::openMore);
        mChannelListAdapter = new ChannelListAdapter(channels, getActivity(), mMembers, this);
        mBinding.frChannel.recView.setAdapter(mChannelListAdapter);
    }

    private void initPrivateList() {
        RealmResults<Channel> channels = ChannelRepository.query(new ChannelRepository.ChannelByTypeSpecification("P"));
        mBinding.frPrivate.recView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.frPrivate.recView.setNestedScrollingEnabled(false);
        mBinding.frPrivate.addGroup.setOnClickListener(this::onCreateChannelClick);
        mPrivateListAdapter = new PrivateListAdapter(channels, getActivity(), mMembers, this);
        mBinding.frPrivate.recView.setAdapter(mPrivateListAdapter);
    }

    private void initDirectList() {
        RealmResults<Channel> channels = getDirectChannelData();
        mBinding.frDirect.recView.setLayoutManager(new LeftMenuLayoutManager(getActivity()));
        mBinding.frDirect.recView.setNestedScrollingEnabled(false);
        mBinding.frDirect.btnMore.setOnClickListener(this::openMore);
        mAdapterDirectMenuLeft = new AdapterDirectMenuLeft(channels,getActivity(),this);
        channels.addChangeListener(element ->{
            mAdapterDirectMenuLeft.update();
            Log.d(TAG, "initNewAdapter: change");
        });
        mBinding.frDirect.recView.setAdapter(mAdapterDirectMenuLeft);

    }


    private void openMore(View view) {
        switch (view.getId()) {
            case R.id.btnMoreChannel:
                AddExistingChannelsActivity.startActivityForResult(this, REQUEST_JOIN_CHANNEL);
                break;
            case R.id.btnMore:
                WholeDirectListActivity.startActivityForResult(this, REQUEST_JOIN_DIRECT);
                break;
        }
    }


    public void showErrorLoading(String message) {
        showErrorScene(message);
    }

    private void showErrorScene(String message) {
        mBinding.errorText.setText(message);
        mBinding.frChannel.getRoot().setVisibility(View.GONE);
        mBinding.frPrivate.getRoot().setVisibility(View.GONE);
        mBinding.frDirect.getRoot().setVisibility(View.GONE);
        mBinding.errorLayout.setVisibility(View.VISIBLE);
        setRefreshAnimation(false);
    }

    public void showLeftMenu(){
        mBinding.frChannel.getRoot().setVisibility(View.VISIBLE);
        mBinding.frPrivate.getRoot().setVisibility(View.VISIBLE);
        mBinding.frDirect.getRoot().setVisibility(View.VISIBLE);
        setRefreshAnimation(false);
    }

    private void showFirstLoadingMenu(){
        mBinding.frChannel.getRoot().setVisibility(View.GONE);
        mBinding.frPrivate.getRoot().setVisibility(View.GONE);
        mBinding.frDirect.getRoot().setVisibility(View.GONE);
        mBinding.errorLayout.setVisibility(View.GONE);
        setRefreshAnimation(true);
    }
}
