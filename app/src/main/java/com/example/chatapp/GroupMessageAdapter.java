package com.example.chatapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MessageViewHolder> {

    private Context context;
    private List<MessageModel> messageList;
    private String currentUserId;
    private OnMessageLongClickListener messageLongClickListener;
    DatabaseReference messagesReference;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int lastPlayedPosition = -1;

    public GroupMessageAdapter(Context context, String currentUserId, DatabaseReference messagesReference) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.messageList = new ArrayList<>();
        this.messagesReference = messagesReference;
    }

    public void setMessageList(List<MessageModel> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    public void add(MessageModel messageModel) {
        messageList.add(messageModel);
        notifyDataSetChanged();
    }

    public void clear() {
        messageList.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<MessageModel> messageModels) {
        int previousSize = messageList.size();
        messageList.addAll(messageModels);
        notifyItemRangeInserted(previousSize, messageModels.size());
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.messageLongClickListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.group_message_row, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MessageModel message = messageList.get(position);

        holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
        holder.message_linearlayoutIdRight.setVisibility(View.GONE);

        if (message.isImage()) {
            // Display image
            if (message.getSenderId().equals(currentUserId)) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.GONE);

                holder.pauseButtonRight.setVisibility(View.GONE);
                holder.playButtonRight.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.imageMessageRight);

            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.imageMessageLeft.setVisibility(View.VISIBLE);
                holder.massageIdLeft.setVisibility(View.GONE);

                holder.pauseButtonLeft.setVisibility(View.GONE);
                holder.playButtonLeft.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.imageMessageLeft);
            }
        } else if (message.isAudio()) {

            // Display audio play option
            if (message.getSenderId().equals(currentUserId)) {
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);

                holder.playButtonRight.setVisibility(View.VISIBLE);
                holder.pauseButtonRight.setVisibility(View.GONE);

                holder.imageMessageRight.setVisibility(View.GONE);
                holder.massageIdRight.setVisibility(View.GONE);




            } else {
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);

                holder.playButtonLeft.setVisibility(View.VISIBLE);
                holder.pauseButtonLeft.setVisibility(View.GONE);

                holder.imageMessageLeft.setVisibility(View.GONE);
                holder.massageIdLeft.setVisibility(View.GONE);

            }

        } else {
            // Display text message
            if (message.getSenderId().equals(currentUserId)) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.GONE);

                holder.pauseButtonRight.setVisibility(View.GONE);
                holder.playButtonRight.setVisibility(View.GONE);

                holder.massageIdRight.setText(message.getMessage());
            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.massageIdLeft.setVisibility(View.VISIBLE);
                holder.imageMessageLeft.setVisibility(View.GONE);

                holder.pauseButtonLeft.setVisibility(View.GONE);
                holder.playButtonLeft.setVisibility(View.GONE);

                holder.massageIdLeft.setText(message.getMessage());
            }
        }

        // Set timestamp
        if (message.getSenderId().equals(currentUserId)) {
            holder.rightTimeTextView.setText(formatTimestamp(message.getTime()));
        } else {
            holder.leftTimeTextView.setText(formatTimestamp(message.getTime()));
            holder.senderTextView.setText(message.getReceiverName());
        }


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDeleteDialog(message);
                return true;
            }
        });

        holder.playButtonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio(message, position);
                holder.playButtonRight.setVisibility(View.GONE);
                holder.pauseButtonRight.setVisibility(View.VISIBLE);
            }
        });

        holder.pauseButtonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAudio();
                holder.pauseButtonRight.setVisibility(View.GONE);
                holder.playButtonRight.setVisibility(View.VISIBLE);
            }
        });

        holder.playButtonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio(message, position);
                holder.playButtonLeft.setVisibility(View.GONE);
                holder.pauseButtonLeft.setVisibility(View.VISIBLE);
            }
        });

        holder.pauseButtonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAudio();
                holder.playButtonLeft.setVisibility(View.VISIBLE);
                holder.pauseButtonLeft.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView massageIdLeft, massageIdRight, leftTimeTextView, rightTimeTextView, senderTextView;
        ConstraintLayout message_linearlayoutIdLeft, message_linearlayoutIdRight;
        ImageView imageMessageLeft, imageMessageRight, playButtonRight, pauseButtonRight, playButtonLeft, pauseButtonLeft;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            massageIdLeft = itemView.findViewById(R.id.massageIdLeft);
            massageIdRight = itemView.findViewById(R.id.massageIdRight);
            rightTimeTextView = itemView.findViewById(R.id.rightTimeTextView);
            leftTimeTextView = itemView.findViewById(R.id.leftTimeTextView);
            message_linearlayoutIdLeft = itemView.findViewById(R.id.message_linearlayoutIdLeft);
            message_linearlayoutIdRight = itemView.findViewById(R.id.message_linearlayoutIdRight);
            imageMessageLeft = itemView.findViewById(R.id.imageMessageLeft);
            imageMessageRight = itemView.findViewById(R.id.imageMessageRight);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            playButtonRight = itemView.findViewById(R.id.playButtonRight);
            pauseButtonRight = itemView.findViewById(R.id.pauseButtonRight);
            playButtonLeft = itemView.findViewById(R.id.playButtonLeft);
            pauseButtonLeft = itemView.findViewById(R.id.pauseButtonLeft);
        }
    }

    private String formatTimestamp(long timestamp) {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat.format(new Date(timestamp));
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageModel message);
    }

    private void showDeleteDialog(MessageModel message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
        if (messageId != null) {
            DatabaseReference messageReference = messagesReference.child(messageId);

            messageReference.removeValue()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(context, "Message ID is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudio(MessageModel message, int position) {
        if (isPlaying) {
            if (lastPlayedPosition == position) {
                // Stop playing audio
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                lastPlayedPosition = -1;
            } else {
                // Stop previously playing audio
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;

                // Start playing new audio
                startPlayingAudio(message, position);
            }
        } else {
            // Start playing audio
            startPlayingAudio(message, position);
        }
    }

    private void startPlayingAudio(MessageModel message, int position) {
        try {
            mediaPlayer = new MediaPlayer();

            // Set the audio attributes for the media player
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);

            // Set the data source for the media player
            mediaPlayer.setDataSource(message.getMessage());

            // Set the audio stream type for the media player
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            // Prepare the media player
            mediaPlayer.prepare();

            // Start playing audio
            mediaPlayer.start();
            isPlaying = true;
            lastPlayedPosition = position;

            // Set completion listener to release media player resources
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlaying = false;
                    lastPlayedPosition = -1;
                    notifyItemChanged(position); // Update the item to refresh the play/pause button
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to play audio", Toast.LENGTH_SHORT).show();
        }
    }


    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
        }
    }
}
