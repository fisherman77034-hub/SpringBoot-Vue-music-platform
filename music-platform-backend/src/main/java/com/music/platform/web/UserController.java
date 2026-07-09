package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.common.StoredRelativePath;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.StorageService;
import com.music.platform.service.UserService;
import com.music.platform.web.dto.UpdateMoodRequest;
import com.music.platform.web.dto.UpdateProfileRequest;
import com.music.platform.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final StorageService storageService;

    public UserController(UserService userService, StorageService storageService) {
        this.userService = userService;
        this.storageService = storageService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        var ju = SecurityUtils.currentJwtUser();
        var u = userService.getById(ju.userId());
        String avatarKey = StoredRelativePath.normalizeOrNull(u.getAvatarPath(), "avatars");
        String avatarUrl = avatarKey != null ? "/files/" + avatarKey : null;
        return ApiResponse.ok(new UserProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getNickname(),
                avatarUrl,
                u.getBio(),
                u.getGender(),
                u.getAge(),
                u.getMood(),
                u.getRole()
        ));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        var ju = SecurityUtils.currentJwtUser();
        userService.updateProfile(ju.userId(), req);
        return ApiResponse.ok();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadAvatar(@org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file)
            throws IOException {
        var ju = SecurityUtils.currentJwtUser();
        String path = storageService.saveAvatar(file);
        userService.updateAvatarPath(ju.userId(), path);
        return ApiResponse.ok("/files/" + path);
    }

    @PostMapping("/mood")
    public ApiResponse<Void> updateMood(@Valid @RequestBody UpdateMoodRequest req) {
        var ju = SecurityUtils.currentJwtUser();
        userService.updateMood(ju.userId(), req.getMood());
        return ApiResponse.ok();
    }
}

