package com.gvw.gvwbackend.mapper;

import com.gvw.gvwbackend.dto.request.UpdateUserAdminRequestDTO;
import com.gvw.gvwbackend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    imports = {com.gvw.gvwbackend.model.Role.class})
public interface UserMapper {
  @Mapping(
      target = "role",
      expression = "java(com.gvw.gvwbackend.model.Role.fromString(dto.role()))")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "rev", ignore = true)
  void updateUserFromDto(UpdateUserAdminRequestDTO dto, @MappingTarget User user);
}
