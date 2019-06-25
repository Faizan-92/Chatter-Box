package com.example.chatterbox;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
public class ChatFragment extends Fragment {

    private View privateChatsView;
    private RecyclerView chatsList;
    private DatabaseReference chatsRef,usersRef;
    private FirebaseAuth mAuth;
    private String curUserId;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        privateChatsView=inflater.inflate(R.layout.fragment_chat, container, false);
        chatsList=privateChatsView.findViewById(R.id.fragment_chat_lists);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));
        mAuth=FirebaseAuth.getInstance();
        curUserId=mAuth.getCurrentUser().getUid();
        chatsRef= FirebaseDatabase.getInstance().getReference().child("Contacts").child(curUserId);
        usersRef=FirebaseDatabase.getInstance().getReference().child("Users");
        return privateChatsView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //FirebaseUI is a set of open-source libraries for Firebase that allow you to quickly connect common
        // UI elements to the Firebase database for data storage, allowing views to be updated in realtime as they change,
        // and providing simple interfaces for common tasks like displaying lists or collections of items.
        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatsRef,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,ChatsViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder chatsViewHolder, int i, @NonNull Contacts contacts) {
                final String userId=getRef(i).getKey();//getRef(i) is a static method of DatabaseReference
                usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String retImage="no_image";
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.hasChild("image")) {
                                retImage = dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(retImage).into(chatsViewHolder.profileImage);
                            }

                            final String retName = dataSnapshot.child("name").getValue().toString();
                            chatsViewHolder.userName.setText(retName);

                            if(dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state=dataSnapshot.child("userState").child("state").getValue().toString();
                                String date=dataSnapshot.child("userState").child("date").getValue().toString();
                                String time=dataSnapshot.child("userState").child("time").getValue().toString();

                                if(state.equals("Online"))
                                {
                                    chatsViewHolder.userStatus.setText("Online");
                                }
                                else
                                 {
                                    chatsViewHolder.userStatus.setText("Last Seen: "+date+" "+time);
                                }
                            }

                            final String finalRetImage = retImage;
                            chatsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent=new Intent(getContext(),ChatActivity.class);
                                    intent.putExtra("visit_user_id",userId);
                                    intent.putExtra("visit_user_name",retName);
                                    intent.putExtra("visit_user_image", finalRetImage);
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(getContext()).inflate(R.layout.users_display_layout,parent,false);
                return new ChatsViewHolder(view);
            }
        };
        chatsList.setAdapter(adapter);
        adapter.startListening();
    }

    private static class ChatsViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userStatus,userName;
        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage=itemView.findViewById(R.id.user_profile_image);
            userName=itemView.findViewById(R.id.user_profile_name);
            userStatus=itemView.findViewById(R.id.user_profile_status);
        }
    }
}
