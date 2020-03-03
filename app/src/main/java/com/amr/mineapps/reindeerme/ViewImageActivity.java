package com.amr.mineapps.reindeerme;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ViewImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        Glide.with(this)
                .load(getIntent().getStringExtra("image_url"))
                .into((ImageView) findViewById(R.id.full_image_imageview));
    }
}
