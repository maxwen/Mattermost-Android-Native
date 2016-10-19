package com.kilogramm.mattermost.view.menu.channelList;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.FragmentMenuChannelListBinding;
import com.kilogramm.mattermost.model.entity.channel.Channel;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.viewmodel.menu.FrMenuChannelViewModel;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Evgeny on 24.08.2016.
 */

public class MenuChannelListFragment extends Fragment {

    private FragmentMenuChannelListBinding binding;
    private FrMenuChannelViewModel viewModel;
    private OnChannelItemClickListener channelItemClickListener;
    private OnSelectedItemChangeListener selectedItemChangeListener;
    private AdapterMenuChannelList adapter;

    private Realm realm;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_menu_channel_list,
                container, false);
        View view = binding.getRoot();
        viewModel = new FrMenuChannelViewModel(getContext());
        binding.setViewModel(viewModel);
        setupListView();
        return view;
    }


    private void setupListView() {
        RealmResults<Channel> results = new ChannelRepository.ChannelByTypeSpecification("O").toRealmResults(realm);
        binding.recView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterMenuChannelList(getContext(), results, binding.recView,
                (itemId, name, type) -> channelItemClickListener.onChannelClick(itemId, name, type));
        if (selectedItemChangeListener != null) {
            adapter.setSelectedItemChangeListener(selectedItemChangeListener);
        }
        binding.recView.setAdapter(adapter);
    }

    public OnChannelItemClickListener getListener() {
        return channelItemClickListener;
    }

    public void setListener(OnChannelItemClickListener listener) {
        this.channelItemClickListener = listener;
    }

    public void setSelectedItemChangeListener(OnSelectedItemChangeListener selectedItemChangeListener) {
        this.selectedItemChangeListener = selectedItemChangeListener;
    }

    public interface OnChannelItemClickListener {
        void onChannelClick(String itemId, String name, String type);
    }

    public interface OnSelectedItemChangeListener {
        void onChangeSelected(int position);
    }

    public void resetSelectItem() {
        adapter.setSelecteditem(-1);
    }

    public void selectItem(String id) {
        OrderedRealmCollection<Channel> channels = adapter.getData();
        int i = 0;
        for (Channel channel : channels) {
            if (channel.getId().equals(id)) {
                adapter.setSelecteditem(i);
                return;
            }
            i++;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(realm!=null && !realm.isClosed()){
            realm.close();
        }
    }
}

