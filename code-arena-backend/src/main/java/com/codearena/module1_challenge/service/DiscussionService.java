package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.CommentRequestDto;
import com.codearena.module1_challenge.dto.CommentResponseDto;

import java.util.List;

public interface DiscussionService {
    CommentResponseDto createComment(Long challengeId, CommentRequestDto request, String userId, String userName);
    List<CommentResponseDto> getCommentsByChallenge(Long challengeId);
    void deleteComment(Long commentId, String userId);
}
