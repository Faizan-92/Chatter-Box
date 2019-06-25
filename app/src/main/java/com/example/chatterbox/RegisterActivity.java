package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class RegisterActivity extends AppCompatActivity {

    private EditText userEmail,userPassword;
    private Button createAccountButton;
    private TextView alreadyHaveAccountLink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference rootReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userEmail=findViewById(R.id.register_email);
        userPassword=findViewById(R.id.register_password);
        createAccountButton=findViewById(R.id.register_button);
        alreadyHaveAccountLink=findViewById(R.id.already_have_account_link);
        loadingBar=new ProgressDialog(this);

        mAuth=FirebaseAuth.getInstance();

        rootReference= FirebaseDatabase.getInstance().getReference();

        alreadyHaveAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RegisterActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=userEmail.getText().toString();
                String password=userPassword.getText().toString();

                if(TextUtils.isEmpty(email))
                    Toast.makeText(RegisterActivity.this,"Please enter your Email ID",Toast.LENGTH_SHORT).show();
                else if(TextUtils.isEmpty(password))
                    Toast.makeText(RegisterActivity.this,"Please enter your password",Toast.LENGTH_SHORT).show();
                else
                {
                    loadingBar.setTitle("Creating new Account");
                    loadingBar.setMessage("Please wait");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful())
                            {
                                final String curUserId=mAuth.getCurrentUser().getUid();
                                FirebaseInstanceId.getInstance().getInstanceId()
                                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                                if(task.isSuccessful()){
                                                    String deviceToken=task.getResult().getToken();
                                                    rootReference.child("Users").child(curUserId).child("device_token").setValue(deviceToken);
                                                }
                                            }
                                        });
                                rootReference.child("Users").child(curUserId).setValue("");
                                Toast.makeText(RegisterActivity.this,"Account created successfully", Toast.LENGTH_SHORT).show();
                                Intent intent=new Intent(RegisterActivity.this,MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //That | is a pipe symbol
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                String error=task.getException().toString();
                                Toast.makeText(RegisterActivity.this,"Error : "+error, Toast.LENGTH_LONG).show();
                            }
                            loadingBar.dismiss();
                        }
                    });
                }

            }
        });
    }
}
