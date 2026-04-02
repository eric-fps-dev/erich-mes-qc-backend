/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:10/31/2025
 * Time:5:42 PM
 */

package com.fps.svmes.utils;

import com.fps.shared.constants.RedisKeys;
import com.fps.shared.entity.primary.user.User;
import com.fps.shared.utils.RedisCacheUtils;
import com.fps.svmes.dto.dtos.user.CommonUserDTO;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserIdToCommonUserDTOConverter implements Converter<Long, CommonUserDTO> {

    private final RedisCacheUtils redisCacheUtils;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public UserIdToCommonUserDTOConverter(RedisCacheUtils redisCacheUtils,
                                          UserRepository userRepository,
                                          @Lazy ModelMapper modelMapper) {
        this.redisCacheUtils = redisCacheUtils;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public CommonUserDTO convert(MappingContext<Long, CommonUserDTO> context) {

        Long userId = context.getSource();

        if (userId == null) {
            return null;
        }

        if (!CommonUserDTO.class.equals(context.getDestinationType())) {
            return null;
        }

        try {
            CommonUserDTO cached = redisCacheUtils.getEntityFromHash(
                    RedisKeys.USER_PROFILE,
                    userId.toString(),
                    CommonUserDTO.class
            );
            if (cached != null) {
                return cached;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.debug("User not found for ID: {}", userId);
                return null;
            }
            return modelMapper.map(user, CommonUserDTO.class);
        } catch (Exception e) {
            log.error("Error converting user ID to CommonUserDTO", e);
            return null;
        }

    }

    private boolean isUserField(String destinationPath) {
        return destinationPath.contains("createdBy") ||
                destinationPath.contains("updatedBy");
    }
}
