package com.example.home_study;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.example.home_study.Adapter.ChatMessageAdapter;
import com.example.home_study.Model.Chat;
import com.example.home_study.Model.Message;
import com.example.home_study.Prevalent.Continuity;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDialogue extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText inputMessage;
    private View sentButton;
    private ProgressBar progressBar;
    private DatabaseReference chatRef;
    private FirebaseFirestore firestore;
    private CollectionReference messageRef;
    private String currentUserId, otherUserId, chatId;
    private ChatMessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private boolean isChatOpen = false;

    @Override
    protected void onStart() {
        super.onStart();
        isChatOpen = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isChatOpen = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_dialogue);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        FirebaseApp.initializeApp(this);
        currentUserId = Continuity.userId;
        otherUserId = getIntent().getStringExtra("teacherUserID");

        String name = getIntent().getStringExtra("teacherName");
        ((TextView) findViewById(R.id.textName)).setText(name);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputChat);
        sentButton = findViewById(R.id.layoutSend);
        progressBar = findViewById(R.id.progressBar);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messageList, currentUserId,
                new ChatMessageAdapter.OnMessageActionListener(){

            @Override
            public void onEdit(Message message, int postion) {
                showEditDialog(message);
            }

            @Override
            public void onDelete(Message message, int postion) {
                deleteMessage(message);
            }
        });
        chatRecyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        chatId = generateChatId( otherUserId,currentUserId);

        messageRef = firestore
                .collection("Chats")
                .document(chatId)
                .collection("Messages");

        chatRef = FirebaseDatabase.getInstance().getReference("Chats")
                        .child(chatId)
                .child("messages");

        listenForMessages();
        sentButton.setOnClickListener(v -> sendMessage());

    }


    private void showEditDialog(Message message) {
        EditText editText = new EditText(this);
        editText.setText(message.getText());

        new AlertDialog.Builder(this)
                .setTitle("Edit message")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = editText.getText().toString().trim();
                    if (!newText.isEmpty()){
                        chatRef.child(message.getMessageId())
                                .updateChildren(new HashMap<String,Object>(){{
                                    put("text", newText);
                                    put("edited", true);
                                }});
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(Message message) {

        chatRef.child(message.getMessageId())
                .updateChildren(new HashMap<String,Object>(){{
                    put("text","This message was deleted");
                    put("deleted", true);
                }});
    }
    private void listenForMessages() {


        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Message msg = snapshot.getValue(Message.class);
                msg.setMessageId(snapshot.getKey());

                if (msg != null){

                    if (isChatOpen && msg.getReceiverId().equals(currentUserId) && !msg.isSeen())
                    {
                    if (msg.getReceiverId().equals(currentUserId) && !msg.isSeen()){
                        chatRef.child(msg.getMessageId())
                                .child("seen")
                                .setValue(true);
                    }
                    }

                    messageList.add(msg);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }

                progressBar.setVisibility(GONE);
                chatRecyclerView.setVisibility(VISIBLE);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Message updated = snapshot.getValue(Message.class);
                if (updated == null) return;

                updated.setMessageId(snapshot.getKey());

                for (int i = 0; i< messageList.size(); i++){
                    if (messageList.get(i).getMessageId().equals(updated.getMessageId())){
                        messageList.set(i, updated);
                        adapter.notifyItemChanged(i);
                        adapter.animateChange(chatRecyclerView, i);
                        break;
                    }
                }
            }


            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage() {


        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty())return;

        long now = System.currentTimeMillis();

        DatabaseReference newMsgRef = chatRef.push();
        String messageId = newMsgRef.getKey();
        Message message = new Message(
                currentUserId,
                otherUserId,
                text,
                now,
                false, false, false
        );

        message.setMessageId(messageId);
        newMsgRef.setValue(message)
                .addOnSuccessListener(aVoid -> {
                    inputMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e("Chat", "message sent failed", e);
                });
    }

    private String generateChatId(String a, String b) {
        if (a.compareTo(b) < 0) return a + "_" + b;
        return b + "_" + a;
    }
}