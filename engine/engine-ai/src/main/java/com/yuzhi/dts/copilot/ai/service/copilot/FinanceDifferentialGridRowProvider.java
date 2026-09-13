package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;

public interface FinanceDifferentialGridRowProvider {

    List<FinanceDifferentialGridService.GridRow> copilotRows(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase);

    List<FinanceDifferentialGridService.GridRow> authorityRows(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase);
}
