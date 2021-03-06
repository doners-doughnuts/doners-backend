package com.doners.donersbackend.application.service;

import com.doners.donersbackend.application.dto.request.comment.CommentChangePatchDTO;
import com.doners.donersbackend.application.dto.request.comment.CommentRegisterPostDTO;
import com.doners.donersbackend.application.dto.response.comment.CommentGetListWrapperResponseDTO;
import com.doners.donersbackend.application.dto.response.comment.CommentResponseDTO;
import com.doners.donersbackend.domain.dao.epilogue.Epilogue;
import com.doners.donersbackend.domain.dao.comment.Comment;
import com.doners.donersbackend.domain.dao.community.Community;
import com.doners.donersbackend.domain.dao.user.User;
import com.doners.donersbackend.domain.repository.epilogue.EpilogueRepository;
import com.doners.donersbackend.domain.repository.CommentRepository;
import com.doners.donersbackend.domain.repository.CommunityRepository;
import com.doners.donersbackend.domain.repository.UserRepository;
import com.doners.donersbackend.security.util.JwtAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{

    private final CommentRepository commentRepository;

    private final CommunityRepository communityRepository;

    private final EpilogueRepository epilogueRepository;

    private final UserRepository userRepository;

    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    @Override
    public void registerComment(String accessToken, CommentRegisterPostDTO commentRegisterPostDTO) {
        User user = getUserFromAccessToken(accessToken);

        Comment parentComment = null;
        if(commentRepository.findByIdAndCommentIsDeleted(commentRegisterPostDTO.getCommentId(), false).isPresent()){
            parentComment = commentRepository.findById(commentRegisterPostDTO.getCommentId()).get();
        }
        // 댓글 작성 정보
        Comment comment = Comment.builder()
                .commentDescription(commentRegisterPostDTO.getCommentDescription())
                .user(user)
                .parentCommentId(parentComment)
                .commentCreateTime(LocalDateTime.now()).build();

        if(commentRegisterPostDTO.getCommunityId()==null){// 감사 글 댓글
            comment.changeEpilogueId(epilogueRepository.findById(commentRegisterPostDTO.getEpilogueId()).get());
        }else{// 커뮤니티 글 댓글
            comment.changeCommunityId(communityRepository.findById(commentRegisterPostDTO.getCommunityId()).get());
        }
        commentRepository.save(comment);
    }

    @Override
    public Integer changeComment(String accessToken, CommentChangePatchDTO commentChangePatchDTO) {
        User user = getUserFromAccessToken(accessToken);

        Comment comment = commentRepository.findByIdAndCommentIsDeleted(commentChangePatchDTO.getCommentId(), false)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다."));

        if(!user.getId().equals(comment.getUser().getId())) {
            return 401;
        }

        try {
            comment.changeComment(commentChangePatchDTO.getCommentDescription());
        } catch(Exception e) {
            return 409;
        }

        commentRepository.save(comment);
        return 200;
    }

    @Override
    public Integer deleteComment(String accessToken, String commentId) {
        User user = getUserFromAccessToken(accessToken);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다."));

        if(!user.getId().equals(comment.getUser().getId())) {
            return 401;
        }

        try {
            comment.deleteComment();
        } catch(Exception e) {
            return 409;
        }

        commentRepository.save(comment);
        return 200;
    }

    @Override
    public CommentGetListWrapperResponseDTO getEpilogueCommentList(String accessToken, String id) {
        User user = getUserFromAccessToken(accessToken);

        List<CommentResponseDTO> list = new ArrayList<>();

        Epilogue epilogue = epilogueRepository.findById(id).get();

        for(Comment c : commentRepository.findAllByEpilogueAndCommentIsDeletedOrderByCommentCreateTime(epilogue, false).get()) {
            list.add(new CommentResponseDTO(c.getId(), c.getCommentCreateTime(), c.getCommentDescription(), c.getUser().getUserNickname()));
        }

        return new CommentGetListWrapperResponseDTO(list);
    }

    @Override
    public CommentGetListWrapperResponseDTO getCommunityCommentList(String accessToken, String id) {
        User user = getUserFromAccessToken(accessToken);

        List<CommentResponseDTO> list = new ArrayList<>();

        Community community = communityRepository.findById(id).get();

        for(Comment c : commentRepository.findAllByCommunityAndCommentIsDeletedOrderByCommentCreateTime(community, false).get()) {
            list.add(new CommentResponseDTO(c.getId(), c.getCommentCreateTime(), c.getCommentDescription(), c.getUser().getUserNickname()));
        }

        return new CommentGetListWrapperResponseDTO(list);
    }

    @Override
    public CommentGetListWrapperResponseDTO getSubCommentList(String accessToken, String parentId) {
        User user = getUserFromAccessToken(accessToken);

        List<CommentResponseDTO> list = new ArrayList<>();

        Comment comment = commentRepository.findById(parentId).get();
        for(Comment c : commentRepository.findAllByParentCommentIdAndCommentIsDeletedOrderByCommentCreateTime(comment, false).get()) {
            list.add(new CommentResponseDTO(c.getId(), c.getCommentCreateTime(), c.getCommentDescription(), c.getUser().getUserNickname()));
        }

        return new CommentGetListWrapperResponseDTO(list);
    }

    public User getUserFromAccessToken(String accessToken) {
        String token = accessToken.split(" ")[1];
        String userAccount = jwtAuthenticationProvider.getUserAccount(token);

        return userRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 찾을 수 없습니다."));
    }
}
