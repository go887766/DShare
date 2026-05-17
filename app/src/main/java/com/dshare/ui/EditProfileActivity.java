package com.dshare.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.model.User;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etNickname;
    private EditText etBio;
    private Button btnSave;
    private String currentAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etNickname = findViewById(R.id.et_nickname);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save_profile);

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentAddress = app.getWallet().getAddress();
        
        loadCurrentData();
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private void loadCurrentData() {
        DShareApplication app = DShareApplication.getInstance();
        User user = app.getDatabase().getUser(currentAddress);
        
        if (user != null) {
            etNickname.setText(user.getNickname() != null ? user.getNickname() : "");
            etBio.setText(user.getBio() != null ? user.getBio() : "");
        }
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        User user = app.getDatabase().getUser(currentAddress);
        
        if (user != null) {
            user.setNickname(nickname);
            user.setBio(bio);
            app.getDatabase().saveUser(user);
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
