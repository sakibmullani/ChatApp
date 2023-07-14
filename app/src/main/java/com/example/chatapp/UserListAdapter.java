package com.example.chatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends BaseAdapter {
    private List<UserModel> userList;
    private List<String> selectedUserIds;

    public UserListAdapter(List<UserModel> userList) {
        this.userList = userList;
        this.selectedUserIds = new ArrayList<>();
    }

    public List<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public Object getItem(int position) {
        return userList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item_multiple_choice, parent, false);
        }

        TextView userNameTextView = convertView.findViewById(R.id.userNameTextView);

        UserModel userModel = userList.get(position);
        userNameTextView.setText(userModel.getUserName());

        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        checkBox.setChecked(selectedUserIds.contains(userModel.getUserid()));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedUserIds.add(userModel.getUserid());
                } else {
                    selectedUserIds.remove(userModel.getUserid());
                }
            }
        });

        return convertView;
    }
}

