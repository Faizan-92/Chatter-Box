package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName,userStatus;
    private CircleImageView userProfileImage;
    private String curUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private static final int galleryPick=1;
    private StorageReference userProfileImagesRef;
    private ProgressDialog loadingBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //Toast.makeText(SettingsActivity.this,"Settings activity called",Toast.LENGTH_SHORT).show();
        updateAccountSettings=findViewById(R.id.update_settings_button);
        userName=findViewById(R.id.set_user_name);
        userStatus=findViewById(R.id.set_profile_status);
        userProfileImage=findViewById(R.id.set_profile_image);
        toolbar=findViewById(R.id.settings_toolbar);

        mAuth=FirebaseAuth.getInstance();
        curUserId=mAuth.getCurrentUser().getUid();
        rootRef= FirebaseDatabase.getInstance().getReference();
        userProfileImagesRef= FirebaseStorage.getInstance().getReference().child("Profile Images");
        loadingBar=new ProgressDialog(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings();
            }
        });

        retrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onActivityResult(galleryPick,RESULT_OK,null);
                //Intent galleryIntent=new Intent();
                //galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                //galleryIntent.setType("image/*");
                //startActivityForResult(galleryIntent,galleryPick);
            }
        });
    }

    private void retrieveUserInfo() {
        final boolean[] flag = {true};
        rootRef.child("Users").child(curUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("status") && dataSnapshot.hasChild("image")){
                    //If the user has previously created account
                    String retrievedUserName=dataSnapshot.child("name").getValue().toString();
                    String retrievedStatus=dataSnapshot.child("status").getValue().toString();
                    String retrievedImage=dataSnapshot.child("image").getValue().toString(); //The path(URL) of image is retrieved here.

                    userName.setText(retrievedUserName);
                    userStatus.setText(retrievedStatus);
                    Picasso.get().load(retrievedImage).into(userProfileImage);
                }
                else if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("status")){
                    //If the user has previously created account
                    String retrievedUserName=dataSnapshot.child("name").getValue().toString();
                    String retrievedStatus=dataSnapshot.child("status").getValue().toString();

                    userName.setText(retrievedUserName);
                    userStatus.setText(retrievedStatus);
                }
                else if(flag[0]){
                    flag[0] =false;
                    Toast.makeText(SettingsActivity.this,"Please set your profile information",Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onActivityResult(int reqCode,int resCode,Intent data){
        super.onActivityResult(reqCode,resCode,data);
        if(reqCode==galleryPick && resCode==RESULT_OK){
            //Uri imageUri=data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            //Toast.makeText(SettingsActivity.this,"Here 1",Toast.LENGTH_SHORT).show();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);

        }
        if (reqCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //Toast.makeText(SettingsActivity.this,"Here 2",Toast.LENGTH_SHORT).show();
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resCode==RESULT_OK){
                //Toast.makeText(SettingsActivity.this,"Result is OK",Toast.LENGTH_SHORT).show();
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is being updated");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                Uri resultUri=result.getUri();//This contains the cropped image

                final StorageReference filePath=userProfileImagesRef.child(curUserId+".jpg");//This way we link the userId with image. This is the file name of the image stored in firebase database.

                filePath.putFile(resultUri)
                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                      @Override
                                      public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                          if (task.isSuccessful()) {
                                              filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                  @Override
                                                  public void onSuccess(Uri uri) {

                                                      String downloadUri = uri.toString();
                                                      if (downloadUri != null) {

                                                          String downloadUrl = downloadUri; //YOU WILL GET THE DOWNLOAD URL HERE !!!!
                                                          rootRef.child("Users").child(curUserId).child("image").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                              @Override
                                                              public void onComplete(@NonNull Task<Void> task) {
                                                                  loadingBar.dismiss();

                                                                  if(!task.isSuccessful()){

                                                                      String error=task.getException().toString();

                                                                  }else{

                                                                  }
                                                              }
                                                          });
                                                      }
                                                  }
                                              });
                                          }

                                          else {
                                              // Handle failures
                                              // ...
                                              Toast.makeText(SettingsActivity.this,"Error",Toast.LENGTH_LONG).show();
                                              loadingBar.dismiss();
                                          }
                                      }
                                  });
                        }
                    }
                }


    private void updateSettings()
    {
        String setUserName=userName.getText().toString();
        String setStatus=userStatus.getText().toString();
        //Profile picture is optional
        if(TextUtils.isEmpty(setUserName))
            Toast.makeText(this,"Please write your user name",Toast.LENGTH_SHORT).show();
        else if(TextUtils.isEmpty(setStatus))
            Toast.makeText(this,"Please write your status",Toast.LENGTH_SHORT).show();
        else
        {
            HashMap<String,Object> profileMap=new HashMap<>();
            profileMap.put("uid",curUserId);
            profileMap.put("name",setUserName);
            profileMap.put("status",setStatus);
            rootRef.child("Users").child(curUserId).updateChildren(profileMap).
                    addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                Toast.makeText(SettingsActivity.this,"Profile Update Successfully",Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                String error=task.getException().toString();
                                Toast.makeText(SettingsActivity.this,"Updatation Failed ! "+error,Toast.LENGTH_SHORT).show();
                            }
                            sendToMainActivity();
                        }
                    });
        }
    }

    private void sendToMainActivity() {
        Intent intent=new Intent(this,MainActivity.class);
        startActivity(intent);
    }
}
