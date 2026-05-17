package com.dshare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.crypto.CryptoManager;
import com.dshare.crypto.Wallet;
import com.dshare.model.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etAddress;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etAddress = findViewById(R.id.et_address);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void handleLogin() {
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter address and password", Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        User savedUser = app.getDatabase().getUser(address);

        if (savedUser == null) {
            Toast.makeText(this, "User not found, please register first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String storedHash = savedUser.getEncryptedPrivateKey().split(":")[0];
            String salt = savedUser.getAddress().substring(0, 16);
            String passwordHash = CryptoManager.hashPassword(password, salt);
            if (!passwordHash.equals(storedHash)) {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                return;
            }

            String privateKeyBase64 = savedUser.getEncryptedPrivateKey().split(":")[1];
            Wallet wallet = Wallet.fromBase64(privateKeyBase64, savedUser.getPublicKeyBase64());
            app.initWallet(wallet);
            app.startP2P();

            savedUser.setLastLoginAt(System.currentTimeMillis());
            app.getDatabase().saveUser(savedUser);

            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainFeedActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}