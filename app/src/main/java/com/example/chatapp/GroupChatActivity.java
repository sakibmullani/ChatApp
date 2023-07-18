package com.example.chatapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.MessageModel;
import com.example.chatapp.databinding.ActivityGroupChatBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GroupChatActivity extends AppCompatActivity {

    private ActivityGroupChatBinding binding;
    private DatabaseReference messagesReference;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private GroupMessageAdapter messageAdapter;
    private String groupId, groupName, groupDesc;
    private StorageReference storageReference;
    String senderName;
    List<MessageModel> messageModelList;
    String receiverId, receiverName;
    Boolean isImage=false;
    boolean isAudio= false;

    private static final int REQUEST_IMAGE_PICK = 1;

    private MediaRecorder mediaRecorder;
    private String audioFilePath = null;
    private MediaPlayer mediaPlayer;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        groupId = getIntent().getStringExtra("id");
        groupName = getIntent().getStringExtra("name");
        groupDesc = getIntent().getStringExtra("desc");

        binding.toolBar.setTitle(groupName);
        binding.toolBar.setSubtitle(groupDesc);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        messagesReference = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("messages");

        storageReference = FirebaseStorage.getInstance().getReference("group_chat_images");

        messageAdapter = new GroupMessageAdapter(this, currentUser.getUid(), messagesReference);



        binding.recyclerChat.setAdapter(messageAdapter);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        loadGroupMessages();

        binding.sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = binding.enterMassage.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message, isImage, receiverId, receiverName,isAudio);
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

        binding.sendAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
    }

    private void loadGroupMessages() {
        messagesReference.addValueEventListener(new ValueEventListener() {
            private List<MessageModel> messageModelList = new ArrayList<>();

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModelList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    messageModelList.add(messageModel);
                }

                messageAdapter.clear();
                messageAdapter.addAll(messageModelList);
                binding.recyclerChat.scrollToPosition(messageModelList.size() - 1);

                for (MessageModel messageModel : messageModelList) {
                    String receiverId = messageModel.getReceiverId();
                    if (receiverId == null) {
                        receiverId = "";
                    }

                    DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
                    receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String receiverName = snapshot.child("username").getValue(String.class);
                                messageModel.setReceiverName(receiverName);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GroupChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String message, boolean isImage, String receiverId, String receiverName,boolean isAudio) {
        DatabaseReference messageRef = messagesReference.push();
        String messageId = messageRef.getKey();
        long timestamp = System.currentTimeMillis();

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("user").child(FirebaseAuth.getInstance().getUid());
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String senderName = dataSnapshot.child("userName").getValue(String.class);
                    MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), "", senderName, message, timestamp, isImage,isAudio);
                    messageAdapter.add(messageModel);
                    messageRef.setValue(messageModel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(GroupChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });

        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), receiverId, receiverName, message, timestamp, isImage,isAudio);
        messageAdapter.add(messageModel);
        messageRef.setValue(messageModel);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                uploadImage(imageUri);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        if (imageUri != null) {
            String messageId = messagesReference.push().getKey();
            StorageReference imageRef = storageReference.child(messageId);

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();

            UploadTask uploadTask = imageRef.putFile(imageUri);
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                }
            });

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    progressDialog.dismiss();

                    if (task.isSuccessful() && task.getResult() != null) {
                        Uri downloadUri = task.getResult();
                        sendMessage(downloadUri.toString(), true, receiverId, receiverName,false);
                    } else {
                        Toast.makeText(GroupChatActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 100;

    private void startRecording() {
        // Check audio recording permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST_CODE);
            return;
        }

        // Prepare the file path for audio recording
        audioFilePath = getExternalFilesDir("audio_recordings").getAbsolutePath() + "/audio_recording.3gp";

        // Create a MediaRecorder instance and configure it
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            // Start recording
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.sendAudio.setImageResource(R.drawable.pause_circle_24);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start audio recording or perform related tasks
                startRecording();
            } else {
                // Permission denied
                Toast.makeText(this, "Audio recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            // Stop recording
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            binding.sendAudio.setImageResource(R.drawable.mic_24);
            uploadAudio();
        }
    }

    private void uploadAudio() {
        if (audioFilePath != null) {
            String messageId = messagesReference.push().getKey();
            // Create a Firebase storage reference for the audio file
            StorageReference audioRef = storageReference.child("audio/" + messageId);

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading Audio");
            progressDialog.setMessage("Please wait...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();


            // Upload the audio file to Firebase storage
            Uri audioUri = Uri.fromFile(new File(audioFilePath));
            UploadTask uploadTask = audioRef.putFile(audioUri);

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                }
            });

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return audioRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    progressDialog.dismiss();

                    if (task.isSuccessful() && task.getResult() != null) {
                        Uri downloadUri = task.getResult();
//                        sendMessage(downloadUri.toString(), false, receiverId, receiverName);
                        String audioUrl = downloadUri.toString();
                        sendMessage(audioUrl, false, receiverId, receiverName,true);
                    } else {
                        Toast.makeText(GroupChatActivity.this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            // Release the MediaPlayer resources
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
