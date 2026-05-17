package com.dshare.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.model.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAddress;
    private TextView tvNickname;
    private TextView tvBio;
    private TextView tvPostCount;
    private TextView tvGoldBalance;
    private Button btnEditProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvAddress = findViewById(R.id.tv_address);
        tvNickname = findViewById(R.id.tv_nickname);
        tvBio = findViewById(R.id.tv_bio);
        tvPostCount = findViewById(R.id.tv_post_count);
        tvGoldBalance = findViewById(R.id.tv_gold_balance);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        loadProfile();
    }

    private void loadProfile() {
        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String address = app.getWallet().getAddress();
        User user = app.getDatabase().getUser(address);

        if (user != null) {
            tvAddress.setText("Address: " + address);
            tvNickname.setText("Nickname: " + user.getNickname());
            tvBio.setText("Bio: " + (user.getBio() != null ? user.getBio() : "No bio yet"));
            tvGoldBalance.setText("Gold: " + app.getGoldBalance());
        }

        try {
            int postCount = app.getDatabase().getAllPosts().size();
            tvPostCount.setText("Posts: " + postCount);
        } catch (Exception e) {
            tvPostCount.setText("Posts: 0");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}