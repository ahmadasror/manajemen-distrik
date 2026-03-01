package com.template.usermanagement.wilayah;

import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.wilayah.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WilayahService {

    private final ProvinceRepository provinceRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final SubDistrictRepository subDistrictRepository;
    private final AuditTrailService auditTrailService;

    // ─── Province ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProvinceResponse> searchProvinces(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return provinceRepository.findByNameContainingIgnoreCase(search, pageable).map(ProvinceResponse::from);
        }
        return provinceRepository.findAll(pageable).map(ProvinceResponse::from);
    }

    @Transactional(readOnly = true)
    public ProvinceResponse getProvince(String id) {
        return ProvinceResponse.from(findProvince(id));
    }

    @Transactional
    public ProvinceResponse createProvince(WilayahRequest request, String performedBy) {
        if (provinceRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Provinsi dengan nama '" + request.getName() + "' sudah ada", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        Province province = Province.builder()
                .provinceId(request.getId())
                .name(request.getName())
                .build();
        province = provinceRepository.save(province);
        auditTrailService.recordAudit("PROVINCE", null, "CREATE_PROVINCE",
                null, Map.of("provinceId", province.getProvinceId(), "name", province.getName()),
                performedBy, null);
        return ProvinceResponse.from(province);
    }

    @Transactional
    public ProvinceResponse updateProvince(String id, WilayahRequest request, String performedBy) {
        Province province = findProvince(id);
        if (provinceRepository.existsByNameIgnoreCaseAndProvinceIdNot(request.getName(), id)) {
            throw new BusinessException("Provinsi dengan nama '" + request.getName() + "' sudah ada", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        Map<String, Object> before = Map.of("provinceId", province.getProvinceId(), "name", province.getName());
        province.setName(request.getName());
        province = provinceRepository.save(province);
        auditTrailService.recordAudit("PROVINCE", null, "UPDATE_PROVINCE",
                before, Map.of("provinceId", province.getProvinceId(), "name", province.getName()),
                performedBy, null);
        return ProvinceResponse.from(province);
    }

    @Transactional
    public void deleteProvince(String id, String performedBy) {
        Province province = findProvince(id);
        Map<String, Object> before = Map.of("provinceId", province.getProvinceId(), "name", province.getName());
        provinceRepository.delete(province);
        auditTrailService.recordAudit("PROVINCE", null, "DELETE_PROVINCE", before, null, performedBy, null);
    }

    private Province findProvince(String id) {
        return provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found", ErrorCode.WILAYAH_PROVINCE_NOT_FOUND));
    }

    // ─── State ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StateResponse> searchStates(String provinceId, String search, Pageable pageable) {
        if (provinceId != null && !provinceId.isBlank()) {
            if (search != null && !search.isBlank()) {
                return stateRepository.findByProvinceProvinceIdAndNameContainingIgnoreCase(provinceId, search, pageable).map(StateResponse::from);
            }
            return stateRepository.findByProvinceProvinceId(provinceId, pageable).map(StateResponse::from);
        }
        if (search != null && !search.isBlank()) {
            return stateRepository.findByNameContainingIgnoreCase(search, pageable).map(StateResponse::from);
        }
        return stateRepository.findAll(pageable).map(StateResponse::from);
    }

    @Transactional(readOnly = true)
    public StateResponse getState(String id) {
        return StateResponse.from(findState(id));
    }

    @Transactional
    public StateResponse createState(WilayahRequest request, String performedBy) {
        Province province = findProvince(request.getParentId());
        if (stateRepository.existsByNameIgnoreCaseAndProvinceProvinceId(request.getName(), province.getProvinceId())) {
            throw new BusinessException("Kab/Kota dengan nama '" + request.getName() + "' sudah ada di provinsi ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        State state = State.builder()
                .stateId(request.getId())
                .name(request.getName())
                .province(province)
                .build();
        state = stateRepository.save(state);
        auditTrailService.recordAudit("STATE", null, "CREATE_STATE",
                null, Map.of("stateId", state.getStateId(), "name", state.getName(), "provinceId", province.getProvinceId()),
                performedBy, null);
        return StateResponse.from(state);
    }

    @Transactional
    public StateResponse updateState(String id, WilayahRequest request, String performedBy) {
        State state = findState(id);
        String targetProvinceId = request.getParentId() != null ? request.getParentId() : state.getProvince().getProvinceId();
        if (stateRepository.existsByNameIgnoreCaseAndProvinceProvinceIdAndStateIdNot(request.getName(), targetProvinceId, id)) {
            throw new BusinessException("Kab/Kota dengan nama '" + request.getName() + "' sudah ada di provinsi ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        Map<String, Object> before = Map.of("stateId", state.getStateId(), "name", state.getName());
        state.setName(request.getName());
        if (request.getParentId() != null) {
            state.setProvince(findProvince(request.getParentId()));
        }
        state = stateRepository.save(state);
        auditTrailService.recordAudit("STATE", null, "UPDATE_STATE",
                before, Map.of("stateId", state.getStateId(), "name", state.getName()),
                performedBy, null);
        return StateResponse.from(state);
    }

    @Transactional
    public void deleteState(String id, String performedBy) {
        State state = findState(id);
        Map<String, Object> before = Map.of("stateId", state.getStateId(), "name", state.getName());
        stateRepository.delete(state);
        auditTrailService.recordAudit("STATE", null, "DELETE_STATE", before, null, performedBy, null);
    }

    private State findState(String id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State not found", ErrorCode.WILAYAH_STATE_NOT_FOUND));
    }

    // ─── District ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DistrictResponse> searchDistricts(String stateId, String search, Pageable pageable) {
        if (stateId != null && !stateId.isBlank()) {
            if (search != null && !search.isBlank()) {
                return districtRepository.findByStateStateIdAndNameContainingIgnoreCase(stateId, search, pageable).map(DistrictResponse::from);
            }
            return districtRepository.findByStateStateId(stateId, pageable).map(DistrictResponse::from);
        }
        if (search != null && !search.isBlank()) {
            return districtRepository.findByNameContainingIgnoreCase(search, pageable).map(DistrictResponse::from);
        }
        return districtRepository.findAll(pageable).map(DistrictResponse::from);
    }

    @Transactional(readOnly = true)
    public DistrictResponse getDistrict(String id) {
        return DistrictResponse.from(findDistrict(id));
    }

    @Transactional
    public DistrictResponse createDistrict(WilayahRequest request, String performedBy) {
        State state = findState(request.getParentId());
        if (districtRepository.existsByNameIgnoreCaseAndStateStateId(request.getName(), state.getStateId())) {
            throw new BusinessException("Kecamatan dengan nama '" + request.getName() + "' sudah ada di kab/kota ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        District district = District.builder()
                .districtId(request.getId())
                .name(request.getName())
                .state(state)
                .build();
        district = districtRepository.save(district);
        auditTrailService.recordAudit("DISTRICT", null, "CREATE_DISTRICT",
                null, Map.of("districtId", district.getDistrictId(), "name", district.getName(), "stateId", state.getStateId()),
                performedBy, null);
        return DistrictResponse.from(district);
    }

    @Transactional
    public DistrictResponse updateDistrict(String id, WilayahRequest request, String performedBy) {
        District district = findDistrict(id);
        String targetStateId = request.getParentId() != null ? request.getParentId() : district.getState().getStateId();
        if (districtRepository.existsByNameIgnoreCaseAndStateStateIdAndDistrictIdNot(request.getName(), targetStateId, id)) {
            throw new BusinessException("Kecamatan dengan nama '" + request.getName() + "' sudah ada di kab/kota ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        Map<String, Object> before = Map.of("districtId", district.getDistrictId(), "name", district.getName());
        district.setName(request.getName());
        if (request.getParentId() != null) {
            district.setState(findState(request.getParentId()));
        }
        district = districtRepository.save(district);
        auditTrailService.recordAudit("DISTRICT", null, "UPDATE_DISTRICT",
                before, Map.of("districtId", district.getDistrictId(), "name", district.getName()),
                performedBy, null);
        return DistrictResponse.from(district);
    }

    @Transactional
    public void deleteDistrict(String id, String performedBy) {
        District district = findDistrict(id);
        Map<String, Object> before = Map.of("districtId", district.getDistrictId(), "name", district.getName());
        districtRepository.delete(district);
        auditTrailService.recordAudit("DISTRICT", null, "DELETE_DISTRICT", before, null, performedBy, null);
    }

    private District findDistrict(String id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found", ErrorCode.WILAYAH_DISTRICT_NOT_FOUND));
    }

    // ─── SubDistrict ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    /**
     * Inquiry endpoint: semua filter optional dan dikombinasikan dengan AND.
     * Jika name/zipCode diisi, filter provinsi/state/district tetap diterapkan.
     */
    public Page<SubDistrictResponse> inquiry(String q, String zipCode,
                                             String provinceId, String stateId, String districtId,
                                             Pageable pageable) {
        String name      = (q        != null && !q.isBlank())        ? q.trim()        : null;
        String zip       = (zipCode  != null && !zipCode.isBlank())  ? zipCode.trim()  : null;
        String provId    = (provinceId != null && !provinceId.isBlank()) ? provinceId  : null;
        String stId      = (stateId  != null && !stateId.isBlank())  ? stateId         : null;
        String distId    = (districtId != null && !districtId.isBlank()) ? districtId  : null;
        return subDistrictRepository.inquiry(name, zip, distId, stId, provId, pageable)
                .map(SubDistrictResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SubDistrictResponse> searchSubDistricts(String districtId, String zipCode, String search, Pageable pageable) {
        if (zipCode != null && !zipCode.isBlank()) {
            return subDistrictRepository.findByZipCode(zipCode, pageable).map(SubDistrictResponse::from);
        }
        if (districtId != null && !districtId.isBlank()) {
            if (search != null && !search.isBlank()) {
                return subDistrictRepository.findByDistrictDistrictIdAndNameContainingIgnoreCase(districtId, search, pageable).map(SubDistrictResponse::from);
            }
            return subDistrictRepository.findByDistrictDistrictId(districtId, pageable).map(SubDistrictResponse::from);
        }
        if (search != null && !search.isBlank()) {
            return subDistrictRepository.findByNameContainingIgnoreCase(search, pageable).map(SubDistrictResponse::from);
        }
        return subDistrictRepository.findAll(pageable).map(SubDistrictResponse::from);
    }

    @Transactional(readOnly = true)
    public SubDistrictResponse getSubDistrict(String id) {
        return SubDistrictResponse.from(findSubDistrict(id));
    }

    @Transactional
    public SubDistrictResponse createSubDistrict(WilayahRequest request, String performedBy) {
        District district = findDistrict(request.getParentId());
        if (subDistrictRepository.existsByNameIgnoreCaseAndDistrictDistrictId(request.getName(), district.getDistrictId())) {
            throw new BusinessException("Kel/Desa dengan nama '" + request.getName() + "' sudah ada di kecamatan ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        SubDistrict sd = SubDistrict.builder()
                .subDistrictId(request.getId())
                .name(request.getName())
                .district(district)
                .zipCode(request.getZipCode())
                .build();
        sd = subDistrictRepository.save(sd);
        auditTrailService.recordAudit("SUBDISTRICT", null, "CREATE_SUBDISTRICT",
                null, Map.of("subDistrictId", sd.getSubDistrictId(), "name", sd.getName(), "districtId", district.getDistrictId()),
                performedBy, null);
        return SubDistrictResponse.from(sd);
    }

    @Transactional
    public SubDistrictResponse updateSubDistrict(String id, WilayahRequest request, String performedBy) {
        SubDistrict sd = findSubDistrict(id);
        String targetDistrictId = request.getParentId() != null ? request.getParentId() : sd.getDistrict().getDistrictId();
        if (subDistrictRepository.existsByNameIgnoreCaseAndDistrictDistrictIdAndSubDistrictIdNot(request.getName(), targetDistrictId, id)) {
            throw new BusinessException("Kel/Desa dengan nama '" + request.getName() + "' sudah ada di kecamatan ini", ErrorCode.WILAYAH_DUPLICATE_NAME);
        }
        Map<String, Object> before = Map.of("subDistrictId", sd.getSubDistrictId(), "name", sd.getName());
        sd.setName(request.getName());
        if (request.getZipCode() != null) sd.setZipCode(request.getZipCode());
        if (request.getParentId() != null) {
            sd.setDistrict(findDistrict(request.getParentId()));
        }
        sd = subDistrictRepository.save(sd);
        auditTrailService.recordAudit("SUBDISTRICT", null, "UPDATE_SUBDISTRICT",
                before, Map.of("subDistrictId", sd.getSubDistrictId(), "name", sd.getName()),
                performedBy, null);
        return SubDistrictResponse.from(sd);
    }

    @Transactional
    public void deleteSubDistrict(String id, String performedBy) {
        SubDistrict sd = findSubDistrict(id);
        Map<String, Object> before = Map.of("subDistrictId", sd.getSubDistrictId(), "name", sd.getName());
        subDistrictRepository.delete(sd);
        auditTrailService.recordAudit("SUBDISTRICT", null, "DELETE_SUBDISTRICT", before, null, performedBy, null);
    }

    private SubDistrict findSubDistrict(String id) {
        return subDistrictRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDistrict not found", ErrorCode.WILAYAH_SUBDISTRICT_NOT_FOUND));
    }
}
