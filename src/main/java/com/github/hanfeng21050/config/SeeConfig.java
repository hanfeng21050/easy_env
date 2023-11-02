package com.github.hanfeng21050.config;

/**
 * @Author hanfeng32305
 * @Date 2023/11/1 16:05
 */
public class SeeConfig {
    private String address;
    private String username;
    private String password;

    public SeeConfig(String address, String username, String password) {
        this.address = address;
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "SeeConfig{" +
                "address='" + address + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
