package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.graphics.Rect;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.example.chatapp.MessageAdapter;
import com.example.chatapp.MessageModel;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity  implements MessageAdapter.OnMessageLongClickListener{

    ActivityChatBinding binding;

    DatabaseReference databaseReferenceSender, databaseReferenceReciever;
    String recieverId,senderRoom, recieverRoom;
    MessageAdapter messageAdapter;
    private String recieverName;

    private static final int REQUEST_IMAGE_PICK = 1;
    private StorageReference storageReference;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent from userAdapter id and Name
        recieverId=getIntent().getStringExtra("id");
        recieverName=getIntent().getStringExtra("name");

        binding.toolBar.setTitle(recieverName);

        senderRoom= FirebaseAuth.getInstance().getUid()+recieverId;
        recieverRoom=recieverId+FirebaseAuth.getInstance().getUid();

        //adapter & recyclerView setup

        messageAdapter=new MessageAdapter(this);
        binding.recyclerChat.setAdapter(messageAdapter);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firebase Storage reference
        storageReference = FirebaseStorage.getInstance().getReference("chat_images");

        databaseReferenceSender=FirebaseDatabase.getInstance().getReference("chats").child(senderRoom);
        databaseReferenceReciever=FirebaseDatabase.getInstance().getReference("chats").child(recieverRoom);

        messageAdapter.setOnMessageLongClickListener(this);

        databaseReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messageList = new ArrayList<>();

                for (DataSnapshot dataSnapshot:snapshot.getChildren()){
                    MessageModel messageModel= dataSnapshot.getValue(MessageModel.class);
                    messageList.add(messageModel);
                }

                // Sort the message list based on the timestamp
                Collections.sort(messageList, new Comparator<MessageModel>() {
                    @Override
                    public int compare(MessageModel message1, MessageModel message2) {
                        return Long.compare(message1.getTime(), message2.getTime());
                    }
                });

//                messageAdapter.clear();
                messageAdapter.addAll(messageList);
                binding.recyclerChat.scrollToPosition(messageAdapter.getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message=binding.enterMassage.getText().toString();

                if (message.trim().length()>0){
                    sendMessage(message);

                    binding.enterMassage.setText("");

                }
            }
        });

        binding.uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        // Set EditText to adjust resize to prevent it from overlapping with the keyboard
        binding.enterMassage.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        binding.enterMassage.setRawInputType(InputType.TYPE_CLASS_TEXT);

        // Add an OnGlobalLayoutListener to the root layout to detect keyboard changes
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = 0;

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                binding.getRoot().getWindowVisibleDisplayFrame(r);
                int screenHeight = binding.getRoot().getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                if (previousHeight != keyboardHeight) {
                    previousHeight = keyboardHeight;

                    // Scroll the RecyclerView to the last item
                    if (keyboardHeight > 0) {
                        binding.recyclerChat.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int targetPosition = messageAdapter.getItemCount() - 1;
                                if (targetPosition >= 0 && targetPosition < messageAdapter.getItemCount()) {
                                    binding.recyclerChat.scrollToPosition(targetPosition);
                                }
                            }
                        }, 100);
                    }
                }
            }
        });
    }

    // Open gallery to select an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // Handle gallery intent result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            // Upload the image to Firebase Storage
            uploadImage(imageUri);
        }
    }


    // Upload image to Firebase Storage
    private void uploadImage(Uri imageUri) {
        if (imageUri != null) {
            // Generate a unique ID for the image
            String imageId = UUID.randomUUID().toString();

            // Create a storage reference for the image
            StorageReference imageRef = storageReference.child(imageId);

            // Create a ProgressDialog to show the uploading progress
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Upload the image to Firebase Storage
            UploadTask uploadTask = imageRef.putFile(imageUri);

            // Register a listener to track the upload progress
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    // Calculate the upload progress percentage
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                    // Update the progress bar
                    progressDialog.setProgress((int) progress);
                }
            });

            // Monitor the upload completion
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Return the download URL of the uploaded image
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    progressDialog.dismiss();

                    if (task.isSuccessful() && task.getResult() != null) {
                        Uri downloadUri = task.getResult();

                        // Send the image message with the download URL
                        sendMessage(downloadUri.toString());
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void sendMessage(String message) {
        String msdid = UUID.randomUUID().toString();
        long time = System.currentTimeMillis();
        MessageModel messageModel = new MessageModel(msdid, FirebaseAuth.getInstance().getUid(), message, time);

        // Check if the message is an image
        if (isImageUrl(message)) {
            messageModel.setImageUrl(message);
            messageModel.setImage(true);
        } else {
            messageModel.setImage(false);
        }

        messageAdapter.addData(messageModel);
        databaseReferenceSender
                .child(msdid)
                .setValue(messageModel);

        databaseReferenceReciever
                .child(msdid)
                .setValue(messageModel);

    }

    private boolean isImageUrl(String message) {
        // Check if the message is a Firebase Storage URL
        if (message.startsWith("https://firebasestorage.googleapis.com/") || message.contains("firebaseapp.com")) {
         return true;
        }

        // Check if the message is a valid image URL
        try {
            Uri uri = Uri.parse(message);
            return uri != null && "image".equals(uri.getScheme());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onMessageLongClick(MessageModel message) {
        showDeleteDialog(message);
    }

    private void showDeleteDialog(MessageModel message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Message");
        builder.setMessage("Are you sure you want to delete this message?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteMessage(message);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void deleteMessage(MessageModel message) {
        String messageId = message.getMsgid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("chats")
                .child(senderRoom).child(messageId);
        databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    messageAdapter.deleteMessage(message);
                }
            }
        });
    }
}
