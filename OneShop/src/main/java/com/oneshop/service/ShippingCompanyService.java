package com.oneshop.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.entity.ShippingCompany;
import com.oneshop.repository.ShippingCompanyRepository;

@Service
@Transactional // Đảm bảo các thao tác database được thực hiện trong một transaction
public class ShippingCompanyService {

	@Autowired
	private ShippingCompanyRepository shippingCompanyRepository;

	public List<ShippingCompany> findAll() {
		return shippingCompanyRepository.findAll();
	}

	public ShippingCompany findById(Long id) {
		Optional<ShippingCompany> result = shippingCompanyRepository.findById(id);
		return result.orElse(null);
	}

	public ShippingCompany save(ShippingCompany company) {
		return shippingCompanyRepository.save(company);
	}

}