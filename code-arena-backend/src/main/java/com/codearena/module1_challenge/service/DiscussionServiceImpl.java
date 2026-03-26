package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.CommentRequestDto;
import com.codearena.module1_challenge.dto.CommentResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.ChallengeComment;
import com.codearena.module1_challenge.repository.ChallengeCommentRepository;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final ChallengeCommentRepository commentRepository;
    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long challengeId, CommentRequestDto request, String userId, String userName) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found: " + challengeId));

        ChallengeComment comment = ChallengeComment.builder()
                .challenge(challenge)
                .userId(userId)
                .userName(userName)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);
        return mapToDto(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByChallenge(Long challengeId) {
        return commentRepository.findByChallengeIdOrderByCreatedAtDesc(challengeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        ChallengeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private CommentResponseDto mapToDto(ChallengeComment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .challengeId(comment.getChallenge().getId())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
