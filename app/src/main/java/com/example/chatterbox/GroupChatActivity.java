package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class GroupChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton sendMessageButton;
    private EditText userMessageInput;
    private ScrollView mScrollView;
    private TextView displayTextMessages;
    private String curGroupName,curUserId,curUserName,curDate,curTime;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef,groupNameRef,groupMessageKeyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        mToolbar=findViewById(R.id.group_chat_bar_layout);
        curGroupName=getIntent().getExtras().get("groupName").toString();
        mToolbar.setTitle(curGroupName);

        sendMessageButton=findViewById(R.id.send_message_button);
        userMessageInput=findViewById(R.id.input_group_message);
        mScrollView=findViewById(R.id.my_scroll_view);
        displayTextMessages=findViewById(R.id.group_chat_text_display);
        mAuth=FirebaseAuth.getInstance();
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        groupNameRef=FirebaseDatabase.getInstance().getReference().child("Groups").child(curGroupName);
        curUserId=mAuth.getCurrentUser().getUid();

        usersRef.child(curUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    curUserName=dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMessageToDatabase();
                userMessageInput.setText("");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }

   protected void onStart(){
        super.onStart();
        groupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    displayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    displayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayMessages(DataSnapshot dataSnapshot) {
        Iterator iterator=dataSnapshot.getChildren().iterator();
        while(iterator.hasNext()){
            String chatDate=((DataSnapshot)iterator.next()).getValue().toString();
            String chatMessage=((DataSnapshot)iterator.next()).getValue().toString();
            String chatName=((DataSnapshot)iterator.next()).getValue().toString();
            String chatTime=((DataSnapshot)iterator.next()).getValue().toString();
            displayTextMessages.append(chatName+"\n\n"+chatMessage+"\n\n"+chatTime+"\t"+chatDate+"\n\n\n");
        }
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void saveMessageToDatabase() {
        String message=userMessageInput.getText().toString();
        if(!TextUtils.isEmpty(message)){
            Calendar calForDate=Calendar.getInstance();
            SimpleDateFormat currentDateFormat=new SimpleDateFormat("MMM dd, yyyy");
            curDate=currentDateFormat.format(calForDate.getTime());

            Calendar calForTime=Calendar.getInstance();
            SimpleDateFormat currentTimeFormat=new SimpleDateFormat("hh:mm a"); //last a for AM or PM formatting
            curTime=currentTimeFormat.format(calForTime.getTime());

            //HashMap<String,Object> groupMessageKey=new HashMap<>();
            //groupNameRef.updateChildren(groupMessageKey);
            String messageKey=groupNameRef.push().getKey();
            groupMessageKeyRef=groupNameRef.child(messageKey);

            HashMap<String,Object> messageInfoMap=new HashMap<>();
            messageInfoMap.put("name",curUserName);
            messageInfoMap.put("message",message);
            messageInfoMap.put("date",curDate);
            messageInfoMap.put("time",curTime);

            groupMessageKeyRef.updateChildren(messageInfoMap);

        }
    }
}
