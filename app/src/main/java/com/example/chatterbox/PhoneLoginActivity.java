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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private Button sendVerificationCodeButton,verifyButton;
    private EditText inputPhoneNumber,inputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private ProgressDialog loadingBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        sendVerificationCodeButton=findViewById(R.id.send_ver_code_button);
        verifyButton=findViewById(R.id.verify_button);
        inputPhoneNumber=findViewById(R.id.phone_number_input);
        inputVerificationCode=findViewById(R.id.verification_code_input);
        loadingBar=new ProgressDialog(this);

        mAuth=FirebaseAuth.getInstance();

        sendVerificationCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingBar.setTitle("Phone Verification");
                loadingBar.setMessage("In progress");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(false);
                String phoneNumber=inputPhoneNumber.getText().toString();
                if(!TextUtils.isEmpty(phoneNumber)){
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneLoginActivity.this,               // Activity (for callback binding)
                            callbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });
        callbacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(PhoneLoginActivity.this,"Invalid Phone Number",Toast.LENGTH_LONG).show();
                sendVerificationCodeButton.setVisibility(View.VISIBLE);
                inputPhoneNumber.setVisibility(View.VISIBLE);
                verifyButton.setVisibility(View.INVISIBLE);
                inputVerificationCode.setVisibility(View.INVISIBLE);
                loadingBar.dismiss();
            }
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                Toast.makeText(PhoneLoginActivity.this,"OTP sent",Toast.LENGTH_LONG).show();

                sendVerificationCodeButton.setVisibility(View.INVISIBLE);
                inputPhoneNumber.setVisibility(View.INVISIBLE);
                verifyButton.setVisibility(View.VISIBLE);
                inputVerificationCode.setVisibility(View.VISIBLE);
                loadingBar.dismiss();
                // ...
            }
        };

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String verificationCode=inputVerificationCode.getText().toString();
                if(!TextUtils.isEmpty(verificationCode)){

                    loadingBar.setTitle("Code Verification");
                    loadingBar.setTitle("Please wait...");
                    loadingBar.show();
                    loadingBar.setCanceledOnTouchOutside(false);
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        loadingBar.dismiss();
                        if (task.isSuccessful()) {
                            // Sign in success
                            Toast.makeText(PhoneLoginActivity.this,"Logged in successfully",Toast.LENGTH_SHORT).show();
                            final String curUserId=mAuth.getCurrentUser().getUid();
                            usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
                            FirebaseInstanceId.getInstance().getInstanceId()
                                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                            if(task.isSuccessful()){
                                                String deviceToken=task.getResult().getToken();
                                                usersRef.child(curUserId).child("device_token")
                                                        .setValue(deviceToken);

                                                sendUserToMainActivity();
                                            }
                                        }
                                    });
                        } else {
                            // Sign in failed
                            String error=task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this,"Error logging in :"+error,Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendUserToMainActivity() {
        Intent intent=new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//Destroy all previous activities so that back button won't lead to this or login page
        startActivity(intent);

        finish();
    }

}
