package com.example.chatapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    public GroupMessageAdapter(Context context, String currentUserId, DatabaseReference messagesReference) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.messageList = new ArrayList<>();
        this.messagesReference=messagesReference;
    }

    public void setMessageList(List<MessageModel> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    public  void  add(MessageModel messageModel){
        messageList.add(messageModel);
        notifyDataSetChanged();
    }

    public void clear() {
        messageList.clear();
        notifyDataSetChanged();
    }

    public  void  addAll(List<MessageModel>messageModels){
        int previousSize =messageList.size();
        messageList.addAll(messageModels);
        notifyItemRangeInserted(previousSize,messageModels.size());
    }

    /*public void deleteMessage(MessageModel message) {
        int position = messageList.indexOf(message);
        if (position != -1) {
            messageList.remove(position);
            notifyItemRemoved(position);
        }
    }*/

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
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
        holder.message_linearlayoutIdRight.setVisibility(View.GONE);

        if (message.isImage()) {
            //display image
            if (message.getSenderId().equals(currentUserId)) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.imageMessageRight);


            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.imageMessageLeft.setVisibility(View.VISIBLE);
                holder.massageIdLeft.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.imageMessageLeft);
            }
        } else {
            // Display text message
            if (message.getSenderId().equals(currentUserId)) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.GONE);

                holder.massageIdRight.setText(message.getMessage());
            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.massageIdLeft.setVisibility(View.VISIBLE);
                holder.imageMessageLeft.setVisibility(View.GONE);

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

        // Set long click listener for the item
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
               showDeleteDialog(message);
                return true;
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
        ImageView imageMessageLeft, imageMessageRight;

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


}
