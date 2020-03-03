package com.amr.mineapps.reindeerme.friendrequest_classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amr.mineapps.reindeerme.MainActivity;
import com.amr.mineapps.reindeerme.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Amr on 24-Jun-19.
 */

public class FriendRequestAdapter extends ArrayAdapter<FriendRequest> {

    private Context mContext;
    private ArrayList<FriendRequest> mList;

    public FriendRequestAdapter(Context context, ArrayList<FriendRequest> list) {
        super(context, 0, list);
        mContext = context;
        mList = list;
    }

    public void setList(ArrayList<FriendRequest> list) {
        mList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View itemView = convertView;
        if (convertView == null) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.friend_request_card, parent, false);
        }

        final FriendRequest friendRequest = mList.get(position);

        ImageView imageView = itemView.findViewById(R.id.imageView);
        Glide.with(mContext).load(friendRequest.getmReqPP()).error(R.drawable.default_pp).circleCrop().into(imageView);

        TextView textView = itemView.findViewById(R.id.req_name);
        textView.setText(friendRequest.getmReqName());

        Button acceptBtn = itemView.findViewById(R.id.req_accept_btn);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, R.string.accepted, Toast.LENGTH_SHORT).show();
                MainActivity.usersRef
                        .child(MainActivity.currentUserUid)
                        .child("inpendingfriendreq")
                        .child(friendRequest.getmReqUid())
                        .setValue(null);
                MainActivity.usersRef
                        .child(MainActivity.currentUserUid)
                        .child("friends")
                        .child(friendRequest.getmReqUid())
                        .child("name")
                        .setValue(friendRequest.getmReqName());
                MainActivity.usersRef
                        .child(MainActivity.currentUserUid)
                        .child("friends")
                        .child(friendRequest.getmReqUid())
                        .child("ppurl")
                        .setValue(friendRequest.getmReqPP());
            }
        });
        Button deleteBtn = itemView.findViewById(R.id.req_delete_btn);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.usersRef
                        .child(MainActivity.currentUserUid)
                        .child("inpendingfriendreq")
                        .child(friendRequest.getmReqUid())
                        .setValue(null);
            }
        });

        return itemView;
    }

    public void removeFriendObjByUid(String uid) {
        FriendRequest obj;
        for (int i = 0, x = mList.size(); i < x; i++) {
            obj = mList.get(i);
            if (obj.getmReqUid().equals(uid)) {
                mList.remove(obj);
                this.notifyDataSetChanged();
                break;
            }
        }
    }

}
