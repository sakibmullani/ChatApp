package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.chatapp.AuthenticationActivity;
import com.example.chatapp.GroupModel;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference userReference;
    private DatabaseReference groupReference;
    private UserAdapter userAdapter;
    private List<GroupModel> groupList;
    private List<UserModel> userList;
//    private SwipeRefreshLayout swipeRefreshLayout;
    AlertDialog createGroupDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Adapter and RecyclerView setup
        userAdapter = new UserAdapter(this);
        binding.recyclerViewid.setAdapter(userAdapter);
        binding.recyclerViewid.setLayoutManager(new LinearLayoutManager(this));

        userReference = FirebaseDatabase.getInstance().getReference("user");
        groupReference = FirebaseDatabase.getInstance().getReference("groups");
        groupList = new ArrayList<>();
        userList = new ArrayList<>();

//        swipeRefreshLayout = binding.swipeRefreshLayout;
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                retrieveUsers();
//                retrieveGroups();
//                swipeRefreshLayout.setRefreshing(false);
//            }
//        });

        retrieveUsers();
        retrieveGroups();


        binding.fabCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retriveUsersFromGroupcreation();
            }
        });
    }

    private void retriveUsersFromGroupcreation() {
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String uid = dataSnapshot.getKey();
                    if (!uid.equals(FirebaseAuth.getInstance().getUid())) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        userList.add(userModel);
                    }
                }
                showCreateGroupDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Group");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        builder.setView(view);

        EditText groupNameEditText = view.findViewById(R.id.groupNameEditText);
        EditText groupDescriptionEditText = view.findViewById(R.id.groupDescriptionEditText);
        ListView membersListView = view.findViewById(R.id.membersListView);

        UserListAdapter userListAdapter = new UserListAdapter(userList);
        membersListView.setAdapter(userListAdapter);

        builder.setPositiveButton("Create", null);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        createGroupDialog = builder.create();


        createGroupDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button createButton = createGroupDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                createButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String groupName = groupNameEditText.getText().toString().trim();
                        String groupDescription = groupDescriptionEditText.getText().toString().trim();

                        if (groupName.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Enter Group Name", Toast.LENGTH_SHORT).show();
                        }
                        if (groupDescription.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Enter Group Description", Toast.LENGTH_SHORT).show();
                        } else if (groupName.length() < 3) {
                            Toast.makeText(MainActivity.this, "Group Name must be at least 3 characters long", Toast.LENGTH_SHORT).show();
                        } else {
                            List<String> selectedUserIds = userListAdapter.getSelectedUserIds();
                            if (selectedUserIds.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Select at least one member for the group", Toast.LENGTH_SHORT).show();
                            } else {
                                createGroup(groupName,groupDescription,selectedUserIds);
                                createGroupDialog.dismiss();
                            }
                        }
                    }
                });
            }
        });

        createGroupDialog.show();
    }

    private void createGroup(String groupName, String groupDescription, List<String> selectedUserIds) {
        DatabaseReference groupRef = groupReference.push();
        String groupId = groupRef.getKey();

        GroupModel groupModel = new GroupModel(groupId, groupName, groupDescription);
        groupRef.setValue(groupModel);

        selectedUserIds.add(FirebaseAuth.getInstance().getUid());

        for (String userId : selectedUserIds) {
            DatabaseReference userGroupRef = userReference.child(userId).child("groups").child(groupId);
            userGroupRef.setValue(true);
        }

        // Add the "members" node to the group entry
        DatabaseReference groupMembersRef = groupRef.child("members");
        for (String userId : selectedUserIds) {
            groupMembersRef.child(userId).setValue(true);
        }

        groupList.add(groupModel);
        userAdapter.clearGroups();
        userAdapter.addAllGroups(groupList);

        Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
    }

    private void retrieveUsers() {
        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userAdapter.clearUsers();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String uid = dataSnapshot.getKey();
                    if (!uid.equals(FirebaseAuth.getInstance().getUid())) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        userAdapter.addUser(userModel);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void retrieveGroups() {
        groupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear(); // Clear the list before adding groups

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    GroupModel groupModel = dataSnapshot.getValue(GroupModel.class);
                    String groupId = groupModel.getGroupId();

                    DatabaseReference groupMembersRef = groupReference.child(groupId).child("members");
                    groupMembersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(FirebaseAuth.getInstance().getUid())) {
                                groupList.add(groupModel);
                                userAdapter.addGroup(groupModel);
                            } else {
                                userAdapter.removeGroup(groupId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(MainActivity.this, "Failed to retrieve group members", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to retrieve groups", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
