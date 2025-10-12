package com.oneshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.User;
@Repository
public interface UserRepository extends JpaRepository<User, Long>{

}
