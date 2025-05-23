package com.example.mymegaawesomechat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class UserListActivity extends AppCompatActivity {
    private String userName;

    private FirebaseAuth auth;
    private DatabaseReference usersDatabaseReference;
    private ChildEventListener usersChildEventListener;

    private ArrayList<User> userArrayList;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private RecyclerView.LayoutManager userLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);


        userArrayList = new ArrayList<>();

        buildRecyclerView();

        attachUserDatabaseReferenceListener();



        
    }

    private void attachUserDatabaseReferenceListener() {
        usersDatabaseReference = FirebaseDatabase
                .getInstance("https://firstfirebaseandroidproj-6bc5e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference().child("users");

        if (usersChildEventListener == null){
            Log.d("attachTest","testing");
            usersChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot,
                                         @Nullable String previousChildName) {
                    User user = snapshot.getValue(User.class);
                    user.setAvatarMockUpResource(R.drawable.baseline_person_24);
                    userArrayList.add(user);
                    userAdapter.notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot,
                                           @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot,
                                         @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            usersDatabaseReference.addChildEventListener(usersChildEventListener);
        }
    }

    private void buildRecyclerView() {
        Log.d("buildRecyclerVTest","testing2");
        userRecyclerView = findViewById(R.id.userListRecyclerView);
        userRecyclerView.setHasFixedSize(true);
        userLayoutManager = new LinearLayoutManager(this);
        userAdapter = new UserAdapter(userArrayList);

        userRecyclerView.setLayoutManager(userLayoutManager);
        userRecyclerView.setAdapter(userAdapter);

    }
}