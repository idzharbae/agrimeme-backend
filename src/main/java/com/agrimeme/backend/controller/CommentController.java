package com.agrimeme.backend.controller;

import com.agrimeme.backend.exceptions.ResourceNotFoundException;
import com.agrimeme.backend.exceptions.BadRequestException;

import com.agrimeme.backend.model.Comment;
import com.agrimeme.backend.model.Post;
import com.agrimeme.backend.model.User;
import com.agrimeme.backend.repository.CommentRepository;
import com.agrimeme.backend.repository.PostRepository;
import com.agrimeme.backend.repository.UserRepository;
import com.agrimeme.backend.security.UserPrincipal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;

@RequestMapping("/api")
@RestController
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @GetMapping("/posts/{postId}/comments")
    public Page<Comment> getAllCommentsByPostId(@PathVariable (value = "postId") Long postId,
                                                Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable);
    }
    
    @GetMapping("/comments")
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    @RolesAllowed("ROLE_USER")
    @PostMapping("/posts/{postId}/comments")
    public Comment createComment(@PathVariable (value = "postId") Long postId,
                                 @Valid @RequestBody Comment comment) {
    	Long userId = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        return postRepository.findById(postId).map(post -> {
        	User user = userRepository.findById(userId).orElseThrow(() 
        			-> new ResourceNotFoundException("Invalid User id: " + userId));
        	post.setCommentCount(post.getCommentCount() + 1);
            comment.setUserId(user.getId());
            comment.setUsername(user.getUsername());
            comment.setPostId(postId);
            return commentRepository.save(comment);
        }).orElseThrow(() -> new ResourceNotFoundException("PostId " + postId + " not found"));
    }
    @RolesAllowed("ROLE_USER")
    @PutMapping("/posts/{postId}/comments/{commentId}")
    public Comment updateComment(@PathVariable (value = "postId") Long postId,
                                 @PathVariable (value = "commentId") Long commentId,
                                 @Valid @RequestBody Comment commentRequest) {
        if(!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("PostId " + postId + " not found");
        }
        Long userId = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        return commentRepository.findById(commentId).map(comment -> {
            if(comment.getUserId() != userId)
                throw new BadRequestException("Unauthorized Request.");
            comment.setText(commentRequest.getText());
            return commentRepository.save(comment);
        }).orElseThrow(() -> new ResourceNotFoundException("CommentId " + commentId + "not found"));
    }
    @RolesAllowed("ROLE_USER")
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable (value = "postId") Long postId,
                              @PathVariable (value = "commentId") Long commentId) {
        Long userId = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        return commentRepository.findByIdAndPostId(commentId, postId).map(comment -> {
            Post post = postRepository.findById(postId).orElseThrow(()
                        -> new ResourceNotFoundException("Invalid Post id: " + postId));
            if(comment.getUserId() != userId)
                throw new BadRequestException("Unauthorized Request.");
            post.setCommentCount(post.getCommentCount() - 1);
            postRepository.save(post);
            commentRepository.delete(comment);
            return ResponseEntity.ok().build();
        }).orElseThrow(() -> new ResourceNotFoundException("Comment not found with id " + commentId + " and postId " + postId));
    }
}
