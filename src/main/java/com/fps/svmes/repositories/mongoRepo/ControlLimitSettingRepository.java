package com.fps.svmes.repositories.mongoRepo;

import com.fps.svmes.models.nosql.ControlLimitSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ControlLimitSettingRepository extends MongoRepository<ControlLimitSetting, String> {
    Optional<ControlLimitSetting> findByQcFormTemplateId(Long qcFormTemplateId);
}