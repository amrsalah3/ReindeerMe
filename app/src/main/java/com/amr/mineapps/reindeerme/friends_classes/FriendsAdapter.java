package com.amr.mineapps.reindeerme.friends_classes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amr.mineapps.reindeerme.MainActivity;
import com.amr.mineapps.reindeerme.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class FriendsAdapter extends ArrayAdapter<Friend> {

    private Context mContext;
    private ArrayList<Friend> mList;
    private ArrayList<Friend> backupList = new ArrayList<>();

    public FriendsAdapter(Context context, ArrayList<Friend> arrList) {
        super(context, 0, arrList);
        mContext = context;
        mList = arrList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View itemView = convertView;
        if (convertView == null) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.friends_list_item, parent, false);
        }

        final Friend friend = mList.get(position);
        ImageView ppView = itemView.findViewById(R.id.friend_pic);
        Glide.with(mContext).load(friend.getPpUrl()).circleCrop().error(R.drawable.default_pp).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(ppView);

        TextView nameView = itemView.findViewById(R.id.friend_name);
        nameView.setText(friend.getName());

        DataSnapshot lastMsg = friend.getLastMessage();
        if (lastMsg.getValue() != null && lastMsg.child("msg_type").getValue(Integer.class) != null) {
            TextView lastMsgView = itemView.findViewById(R.id.last_msg);
            // Last msg is text or image
            String s = lastMsg.child("msg_type").getValue(Integer.class) == 1 ?
                    lastMsg.child("msg_content").getValue(String.class) : "photo";

            if (s != null) {
                if (s.length() > 15) {
                    s = s.substring(0, 15) + "...";
                }
                if (MainActivity.currentUserUid.equals(lastMsg.child("sender_id").getValue(String.class))) {
                    lastMsgView.setTypeface(null, Typeface.NORMAL);
                    lastMsgView.setText(mContext.getString(R.string.you) + " " + s);
                } else {
                    if (friend.getLastMsgStatus() != 3) { // not seen yet
                        lastMsgView.setTextColor(Color.parseColor("#000000"));
                        lastMsgView.setTypeface(null, Typeface.BOLD_ITALIC);
                    } else {
                        lastMsgView.setTextColor(Color.GRAY);
                        lastMsgView.setTypeface(null, Typeface.NORMAL);
                    }
                    lastMsgView.setText(s);
                }
            }
        }

        return itemView;
    }


    public Friend getFriendObjByUid(String uid) {
        Friend obj;
        for (int i = 0, x = mList.size(); i < x; i++) {
            obj = mList.get(i);
            if (obj.getUid().equals(uid)) {
                return obj;
            }
        }
        return null;
    }

    public void removeFriendObjByUid(String uid) {
        Friend obj;
        for (int i = 0, x = mList.size(); i < x; i++) {
            obj = mList.get(i);
            if (obj.getUid().equals(uid)) {
                mList.remove(obj);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void filter(String nameText) {
        // Just copy the original list for the first time
        if (backupList.isEmpty()) {
            backupList.addAll(mList);
        }
        // Clear all friends from the list search when clicked
        mList.clear();
        // If empty text add all friends to the list search
        if (nameText.trim().length() == 0) {
            mList.addAll(backupList);
        } else {
            // If someone matches the search show him
            nameText = nameText.toLowerCase();
            for (Friend friend : backupList) {
                if (friend.getName().toLowerCase().contains(nameText)) {
                    mList.add(friend);
                }
            }
        }
        notifyDataSetChanged();
    }
}
