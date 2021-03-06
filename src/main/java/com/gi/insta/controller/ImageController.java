package com.gi.insta.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.gi.insta.model.Image;
import com.gi.insta.model.Likes;
import com.gi.insta.model.Tag;
import com.gi.insta.model.User;
import com.gi.insta.repository.ImageRepository;
import com.gi.insta.repository.LikesRepository;
import com.gi.insta.repository.TagRepository;
import com.gi.insta.service.MyUserDetail;
import com.gi.insta.util.Utils;

@Controller
public class ImageController {

	private static final Logger log = LoggerFactory.getLogger(ImageController.class);

	@Value("${file.path}")
	private String fileRealPath;

	@Autowired
	private ImageRepository mImageRepository;

	@Autowired
	private TagRepository mTagRepository;

	@Autowired
	private LikesRepository mLikesRepository;

	@PostMapping("/image/like/{id}")
	public @ResponseBody String imageLike(
			@PathVariable int id, 
			@AuthenticationPrincipal MyUserDetail userDetail
	) {

		Likes oldLike = mLikesRepository.findByUserIdAndImageId(
				userDetail.getUser().getId(), 
				id);
		
		Optional<Image> oImage = mImageRepository.findById(id);
		Image image = oImage.get();
		
		try {
			
			if(oldLike == null) { // ????????? ?????? (??????)
				Likes newLike = Likes.builder()
						.image(image)
						.user(userDetail.getUser())
						.build();
				
				mLikesRepository.save(newLike);
			} else { // ????????? ??? (??????)
				mLikesRepository.delete(oldLike);
			}
			
			return "ok";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "fail";
	}

	// http://localhost:8080/image/feed/scroll?page=1..2..3..4
	@GetMapping("/image/feed/scroll")
	public @ResponseBody List<Image> imageFeedScroll(@AuthenticationPrincipal MyUserDetail userDetail,
			@PageableDefault(size = 3, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {

		Page<Image> pageImages = mImageRepository.findImage(userDetail.getUser().getId(), pageable);
		List<Image> images = pageImages.getContent();

		for (Image image : images) {
			Likes like = mLikesRepository.findByUserIdAndImageId(userDetail.getUser().getId(), image.getId());
			if (like != null) {
				image.setHeart(true);
			}
		}

		return images;
	}

	@GetMapping({ "/", "/image/feed" })
	public String imageFeed(@AuthenticationPrincipal MyUserDetail userDetail,
			@PageableDefault(size = 3, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
		log.info("username : " + userDetail.getUsername());

		// ?????? ???????????? ???????????? ??????
		Page<Image> pageImages = mImageRepository.findImage(userDetail.getUser().getId(), pageable);

		List<Image> images = pageImages.getContent();

		for (Image image : images) {
			Likes like = mLikesRepository.findByUserIdAndImageId(userDetail.getUser().getId(), image.getId());
			if (like != null) {
				image.setHeart(true);
			}
		}

		model.addAttribute("images", images);

		return "image/feed";
	}

	@GetMapping("/image/upload")
	public String imageUpload() {
		return "image/image_upload";
	}

	@PostMapping("/image/uploadProc")
	public String imageUploadProc(@AuthenticationPrincipal MyUserDetail userDetail,
			@RequestParam("file") MultipartFile file, @RequestParam("caption") String caption,
			@RequestParam("location") String location, @RequestParam("tags") String tags) {

		// ????????? ????????? ??????
		UUID uuid = UUID.randomUUID();
		String uuidFilename = uuid + "_" + file.getOriginalFilename();

		Path filePath = Paths.get(fileRealPath + uuidFilename);

		try {
			Files.write(filePath, file.getBytes()); // ?????? ????????? ??????
		} catch (IOException e) {
			e.printStackTrace();
		}

		User principal = userDetail.getUser();

		Image image = new Image();
		image.setCaption(caption);
		image.setLocation(location);
		image.setUser(principal);
		image.setPostImage(uuidFilename);

		// <img src="/upload/?????????" />

		mImageRepository.save(image);

		// Tag ?????? ??????
		List<String> tagList = Utils.tagParser(tags);

		for (String tag : tagList) {
			Tag t = new Tag();
			t.setName(tag);
			t.setImage(image);
			mTagRepository.save(t);
			image.getTags().add(t);
		}

		return "redirect:/";
	}

}
