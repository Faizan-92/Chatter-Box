package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private FirebaseAuth mAuth;
    private TabAccessorAdapter mTabsAccessorAdapter;
    private DatabaseReference rootRef,newReqRef;
    private String curUserId;
    private String saveCurTime,saveCurDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar=findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Chatter Box By Faizan");

        mAuth=FirebaseAuth.getInstance();
        rootRef=FirebaseDatabase.getInstance().getReference();
        newReqRef=rootRef.child("New Requests");

        mViewPager=findViewById(R.id.main_tabs_pager);

        mTabsAccessorAdapter=new TabAccessorAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabsAccessorAdapter);

        mTabLayout=findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);


    }

    protected void onStart()
    {
        super.onStart();
        FirebaseUser currentUser=mAuth.getCurrentUser();

        if(currentUser==null)
        {
            Intent intent=new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //That | is a pipe symbol
            startActivity(intent);
            finish();
        }
        else
        {
            updateUserStatus("Online");
            verifyUserExistence(); //To check whether the user has previously logged in to this app, or is he a new user ?
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser=mAuth.getCurrentUser();
        if(currentUser!=null)
        {
            updateUserStatus("Offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //If the app crashes
        FirebaseUser currentUser=mAuth.getCurrentUser();
        if(currentUser!=null)
        {
            updateUserStatus("Offline");
        }
    }

    private void verifyUserExistence() {
        String curUserId=mAuth.getCurrentUser().getUid();
        rootRef.child("Users").child(curUserId).addValueEventListener(new ValueEventListener() {

            //This will be called every time there will be a change in the database and also initially when you start the activity.
            //Datasnapshot, as the name suggests, captures the whole database. You need to do is iterate through the snapshot.
            boolean flag=true;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("name").exists())
                {
                    //If we already have information of his name, it means he has logged in previously as well.
                }
                else if(flag)
                {
                    flag=false;
                    Toast.makeText(MainActivity.this,"Welcome New User",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //That | is a pipe symbol
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getItemId()==R.id.main_logout_option)
        {
            updateUserStatus("Offline");
            mAuth.signOut();
            Intent intent=new Intent(MainActivity.this,LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        if(item.getItemId()==R.id.main_create_group_option)
        {
            AlertDialog.Builder builder=new AlertDialog.Builder(this,R.style.AlertDialog);
            builder.setTitle("Enter group name : ");
            final EditText groupNameField=new EditText(this);
            groupNameField.setHint("e.g. Rockstars");
            builder.setView(groupNameField);

            builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String groupName=groupNameField.getText().toString();
                    if(TextUtils.isEmpty(groupName)){
                        Toast.makeText(MainActivity.this,"Please write group name",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        createNewGroup(groupName);
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
        }
        if(item.getItemId()==R.id.main_settings_option)
        {
            Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
        }
        if(item.getItemId()==R.id.main_find_friends_option)
        {
            sendUserToFindFriendActivity();
        }
        return true;
    }

    private void sendUserToFindFriendActivity() {
        Intent intent=new Intent(MainActivity.this,FindFriendsActivity.class);
        startActivity(intent);
    }

    private void createNewGroup(String groupName) {
        rootRef.child("Groups").child(groupName).setValue("").
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(MainActivity.this,"Group created successfully",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String exception=task.getException().toString();
                            Toast.makeText(MainActivity.this,"Error creating group : "+exception,Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void updateUserStatus(String state)
    {

        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat curDate=new SimpleDateFormat("MMM dd, yyyy");
        saveCurDate=curDate.format(calendar.getTime());
        SimpleDateFormat curTime=new SimpleDateFormat("hh:mm a");
        saveCurTime=curTime.format(calendar.getTime());

        HashMap<String,Object> hm=new HashMap<>();
        hm.put("time",saveCurTime);
        hm.put("date",saveCurDate);
        hm.put("state",state);

        curUserId=mAuth.getCurrentUser().getUid();

        rootRef.child("Users").child(curUserId).child("userState")
                .updateChildren(hm);
    }
}
