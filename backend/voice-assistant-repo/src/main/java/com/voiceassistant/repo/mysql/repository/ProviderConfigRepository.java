package com.voiceassistant.repo.mysql.repository;

import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, Long> {

    Optional<ProviderConfig> findFirstByActiveTrue();

    boolean existsByName(String name);
}
