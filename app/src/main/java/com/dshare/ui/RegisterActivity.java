package com.dshare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.crypto.CryptoManager;
import com.dshare.crypto.Wallet;
import com.dshare.model.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNickname;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegister();
            }
        });
    }

    private void handleRegister() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nickname) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Wallet wallet = new Wallet();
            String salt = wallet.getAddress().substring(0, 16);
            String passwordHash = CryptoManager.hashPassword(password, salt);
            String encryptedKey = passwordHash + ":" + wallet.getPrivateKeyBase64();

            User user = new User(wallet.getAddress(), wallet.getPublicKeyBase64());
            user.setNickname(nickname);
            user.setEncryptedPrivateKey(encryptedKey);

            DShareApplication app = DShareApplication.getInstance();
            app.getDatabase().saveUser(user);
            app.initWallet(wallet);

            Toast.makeText(this, "Registration successful!\nYour address: " + wallet.getAddress(), Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, MainFeedActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}