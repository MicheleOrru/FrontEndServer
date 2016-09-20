package com.ogb.fes.domainRepositories;


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.ogb.fes.domain.User;


@Transactional
public interface UserRepository extends CrudRepository<User, String>
{
	public User findByToken(String token);
	public User findByUserID(String userID);
}
