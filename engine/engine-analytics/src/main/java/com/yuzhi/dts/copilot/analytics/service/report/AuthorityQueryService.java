package com.yuzhi.dts.copilot.analytics.service.report;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuthorityQueryService {

    public AuthorityAdapter resolve(AnalyticsReportTemplate template) {
        if (template == null) {
            return AuthorityAdapter.unresolved();
        }
        String targetObject = trimToNull(template.getTargetObject());
        if (targetObject == null) {
            return AuthorityAdapter.unresolved();
        }
        String normalizedTargetObject = normalize(targetObject);
        boolean realtime = isRealtime(template.getRefreshPolicy());
        String adapterKey = adapterKey(template, normalizedTargetObject);
        if (adapterKey == null) {
            return AuthorityAdapter.unresolved();
        }

        if (normalizedTargetObject.startsWith("v_")) {
            return new AuthorityAdapter(
                    ReportExecutionPlanService.Route.AUTHORITY_VIEW,
                    adapterKey,
                    targetObject);
        }

        if (normalizedTargetObject.startsWith("authority.finance.")) {
            if (normalizedTargetObject.endsWith(".settlement_summary")) {
                return new AuthorityAdapter(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        adapterKey,
                        targetObject);
            }
            if (realtime && normalizedTargetObject.endsWith(".receivable_overview")) {
                return new AuthorityAdapter(
                        ReportExecutionPlanService.Route.AUTHORITY_VIEW,
                        adapterKey,
                        targetObject);
            }
            return new AuthorityAdapter(
                    ReportExecutionPlanService.Route.AUTHORITY_SQL,
                    adapterKey,
                    targetObject);
        }
        if (normalizedTargetObject.startsWith("authority.procurement.")) {
            if (normalizedTargetObject.endsWith(".purchase_summary")) {
                return new AuthorityAdapter(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        adapterKey,
                        targetObject);
            }
            return new AuthorityAdapter(
                    realtime ? ReportExecutionPlanService.Route.AUTHORITY_VIEW : ReportExecutionPlanService.Route.AUTHORITY_SQL,
                    adapterKey,
                    targetObject);
        }
        if (normalizedTargetObject.startsWith("authority.inventory.")) {
            if (normalizedTargetObject.endsWith(".stock_overview")
                    || normalizedTargetObject.endsWith(".low_stock_alert")) {
                return new AuthorityAdapter(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        adapterKey,
                        targetObject);
            }
            return new AuthorityAdapter(
                    realtime ? ReportExecutionPlanService.Route.AUTHORITY_VIEW : ReportExecutionPlanService.Route.AUTHORITY_SQL,
                    adapterKey,
                    targetObject);
        }
        if (normalizedTargetObject.startsWith("authority.")) {
            return new AuthorityAdapter(
                    realtime ? ReportExecutionPlanService.Route.AUTHORITY_VIEW : ReportExecutionPlanService.Route.AUTHORITY_SQL,
                    adapterKey,
                    targetObject);
        }
        return AuthorityAdapter.unresolved();
    }

    private static String adapterKey(AnalyticsReportTemplate template, String normalizedTargetObject) {
        if (normalizedTargetObject.startsWith("authority.finance.")) {
            return "authority.finance";
        }
        if (normalizedTargetObject.startsWith("authority.procurement.")) {
            return "authority.procurement";
        }
        if (normalizedTargetObject.startsWith("authority.inventory.")) {
            return "authority.inventory";
        }

        String domain = normalize(template.getDomain());
        if ("财务".equals(domain) || domain.contains("finance")) {
            return "authority.finance";
        }
        if ("采购".equals(domain) || domain.contains("procurement")) {
            return "authority.procurement";
        }
        if ("仓库".equals(domain) || domain.contains("warehouse") || domain.contains("inventory")) {
            return "authority.inventory";
        }
        return null;
    }

    private static boolean isRealtime(String refreshPolicy) {
        return "realtime".equals(normalize(refreshPolicy));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record AuthorityAdapter(ReportExecutionPlanService.Route route, String adapterKey, String targetObject) {
        private static AuthorityAdapter unresolved() {
            return new AuthorityAdapter(ReportExecutionPlanService.Route.EXPLORATION, null, null);
        }
    }
}
