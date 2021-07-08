package com.gi.insta.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.gi.insta.model.Image;
import com.gi.insta.model.User;
import com.gi.insta.repository.FollowRepository;
import com.gi.insta.repository.LikesRepository;
import com.gi.insta.repository.UserRepository;
import com.gi.insta.service.MyUserDetail;

@Controller
public class UserController {
	
	
	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private BCryptPasswordEncoder encoder;
	
	@Autowired
	private UserRepository mUserRepository;
	
	@Autowired
	private FollowRepository mFollowRepository;
	
	@Autowired
	private LikesRepository mLikesRepository;
	
	@GetMapping("/auth/login")
	public String authLogin() {
		return "auth/login";
		
	}
	
	@GetMapping("/auth/join")
	public String authJoin() {
		return "auth/join";
		
	}
	
	@PostMapping("/auth/joinProc")
	public String authJoinProc(User user) {
		
		String rawPassword = user.getPassword();
		String encPassword = encoder.encode(user.getPassword());
		
		user.setPassword(encPassword);
		
		log.info("rawPassword : " + rawPassword);
		log.info("encPassword : " + encPassword);
		
		mUserRepository.save(user);
		
		return "redirect:/auth/login";
		
	}
	
	@GetMapping("/user/{id}")
	public String profile(
			@PathVariable int id,
			@AuthenticationPrincipal MyUserDetail userDetail,
			Model model) {
		
		// id를 통해서 해당 유저를 검색 ( 이미지 + 유저정보 )
		
		/*
		 *  1. imageCount
		 *  2. followerCount
		 *  3. followingCount
		 *	 4. User 오브젝트 (Image (likeCount) 컬렉션)  
		 *	 5. followCheck 팔로우 유무 ( 1 팔로우중, 1이 아니면 언팔로우)
		 * */
		
		// 4번 임시(수정해야됨)
		Optional<User> oToUser = mUserRepository.findById(id);
		User user = oToUser.get();
		model.addAttribute("user", user);
		
		// 1번 imageCount
		int imageCount = user.getImages().size();
		model.addAttribute("imageCount", imageCount);
		
		// 2번 followCount
		// (selelct count(*) from follow where formUserId = 1)
		int followCount = 
				mFollowRepository.countByFromUserId(user.getId());
		model.addAttribute("followCount", followCount);
		
		//3번 followerCount
		// (select count(*) from follower where toUserId = 1)
		int followerCount =
				mFollowRepository.countByToUserId(user.getId());
		model.addAttribute("followerCount", followerCount);
		
		// 4번 likeCount
		for(Image item : user.getImages()) {
			int likeCount =
					mLikesRepository.countByImageId(item.getId());
			item.setLikeCount(likeCount);
		}
		
		model.addAttribute("user", user);
		
		// 5번
		User principal = userDetail.getUser();
		
		int followCheck = mFollowRepository.countByFromUserIdAndToUserId(principal.getId(), id);
		log.info("followCheck : " + followCheck);
		model.addAttribute("followCheck", followCheck);
		
		return "user/profile";
		
	}
	
	@GetMapping("/user/edit/{id}")
	public String userEdit(@PathVariable int id) {
		
		// 해당 ID로 Select 하기
		// findByUserInfo() 사용 ( 만들어야댐)
		
		return "user/profile_edit";
	}
	
}
