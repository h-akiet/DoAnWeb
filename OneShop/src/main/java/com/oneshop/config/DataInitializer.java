package com.oneshop.config; 
import com.oneshop.entity.Role;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;
	private static final String DEFAULT_ADMIN_USERNAME = "admin";
	private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
	private static final String DEFAULT_ADMIN_EMAIL = "admin@oneshop.com";

//	private static final String DEFAULT_SHIPPER_USERNAME = "shipper";
//	private static final String DEFAULT_SHIPPER_PASSWORD = "shipper123";
//	private static final String DEFAULT_SHIPPER_EMAIL = "shipper@oneshop.com";

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		logger.info("Initializing roles...");
		// 1. KHỞI TẠO ROLES
		for (RoleName roleName : RoleName.values()) {
			if (roleRepository.findByName(roleName).isEmpty()) {
				Role newRole = new Role(roleName);
				roleRepository.save(newRole);
				logger.info("Created role: {}", roleName);
			} else {
				logger.debug("Role {} already exists.", roleName);
			}
		}

		logger.info("Role initialization complete.");

		initializeAdminUser();
		//initializeShipperUser(); 
	}

	private void initializeAdminUser() {
		if (userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).isEmpty()) {
			logger.info("Creating default Admin user...");
			Role adminRole = roleRepository.findByName(RoleName.ADMIN)
					.orElseThrow(() -> new IllegalStateException("Role ADMIN not found. Initialization error."));

			User admin = new User();
			admin.setUsername(DEFAULT_ADMIN_USERNAME);
			admin.setEmail(DEFAULT_ADMIN_EMAIL);
			admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
			admin.setRole(adminRole);
			admin.setActivated(true);
			userRepository.save(admin);

			logger.info("Default Admin user created successfully:");
			logger.info("  Username: {}", DEFAULT_ADMIN_USERNAME);
			logger.info("  Password: {}", DEFAULT_ADMIN_PASSWORD);
			logger.info("  Email: {}", DEFAULT_ADMIN_EMAIL);
		} else {
			User admin = userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).get();
			if (!admin.isActivated()) {
				admin.setActivated(true);
				userRepository.save(admin);
				logger.info("Admin user '{}' found and activated.", DEFAULT_ADMIN_USERNAME);
			} else {
				logger.info("Admin user '{}' already exists.", DEFAULT_ADMIN_USERNAME);
			}
		}
	}

//	// ====SHIPPER====
//	private void initializeShipperUser() {
//		if (userRepository.findByUsername(DEFAULT_SHIPPER_USERNAME).isEmpty()) {
//			logger.info("Creating default Shipper user...");
//
//			// Lấy Role SHIPPER
//			Role shipperRole = roleRepository.findByName(RoleName.SHIPPER)
//					.orElseThrow(() -> new IllegalStateException("Role SHIPPER not found. Initialization error."));
//
//			// Tạo User Shipper
//			User shipper = new User();
//			shipper.setUsername(DEFAULT_SHIPPER_USERNAME);
//			shipper.setEmail(DEFAULT_SHIPPER_EMAIL);
//			shipper.setPassword(passwordEncoder.encode(DEFAULT_SHIPPER_PASSWORD));
//			shipper.setRole(shipperRole);
//			shipper.setActivated(true);
//
//			userRepository.save(shipper);
//
//			logger.info("Default Shipper user created successfully:");
//			logger.info("  Username: {}", DEFAULT_SHIPPER_USERNAME);
//			logger.info("  Password: {}", DEFAULT_SHIPPER_PASSWORD);
//		} else {
//			logger.info("Shipper user '{}' already exists.", DEFAULT_SHIPPER_USERNAME);
//		}
//	}
}