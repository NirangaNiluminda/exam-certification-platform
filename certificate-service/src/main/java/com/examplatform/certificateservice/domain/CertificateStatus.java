package com.examplatform.certificateservice.domain;

public enum CertificateStatus {
    PENDING,    // event received, PDF not yet generated
    ISSUED,     // PDF generated and stored in MinIO
    FAILED      // PDF generation or storage failed
}
