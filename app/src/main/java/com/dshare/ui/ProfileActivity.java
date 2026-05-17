package com.dshare.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    private Button btnCopyAddress;
    private Button btnEditProfile;
    private String walletAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvAddress = findViewById(R.id.tv_address);
        tvNickname = findViewById(R.id.tv_nickname);
        tvBio = findViewById(R.id.tv_bio);
        tvPostCount = findViewById(R.id.tv_post_count);
        tvGoldBalance = findViewById(R.id.tv_gold_balance);
        btnCopyAddress = findViewById(R.id.btn_copy_address);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        loadProfile();

        btnCopyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (walletAddress != null) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Wallet Address", walletAddress);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ProfileActivity.this, R.string.address_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadProfile() {
        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        walletAddress = app.getWallet().getAddress();
        User user = app.getDatabase().getUser(walletAddress);

        if (user != null) {
            tvAddress.setText(getString(R.string.address_prefix) + walletAddress);
            tvNickname.setText(getString(R.string.nickname_prefix) + user.getNickname());
            tvBio.setText(getString(R.string.bio_prefix) + (user.getBio() != null ? user.getBio() : getString(R.string.no_bio_yet)));
            tvGoldBalance.setText(getString(R.string.gold_prefix) + app.getGoldBalance());
        }

        try {
            int postCount = app.getDatabase().getAllPosts().size();
            tvPostCount.setText(getString(R.string.posts_prefix) + postCount);
        } catch (Exception e) {
            tvPostCount.setText(getString(R.string.posts_prefix) + "0");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}
