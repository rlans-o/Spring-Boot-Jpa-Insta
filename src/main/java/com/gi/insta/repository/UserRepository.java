package com.gi.insta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gi.insta.model.User;

public interface UserRepository extends JpaRepository<User, Integer>{
	

	User findByUsername(String username);
}
