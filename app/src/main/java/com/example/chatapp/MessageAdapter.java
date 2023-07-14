package com.example.chatapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageModel;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_IMAGE = 2;

    private Context context;
    private List<MessageModel> messageModelList;
    private OnMessageLongClickListener messageLongClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageModel message);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.messageLongClickListener = listener;
    }

    public MessageAdapter(Context context) {
        this.context = context;
        messageModelList = new ArrayList<>();
    }

    public void deleteMessage(MessageModel message) {
        int position = messageModelList.indexOf(message);
        if (position != -1) {
            messageModelList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addAll(List<MessageModel> messages) {
        messageModelList.addAll(messages);
        notifyDataSetChanged();
    }

    public void addData(MessageModel message) {
        messageModelList.add(message);
        notifyDataSetChanged();
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);

        holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
        holder.message_linearlayoutIdRight.setVisibility(View.GONE);

        if (messageModel.isImage()) {
            // Display image message
            if (messageModel.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.GONE);

                Glide.with(context)
                        .load(messageModel.getImageUrl())
                        .into(holder.imageMessageRight);
            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.imageMessageLeft.setVisibility(View.VISIBLE);
                holder.massageIdLeft.setVisibility(View.GONE);

                Glide.with(context)
                        .load(messageModel.getImageUrl())
                        .into(holder.imageMessageLeft);
            }
        } else {
            // Display text message
            if (messageModel.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                holder.message_linearlayoutIdLeft.setVisibility(View.GONE);
                holder.message_linearlayoutIdRight.setVisibility(View.VISIBLE);
                holder.massageIdRight.setVisibility(View.VISIBLE);
                holder.imageMessageRight.setVisibility(View.GONE);

                holder.massageIdRight.setText(messageModel.getMessage());
            } else {
                holder.message_linearlayoutIdLeft.setVisibility(View.VISIBLE);
                holder.message_linearlayoutIdRight.setVisibility(View.GONE);
                holder.massageIdLeft.setVisibility(View.VISIBLE);
                holder.imageMessageLeft.setVisibility(View.GONE);

                holder.massageIdLeft.setText(messageModel.getMessage());
            }
        }

        // Set timestamp
        if (messageModel.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            holder.rightTimeTextView.setText(formatTimestamp(messageModel.getTime()));
        } else {
            holder.leftTimeTextView.setText(formatTimestamp(messageModel.getTime()));
        }

        // Set long click listener for the item
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (messageLongClickListener != null) {
                    messageLongClickListener.onMessageLongClick(messageModel);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView massageIdLeft, massageIdRight, leftTimeTextView, rightTimeTextView;
        ConstraintLayout message_linearlayoutIdLeft, message_linearlayoutIdRight;
        ImageView imageMessageLeft, imageMessageRight;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            massageIdLeft = itemView.findViewById(R.id.massageIdLeft);
            massageIdRight = itemView.findViewById(R.id.massageIdRight);
            rightTimeTextView = itemView.findViewById(R.id.rightTimeTextView);
            leftTimeTextView = itemView.findViewById(R.id.leftTimeTextView);
            message_linearlayoutIdLeft = itemView.findViewById(R.id.message_linearlayoutIdLeft);
            message_linearlayoutIdRight = itemView.findViewById(R.id.message_linearlayoutIdRight);
            imageMessageLeft = itemView.findViewById(R.id.imageMessageLeft);
            imageMessageRight = itemView.findViewById(R.id.imageMessageRight);
        }
    }

    private String formatTimestamp(long timestamp) {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat.format(new Date(timestamp));
    }
}
