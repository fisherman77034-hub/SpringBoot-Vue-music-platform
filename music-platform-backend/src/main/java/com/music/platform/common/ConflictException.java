package com.music.platform.common;


 // 资源冲突（如用户名唯一约束），对应 HTTP 409。

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
