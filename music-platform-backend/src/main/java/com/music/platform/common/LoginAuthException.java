package com.music.platform.common;

//登录失败报错
public class LoginAuthException extends RuntimeException {
    public LoginAuthException(String message) {
        super(message);
    }
}
