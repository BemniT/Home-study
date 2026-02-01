package com.example.home_study;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Adapter.ChatMessageAdapter;
import com.example.home_study.Model.Message;
import com.example.home_study.Prevalent.Continuity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Picasso;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatDialogue extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText inputMessage;
    private CircleImageView profileImage;
    private View sentButton;
    private ImageView imageBack;
    private ImageView attachFile;
    private ProgressBar progressBar;
    private DatabaseReference messagesRef;
    private DatabaseReference chatRootRef;
    private String currentUserId, otherUserId, chatId;
    private ChatMessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private boolean isChatOpen = false;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onStart() {
        super.onStart();
        isChatOpen = true;
        if (chatRootRef != null) {
            chatRootRef.child("unread").child(currentUserId).setValue(0);
        }
        // mark messages seen (same logic as before)...
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

        FirebaseApp.initializeApp(this);
        currentUserId = Continuity.userId;
        otherUserId = getIntent().getStringExtra("otherUserId");

        imageBack = findViewById(R.id.backImage);
        imageBack.setOnClickListener(v -> finish());

        String name = getIntent().getStringExtra("name");
        String profileImageUrl = getIntent().getStringExtra("image");
        ((TextView) findViewById(R.id.textName)).setText(name);
        profileImage = findViewById(R.id.profileImage);
        Picasso.get().load(profileImageUrl).placeholder(R.drawable.profile_image).into(profileImage);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputChat);
        sentButton = findViewById(R.id.layoutSend);
        progressBar = findViewById(R.id.progressBar);
        attachFile = findViewById(R.id.attachFile);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messageList, currentUserId,
                new ChatMessageAdapter.OnMessageActionListener() {
                    @Override public void onEdit(Message message, int postion) { showEditDialog(message); }
                    @Override public void onDelete(Message message, int postion) { showDeleteConfirmationDialog(message); }
                });
        chatRecyclerView.setAdapter(adapter);

        chatId = generateChatId(otherUserId, currentUserId);
        chatRootRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatId);
        messagesRef = chatRootRef.child("messages");

        // image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                (ActivityResultCallback<Uri>) uri -> {
                    if (uri != null) uploadImageAndSend(uri);
                });

        listenForMessages();
        sentButton.setOnClickListener(v -> sendMessage());
        attachFile.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    /**
     * Modern Edit dialog:
     * - custom layout (TextInputEditText)
     * - Save button disabled until text changed and non-empty
     * - shows small circular progress indicator while saving
     * - performs the DB update and updates chat lastMessage
     */
    private void showEditDialog(Message message) {
        if (message == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit, null, false);

        final com.google.android.material.textfield.TextInputEditText editInput =
                dialogView.findViewById(R.id.edit_message_input);
        final CircularProgressIndicator progress =
                dialogView.findViewById(R.id.edit_progress);

        final String originalText = message.getText() == null ? "" : message.getText();

        editInput.setText(originalText);
        editInput.setSelection(originalText.length());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog)
                .setTitle("Edit message")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> { /* dismiss */ })
                .setPositiveButton("Save", null); // click overridden below to prevent auto-dismiss

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final android.widget.Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            final android.widget.Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            // initial state: disable save because nothing changed yet
            saveBtn.setEnabled(false);

            // enable save when text non-empty and changed
            editInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String val = s.toString().trim();
                    boolean changed = !val.equals(originalText.trim());
                    saveBtn.setEnabled(changed && !val.isEmpty());
                }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });

            saveBtn.setOnClickListener(v -> {
                String newText = editInput.getText() == null ? "" : editInput.getText().toString().trim();
                if (newText.isEmpty()) {
                    editInput.setError("Message cannot be empty");
                    return;
                }
                // disable buttons and show progress
                saveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                progress.setVisibility(View.VISIBLE);

                Map<String, Object> update = new HashMap<>();
                update.put("text", newText);
                update.put("edited", true);

                messagesRef.child(message.getMessageId())
                        .updateChildren(update)
                        .addOnSuccessListener(aVoid -> {
                            // update lastMessage if necessary
                            Map<String, Object> metaUpdate = new HashMap<>();
                            metaUpdate.put("text", newText);
                            metaUpdate.put("senderId", currentUserId);
                            metaUpdate.put("timeStamp", ServerValue.TIMESTAMP);
                            metaUpdate.put("seen", false);
                            chatRootRef.child("lastMessage").updateChildren(metaUpdate);

                            progress.setVisibility(GONE);
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            progress.setVisibility(GONE);
                            saveBtn.setEnabled(true);
                            cancelBtn.setEnabled(true);
                            Toast.makeText(ChatDialogue.this, "Failed to save edit", Toast.LENGTH_SHORT).show();
                        });
            });
        });

        dialog.show();
    }

    /**
     * Shows a modern delete confirmation dialog; on delete performs existing deleteMessage().
     * After deletion shows a Snackbar with Undo that restores the previous message text and deleted=false.
     */
    private void showDeleteConfirmationDialog(final Message message) {
        if (message == null) return;
        final String originalText = message.getText() == null ? "" : message.getText();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete message")
                .setMessage("Are you sure you want to delete this message? You can undo this action briefly.")
                .setNegativeButton("Cancel", (d, w) -> { /* dismiss */ })
                .setPositiveButton("Delete", (d, w) -> {
                    // perform deletion and show undo
                    performDeleteWithUndo(message, originalText);
                });

        androidx.appcompat.app.AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialogInterface -> {
            // style the positive (delete) button in red to emphasize destructive action
            android.widget.Button pos = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (pos != null) pos.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        });
        dlg.show();
    }

    /**
     * Executes the deletion and shows a Snackbar with Undo option.
     */
    private void performDeleteWithUndo(final Message message, final String originalText) {
        // update DB to mark deleted + change text
        Map<String, Object> update = new HashMap<>();
        update.put("text", "This message was deleted");
        update.put("deleted", true);

        messagesRef.child(message.getMessageId())
                .updateChildren(update)
                .addOnSuccessListener(aVoid -> {
                    // If this message was last, update lastMessage metadata
                    if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessageId().equals(message.getMessageId())) {
                        Map<String, Object> metaUpdate = new HashMap<>();
                        metaUpdate.put("text", "This message was deleted");
                        metaUpdate.put("senderId", currentUserId);
                        metaUpdate.put("timeStamp", ServerValue.TIMESTAMP);
                        metaUpdate.put("seen", false);
                        chatRootRef.child("lastMessage").updateChildren(metaUpdate);
                    }

                    // show undo snackbar anchored to RecyclerView
                    View anchor = findViewById(R.id.chatRecyclerView);
                    Snackbar.make(anchor != null ? anchor : findViewById(android.R.id.content),
                                    "Message deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                // undo: restore original text and deleted=false
                                Map<String, Object> undo = new HashMap<>();
                                undo.put("text", originalText);
                                undo.put("deleted", false);
                                messagesRef.child(message.getMessageId()).updateChildren(undo)
                                        .addOnSuccessListener(aVoid1 -> {
                                            // restore lastMessage if needed (optional)
                                            if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessageId().equals(message.getMessageId())) {
                                                Map<String, Object> meta = new HashMap<>();
                                                meta.put("text", originalText);
                                                meta.put("senderId", currentUserId);
                                                meta.put("timeStamp", ServerValue.TIMESTAMP);
                                                meta.put("seen", false);
                                                chatRootRef.child("lastMessage").updateChildren(meta);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(ChatDialogue.this, "Undo failed", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .show();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatDialogue.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                });
    }

    // ... rest of your code (listenForMessages, uploadImageAndSend, sendMessage, etc.)
    // copied unchanged from your original implementation; omitted here for brevity but keep them in your file.
    // Make sure to keep methods listenForMessages(), uploadImageAndSend(...), sendMessage(), generateChatId(...) unchanged.

    // For completeness, include your existing methods below (unchanged):
    private void listenForMessages() {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message msg = snapshot.getValue(Message.class);
                if (msg == null) {
                    msg = new Message();
                    msg.setMessageId(snapshot.getKey());
                    msg.setSenderId(snapshot.child("senderId").getValue(String.class));
                    msg.setReceiverId(snapshot.child("receiverId").getValue(String.class));
                    msg.setText(snapshot.child("text").getValue(String.class));
                    Boolean seen = snapshot.child("seen").getValue(Boolean.class);
                    msg.setSeen(seen != null ? seen : false);
                    Boolean deleted = snapshot.child("deleted").getValue(Boolean.class);
                    msg.setDeleted(deleted != null ? deleted : false);
                    Boolean edited = snapshot.child("edited").getValue(Boolean.class);
                    msg.setEdited(edited != null ? edited : false);

                    Long t = snapshot.child("timeStamp").getValue(Long.class);
                    if (t == null) {
                        Integer ti = snapshot.child("timeStamp").getValue(Integer.class);
                        if (ti != null) t = ti.longValue();
                        else {
                            t = snapshot.child("timestamp").getValue(Long.class);
                            if (t == null) {
                                Integer t2 = snapshot.child("timestamp").getValue(Integer.class);
                                if (t2 != null) t = t2.longValue();
                            }
                        }
                    }
                    if (t != null) {
                        if (t < 1_000_000_000_000L) t = t * 1000L;
                        msg.setTimeStamp(t);
                    }
                } else {
                    if (msg.getTimeStamp() == 0) {
                        Long t = snapshot.child("timeStamp").getValue(Long.class);
                        if (t == null) {
                            Integer ti = snapshot.child("timeStamp").getValue(Integer.class);
                            if (ti != null) t = ti.longValue();
                            else {
                                t = snapshot.child("timestamp").getValue(Long.class);
                                if (t == null) {
                                    Integer t2 = snapshot.child("timestamp").getValue(Integer.class);
                                    if (t2 != null) t = t2.longValue();
                                }
                            }
                        }
                        if (t != null) {
                            if (t < 1_000_000_000_000L) t = t * 1000L;
                            msg.setTimeStamp(t);
                        }
                    }
                    msg.setMessageId(snapshot.getKey());
                }

                if (msg != null) {
                    if (isChatOpen && msg.getReceiverId() != null && msg.getReceiverId().equals(currentUserId) && !msg.isSeen()) {
                        messagesRef.child(msg.getMessageId()).child("seen").setValue(true);
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
                if (updated == null) {
                    updated = new Message();
                    updated.setMessageId(snapshot.getKey());
                    updated.setSenderId(snapshot.child("senderId").getValue(String.class));
                    updated.setReceiverId(snapshot.child("receiverId").getValue(String.class));
                    updated.setText(snapshot.child("text").getValue(String.class));
                    Boolean seen = snapshot.child("seen").getValue(Boolean.class);
                    updated.setSeen(seen != null ? seen : false);
                    Boolean deleted = snapshot.child("deleted").getValue(Boolean.class);
                    updated.setDeleted(deleted != null ? deleted : false);
                    Boolean edited = snapshot.child("edited").getValue(Boolean.class);
                    updated.setEdited(edited != null ? edited : false);

                    Long t = snapshot.child("timeStamp").getValue(Long.class);
                    if (t == null) {
                        Integer ti = snapshot.child("timeStamp").getValue(Integer.class);
                        if (ti != null) t = ti.longValue();
                        else {
                            t = snapshot.child("timestamp").getValue(Long.class);
                            if (t == null) {
                                Integer t2 = snapshot.child("timestamp").getValue(Integer.class);
                                if (t2 != null) t = t2.longValue();
                            }
                        }
                    }
                    if (t != null) {
                        if (t < 1_000_000_000_000L) t = t * 1000L;
                        updated.setTimeStamp(t);
                    }
                } else {
                    if (updated.getTimeStamp() == 0) {
                        Long t = snapshot.child("timeStamp").getValue(Long.class);
                        if (t == null) {
                            Integer ti = snapshot.child("timeStamp").getValue(Integer.class);
                            if (ti != null) t = ti.longValue();
                            else {
                                t = snapshot.child("timestamp").getValue(Long.class);
                                if (t == null) {
                                    Integer t2 = snapshot.child("timestamp").getValue(Integer.class);
                                    if (t2 != null) t = t2.longValue();
                                }
                            }
                        }
                        if (t != null) {
                            if (t < 1_000_000_000_000L) t = t * 1000L;
                            updated.setTimeStamp(t);
                        }
                    }
                    updated.setMessageId(snapshot.getKey());
                }

                for (int i = 0; i < messageList.size(); i++) {
                    if (messageList.get(i).getMessageId().equals(updated.getMessageId())) {
                        messageList.set(i, updated);
                        adapter.notifyItemChanged(i);
                        adapter.animateChange(chatRecyclerView, i);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) { }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void uploadImageAndSend(Uri imageUri) {
        if (imageUri == null) return;
        progressBar.setVisibility(VISIBLE);
        attachFile.setEnabled(false);

        DatabaseReference newMsgRef = messagesRef.push();
        String messageId = newMsgRef.getKey();
        if (messageId == null) {
            Toast.makeText(this, "Could not create message", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(GONE);
            attachFile.setEnabled(true);
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("chatImages").child(chatId).child(messageId + ".jpg");

        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            progressBar.setVisibility(GONE);
            attachFile.setEnabled(true);
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String imageUrl = downloadUri != null ? downloadUri.toString() : null;

                long now = System.currentTimeMillis();
                Message msg = new Message(currentUserId, otherUserId, "", now, false, false, false);
                msg.setMessageId(messageId);
                msg.setImageUrl(imageUrl);
                msg.setType("image");

                newMsgRef.setValue(msg)
                        .addOnSuccessListener(aVoid -> {
                            inputMessage.setText("");

                            // Update lastMessage with image indicator + set type = image
                            Map<String, Object> lastMessage = new HashMap<>();
                            lastMessage.put("text", "📷 Image");
                            lastMessage.put("senderId", currentUserId);
                            lastMessage.put("timeStamp", ServerValue.TIMESTAMP);
                            lastMessage.put("seen", false);
                            lastMessage.put("type", "image");
                            chatRootRef.child("lastMessage").updateChildren(lastMessage);

                            Map<String, Object> parts = new HashMap<>();
                            parts.put(currentUserId, true);
                            parts.put(otherUserId, true);
                            chatRootRef.child("participants").updateChildren(parts);

                            chatRootRef.child("unread").child(otherUserId).setValue(ServerValue.increment(1));
                            chatRootRef.child("unread").child(currentUserId).setValue(0);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Chat", "failed to write image message", e);
                        });

            } else {
                Log.e("Chat", "Image upload failed", task.getException());
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        long now = System.currentTimeMillis();
        DatabaseReference newMsgRef = messagesRef.push();
        String messageId = newMsgRef.getKey();

        Message message = new Message(currentUserId, otherUserId, text, now, false, false, false);
        message.setMessageId(messageId);
        message.setType("text");

        newMsgRef.setValue(message)
                .addOnSuccessListener(aVoid -> {
                    inputMessage.setText("");
                    Map<String, Object> lastMessage = new HashMap<>();
                    lastMessage.put("text", text);
                    lastMessage.put("senderId", currentUserId);
                    lastMessage.put("timeStamp", ServerValue.TIMESTAMP);
                    lastMessage.put("seen", false);
                    lastMessage.put("type", "text");
                    chatRootRef.child("lastMessage").updateChildren(lastMessage);

                    Map<String, Object> parts = new HashMap<>();
                    parts.put(currentUserId, true);
                    parts.put(otherUserId, true);
                    chatRootRef.child("participants").updateChildren(parts);

                    chatRootRef.child("unread").child(otherUserId).setValue(ServerValue.increment(1));
                    chatRootRef.child("unread").child(currentUserId).setValue(0);

                })
                .addOnFailureListener(e -> Log.e("Chat", "message sent failed", e));
    }

    private String generateChatId(String a, String b) {
        if (a.compareTo(b) < 0) return a + "_" + b;
        return b + "_" + a;
    }
}