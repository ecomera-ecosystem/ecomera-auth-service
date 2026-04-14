package com.youssef.ecomeraauthservice.user;

import com.youssef.ecomeraauthservice.auth.dto.response.MeResponse;
import com.youssef.ecomeraauthservice.auth.dto.response.UserResponse;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    MeResponse toMeResponse(User user);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponses(List<User> users);

    // reusable converters
    @Named("uuidToString")
    default String map(UUID id) {
        return id != null ? id.toString() : null;
    }

    @Named("roleToString")
    default String map(Role role) {
        return role != null ? role.name() : null;
    }
}