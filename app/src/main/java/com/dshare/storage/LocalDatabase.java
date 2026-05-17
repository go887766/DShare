package com.dshare.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.dshare.model.User;
import com.dshare.model.Post;
import com.dshare.model.Comment;
import com.dshare.model.MediaContent;
import com.dshare.blockchain.Block;
import com.dshare.blockchain.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalDatabase extends SQLiteOpenHelper {

    private static final String TAG = "LocalDatabase";
    private static final String DB_NAME = "dshare.db";
    private static final int DB_VERSION = 1;
    private Gson gson;
    
    private final AtomicInteger openCounter = new AtomicInteger();
    private SQLiteDatabase database;

    public LocalDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        gson = new Gson();
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "address TEXT PRIMARY KEY," +
                "public_key_base64 TEXT," +
                "encrypted_private_key TEXT," +
                "nickname TEXT," +
                "bio TEXT," +
                "avatar_hash TEXT," +
                "created_at INTEGER," +
                "last_login_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS posts (" +
                "post_id TEXT PRIMARY KEY," +
                "author_address TEXT," +
                "author_nickname TEXT," +
                "content TEXT," +
                "media_list TEXT," +
                "timestamp INTEGER," +
                "like_count INTEGER DEFAULT 0," +
                "dislike_count INTEGER DEFAULT 0," +
                "comment_count INTEGER DEFAULT 0," +
                "gold_reward INTEGER DEFAULT 0," +
                "liked_by TEXT," +
                "disliked_by TEXT," +
                "signature TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS comments (" +
                "comment_id TEXT PRIMARY KEY," +
                "post_id TEXT," +
                "author_address TEXT," +
                "author_nickname TEXT," +
                "content TEXT," +
                "timestamp INTEGER," +
                "like_count INTEGER DEFAULT 0," +
                "gold_reward INTEGER DEFAULT 0," +
                "signature TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS blockchain (" +
                "block_index INTEGER PRIMARY KEY," +
                "block_data TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS transactions (" +
                "tx_id TEXT PRIMARY KEY," +
                "from_address TEXT," +
                "to_address TEXT," +
                "amount INTEGER," +
                "timestamp INTEGER," +
                "type TEXT," +
                "data_hash TEXT," +
                "signature TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS peers (" +
                "peer_id TEXT PRIMARY KEY," +
                "peer_address TEXT," +
                "public_key TEXT," +
                "last_seen INTEGER," +
                "is_connected INTEGER DEFAULT 0)");
        
        createIndexes(db);
    }
    
    private void createIndexes(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_posts_timestamp ON posts(timestamp)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_posts_author ON posts(author_address)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_comments_post ON comments(post_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_address ON transactions(from_address, to_address)");
        } catch (Exception e) {
            Log.w(TAG, "Error creating indexes", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS posts");
        db.execSQL("DROP TABLE IF EXISTS comments");
        db.execSQL("DROP TABLE IF EXISTS blockchain");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS peers");
        onCreate(db);
    }
    
    public synchronized SQLiteDatabase openDatabase() {
        if (openCounter.incrementAndGet() == 1) {
            database = getWritableDatabase();
        }
        return database;
    }
    
    public synchronized void closeDatabase() {
        if (openCounter.decrementAndGet() == 0) {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    public void saveUser(User user) {
        SQLiteDatabase db = openDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("address", user.getAddress());
            values.put("public_key_base64", user.getPublicKeyBase64());
            values.put("encrypted_private_key", user.getEncryptedPrivateKey());
            values.put("nickname", user.getNickname());
            values.put("bio", user.getBio());
            values.put("avatar_hash", user.getAvatarHash());
            values.put("created_at", user.getCreatedAt());
            values.put("last_login_at", user.getLastLoginAt());
            db.replace("users", null, values);
        } finally {
            closeDatabase();
        }
    }

    public User getUser(String address) {
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM users WHERE address = ?", new String[]{address});
            if (cursor.moveToFirst()) {
                User user = new User();
                user.setAddress(cursor.getString(0));
                user.setPublicKeyBase64(cursor.getString(1));
                user.setEncryptedPrivateKey(cursor.getString(2));
                user.setNickname(cursor.getString(3));
                user.setBio(cursor.getString(4));
                user.setAvatarHash(cursor.getString(5));
                user.setCreatedAt(cursor.getLong(6));
                user.setLastLoginAt(cursor.getLong(7));
                return user;
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return null;
    }

    public void savePost(Post post) {
        SQLiteDatabase db = openDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("post_id", post.getPostId());
            values.put("author_address", post.getAuthorAddress());
            values.put("author_nickname", post.getAuthorNickname());
            values.put("content", post.getContent());
            values.put("media_list", gson.toJson(post.getMediaList()));
            values.put("timestamp", post.getTimestamp());
            values.put("like_count", post.getLikeCount());
            values.put("dislike_count", post.getDislikeCount());
            values.put("comment_count", post.getCommentCount());
            values.put("gold_reward", post.getGoldReward());
            values.put("liked_by", gson.toJson(post.getLikedBy()));
            values.put("disliked_by", gson.toJson(post.getDislikedBy()));
            values.put("signature", post.getSignature());
            db.replace("posts", null, values);
        } finally {
            closeDatabase();
        }
    }

    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM posts ORDER BY timestamp DESC LIMIT 100", null);
            while (cursor.moveToNext()) {
                Post post = new Post();
                post.setPostId(cursor.getString(0));
                post.setAuthorAddress(cursor.getString(1));
                post.setAuthorNickname(cursor.getString(2));
                post.setContent(cursor.getString(3));
                String mediaJson = cursor.getString(4);
                if (mediaJson != null) {
                    post.setMediaList(gson.fromJson(mediaJson, new TypeToken<List<MediaContent>>(){}.getType()));
                }
                post.setTimestamp(cursor.getLong(5));
                post.setLikeCount(cursor.getInt(6));
                post.setDislikeCount(cursor.getInt(7));
                post.setCommentCount(cursor.getInt(8));
                post.setGoldReward(cursor.getInt(9));
                String likedByJson = cursor.getString(10);
                if (likedByJson != null) {
                    post.setLikedBy(gson.fromJson(likedByJson, new TypeToken<List<String>>(){}.getType()));
                }
                String dislikedByJson = cursor.getString(11);
                if (dislikedByJson != null) {
                    post.setDislikedBy(gson.fromJson(dislikedByJson, new TypeToken<List<String>>(){}.getType()));
                }
                post.setSignature(cursor.getString(12));
                posts.add(post);
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return posts;
    }

    public Post getPost(String postId) {
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM posts WHERE post_id = ?", new String[]{postId});
            if (cursor.moveToFirst()) {
                Post post = new Post();
                post.setPostId(cursor.getString(0));
                post.setAuthorAddress(cursor.getString(1));
                post.setAuthorNickname(cursor.getString(2));
                post.setContent(cursor.getString(3));
                String mediaJson = cursor.getString(4);
                if (mediaJson != null) {
                    post.setMediaList(gson.fromJson(mediaJson, new TypeToken<List<MediaContent>>(){}.getType()));
                }
                post.setTimestamp(cursor.getLong(5));
                post.setLikeCount(cursor.getInt(6));
                post.setDislikeCount(cursor.getInt(7));
                post.setCommentCount(cursor.getInt(8));
                post.setGoldReward(cursor.getInt(9));
                post.setSignature(cursor.getString(12));
                return post;
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return null;
    }

    public void saveComment(Comment comment) {
        SQLiteDatabase db = openDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("comment_id", comment.getCommentId());
            values.put("post_id", comment.getPostId());
            values.put("author_address", comment.getAuthorAddress());
            values.put("author_nickname", comment.getAuthorNickname());
            values.put("content", comment.getContent());
            values.put("timestamp", comment.getTimestamp());
            values.put("like_count", comment.getLikeCount());
            values.put("gold_reward", comment.getGoldReward());
            values.put("signature", comment.getSignature());
            db.replace("comments", null, values);
            updatePostCommentCount(comment.getPostId());
        } finally {
            closeDatabase();
        }
    }

    public List<Comment> getCommentsForPost(String postId) {
        List<Comment> comments = new ArrayList<>();
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM comments WHERE post_id = ? ORDER BY timestamp ASC", new String[]{postId});
            while (cursor.moveToNext()) {
                Comment comment = new Comment();
                comment.setCommentId(cursor.getString(0));
                comment.setPostId(cursor.getString(1));
                comment.setAuthorAddress(cursor.getString(2));
                comment.setAuthorNickname(cursor.getString(3));
                comment.setContent(cursor.getString(4));
                comment.setTimestamp(cursor.getLong(5));
                comment.setLikeCount(cursor.getInt(6));
                comment.setGoldReward(cursor.getInt(7));
                comment.setSignature(cursor.getString(8));
                comments.add(comment);
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return comments;
    }

    private void updatePostCommentCount(String postId) {
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM comments WHERE post_id = ?", new String[]{postId});
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                ContentValues values = new ContentValues();
                values.put("comment_count", count);
                db.update("posts", values, "post_id = ?", new String[]{postId});
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void saveBlock(Block block) {
        SQLiteDatabase db = openDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("block_index", block.getIndex());
            values.put("block_data", gson.toJson(block));
            db.replace("blockchain", null, values);
        } finally {
            closeDatabase();
        }
    }

    public List<Block> getAllBlocks() {
        List<Block> blocks = new ArrayList<>();
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM blockchain ORDER BY block_index ASC", null);
            while (cursor.moveToNext()) {
                String blockData = cursor.getString(1);
                Block block = gson.fromJson(blockData, Block.class);
                blocks.add(block);
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return blocks;
    }

    public void saveTransaction(Transaction tx) {
        SQLiteDatabase db = openDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("tx_id", tx.getTxId());
            values.put("from_address", tx.getFromAddress());
            values.put("to_address", tx.getToAddress());
            values.put("amount", tx.getAmount());
            values.put("timestamp", tx.getTimestamp());
            values.put("type", tx.getType());
            values.put("data_hash", tx.getDataHash());
            values.put("signature", tx.getSignature());
            db.replace("transactions", null, values);
        } finally {
            closeDatabase();
        }
    }

    public List<Transaction> getTransactionsForAddress(String address) {
        List<Transaction> txs = new ArrayList<>();
        SQLiteDatabase db = openDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM transactions WHERE from_address = ? OR to_address = ? ORDER BY timestamp DESC LIMIT 50",
                    new String[]{address, address});
            while (cursor.moveToNext()) {
                Transaction tx = new Transaction();
                tx.setTxId(cursor.getString(0));
                tx.setFromAddress(cursor.getString(1));
                tx.setToAddress(cursor.getString(2));
                tx.setAmount(cursor.getLong(3));
                tx.setTimestamp(cursor.getLong(4));
                tx.setType(cursor.getString(5));
                tx.setDataHash(cursor.getString(6));
                tx.setSignature(cursor.getString(7));
                txs.add(tx);
            }
        } finally {
            if (cursor != null) cursor.close();
            closeDatabase();
        }
        return txs;
    }

    public void deletePost(String postId) {
        SQLiteDatabase db = openDatabase();
        try {
            db.delete("posts", "post_id = ?", new String[]{postId});
            db.delete("comments", "post_id = ?", new String[]{postId});
        } finally {
            closeDatabase();
        }
    }
}
