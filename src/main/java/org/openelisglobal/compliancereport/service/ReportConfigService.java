package org.openelisglobal.compliancereport.service;

import org.openelisglobal.compliancereport.dto.OrderPreviewDto.ReportConfigDto;

public interface ReportConfigService {

    ReportConfigDto getConfig();

    void saveConfig(ReportConfigDto dto, String sysUserId);
}
