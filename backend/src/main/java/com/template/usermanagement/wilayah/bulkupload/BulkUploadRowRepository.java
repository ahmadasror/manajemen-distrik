package com.template.usermanagement.wilayah.bulkupload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkUploadRowRepository extends JpaRepository<BulkUploadRow, Long> {

    List<BulkUploadRow> findByBulkUploadId(Long bulkUploadId);

    List<BulkUploadRow> findByBulkUploadIdAndIsValidTrue(Long bulkUploadId);
}
