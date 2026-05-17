package com.dshare.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Wallet {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String address;

    public Wallet() {
        KeyPair keyPair = CryptoManager.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        this.address = CryptoManager.getAddressFromPublicKey(this.publicKey);
    }

    public Wallet(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.address = CryptoManager.getAddressFromPublicKey(this.publicKey);
    }

    public String sign(String data) {
        return CryptoManager.signData(data, privateKey);
    }

    public boolean verify(String data, String signature) {
        return CryptoManager.verifySignature(data, signature, publicKey);
    }

    public boolean verify(String data, String signature, PublicKey otherPublicKey) {
        return CryptoManager.verifySignature(data, signature, otherPublicKey);
    }

    public String getPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getAddress() {
        return address;
    }

    public static Wallet fromBase64(String privateKeyBase64, String publicKeyBase64) {
        try {
            PrivateKey privateKey = CryptoManager.bytesToPrivateKey(
                    Base64.getDecoder().decode(privateKeyBase64));
            PublicKey publicKey = CryptoManager.bytesToPublicKey(
                    Base64.getDecoder().decode(publicKeyBase64));
            return new Wallet(privateKey, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore wallet", e);
        }
    }
}