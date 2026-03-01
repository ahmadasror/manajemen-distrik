package com.template.usermanagement.wilayah;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.common.PageResponse;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.wilayah.dto.*;
import com.template.usermanagement.wilayah.dto.ValidationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wilayah")
@RequiredArgsConstructor
public class WilayahController {

    private final WilayahService wilayahService;
    private final WilayahValidationService wilayahValidationService;

    // ─── Province ─────────────────────────────────────────────────────────────

    @GetMapping("/provinces")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ProvinceResponse>>> getProvinces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Page<ProvinceResponse> result = wilayahService.searchProvinces(search, PageRequest.of(page, Math.min(size, 100), Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/provinces/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProvinceResponse>> getProvince(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(wilayahService.getProvince(id)));
    }

    @PostMapping("/provinces")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<ProvinceResponse>> createProvince(
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ProvinceResponse resp = wilayahService.createProvince(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Province created", resp));
    }

    @PutMapping("/provinces/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<ProvinceResponse>> updateProvince(
            @PathVariable String id,
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Province updated", wilayahService.updateProvince(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/provinces/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<Void>> deleteProvince(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        wilayahService.deleteProvince(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Province deleted", null));
    }

    // ─── State ────────────────────────────────────────────────────────────────

    @GetMapping("/states")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<StateResponse>>> getStates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) String search) {
        Page<StateResponse> result = wilayahService.searchStates(provinceId, search, PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/states/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StateResponse>> getState(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(wilayahService.getState(id)));
    }

    @PostMapping("/states")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<StateResponse>> createState(
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("State created", wilayahService.createState(request, userDetails.getUsername())));
    }

    @PutMapping("/states/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<StateResponse>> updateState(
            @PathVariable String id,
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success("State updated", wilayahService.updateState(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/states/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<Void>> deleteState(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        wilayahService.deleteState(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("State deleted", null));
    }

    // ─── District ─────────────────────────────────────────────────────────────

    @GetMapping("/districts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<DistrictResponse>>> getDistricts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String stateId,
            @RequestParam(required = false) String search) {
        Page<DistrictResponse> result = wilayahService.searchDistricts(stateId, search, PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/districts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DistrictResponse>> getDistrict(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(wilayahService.getDistrict(id)));
    }

    @PostMapping("/districts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<DistrictResponse>> createDistrict(
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("District created", wilayahService.createDistrict(request, userDetails.getUsername())));
    }

    @PutMapping("/districts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<DistrictResponse>> updateDistrict(
            @PathVariable String id,
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success("District updated", wilayahService.updateDistrict(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/districts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<Void>> deleteDistrict(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        wilayahService.deleteDistrict(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("District deleted", null));
    }

    // ─── SubDistrict ──────────────────────────────────────────────────────────

    @GetMapping("/subdistricts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<SubDistrictResponse>>> getSubDistricts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String search) {
        Page<SubDistrictResponse> result = wilayahService.searchSubDistricts(districtId, zipCode, search, PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/subdistricts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubDistrictResponse>> getSubDistrict(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(wilayahService.getSubDistrict(id)));
    }

    @PostMapping("/subdistricts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<SubDistrictResponse>> createSubDistrict(
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SubDistrict created", wilayahService.createSubDistrict(request, userDetails.getUsername())));
    }

    @PutMapping("/subdistricts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<SubDistrictResponse>> updateSubDistrict(
            @PathVariable String id,
            @Valid @RequestBody WilayahRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success("SubDistrict updated", wilayahService.updateSubDistrict(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/subdistricts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<Void>> deleteSubDistrict(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        wilayahService.deleteSubDistrict(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("SubDistrict deleted", null));
    }

    // ─── Validate (Nominatim / OSM) ───────────────────────────────────────────

    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ValidationResult>> validate(
            @RequestParam String name,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String provinceName,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String districtName) {
        ValidationResult result = wilayahValidationService.validate(name, zipCode, provinceName, stateName, districtName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─── Inquiry ──────────────────────────────────────────────────────────────

    @GetMapping("/inquiry")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<SubDistrictResponse>>> inquiry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) String stateId,
            @RequestParam(required = false) String districtId) {
        int size = 50;
        Page<SubDistrictResponse> result = wilayahService.inquiry(
                q, zipCode, provinceId, stateId, districtId,
                PageRequest.of(page, size, Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }
}
