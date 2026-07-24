package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMemberStatusRequestDTO;
import com.gvw.gvwbackend.dto.response.MembersResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController {
  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping("/all")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  public MembersResponseDTO getAllMembers() {
    return memberService.getMembers();
  }

  @GetMapping("/check/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ResponseStatus(HttpStatus.OK)
  public void checkMember(@PathVariable String id) {
    memberService.checkMember(id);
  }

  @PostMapping("/add")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.MEMBER,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addMember(@Valid @RequestBody AddMemberRequestDTO requestDTO) {
    memberService.addMember(requestDTO);
  }

  @DeleteMapping("/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  public void deleteMember(@PathVariable String id) {
    memberService.deleteMember(id);
  }

  @PatchMapping("/update")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.MEMBER,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateMember(@Valid @RequestBody UpdateMemberRequestDTO requestDTO) {
    List<String> revisions = memberService.updateMember(requestDTO);
    return Map.of("rev_member", revisions.get(0), "rev_user", revisions.get(1));
  }

  @PatchMapping("/update/status/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.MEMBER,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateMemberStatus(
      @PathVariable String id, @Valid @RequestBody UpdateMemberStatusRequestDTO request) {
    String rev = memberService.updateMemberStatus(id, request.rev());
    return Map.of("rev", rev);
  }
}
