package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private String msgReceiverId,msgSenderId,msgReceiverName,msgReceiverImage,checker="",myUrl="";
    private TextView userName,userLastSeen;
    private FirebaseAuth mAuth;
    private CircleImageView userImage;
    private DatabaseReference rootReference;
    private ImageButton sendMessageButton,sendFilesButton;
    private EditText sendMessageText;
    private final List<Message> messageList=new ArrayList<>();
    private LinearLayoutManager llm;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private String saveCurDate;
    private String saveCurTime;
    private Uri fileUri;
    private StorageTask uploadTask;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth=FirebaseAuth.getInstance();
        msgSenderId=mAuth.getCurrentUser().getUid();
        rootReference= FirebaseDatabase.getInstance().getReference();

        mRecyclerView=findViewById(R.id.chat_activity_recycler_view);
        sendMessageButton=findViewById(R.id.chat_activity_send_message_button);
        sendFilesButton=findViewById(R.id.send_files_button);
        sendMessageText=findViewById(R.id.chat_activity_send_message_text);

        msgReceiverId=getIntent().getExtras().get("visit_user_id").toString();
        msgReceiverName=getIntent().getExtras().get("visit_user_name").toString();
        msgReceiverImage=getIntent().getExtras().get("visit_user_image").toString();

        mToolbar=findViewById(R.id.chat_activity_toolbar);
        setSupportActionBar(mToolbar);

        LayoutInflater lin= (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView=lin.inflate(R.layout.custom_chat_bar,null);
        getSupportActionBar().setDisplayShowCustomEnabled(true);//You need to tell explicitly that you want custom toolbar. i.e. for the next line to work.
        getSupportActionBar().setCustomView(actionBarView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        userImage=findViewById(R.id.custom_profile_image);
        userName=findViewById(R.id.custom_profile_name);
        userLastSeen=findViewById(R.id.custom_user_last_seen);

        userName.setText(msgReceiverName);

        if(msgReceiverImage.equals("no_image"))
            Picasso.get().load(R.drawable.profile_image).into(userImage);
        else
            Picasso.get().load(msgReceiverImage).into(userImage);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        sendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence options [] =new CharSequence[]
                        {
                                "Images",
                                "PDF Files",
                                "MS Word Files"
                        };
                AlertDialog.Builder builder=new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select the file");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    Intent intent;
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i)
                        {
                            case 0:
                                checker="image";
                                intent=new Intent();
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                startActivityForResult(Intent.createChooser(intent,"Select Image"),438);
                                break;
                            case 1:
                                checker="pdf";
                                intent=new Intent();
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.setType("application/pdf");
                                startActivityForResult(Intent.createChooser(intent,"Select PDF File"),438);
                                break;
                            case 2:
                                checker="docx";
                                intent=new Intent();
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.setType("application/msword");
                                startActivityForResult(Intent.createChooser(intent,"Select MS Word File"),438);
                                break;
                        }
                    }
                });
                builder.show();
            }
        });


        messageAdapter=new MessageAdapter(messageList);
        userMessagesList=findViewById(R.id.chat_activity_recycler_view);
        llm=new LinearLayoutManager(this);
        userMessagesList.setAdapter(messageAdapter);
        userMessagesList.setLayoutManager(llm);
        loadingBar=new ProgressDialog(this);

        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat curDate=new SimpleDateFormat("MMM dd, yyyy");
        saveCurDate=curDate.format(calendar.getTime());
        SimpleDateFormat curTime=new SimpleDateFormat("hh:mm a");
        saveCurTime=curTime.format(calendar.getTime());

        displayLastSeen();
        rootReference.child("Messages").child(msgSenderId).child(msgReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message message=dataSnapshot.getValue(Message.class);
                        messageList.add(message);
                        messageAdapter.notifyDataSetChanged();
                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==438 && resultCode==RESULT_OK && data!=null && data.getData()!=null)
        {
            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            fileUri=data.getData();
            if(checker.equals("image"))
            {
                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Image Files");

                final String msgSenderRef="Messages/"+msgSenderId+"/"+msgReceiverId;
                final String msgReceiverRef="Messages/"+msgReceiverId+"/"+msgSenderId;
                DatabaseReference msgId=rootReference.child("Messages").child(msgSenderId).child(msgReceiverId).push();
                final String pushId=msgId.getKey();

                final StorageReference filePath=storageReference.child(pushId+".jpg");
                uploadTask=filePath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if(!task.isSuccessful()){
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()){
                            Uri downloadUrl=task.getResult();
                            myUrl=downloadUrl.toString();


                            Map hm=new HashMap();
                            hm.put("message",myUrl);
                            hm.put("name",fileUri.getLastPathSegment());
                            hm.put("type",checker);
                            hm.put("from",msgSenderId);
                            hm.put("to",msgReceiverId);
                            hm.put("messageId",pushId);
                            hm.put("time",saveCurTime);
                            hm.put("date",saveCurDate);

                            Map msgBodyDetail=new HashMap();
                            msgBodyDetail.put(msgSenderRef+"/"+pushId,hm);
                            msgBodyDetail.put(msgReceiverRef+"/"+pushId,hm);


                            rootReference.updateChildren(msgBodyDetail).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if(!task.isSuccessful())
                                    {
                                        Toast.makeText(ChatActivity.this,"Error, Image not sent.",Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {

                                    }
                                    loadingBar.dismiss();
                                    sendMessageText.setText("");
                                }
                            });
                        }
                    }
                });

            }
            else
            {
                //PDF or Word File
                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Document Files");
                final String msgSenderRef="Messages/"+msgSenderId+"/"+msgReceiverId;
                final String msgReceiverRef="Messages/"+msgReceiverId+"/"+msgSenderId;
                DatabaseReference msgId=rootReference.child("Messages").child(msgSenderId).child(msgReceiverId).push();
                final String pushId=msgId.getKey();
                final StorageReference filePath=storageReference.child(pushId+"."+checker);
                filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                             if(task.isSuccessful())
                             {
                                 final Map hm=new HashMap();
                                 filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                     @Override
                                     public void onSuccess(Uri uri) {
                                         String downloadUrl=uri.toString();
                                         hm.put("message",downloadUrl);
                                         hm.put("from",msgSenderId);
                                         hm.put("to",msgReceiverId);
                                         hm.put("type",checker);
                                         hm.put("messageId",pushId);
                                         hm.put("time",saveCurTime);
                                         hm.put("date",saveCurDate);

                                         Map msgBodyDetail=new HashMap();
                                         msgBodyDetail.put(msgSenderRef+"/"+pushId,hm);
                                         msgBodyDetail.put(msgReceiverRef+"/"+pushId,hm);

                                         rootReference.updateChildren(msgBodyDetail);
                                     }
                                 });
                             }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this,"Error : "+e.getMessage(),Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress=(100.0 * taskSnapshot.getBytesTransferred())/(taskSnapshot.getTotalByteCount());
                        loadingBar.setMessage((int)progress+" % Uploaded");
                        loadingBar.setProgress((int)progress);
                        if(progress==100)
                        {
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void sendMessage()
    {
        String msgText=sendMessageText.getText().toString();
        if(!TextUtils.isEmpty(msgText)){
            String msgSenderRef="Messages/"+msgSenderId+"/"+msgReceiverId;
            String msgReceiverRef="Messages/"+msgReceiverId+"/"+msgSenderId;
            DatabaseReference msgId=rootReference.child("Messages").child(msgSenderId).child(msgReceiverId).push();
            String pushId=msgId.getKey();

            Map hm=new HashMap();
            hm.put("message",msgText);
            hm.put("type","text");
            hm.put("from",msgSenderId);
            hm.put("to",msgReceiverId);
            hm.put("time",saveCurTime);
            hm.put("date",saveCurDate);
            hm.put("messageId",pushId);

            Map msgBodyDetail=new HashMap();
            msgBodyDetail.put(msgSenderRef+"/"+pushId,hm);
            msgBodyDetail.put(msgReceiverRef+"/"+pushId,hm);


            rootReference.updateChildren(msgBodyDetail).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(!task.isSuccessful()){
                        Toast.makeText(ChatActivity.this,"Error, Message not sent.",Toast.LENGTH_SHORT).show();
                    }else{
                        sendMessageText.setText("");
                    }
                }
            });
        }
    }

    private void displayLastSeen()
    {
        rootReference.child("Users").child(msgReceiverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("userState").hasChild("state"))
                        {
                            String state=dataSnapshot.child("userState").child("state").getValue().toString();
                            String date=dataSnapshot.child("userState").child("date").getValue().toString();
                            String time=dataSnapshot.child("userState").child("time").getValue().toString();

                            if(state.equals("Online"))
                            {
                                userLastSeen.setText("Online");

                            }
                            else
                            {
                                userLastSeen.setText("Last Seen: "+date+" "+time);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
