package com.dshare.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.blockchain.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBalance;
    private TextView tvAddress;
    private RecyclerView rvTransactions;
    private EditText etToAddress;
    private EditText etAmount;
    private Button btnSend;
    private Button btnMine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        tvBalance = findViewById(R.id.tv_gold_balance);
        tvAddress = findViewById(R.id.tv_wallet_address);
        rvTransactions = findViewById(R.id.rv_transactions);
        etToAddress = findViewById(R.id.et_to_address);
        etAmount = findViewById(R.id.et_amount);
        btnSend = findViewById(R.id.btn_send_gold);
        btnMine = findViewById(R.id.btn_mine);

        loadWalletInfo();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendGold();
            }
        });

        btnMine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mineGold();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletInfo();
    }

    private void loadWalletInfo() {
        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() != null) {
            tvAddress.setText("Address: " + app.getWallet().getAddress());
            long balance = app.getGoldBalance();
            tvBalance.setText(balance + " DShare Gold");

            List<Transaction> transactions = app.getDatabase().getTransactionsForAddress(app.getWallet().getAddress());
            TransactionAdapter adapter = new TransactionAdapter(transactions);
            rvTransactions.setLayoutManager(new LinearLayoutManager(this));
            rvTransactions.setAdapter(adapter);
        }
    }

    private void sendGold() {
        String toAddress = etToAddress.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (toAddress.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0 || amount > app.getGoldBalance()) {
            Toast.makeText(this, "Invalid amount or insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                app.getWallet().getAddress(),
                toAddress,
                amount,
                Transaction.TYPE_TRANSFER
            );

            String dataToSign = tx.getTxId() + tx.getFromAddress() + tx.getToAddress() + tx.getAmount();
            tx.setSignature(app.getWallet().sign(dataToSign));

            app.getBlockchain().addTransaction(tx);
            app.getDatabase().saveTransaction(tx);

            Toast.makeText(this, "Transaction sent!", Toast.LENGTH_SHORT).show();
            etToAddress.setText("");
            etAmount.setText("");
            loadWalletInfo();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mineGold() {
        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Mining started...", Toast.LENGTH_LONG).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    com.dshare.blockchain.Blockchain blockchain = app.getBlockchain();
                    blockchain.minePendingTransactions(app.getWallet().getAddress());

                    for (com.dshare.blockchain.Block block : blockchain.getChain()) {
                        app.getDatabase().saveBlock(block);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WalletActivity.this, "Mining complete! Reward: " + blockchain.getMiningReward() + " Gold", Toast.LENGTH_LONG).show();
                            loadWalletInfo();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WalletActivity.this, "Mining failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<Transaction> transactions;

        TransactionAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Transaction tx = transactions.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.text1.setText(tx.getType() + ": " + tx.getAmount() + " Gold");
            holder.text2.setText("From: " + tx.getFromAddress().substring(0, 8) + "... To: " + tx.getToAddress().substring(0, 8) + "... " + sdf.format(new Date(tx.getTimestamp())));
        }

        @Override
        public int getItemCount() { return transactions.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}