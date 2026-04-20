package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.recipe.ControlLimitSettingDTO;
import com.fps.svmes.models.nosql.ControlLimitSetting;
import com.fps.svmes.repositories.mongoRepo.ControlLimitSettingRepository;
import com.fps.svmes.services.RecipeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private ControlLimitSettingRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ControlLimitSettingDTO getByQcFormTemplateId(Long templateId) {
        return repository.findByQcFormTemplateId(templateId)
                .map(setting -> {
                    ControlLimitSettingDTO dto = new ControlLimitSettingDTO();
                    dto.setQcFormTemplateId(setting.getQcFormTemplateId());

                    LinkedHashMap<String, ControlLimitSettingDTO.ControlLimitEntry> mappedLimits = new LinkedHashMap<>();
                    setting.getControlLimits().forEach((key, raw) -> {
                        ControlLimitSettingDTO.ControlLimitEntry entry = new ControlLimitSettingDTO.ControlLimitEntry();
                        entry.setLabel(raw.getLabel());
                        entry.setLowerControlLimit(raw.getLowerControlLimit());
                        entry.setUpperControlLimit(raw.getUpperControlLimit());
                        entry.setValidKeys(raw.getValidKeys());
                        entry.setOptionItems(
                                raw.getOptionItems() != null
                                        ? raw.getOptionItems().stream()
                                        .map(item -> modelMapper.map(item, ControlLimitSettingDTO.ControlLimitEntry.OptionItem.class))
                                        .collect(Collectors.toList())
                                        : null
                        );

                        mappedLimits.put(key, entry);
                    });

                    dto.setControlLimits(mappedLimits);
                    return dto;
                })
                .orElse(null);
    }

    @Override
    public void updateControlLimits(ControlLimitSettingDTO dto) {
        ControlLimitSetting setting = repository.findByQcFormTemplateId(dto.getQcFormTemplateId())
                .orElse(new ControlLimitSetting());
        setting.setQcFormTemplateId(dto.getQcFormTemplateId());

        LinkedHashMap<String, ControlLimitSetting.ControlLimitEntry> mappedLimits = new LinkedHashMap<>();

        dto.getControlLimits().forEach((key, sourceEntry) -> {
            ControlLimitSetting.ControlLimitEntry entry = new ControlLimitSetting.ControlLimitEntry();
            entry.setLabel(sourceEntry.getLabel());
            entry.setUpperControlLimit(sourceEntry.getUpperControlLimit());
            entry.setLowerControlLimit(sourceEntry.getLowerControlLimit());
            entry.setValidKeys(sourceEntry.getValidKeys());
            entry.setOptionItems(sourceEntry.getOptionItems() != null
                    ? sourceEntry.getOptionItems().stream()
                    .map(item -> modelMapper.map(item, ControlLimitSetting.ControlLimitEntry.OptionItem.class))
                    .collect(Collectors.toList())
                    : null
            );

            mappedLimits.put(key, entry);
        });

        setting.setControlLimits(mappedLimits);

        repository.save(setting);
    }

}