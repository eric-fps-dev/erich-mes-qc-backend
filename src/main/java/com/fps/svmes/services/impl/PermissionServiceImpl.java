/**
 * Author: Eric Huang
 * User:eric.huang
 * Date:6/24/2025
 * Time:2:56 PM
 */

package com.fps.svmes.services.impl;

import com.fps.shared.entity.primary.rbac.Permission;
import com.fps.shared.entity.primary.rbac.Role;
import com.fps.shared.exceptions.ResourceNotFoundException;
import com.fps.shared.service.PermissionService;
import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(Long userId) {
        try {
            List<Role> roles = userRoleRepository.findRolesByUserId(userId);
            if (roles.isEmpty()) {
                return Collections.emptySet();
            }
            // Collect the valid permissions of all roles (taking into account the inheritance of parent roles)
            Set<String> permissionCodes = new HashSet<>();
            for (Role role : roles) {
                try {
                    List<Permission> effectivePermissions = getEffectivePermissionsByRoleId(role.getId());
                    effectivePermissions.forEach(p -> permissionCodes.add(p.getCode()));
                } catch (ResourceNotFoundException e) {
                    log.warn("Role not found: {}, skipping permissions", role.getId(), e);
                }
            }
            return permissionCodes;
        } catch (Exception e) {
            log.error("Error fetching permissions for user: {}", userId, e);
            return Collections.emptySet();
        }
    }

    @Transactional(readOnly = true)
    public List<Permission> getEffectivePermissionsByRoleId(Integer id) throws ResourceNotFoundException {

        Role role = roleRepository.findWithChildrenById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));

        // Collect the current role and all its subordinate roles (recursively)
        Set<Permission> collectedPermissions = new LinkedHashSet<>();
        Set<Integer> visited = new HashSet<>();

        collectPermissionsDownward(role, collectedPermissions, visited);

        List<Permission> permissions = collectedPermissions.stream()
                .distinct()
                .toList();

        return permissions;
    }

    private void collectPermissionsDownward(Role role, Set<Permission> collected, Set<Integer> visited) {
        if (role == null || visited.contains(role.getId())) return;
        visited.add(role.getId());

        if (role.getRolePermissions() != null) {
            role.getRolePermissions().forEach(rp -> {
                if (rp.getPermission() != null) {
                    collected.add(rp.getPermission());
                }
            });
        }

        if (role.getChildren() != null) {
            for (Role child : role.getChildren()) {
                collectPermissionsDownward(child, collected, visited);
            }
        }
    }
}
