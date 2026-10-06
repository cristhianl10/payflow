package com.payflow.beneficiary.presentation;

import java.util.List;
import java.util.UUID;

import com.payflow.beneficiary.application.BeneficiaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaries;

    public BeneficiaryController(BeneficiaryService beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    @GetMapping
    List<BeneficiaryService.BeneficiaryView> list(@AuthenticationPrincipal Jwt jwt) {
        return beneficiaries.list(user(jwt));
    }

    @PostMapping
    ResponseEntity<BeneficiaryService.BeneficiaryView> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBeneficiaryRequest request) {
        return ResponseEntity.status(201).body(
                beneficiaries.create(user(jwt), request.email, request.alias));
    }

    @PutMapping("/{publicId}")
    BeneficiaryService.BeneficiaryView update(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Size(max = 64) String publicId,
            @Valid @RequestBody UpdateBeneficiaryRequest request) {
        return beneficiaries.update(user(jwt), publicId, request.alias);
    }

    @DeleteMapping("/{publicId}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Size(max = 64) String publicId) {
        beneficiaries.delete(user(jwt), publicId);
        return ResponseEntity.noContent().build();
    }

    private UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public record CreateBeneficiaryRequest(@NotBlank @Email @Size(max = 254) String email,
            @Size(max = 100) String alias) {}
    public record UpdateBeneficiaryRequest(@Size(max = 100) String alias) {}
}
