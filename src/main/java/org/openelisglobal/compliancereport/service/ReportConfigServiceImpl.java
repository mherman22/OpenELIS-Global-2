package org.openelisglobal.compliancereport.service;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.ReportConfigDto;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportConfigServiceImpl implements ReportConfigService {

    private static final String KEY_LAB_LOGO = "report.config.labLogoBase64";
    private static final String KEY_ACCREDITATION_LOGO = "report.config.accreditationLogoBase64";

    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    public ReportConfigDto getConfig() {
        ConfigurationProperties config = ConfigurationProperties.getInstance();

        ReportConfigDto dto = new ReportConfigDto();
        dto.setLabName(val(config.getPropertyValue(Property.LH_REPORT_LAB_NAME)));
        dto.setLabSubtitle(val(config.getPropertyValue(Property.LH_REPORT_LAB_SUBTITLE)));
        dto.setAddressLine1(val(config.getPropertyValue(Property.LH_REPORT_ADDRESS_LINE1)));
        dto.setAddressLine2(val(config.getPropertyValue(Property.LH_REPORT_ADDRESS_LINE2)));
        dto.setPhone(val(config.getPropertyValue(Property.LH_REPORT_PHONE)));
        dto.setEmail(val(config.getPropertyValue(Property.LH_REPORT_EMAIL)));
        dto.setWebsite(val(config.getPropertyValue(Property.LH_REPORT_WEBSITE)));
        dto.setAccreditationNumber(val(config.getPropertyValue(Property.LH_REPORT_ACCREDITATION_NUMBER)));
        dto.setAccreditationBody(val(config.getPropertyValue(Property.LH_REPORT_ACCREDITATION_BODY)));
        dto.setFooterText(val(config.getPropertyValue(Property.LH_REPORT_FOOTER_TEXT)));
        dto.setShowPageNumbers(
                !"false".equalsIgnoreCase(config.getPropertyValue(Property.LH_REPORT_SHOW_PAGE_NUMBERS)));
        dto.setPageNumberFormat(
                val(config.getPropertyValue(Property.LH_REPORT_PAGE_NUMBER_FORMAT), "Page {page} of {total}"));
        dto.setCertificatePrefix(val(config.getPropertyValue(Property.LH_REPORT_CERTIFICATE_PREFIX), "LH"));
        dto.setDateFormat(val(config.getPropertyValue(Property.LH_REPORT_DATE_FORMAT), "YYYY"));

        // logos are large binary data — fetched directly from DB, excluded from
        // ConfigurationProperties cache
        dto.setLabLogoBase64(dbValue(KEY_LAB_LOGO));
        dto.setAccreditationLogoBase64(dbValue(KEY_ACCREDITATION_LOGO));

        return dto;
    }

    @Override
    @Transactional
    public void saveConfig(ReportConfigDto dto, String sysUserId) {
        persist(Property.LH_REPORT_LAB_NAME.getDBName(), "text", dto.getLabName(), sysUserId);
        persist(Property.LH_REPORT_LAB_SUBTITLE.getDBName(), "text", dto.getLabSubtitle(), sysUserId);
        persist(Property.LH_REPORT_ADDRESS_LINE1.getDBName(), "text", dto.getAddressLine1(), sysUserId);
        persist(Property.LH_REPORT_ADDRESS_LINE2.getDBName(), "text", dto.getAddressLine2(), sysUserId);
        persist(Property.LH_REPORT_PHONE.getDBName(), "text", dto.getPhone(), sysUserId);
        persist(Property.LH_REPORT_EMAIL.getDBName(), "text", dto.getEmail(), sysUserId);
        persist(Property.LH_REPORT_WEBSITE.getDBName(), "text", dto.getWebsite(), sysUserId);
        persist(Property.LH_REPORT_ACCREDITATION_NUMBER.getDBName(), "text", dto.getAccreditationNumber(), sysUserId);
        persist(Property.LH_REPORT_ACCREDITATION_BODY.getDBName(), "text", dto.getAccreditationBody(), sysUserId);
        persist(Property.LH_REPORT_FOOTER_TEXT.getDBName(), "text", dto.getFooterText(), sysUserId);
        persist(Property.LH_REPORT_SHOW_PAGE_NUMBERS.getDBName(), "text", String.valueOf(dto.isShowPageNumbers()),
                sysUserId);
        persist(Property.LH_REPORT_PAGE_NUMBER_FORMAT.getDBName(), "text", dto.getPageNumberFormat(), sysUserId);
        persist(Property.LH_REPORT_CERTIFICATE_PREFIX.getDBName(), "text", dto.getCertificatePrefix(), sysUserId);
        persist(Property.LH_REPORT_DATE_FORMAT.getDBName(), "text", dto.getDateFormat(), sysUserId);

        if (dto.getLabLogoBase64() != null) {
            persist(KEY_LAB_LOGO, "freeText", dto.getLabLogoBase64(), sysUserId);
        }
        if (dto.getAccreditationLogoBase64() != null) {
            persist(KEY_ACCREDITATION_LOGO, "freeText", dto.getAccreditationLogoBase64(), sysUserId);
        }
    }

    private void persist(String name, String valueType, String value, String sysUserId) {
        try {
            SiteInformation info = siteInformationService.getSiteInformationByName(name);
            boolean isNew = (info == null);
            if (isNew) {
                info = new SiteInformation();
                info.setName(name);
                info.setValueType(valueType);
            }
            info.setValue(value != null ? value : "");
            info.setSysUserId(sysUserId);
            siteInformationService.persistData(info, isNew);
        } catch (Exception e) {
            LogEvent.logError(e);
        }
    }

    private String dbValue(String name) {
        try {
            SiteInformation info = siteInformationService.getSiteInformationByName(name);
            return (info != null && info.getValue() != null) ? info.getValue() : "";
        } catch (Exception e) {
            LogEvent.logError(e);
            return "";
        }
    }

    private static String val(String v) {
        return v != null ? v : "";
    }

    private static String val(String v, String def) {
        return v != null ? v : def;
    }
}
