package com.template.usermanagement.wilayah;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WilayahDataSeeder implements ApplicationRunner {

    private final ProvinceRepository provinceRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final SubDistrictRepository subDistrictRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (provinceRepository.count() > 0) {
            log.info("[seeder] Wilayah data already seeded — skipping");
            return;
        }

        log.info("[seeder] Starting wilayah data seeding...");
        ClassPathResource resource = new ClassPathResource("data/kodepos_master.csv");
        if (!resource.exists()) {
            log.warn("[seeder] kodepos_master.csv not found in classpath:data/ — skipping seed");
            return;
        }

        Map<String, Province> provinces = new LinkedHashMap<>();
        Map<String, State> states = new LinkedHashMap<>();
        Map<String, District> districts = new LinkedHashMap<>();
        List<SubDistrict> subDistricts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // skip header
            if (headerLine == null) {
                log.warn("[seeder] CSV is empty");
                return;
            }

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                lineNum++;

                try {
                    String[] values = parseCsvLine(line);
                    if (values.length < 9) continue;

                    String provinceId = values[0].trim();
                    String provinceName = values[1].trim();
                    String stateId = values[2].trim();
                    String stateName = values[3].trim();
                    String districtId = values[4].trim();
                    String districtName = values[5].trim();
                    String subDistrictId = values[6].trim();
                    String subDistrictName = values[7].trim();
                    String zipCode = values[8].trim();

                    if (provinceId.isEmpty() || subDistrictId.isEmpty()) continue;

                    provinces.computeIfAbsent(provinceId, id ->
                            Province.builder().provinceId(id).name(provinceName).build());

                    if (!stateId.isEmpty()) {
                        Province prov = provinces.get(provinceId);
                        states.computeIfAbsent(stateId, id ->
                                State.builder().stateId(id).name(stateName).province(prov).build());
                    }

                    if (!districtId.isEmpty() && states.containsKey(stateId)) {
                        State st = states.get(stateId);
                        districts.computeIfAbsent(districtId, id ->
                                District.builder().districtId(id).name(districtName).state(st).build());
                    }

                    if (!subDistrictId.isEmpty() && districts.containsKey(districtId)) {
                        District dist = districts.get(districtId);
                        subDistricts.add(SubDistrict.builder()
                                .subDistrictId(subDistrictId)
                                .name(subDistrictName)
                                .district(dist)
                                .zipCode(zipCode.isEmpty() ? null : zipCode)
                                .build());
                    }
                } catch (Exception e) {
                    log.debug("[seeder] Skipping line {}: {}", lineNum, e.getMessage());
                }
            }
        }

        log.info("[seeder] Parsed: {} provinces, {} states, {} districts, {} subdistricts",
                provinces.size(), states.size(), districts.size(), subDistricts.size());

        // Batch save
        List<Province> provList = new ArrayList<>(provinces.values());
        for (int i = 0; i < provList.size(); i += 500) {
            provinceRepository.saveAll(provList.subList(i, Math.min(i + 500, provList.size())));
        }

        List<State> stateList = new ArrayList<>(states.values());
        for (int i = 0; i < stateList.size(); i += 500) {
            stateRepository.saveAll(stateList.subList(i, Math.min(i + 500, stateList.size())));
        }

        List<District> distList = new ArrayList<>(districts.values());
        for (int i = 0; i < distList.size(); i += 500) {
            districtRepository.saveAll(distList.subList(i, Math.min(i + 500, distList.size())));
        }

        for (int i = 0; i < subDistricts.size(); i += 500) {
            subDistrictRepository.saveAll(subDistricts.subList(i, Math.min(i + 500, subDistricts.size())));
        }

        log.info("[seeder] {} rows seeded successfully", subDistricts.size());
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' || c == '\t') && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
