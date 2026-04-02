//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.user.TeamForUserTableDTO;
//import com.fps.svmes.dto.dtos.user.UserDTO;
//import com.fps.svmes.models.sql.user.Role;
//import com.fps.svmes.models.sql.user.Team;
//import com.fps.shared.entity.user.User;
//
//import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//import com.fps.svmes.services.UserService;
//import lombok.RequiredArgsConstructor;
//import org.modelmapper.ModelMapper;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class UserServiceImpl implements UserService {
//
//    private final UserRepository userRepository;
//
//    private final TeamUserRepository teamUserRepository;
//
//    private final RoleRepository roleRepository;
//
//    private final ModelMapper modelMapper;
//
//    private final TeamRepository teamRepository;
//
//    // Get all users
//    // TODO: add the teams
//    @Override
//    @Transactional(readOnly = true)
//    public List<UserDTO> getAllUsers() {
//        return userRepository.findAll()
//                .stream()
//                .filter(user -> user.getStatus().equals(1))
//                .map(user -> {
//                    // Map the user entity to UserDTO
//                    UserDTO userDTO = modelMapper.map(user, UserDTO.class);
//
//                    // Fetch teams directly using the repository method that returns TeamForUserTableDTO
//                    List<TeamForUserTableDTO> teams = teamUserRepository.findTeamsByUserId(Math.toIntExact(user.getId()));
//
//                    // Check if user is leader of any team, and return list of these ids.
//                    Optional<Team> optionalLeaderTeam = teamRepository.findByLeaderId(Math.toIntExact(user.getId()));
//                    if (optionalLeaderTeam.isPresent() && optionalLeaderTeam.get().getStatus().equals(1)){
//                        List<Integer> t = new ArrayList<Integer>();
//                        t.add(optionalLeaderTeam.get().getId());
//                        userDTO.setLeadershipTeams(t);
//                    } else {
//                        userDTO.setLeadershipTeams(null);
//                    }
//
//                    // Assign the mapped teams to the UserDTO
//                    userDTO.setTeams(teams);
//
//                    return userDTO;
//                })
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public UserDTO getUserById(Integer id) {
//        Optional<User> userOptional = userRepository.findById(id);
//
//        if (userOptional.isPresent() && userOptional.get().getStatus().equals(1)) {
//            User user = userOptional.get();
//            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
//
//            // Fetch teams directly using the repository method that returns TeamForUserTableDTO
//            List<TeamForUserTableDTO> teams = teamUserRepository.findTeamsByUserId(Math.toIntExact(user.getId()));
//
//            // Check if user is leader of any team, and return list of these ids.
//            Optional<Team> optionalLeaderTeam = teamRepository.findByLeaderId(user.getId());
//
//            if (optionalLeaderTeam.isPresent() && optionalLeaderTeam.get().getStatus().equals(1)){
//                List<Integer> t = new ArrayList<Integer>();
//                t.add(optionalLeaderTeam.get().getId());
//                userDTO.setLeadershipTeams(t);
//            } else {
//                userDTO.setLeadershipTeams(null);
//            }
//
//            // Assign the mapped teams to the UserDTO
//            userDTO.setTeams(teams);
//
//            return userDTO;
//        } else {
//            throw new RuntimeException("User with id " + id  + " not found");
//        }
//    }
//
//    public List<UserDTO> getUsersByIds(List<Integer> userIds) {
//        return getAllUsers().stream()
//                .filter(user -> userIds.contains(user.getId()))
//                .toList();
//    }
//
//    // Create a new user
//    @Override
//    @Transactional
//    public UserDTO createUser(UserDTO userDTO) {
//        User user = modelMapper.map(userDTO, User.class);
//        User savedUser = userRepository.save(user);
//        return modelMapper.map(savedUser, UserDTO.class);
//    }
//
//    // Update an existing user
//    // TODO: add the teams
//    @Override
//    @Transactional
//    public UserDTO updateUser(Integer id, UserDTO userDTO) {
//        Optional<User> optionalUser = userRepository.findById(id);
//
//        if (optionalUser.isPresent()) {
//            User existingUser = optionalUser.get();
//
//            // Update fields only if they are not null
//            if (userDTO.getName() != null) {
//                existingUser.set(userDTO.getName());
//            }
//            if (userDTO.getRole() != null && userDTO.getRole().getId() != null) {
//                // Fetch the Role object from the RoleRepository using the ID
//                Optional<Role> roleOptional = roleRepository.findById(userDTO.getRole().getId());
//
//                if (roleOptional.isPresent()) {
//                    existingUser.setRole(roleOptional.get());  // Set the fetched Role object
//                } else {
//                    throw new RuntimeException("Role with ID " + userDTO.getRole().getId() + " not found");
//                }
//            }
//
//            if (userDTO.getWecomId() != null) {
//                existingUser.setWecomId(userDTO.getWecomId());
//            }
//            if (userDTO.getUsername() != null) {
//                existingUser.setUsername(userDTO.getUsername());
//            }
//            if (userDTO.getPassword() != null) {
//                existingUser.setPassword(userDTO.getPassword());
//            }
//            if (userDTO.getEmail() != null) {
//                existingUser.setEmail(userDTO.getEmail());
//            }
//            if (userDTO.getPhoneNumber() != null) {
//                existingUser.setPhoneNumber(userDTO.getPhoneNumber());
//            }
//
//            if (userDTO.getStatus() != null) {
//                existingUser.setStatus(userDTO.getStatus());
//            }
//
//            if (userDTO.getActivationStatus() != null) {
//                existingUser.setActivationStatus(userDTO.getActivationStatus());
//            }
//
//            if (userDTO.getCreatedBy() != null) {
//                existingUser.setCreatedBy(userDTO.getCreatedBy());
//            }
//
//            if (userDTO.getUpdatedBy() != null) {
//                existingUser.setUpdatedBy(userDTO.getUpdatedBy());
//            }
//
//            if (userDTO.getCreatedAt() != null) {
//                existingUser.setCreatedAt(userDTO.getCreatedAt());
//            }
//
//            if (userDTO.getUpdatedAt() != null) {
//                existingUser.setUpdatedAt(userDTO.getUpdatedAt());
//            }
//
//            User updatedUser = userRepository.save(existingUser);
//            return modelMapper.map(updatedUser, UserDTO.class);
//        } else {
//            throw new RuntimeException("User with ID " + id + " not found");
//        }
//    }
//
//    // Hard delete a user by ID
//    @Override
//    @Transactional
//    public void hardDeleteUser(Integer id) {
//        if (userRepository.existsById(id)) {
//            userRepository.deleteById(id);
//        } else {
//            throw new RuntimeException("User with ID " + id + " not found");
//        }
//    }
//
//    // Soft delete a user by ID
//    @Override
//    @Transactional
//    public void softDeleteUser(Integer id) {
//        Optional<User> userOptional = userRepository.findById(id);
//
//        if (userOptional.isPresent()){
//            userOptional.get().setStatus(0);
//        } else {
//            throw new RuntimeException("User with ID " + id + " not found");
//        }
//
//        // Clean up team user association for this user
//        teamUserRepository.deleteByIdUserId(id);
//
//        // Clean up any team where leader is this user
//        Optional<Team> optionalTeam = teamRepository.findByLeaderId(id);
//
//        if (optionalTeam.isPresent()){
//            Team team = optionalTeam.get();
//            team.setLeader(null);
//            teamRepository.save(team);
//        }
//    }
//
//    @Override
//    @Transactional
//    public boolean validateCredentials(String username, String password) {
//        Optional<User> userOptional = userRepository.findByUsername(username);
//
//        // Check if the Optional contains a User and compare the passwords
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//            return password.equals(user.getPassword());
//        }
//
//        return false;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public UserDTO getUserByUsername(String username) {
//        Optional<User> userOptional = userRepository.findByUsername(username);
//
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//            return modelMapper.map(user, UserDTO.class);
//        } else {
//            throw new RuntimeException("User with username " + username + " not found");
//        }
//    }
//}
