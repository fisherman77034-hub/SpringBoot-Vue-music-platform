package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.InteractionService;
import com.music.platform.web.dto.CommentRequest;
import com.music.platform.web.dto.RateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {
    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/{musicId}/comment")
    public ApiResponse<Void> comment(@PathVariable long musicId, @Valid @RequestBody CommentRequest req) {
        var ju = SecurityUtils.currentJwtUser();
        interactionService.comment(ju.userId(), musicId, req.getContent());
        return ApiResponse.ok();
    }

    @GetMapping("/{musicId}/comments")
    public ApiResponse<List<Map<String, Object>>> comments(@PathVariable long musicId) {
        var list = interactionService.comments(musicId).stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "musicId", c.getMusicId(),
                        "userId", c.getUserId(),
                        "username", c.getUsername(),
                        "nickname", c.getNickname(),
                        "content", c.getContent(),
                        "createdAt", c.getCreatedAt()
                ))
                .toList();
        return ApiResponse.ok(list);
    }

    @PostMapping("/{musicId}/rate")
    public ApiResponse<Void> rate(@PathVariable long musicId, @Valid @RequestBody RateRequest req) {
        var ju = SecurityUtils.currentJwtUser();
        interactionService.rate(ju.userId(), musicId, req.getStar());
        return ApiResponse.ok();
    }

    @GetMapping("/{musicId}/rating-stat")
    public ApiResponse<Map<String, Object>> ratingStat(@PathVariable long musicId) {
        Map<String, Object> map = new LinkedHashMap<>(interactionService.ratingStat(musicId));
        SecurityUtils.optionalJwtUser().ifPresent(ju -> {
            Integer my = interactionService.findUserStar(ju.userId(), musicId);
            if (my != null) {
                map.put("myStar", my);
            }
        });
        return ApiResponse.ok(map);
    }
}

