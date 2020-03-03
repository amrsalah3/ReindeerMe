package com.amr.mineapps.reindeerme.chat_classes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.amr.mineapps.reindeerme.R;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;


public class MessageAdapter extends ArrayAdapter<Message> {
    private Context mContext;
    private ArrayList<Message> mList;
    private RelativeLayout.LayoutParams receiverParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    private RelativeLayout.LayoutParams senderParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

    public MessageAdapter(Context context, ArrayList<Message> list) {
        super(context, 0, list);
        mContext = context;
        mList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View itemView = convertView;

        if (convertView == null) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.message_item, parent, false);
        }

        Message message = mList.get(position);
        // Set message layout style (alignment and color) for sending or receiving message
        senderParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        receiverParams.addRule(RelativeLayout.ALIGN_PARENT_START);

        ConstraintLayout replyMsgLayout = itemView.findViewById(R.id.reply_msg_layout);
        ConstraintLayout msgLayout = itemView.findViewById(R.id.message_layout);
        // Change background color of the layout (which is the "rounded_corners.xml" drawable)
        GradientDrawable messageBackground = (GradientDrawable) msgLayout.getBackground();

        TextView textView = itemView.findViewById(R.id.msg_tv);
        ImageView imageView = itemView.findViewById(R.id.msg_image);
        TextView replyTextView = itemView.findViewById(R.id.reply_tv_item);
        ImageView replyImageView = itemView.findViewById(R.id.reply_image_item);

        // Show date of the message
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd MMM, hh:mm a");
        TextView msgTimeTextView = itemView.findViewById(R.id.timeofmsg_tv);
        msgTimeTextView.setText(formattedDate.format(message.getDate_in_millis()));

        // if the message is deleted
        if (message.getDeleted() == 1) {
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            replyTextView.setVisibility(View.GONE);
            replyImageView.setVisibility(View.GONE);
            textView.setTypeface(null, Typeface.ITALIC);
        } else {
            textView.setTypeface(null, Typeface.NORMAL);
        }

        // text msg or image msg :
        if (message.getMsg_type() == ChatPage.MSG_TEXT) {
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            replyTextView.setVisibility(View.GONE);
            replyImageView.setVisibility(View.GONE);
            textView.setText(message.getMsg_content());
        } else {
            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            replyTextView.setVisibility(View.GONE);
            replyImageView.setVisibility(View.GONE);
            String url = message.getMsg_content();
            Glide.with(mContext).load(url).into(imageView);
        }

        ImageView messageStatus = itemView.findViewById(R.id.msg_status_imageview);
        if (message.getmSentOrRecieved() == ChatPage.SENDER_CODE) {
            // If the message is sent
            messageBackground.setColor(mContext.getResources().getColor(R.color.colorPrimary));
            textView.setTextColor(Color.parseColor("#ffffff"));
            msgTimeTextView.setTextColor(Color.parseColor("#ffffff"));
            replyMsgLayout.setLayoutParams(senderParams);
            // Change message status
            messageStatus.setColorFilter(null);
            switch (message.getMsg_status()) {
                case 0:
                    messageStatus.setImageDrawable(mContext.getDrawable(R.drawable.pending));
                    break;
                case 1:
                    messageStatus.setImageDrawable(mContext.getDrawable(R.drawable.sent));
                    break;
                case 2:
                    messageStatus.setImageDrawable(mContext.getDrawable(R.drawable.received));
                    break;
                case 3:
                    messageStatus.setImageDrawable(mContext.getDrawable(R.drawable.received));
                    messageStatus.setColorFilter(Color.GREEN);
                    break;
            }
            messageStatus.setVisibility(View.VISIBLE);
        } else {
            // Otherwise the message is received, Change style!
            messageBackground.setColor(Color.parseColor("#ffffff"));
            textView.setTextColor(Color.parseColor("#000000"));
            msgTimeTextView.setTextColor(Color.parseColor("#000000"));
            replyMsgLayout.setLayoutParams(receiverParams);
            messageStatus.setVisibility(View.GONE);
        }
        /* FOR REPLY MSG */
        // If this message is a reply to another one
        if (message.getReply_key() != null) {
            Message replyMsg = getMsgByKey(message.getReply_key());
            // if reply msg is deleted
            if (replyMsg.getDeleted() == 1) {
                replyTextView.setVisibility(View.VISIBLE);
                replyImageView.setVisibility(View.GONE);
                replyTextView.setTypeface(null, Typeface.ITALIC);
            } else {
                replyTextView.setTypeface(null, Typeface.NORMAL);
            }
            // text reply msg or image reply msg :
            if (replyMsg.getMsg_type() == ChatPage.MSG_TEXT) {
                replyImageView.setVisibility(View.GONE);
                replyTextView.setVisibility(View.VISIBLE);
                replyTextView.setText(replyMsg.getMsg_content());
            } else {
                replyTextView.setVisibility(View.GONE);
                replyImageView.setVisibility(View.VISIBLE);
                String url = replyMsg.getMsg_content();
                Glide.with(mContext).load(url).into(replyImageView);
            }

            // If the replied msg was sent by the current user
            // Change background color of the layout (which is the "rounded_corners.xml" drawable)
            GradientDrawable replyMsgBackground = (GradientDrawable) replyMsgLayout.getBackground();
            if (replyMsg.getmSentOrRecieved() == ChatPage.SENDER_CODE) {
                replyMsgBackground.setColor(Color.parseColor("#809e47ff"));
                replyTextView.setTextColor(Color.parseColor("#ffffff"));
            } else {
                // Otherwise change style!
                replyMsgBackground.setColor(Color.parseColor("#80ffffff"));
                replyTextView.setTextColor(Color.parseColor("#000000"));
            }
        }


        return itemView;
    }

    public Message getMsgByKey(String key) {
        Message msgObj;
        for (int i = 0, len = mList.size(); i < len; i++) {
            msgObj = mList.get(i);
            if (key.equals(msgObj.getmKey())) {
                return msgObj;
            }
        }
        return null;
    }

}
