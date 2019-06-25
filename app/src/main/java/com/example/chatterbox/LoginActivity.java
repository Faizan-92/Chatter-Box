package com.example.chatterbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class LoginActivity extends AppCompatActivity {

    private EditText userEmail,userPassword;
    private Button loginButton,phoneLoginButton;
    private TextView needNewAccount,forgetPasswordLink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userEmail=findViewById(R.id.login_email);
        userPassword=findViewById(R.id.login_password);
        loginButton=findViewById(R.id.login_button);
        phoneLoginButton=findViewById(R.id.phone_login_button);
        needNewAccount=findViewById(R.id.need_new_account_link);
        forgetPasswordLink=findViewById(R.id.forget_password_link);
        mAuth=FirebaseAuth.getInstance();
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        loadingBar=new ProgressDialog(this);

        needNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=userEmail.getText().toString();
                String password=userPassword.getText().toString();
                if(TextUtils.isEmpty(email))
                    Toast.makeText(LoginActivity.this,"Please enter your Email ID",Toast.LENGTH_SHORT).show();
                else if(TextUtils.isEmpty(password))
                    Toast.makeText(LoginActivity.this,"Please enter your password",Toast.LENGTH_SHORT).show();
                else
                {
                    loadingBar.setTitle("Logging into Account");
                    loadingBar.setMessage("Please wait");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    Log.d("Here","Hi");
                    mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
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
                                                    Log.d("Current User Token",deviceToken);
                                                    usersRef.child(curUserId).child("device_token")
                                                            .setValue(deviceToken)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //That | is a pipe symbol
                                                                        startActivity(intent);
                                                                        finish();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });

                            }
                            else
                            {
                                //String error=task.getException().toString();
                                Toast.makeText(LoginActivity.this,"Error : Wrong email ID or the email ID is not yet registered", Toast.LENGTH_LONG).show();
                            }
                            loadingBar.dismiss();
                        }
                    });
                }

            }
        });

        phoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LoginActivity.this,PhoneLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
