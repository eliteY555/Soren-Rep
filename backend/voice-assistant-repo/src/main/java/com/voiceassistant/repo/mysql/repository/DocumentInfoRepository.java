package com.voiceassistant.repo.mysql.repository;

import com.voiceassistant.repo.mysql.entity.DocumentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentInfoRepository extends JpaRepository<DocumentInfo, Long> {

    List<DocumentInfo> findAllByOrderByUploadedAtDesc();
}
