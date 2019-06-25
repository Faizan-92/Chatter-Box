package com.example.chatterbox;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment {

    private View requestFragmentView;
    private RecyclerView myRequestsList;
    private DatabaseReference chatReqRef,usersRef,contactsRef;
    private FirebaseAuth mAuth;
    private String curUserId;

    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestFragmentView=inflater.inflate(R.layout.fragment_request, container, false);
        myRequestsList=requestFragmentView.findViewById(R.id.chat_requests_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        chatReqRef= FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        contactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth=FirebaseAuth.getInstance();
        curUserId=mAuth.getCurrentUser().getUid();
        return requestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatReqRef.child(curUserId),Contacts.class)
                .build();
        FirebaseRecyclerAdapter<Contacts,RequestViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder requestViewHolder, int i, @NonNull Contacts contacts)
            {
                requestViewHolder.itemView.findViewById(R.id.request_accept_button).setVisibility(View.VISIBLE);
                requestViewHolder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.VISIBLE);
                final String listUserId=getRef(i).getKey();
                DatabaseReference getTypeRef=getRef(i).child("request_type").getRef();
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            String type=dataSnapshot.getValue().toString();
                            if(type.equals("received"))
                            {
                                //repeat it for each user
                                usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("image")){
                                            final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                            final String requestUserStatus=dataSnapshot.child("status").getValue().toString();
                                            final String requestUserImage=dataSnapshot.child("image").getValue().toString();
                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText("wants to connect with you");
                                            Picasso.get().load(requestUserImage).into(requestViewHolder.profileImage);
                                        }
                                        else
                                        {
                                            final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                            final String requestUserStatus=dataSnapshot.child("status").getValue().toString();
                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText("wants to connect with you");
                                        }
                                        requestViewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                //listUserId has receiver user id
                                                contactsRef.child(curUserId).child(listUserId).child("Contacts").setValue("Saved")
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    contactsRef.child(listUserId).child(curUserId).child("Contacts").setValue("Saved")
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        chatReqRef.child(curUserId).child(listUserId).removeValue().
                                                                                                addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        if(task.isSuccessful()){
                                                                                                            chatReqRef.child(listUserId).child(curUserId).removeValue().
                                                                                                                    addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                        @Override
                                                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                                                            if(task.isSuccessful()){
                                                                                                                                Toast.makeText(getContext(),"Added to Contacts",Toast.LENGTH_SHORT).show();
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
                                                        });
                                            }
                                        });
                                        requestViewHolder.cancelButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                chatReqRef.child(curUserId).child(listUserId).removeValue().
                                                        addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    chatReqRef.child(listUserId).child(curUserId).removeValue().
                                                                            addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        Toast.makeText(getContext(),"Removed successfully",Toast.LENGTH_SHORT).show();
                                                                                    }

                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if(type.equals("sent")){
                                Button request_sent_btn=requestViewHolder.itemView.findViewById(R.id.request_accept_button);
                                request_sent_btn.setText("Undo Request");
                                requestViewHolder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.INVISIBLE);
                                //Almost same code as above
                                usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("image")){
                                            final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                            final String requestUserStatus=dataSnapshot.child("status").getValue().toString();
                                            final String requestUserImage=dataSnapshot.child("image").getValue().toString();
                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText("Request Sent");
                                            Picasso.get().load(requestUserImage).into(requestViewHolder.profileImage);
                                        }
                                        else
                                        {
                                            final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                            final String requestUserStatus=dataSnapshot.child("status").getValue().toString();
                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText("wants to connect with you");
                                        }
                                        requestViewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                chatReqRef.child(curUserId).child(listUserId).removeValue().
                                                        addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    chatReqRef.child(listUserId).child(curUserId).removeValue().
                                                                            addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        Toast.makeText(getContext(),"Removed successfully",Toast.LENGTH_SHORT).show();
                                                                                    }

                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                RequestViewHolder holder=new RequestViewHolder(view);
                return holder;
            }
        };
        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }
    public static class RequestViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName,userStatus;
        CircleImageView profileImage;
        Button acceptButton,cancelButton;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            userName=itemView.findViewById(R.id.user_profile_name);
            userStatus=itemView.findViewById(R.id.user_profile_status);
            profileImage=itemView.findViewById(R.id.user_profile_image);
            acceptButton=itemView.findViewById(R.id.request_accept_button);
            cancelButton=itemView.findViewById(R.id.request_cancel_button);
        }
    }
}
