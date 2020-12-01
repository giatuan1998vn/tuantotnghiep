package com.example.vnchats;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import android.graphics.drawable.RippleDrawable;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity
{

    private  String receiverUserID ,sendertUserID, Current_State;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button SendMessageRequestButton, DeclineMessageRequestButton;

    private DatabaseReference UserRef , ChatRequestRef, ContactsRef, NotificationRef;

    private FirebaseAuth mAuth;

    String mUID;

    private DatabaseReference NotificationsReference;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();

        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");

        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        NotificationsReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
        NotificationsReference.keepSynced(true);


        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();

        sendertUserID = mAuth.getCurrentUser().getUid();



        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_profile_status);
        SendMessageRequestButton = (Button) findViewById(R.id.send_message_request_button);
        DeclineMessageRequestButton = (Button) findViewById(R.id.decline_message_request_button);

        Current_State = "new";

        RetrieveUserInfo();//truy xuất sử dụng thông tin ng dung

        checkUserStatus();
    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }


    private void RetrieveUserInfo()
    {
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if ((dataSnapshot.exists())  && (dataSnapshot.hasChild("image")))
                {
                    String userImage = dataSnapshot.child("image").getValue().toString();
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequests();
                }
                else
                 {
                     String userName = dataSnapshot.child("name").getValue().toString();
                     String userStatus = dataSnapshot.child("status").getValue().toString();

                     userProfileName.setText(userName);
                     userProfileStatus.setText(userStatus);

                     ManageChatRequests();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private void checkUserStatus(){{

        FirebaseUser user = mAuth.getCurrentUser();
        if (user !=null){

            mUID = user.getUid();

            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();
        }
        else {
            startActivity(new Intent(ProfileActivity.this,LoginActivity.class));
            finish();
        }
    }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void ManageChatRequests()//quản lý yêu cầu
    {
        ChatRequestRef.child(sendertUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)//thay đổi dữ liệu
                    {
                            if (dataSnapshot.hasChild(receiverUserID))
                            {
                                String request_type =  dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();

                                if (request_type.equals("sent"))
                                {
                                    Current_State = "request_sent";
                                    SendMessageRequestButton.setText("Cancel Chat Request");
                                }
                                else
                                {
                                    if(request_type.equals("received"))
                                    {
                                        Current_State= "request_received";
                                        SendMessageRequestButton.setText("Accept Chat Request");

                                        DeclineMessageRequestButton.setVisibility(View.VISIBLE);

                                        DeclineMessageRequestButton.setEnabled(true);

                                        DeclineMessageRequestButton.setOnClickListener(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View view)
                                            {
                                                CancelChatRequest();

                                            }
                                        });
                                    }

                                }
                            }
                            else
                            {
                                ContactsRef.child(sendertUserID)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                            {
                                                if(dataSnapshot.hasChild(receiverUserID))
                                                {
                                                    Current_State = "friends";
                                                    SendMessageRequestButton.setText("Remove this Contact");

                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError)
                                            {

                                            }
                                        });

                            }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError)
                    {

                    }
                });

        if (!sendertUserID.equals(receiverUserID))//người nhận không phải bình đẳng với nhau
        {
            SendMessageRequestButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    SendMessageRequestButton.setEnabled(false);

                    if (Current_State.equals("new"))
                    {
                        SendChatRequest();

                    }
                    if (Current_State.equals("request_sent"))
                    {
                        CancelChatRequest();
                    }

//                    Log.d("sinh", String.valueOf(Current_State.equals("request_Sent")));

                    if (Current_State.equals("request_received"))
                    {
                       AcceptChatRequest();
                    }

                    if (Current_State.equals("friends"))//hiện tại là bạn bè
                    {
                        RemoveSpecificContact();//xóa liên lạc cụ thể
                    }


                }
            });
        }
        else
        {
            SendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void RemoveSpecificContact()
    {
        ContactsRef.child(sendertUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            ContactsRef.child(receiverUserID).child(sendertUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {

                                            if (task.isSuccessful())
                                            {
                                                SendMessageRequestButton.setEnabled(true);
                                                Current_State = "new";
                                                SendMessageRequestButton.setText("Send Message");

                                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                DeclineMessageRequestButton.setEnabled(false);
                                            }

                                        }
                                    });
                        }
                    }
                });
    }

    private void AcceptChatRequest()
    {
        ContactsRef.child(sendertUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            ContactsRef.child(receiverUserID).child(sendertUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                ChatRequestRef.child(sendertUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>()
                                                        {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    ChatRequestRef.child(receiverUserID).child(sendertUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>()
                                                                            {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    SendMessageRequestButton.setEnabled(true);
                                                                                    Current_State= "friends";
                                                                                    SendMessageRequestButton.setText("Remove this Contact");

                                                                                    DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                                                    DeclineMessageRequestButton.setEnabled(false);
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




    private void CancelChatRequest()
    {
       ChatRequestRef.child(sendertUserID).child(receiverUserID)
               .removeValue()
               .addOnCompleteListener(new OnCompleteListener<Void>() {
                   @Override
                   public void onComplete(@NonNull Task<Void> task)
                   {
                       if (task.isSuccessful())
                       {
                           ChatRequestRef.child(receiverUserID).child(sendertUserID)
                                   .removeValue()
                                   .addOnCompleteListener(new OnCompleteListener<Void>() {
                                       @Override
                                       public void onComplete(@NonNull Task<Void> task)
                                       {

                                           if (task.isSuccessful())
                                           {
                                               SendMessageRequestButton.setEnabled(true);
                                               Current_State = "new";
                                               SendMessageRequestButton.setText("Send Message");

                                               DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                               DeclineMessageRequestButton.setEnabled(false);
                                           }

                                       }
                                   });
                       }
                   }
               });

    }

    private void SendChatRequest()
    {
        ChatRequestRef.child(sendertUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            ChatRequestRef.child(receiverUserID).child(sendertUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful()){
                                                HashMap<String,String> chatnotificationMap = new HashMap<>();//thông báo tro chuyện
                                                chatnotificationMap.put("from", sendertUserID);
                                                chatnotificationMap.put("type","request");

                                                NotificationRef.child(receiverUserID).push()
                                                        .setValue(chatnotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    SendMessageRequestButton.setEnabled(true);
                                                                    Current_State = "request_sent";
                                                                    SendMessageRequestButton.setText("Cancel Chat Request");
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
