package com.gvw.gvwbackend.mapper;

import com.gvw.gvwbackend.dto.request.UpdateEventRequestDTO;
import com.gvw.gvwbackend.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "rev", ignore = true)
  void updateEventFromDto(UpdateEventRequestDTO dto, @MappingTarget Event event);
}
