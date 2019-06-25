package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receivedUserId,curState,senderUserId;
    private Button sendMessageRequestButton,declineChatRequestButton;
    private TextView userProfileName,userProfileStatus;
    private CircleImageView userProfileImage;
    private DatabaseReference usersRef,chatReqRef,contactsRef,notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        receivedUserId=getIntent().getExtras().get("visitUserId").toString();
        userProfileImage=findViewById(R.id.visit_profile_image);
        userProfileName=findViewById(R.id.visit_user_name);
        userProfileStatus=findViewById(R.id.visit_profile_status);
        sendMessageRequestButton=findViewById(R.id.send_message_request_button);
        declineChatRequestButton=findViewById(R.id.decline_message_request_button);
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        chatReqRef= FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef=FirebaseDatabase.getInstance().getReference().child("Notifications");

        curState="new"; //i.e. assuming that the two users are currently new to each other
        mAuth=FirebaseAuth.getInstance();
        senderUserId=mAuth.getCurrentUser().getUid();
        retrieveUserInfo();

    }

    private void retrieveUserInfo() {
        usersRef.child(receivedUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("image")){
                    String userImage=dataSnapshot.child("image").getValue().toString();
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();
                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);
                }
                else if(dataSnapshot.exists()){
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);
                }
                manageChatRequest();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void manageChatRequest() {

        //If the chat request is already sent
        chatReqRef.child(senderUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(receivedUserId)){
                            String requestType=dataSnapshot.child(receivedUserId).child("request_type").getValue().toString();
                            if(requestType.equals("sent")){
                                curState="request_sent";
                                sendMessageRequestButton.setText("Cancel Chat Request");
                            }
                            else if(requestType.equals("received")){
                                curState="request_received";
                                sendMessageRequestButton.setText("Accept Chat Request");
                                declineChatRequestButton.setVisibility(View.VISIBLE);
                                declineChatRequestButton.setEnabled(true);
                                declineChatRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        cancelChatRequest();
                                    }
                                });
                            }
                        }
                        else{
                            contactsRef.child(senderUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(receivedUserId)){
                                                curState="friends";
                                                sendMessageRequestButton.setText("Unfriend");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        //A user should not be able to chat with himself
        if(!senderUserId.equals(receivedUserId)){
            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessageRequestButton.setEnabled(false);
                    if(curState.equals("new")){
                        sendChatRequest();
                    }
                    if(curState.equals("request_sent")){
                        //Now the  user has the optional to cancel chat request
                        cancelChatRequest();
                    }
                    if(curState.equals("request_received")){
                        acceptChatRequest();
                    }
                    if(curState.equals("friends")){
                        removeSpecificContact();
                    }
                }
            });
        }
        else {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void removeSpecificContact() {
        contactsRef.child(senderUserId).child(receivedUserId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            contactsRef.child(receivedUserId).child(senderUserId).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                curState = "new";
                                                sendMessageRequestButton.setEnabled(true);
                                                sendMessageRequestButton.setText("Send Chat Request");
                                                declineChatRequestButton.setVisibility(View.INVISIBLE);
                                                declineChatRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void acceptChatRequest() {
        contactsRef.child(senderUserId).child(receivedUserId)
                .child("Contacts").setValue("Saved").
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            contactsRef.child(receivedUserId).child(senderUserId)
                                    .child("Contacts").setValue("Saved").
                                    addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                chatReqRef.child(senderUserId).child(receivedUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    chatReqRef.child(receivedUserId).child(senderUserId)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    sendMessageRequestButton.setEnabled(true);
                                                                                    curState="friends";
                                                                                    sendMessageRequestButton.setText("Unfriend");
                                                                                    declineChatRequestButton.setVisibility(View.INVISIBLE);
                                                                                    declineChatRequestButton.setEnabled(false);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void cancelChatRequest() {
        chatReqRef.child(senderUserId).child(receivedUserId).removeValue()
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    chatReqRef.child(receivedUserId).child(senderUserId).removeValue()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                curState = "new";
                                sendMessageRequestButton.setEnabled(true);
                                sendMessageRequestButton.setText("Send Chat Request");
                                declineChatRequestButton.setVisibility(View.INVISIBLE);
                                declineChatRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void sendChatRequest() {
        chatReqRef.child(senderUserId).child(receivedUserId).
                child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            chatReqRef.child(receivedUserId).child(senderUserId)
                                    .child("request_type").setValue("received")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        HashMap<String,String> hm=new HashMap<>();
                                        hm.put("from",senderUserId);
                                        hm.put("type","request");
                                        notificationRef.child(receivedUserId).push().
                                         setValue(hm)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    sendMessageRequestButton.setEnabled(true);
                                                    curState="request_sent";
                                                    sendMessageRequestButton.setText("Cancel Chat Request");
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
    }
}
