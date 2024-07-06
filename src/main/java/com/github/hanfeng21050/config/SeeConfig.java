package com.github.hanfeng21050.config;

import com.github.hanfeng21050.utils.CryptUtil;
import com.github.hanfeng21050.utils.PasswordUtil;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

/**
 * @Author hanfeng32305
 * @Date 2023/11/1 16:05
 */
public class SeeConfig {
    private String uuid;
    private String address;
    private String username;

    public SeeConfig(String uuid, String address, String username) {
        this.uuid = uuid;
        this.address = address;
        this.username = username;
    }

    @Override
    public String toString() {
        return "SeeConfig{" +
                "uuid='" + uuid + '\'' +
                ", address='" + address + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() throws Exception {
        CredentialAttributes credentialAttributes = PasswordUtil.createCredentialAttributes(this.uuid);
        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials != null) {
            String password = credentials.getPasswordAsString();
            return CryptUtil.encryptPassword(password);
        }
        return "";
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
