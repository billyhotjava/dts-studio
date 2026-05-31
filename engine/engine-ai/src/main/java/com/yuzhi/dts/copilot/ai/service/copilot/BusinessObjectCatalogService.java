package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BusinessObjectCatalogService {

    private static final List<BusinessObjectEntry> ENTRIES = List.of(
            entry(
                    "prs.flowerbiz.biz_order",
                    "prs.flowerbiz.biz_order.profile",
                    "flowerbiz",
                    "报花管理 > 报花单据",
                    "business-object:prs.flowerbiz.biz_order",
                    List.of(
                            "报花单据覆盖加花、减花、换花、销售、坏账和回收等现场业务主线，适合做状态、业务类型、项目点和处理时效画像。",
                            "经营汇总优先使用 dbt ADS/DWS；该对象用于明细定位、字段画像和异常排查。"),
                    List.of("项目点", "业务类型", "单号", "标题", "租金日期", "摆位信息", "养护人", "项目经理", "发起人", "发起时间", "完成时间", "打印状态"),
                    List.of("状态分布", "业务类型分布", "项目点单据量", "未完成单据", "处理时效"),
                    List.of("报花单据", "报花单", "报花明细", "报花状态", "单据状态", "加花单", "减花单", "换花单", "调花单", "报损单", "回收单", "坏账单"),
                    List.of(
                            "business-object:prs.flowerbiz.biz_order",
                            "adminweb:报花管理/报花单据",
                            "adminapi:/rs-flowers-base/flower/biz/listPage",
                            "adminapi:/rs-flowers-base/flower/bizAdd/listPage",
                            "adminapi:/rs-flowers-base/flower/cut/listPage")),
            entry(
                    "prs.flowerbiz.change_order",
                    "prs.flowerbiz.change_order.profile",
                    "flowerbiz",
                    "报花管理 > 换花/调花",
                    "business-object:prs.flowerbiz.change_order",
                    List.of(
                            "换花和调花是项目现场调整的高频业务，适合按换花类型、调整类型、项目点、发起人和完成时效做画像。",
                            "该对象应以字段画像和业务页面语义解释为主，金额类经营指标仍回到 dbt 汇总层。"),
                    List.of("项目点", "单号", "标题", "换花类型", "调整类型", "绿植数量", "状态", "发起人", "申请时间", "完成时间"),
                    List.of("换花类型分布", "调花状态分布", "项目点换花次数", "未完成换花", "处理时效"),
                    List.of("换花", "调花", "建议换花", "强制换花", "交接换花", "调整类型", "调整绿植"),
                    List.of(
                            "business-object:prs.flowerbiz.change_order",
                            "adminweb:报花管理/换花",
                            "adminweb:报花管理/调花",
                            "adminapi:/rs-flowers-base/changeInfo/listPage",
                            "adminapi:/rs-flowers-base/flower/transfer/listPage")),
            entry(
                    "prs.flowerbiz.position_placement",
                    "prs.flowerbiz.position_placement.profile",
                    "flowerbiz",
                    "项目点管理 > 摆位/实摆绿植",
                    "business-object:prs.flowerbiz.position_placement",
                    List.of(
                            "摆位和实摆绿植连接项目、养护、租金和库存，是报花业务对象问答的核心上下文。",
                            "若要做租金、成本和收入经营报表，应优先使用已建模的 DWS/ADS。"),
                    List.of("项目点", "摆位", "楼栋", "楼层", "区域", "绿植名称", "规格", "数量", "租金", "养护人", "状态"),
                    List.of("摆位状态分布", "项目点实摆数量", "养护人负责绿植", "绿植规格分布", "异常摆位排查"),
                    List.of("摆位", "摆放位置", "实摆绿植", "项目绿植", "养护人", "绿植状态", "摆位绿植"),
                    List.of(
                            "business-object:prs.flowerbiz.position_placement",
                            "adminweb:项目点管理/摆位",
                            "adminweb:项目点管理/实摆绿植",
                            "adminapi:/rs-flowers-base/project/position/listPage",
                            "adminapi:/rs-flowers-base/project/green/listPage")),
            entry(
                    "prs.procurement.plan_purchase",
                    "prs.procurement.plan_purchase.profile",
                    "purchase_inventory",
                    "采购管理 > 采购计划明细",
                    "business-object:prs.procurement.plan_purchase",
                    List.of(
                            "采购计划明细适合回答计划状态、计划采购量、分配采购人和项目点需求画像。",
                            "采购经营指标应优先沉淀候选 ADS，计划明细仅作为业务状态和字段画像入口。"),
                    List.of("项目点", "摆位", "物品", "计划采购量", "实际采购量", "采购单价", "采购人", "计划日期", "采购日期", "分配时间", "状态"),
                    List.of("计划状态分布", "采购人任务量", "项目点计划量", "未采购计划", "计划到采购转化"),
                    List.of("采购计划", "计划采购", "采购计划明细", "计划日期", "分配采购", "待采购"),
                    List.of(
                            "business-object:prs.procurement.plan_purchase",
                            "adminweb:采购管理/采购计划明细",
                            "adminapi:/rs-flowers-base/self/planPurchase/listPlanPurchaseItemPage")),
            entry(
                    "prs.procurement.purchase_order",
                    "prs.procurement.purchase_order.profile",
                    "purchase_inventory",
                    "采购管理 > 采购汇总/采购明细",
                    "business-object:prs.procurement.purchase_order",
                    List.of(
                            "采购汇总和采购明细适合回答采购人、供应商、付款方式和采购金额的字段画像。",
                            "需要跨月采购经营趋势时，应生成或复用采购 ADS，而不是直接扫源库明细。"),
                    List.of("单号", "标题", "仓库", "用途", "状态", "采购人", "采购时间", "付款方式", "供应商", "采购金额"),
                    List.of("采购状态分布", "采购人采购金额", "供应商分布", "付款方式分布", "历史采购明细"),
                    List.of("采购单", "采购汇总", "采购明细", "采购金额", "采购人", "自采", "市场采购", "供应商"),
                    List.of(
                            "business-object:prs.procurement.purchase_order",
                            "adminweb:采购管理/采购汇总",
                            "adminweb:采购管理/采购明细",
                            "adminapi:/rs-flowers-base/self/purchaseInfo/listPage",
                            "adminapi:/rs-flowers-base/self/info/listPage")),
            entry(
                    "prs.procurement.delivery_record",
                    "prs.procurement.delivery_record.profile",
                    "purchase_inventory",
                    "采购管理 > 配送记录",
                    "business-object:prs.procurement.delivery_record",
                    List.of(
                            "配送记录来自采购业务链路，适合做状态、类型、接收人和配送时效的字段画像。",
                            "ODS 仅作为只读排查面，经营汇总应沉淀候选 ADS 后再复用。"),
                    List.of("标题", "状态", "类型", "起始", "目的地", "配送人", "配送时间", "接收人", "接收时间"),
                    List.of("状态分布", "类型分布", "接收人统计", "未接收排查", "配送时效"),
                    List.of("配送记录", "配送", "接收人", "接收时间", "配送时间", "目的地", "起始", "采购配送", "采购"),
                    List.of(
                            "business-object:prs.procurement.delivery_record",
                            "ods-profile:procurement.delivery_record",
                            "adminweb:采购管理/配送记录",
                            "adminapi:/rs-flowers-base/self/deliveryInfo/listDeliveryInfoPage")),
            entry(
                    "prs.project.project_site",
                    "prs.project.project_site.profile",
                    "project",
                    "项目点管理 > 项目点",
                    "business-object:prs.project.project_site",
                    List.of(
                            "项目点是客户、合同、摆位、养护和财务结算的主对象，适合回答项目状态、负责人和组织归属画像。",
                            "项目经营 TOP 类问题应优先命中 DTS 已汇总的项目/客户月度模型。"),
                    List.of("项目点编码", "项目点名称", "项目状态", "项目经理", "监管人员", "业务经理", "养护负责人", "合同名称", "创建人", "创建时间"),
                    List.of("项目点状态统计", "项目经理项目量", "项目点负责人分布", "异常项目排查", "项目基础清单"),
                    List.of("项目点", "项目点状态", "项目状态", "项目经理", "监管人员", "业务经理", "养护负责人"),
                    List.of(
                            "business-object:prs.project.project_site",
                            "adminweb:项目点管理/项目点",
                            "adminapi:/rs-flowers-base/project/project/listPage")),
            entry(
                    "prs.project.position",
                    "prs.project.position.profile",
                    "project",
                    "项目点管理 > 摆位",
                    "business-object:prs.project.position",
                    List.of(
                            "摆位是项目点实摆、养护和调整的空间维度，适合回答楼栋、楼层、区域和摆位状态画像。",
                            "经营汇总优先使用 Sprint-25 dbt ADS/DWS；该对象用于明细定位和字段解释。"),
                    List.of("项目点", "摆位", "楼栋", "楼层", "区域", "养护周期", "养护方式", "合同部门", "创建时间"),
                    List.of("摆位清单", "楼层分布", "区域分布", "项目点摆位数", "异常摆位排查"),
                    List.of("摆位", "摆放位置", "楼栋", "楼层", "区域", "项目摆位"),
                    List.of(
                            "business-object:prs.project.position",
                            "dbt-model:public.xycyl_dim_position",
                            "adminweb:项目点管理/摆位",
                            "adminapi:/rs-flowers-base/project/position/listPage")),
            entry(
                    "prs.project.green_snapshot",
                    "prs.project.green_snapshot.profile",
                    "project",
                    "项目点管理 > 实摆绿植",
                    "business-object:prs.project.green_snapshot",
                    List.of(
                            "实摆绿植明细来自 Sprint-25 dbt DWD，包含项目、摆位、物品、数量、租金、成本和软外键质量标记。",
                            "金额字段当前为 raw 合计，rent/cost 乘法口径未拍板前不能作为最终财务口径。"),
                    List.of("项目点", "摆位", "绿植名称", "规格", "单位", "状态", "实摆数量", "总数量", "租金", "成本", "摆放时间"),
                    List.of("项目当前实摆绿植", "实摆状态分布", "绿植规格分布", "摆位绿植清单", "孤儿外键排查"),
                    List.of("当前实摆绿植", "实摆绿植", "项目绿植", "在摆绿植", "绿植明细", "绿植状态"),
                    List.of(
                            "business-object:prs.project.green_snapshot",
                            "dbt-model:public.xycyl_dwd_project_green_snapshot",
                            "adminweb:项目点管理/实摆绿植",
                            "adminapi:/rs-flowers-base/project/green/listPage")),
            entry(
                    "prs.project.contract",
                    "prs.project.contract.profile",
                    "project",
                    "项目点管理 > 合同管理",
                    "business-object:prs.project.contract",
                    List.of(
                            "合同对象连接客户、项目和结算周期，适合做合同状态、到期、业务负责人和客户维度画像。",
                            "合同金额趋势或收入贡献应回到 dbt 经营汇总层。"),
                    List.of("客户名称", "合同编码", "合同名称", "开始日期", "结束日期", "业务负责人", "项目经理", "合同状态", "结算周期"),
                    List.of("合同状态分布", "合同到期排查", "客户合同数", "项目经理合同数", "结算周期分布"),
                    List.of("合同", "合同管理", "合同状态", "合同到期", "结算周期", "客户合同"),
                    List.of(
                            "business-object:prs.project.contract",
                            "adminweb:项目点管理/合同管理",
                            "adminapi:/rs-flowers-base/project/contract/listPage")),
            entry(
                    "prs.finance.settlement",
                    "prs.finance.settlement.profile",
                    "finance",
                    "财务管理 > 财务结算",
                    "business-object:prs.finance.settlement",
                    List.of(
                            "财务结算是报花、租金、费用进入财务闭环的核心单据，适合回答结算状态、申请人和结算明细画像。",
                            "应收、已收、欠款等经营汇总优先使用 DTS ADS/DWS，结算对象用于单据级解释。"),
                    List.of("结算单号", "标题", "状态", "租金收入", "成本", "申请人", "申请时间", "结算人", "结算时间"),
                    List.of("结算状态分布", "结算人处理量", "项目结算明细", "未确认结算", "结算处理时效"),
                    List.of("财务结算", "结算单", "结算明细", "待结算", "结算状态", "结算人"),
                    List.of(
                            "business-object:prs.finance.settlement",
                            "adminweb:财务管理/财务结算",
                            "adminapi:/rs-flowers-base/finace/settlement/listPage",
                            "adminapi:/rs-flowers-base/finace/settlement/listsettledItems")),
            entry(
                    "prs.finance.expense",
                    "prs.finance.expense.profile",
                    "finance",
                    "运营侧 > 报销/预支/支出",
                    "business-object:prs.finance.expense",
                    List.of(
                            "报销、预支和支出属于运营财务动作，适合做状态、费用类型、申请人和付款时间画像。",
                            "涉及付款或审批的动作必须输出动作提案，不允许直接写业务系统。"),
                    List.of("单号", "标题", "申请人", "申请时间", "状态", "费用类型", "金额", "付款时间", "付款记录"),
                    List.of("费用状态分布", "费用类型分布", "申请人统计", "待付款排查", "付款时效"),
                    List.of("报销", "预支", "支出", "费用单", "费用类型", "待付款", "付款记录", "申请费用"),
                    List.of(
                            "business-object:prs.finance.expense",
                            "adminweb:运营侧/报销",
                            "adminweb:运营侧/预支和支出",
                            "adminapi:/rs-flowers-base/finace/expense/listPage",
                            "adminapi:/rs-flowers-base/advance/listPage")),
            entry(
                    "prs.finance.bank_statement",
                    "prs.finance.bank_statement.profile",
                    "finance",
                    "财务管理 > 银行流水",
                    "business-object:prs.finance.bank_statement",
                    List.of(
                            "银行流水适合做未核对、未生成凭证、收入支出方向、对方户名和摘要字段画像。",
                            "流水与业务单据关联不完整时，回答必须保留数据质量提示。"),
                    List.of("交易日期", "银行名称", "流水号", "收入金额", "支出金额", "对方户名", "摘要", "凭证号", "发票号", "状态"),
                    List.of("未核对流水", "收入支出分布", "对方户名 TOP", "未生成凭证", "摘要字段画像"),
                    List.of("银行流水", "流水未核对", "未核对", "对方户名", "流水状态", "凭证号", "交易日期"),
                    List.of(
                            "business-object:prs.finance.bank_statement",
                            "adminweb:财务管理/银行流水",
                            "adminapi:/rs-flowers-base/finace/bankStatement/listPage")),
            entry(
                    "prs.finance.voucher",
                    "prs.finance.voucher.profile",
                    "finance",
                    "财务管理 > 凭证管理",
                    "business-object:prs.finance.voucher",
                    List.of(
                            "凭证对象用于解释银行流水、结算和收支业务的会计凭证生成情况。",
                            "凭证生成、合并和确认属于受控动作，Agent 只能生成提案和证据。"),
                    List.of("凭证号", "业务名称", "会计期间", "业务单号", "借方金额", "贷方金额", "支付日期", "摘要", "状态", "创建时间"),
                    List.of("凭证状态分布", "会计期间凭证数", "借贷金额校验", "未确认凭证", "业务单据追溯"),
                    List.of("凭证", "会计凭证", "凭证状态", "借方金额", "贷方金额", "会计期间", "生成凭证"),
                    List.of(
                            "business-object:prs.finance.voucher",
                            "adminweb:财务管理/凭证管理",
                            "adminapi:/rs-flowers-base/finace/voucher/list")),
            entry(
                    "prs.finance.invoice_collection",
                    "prs.finance.invoice_collection.profile",
                    "finance",
                    "运营管理 > 开票/收款",
                    "business-object:prs.finance.invoice_collection",
                    List.of(
                            "开票和收款对象连接项目账单、发票、回款和对账状态，适合单据级状态与字段画像。",
                            "回款率、欠款排行等经营指标应优先使用 DTS 汇总模型。"),
                    List.of("单号", "项目点", "收入类型", "状态", "收款金额", "收款时间", "开票金额", "发票内容", "申请人", "开票人"),
                    List.of("开票状态分布", "收款状态分布", "项目收款明细", "未开票排查", "回款单据追溯"),
                    List.of("开票", "发票", "收款", "回款", "对账", "收款状态", "开票状态"),
                    List.of(
                            "business-object:prs.finance.invoice_collection",
                            "adminweb:运营管理/开票管理",
                            "adminweb:运营管理/收款记录",
                            "adminapi:/rs-flowers-base/operate/invoiceInfo/listInvoiceInfoPage",
                            "adminapi:/rs-flowers-base/operate/collection/listCollectionRecordPage")),
            entry(
                    "prs.warehouse.stock_info",
                    "prs.warehouse.stock_info.profile",
                    "warehouse",
                    "仓库管理 > 库存管理 > 库存",
                    "business-object:prs.warehouse.stock_info",
                    List.of(
                            "库存现量主表是 s_stock_info，库存明细是 s_stock_item，库房主数据是 s_storehouse_info。",
                            "该对象对应现网页面库存 Tab，用于当前可用数量、低库存、启停状态和物品维度画像。"),
                    List.of("所属库房", "物品名称", "物品规格", "物品属性", "物品类型", "单位", "可用数量", "出库单价", "销售单价"),
                    List.of("库存现量", "当前库存", "库存状态分布", "物品库存排行", "低库存预警", "库存成本画像"),
                    List.of("仓库", "库存", "库存现量", "当前库存", "可用库存", "低库存", "库存状态", "库存排行"),
                    List.of(
                            "business-object:prs.warehouse.stock_info",
                            "mysql-table:s_stock_info",
                            "mysql-table:s_stock_item",
                            "mysql-table:s_storehouse_info",
                            "adminweb:仓库管理/库存管理/库存",
                            "adminapi:/rs-flowers-base/store/info/listPage")),
            entry(
                    "prs.warehouse.inout_record",
                    "prs.warehouse.inout_record.profile",
                    "warehouse",
                    "仓库管理 > 库存管理 > 出入库记录",
                    "business-object:prs.warehouse.inout_record",
                    List.of(
                            "出入库记录由 t_warehousing_info/item 与 t_ex_warehouse_info/item 合并分析。",
                            "该对象适合回答入库、出库、调拨、报损和退货的只读明细与状态画像；库存周转经营分析应沉淀候选 ADS 后复用。"),
                    List.of("方向", "入库单号", "出库单号", "所属库房", "物品名称", "物品属性", "数量", "单价", "发生日期", "关联业务单号"),
                    List.of("出入库记录", "入库出库统计", "调拨记录", "报损退货排查", "物品出入库排行"),
                    List.of("出入库", "入库", "出库", "调拨", "报损", "退货", "入库记录", "出库记录"),
                    List.of(
                            "business-object:prs.warehouse.inout_record",
                            "mysql-table:t_warehousing_info",
                            "mysql-table:t_warehousing_item",
                            "mysql-table:t_ex_warehouse_info",
                            "mysql-table:t_ex_warehouse_item",
                            "adminweb:仓库管理/库存管理/出入库记录",
                            "adminapi:/rs-flowers-base/store/info/listInOutItemPage",
                            "adminapi:/rs-flowers-base/store/ware/listPage",
                            "adminapi:/rs-flowers-base/store/exWarehouseInfo/listPage"))
    );

    public List<BusinessObjectEntry> entries() {
        return ENTRIES;
    }

    public Optional<BusinessObjectEntry> findBestMatch(String userQuestion, String routedDomain) {
        if (!StringUtils.hasText(userQuestion)) {
            return Optional.empty();
        }
        String normalizedQuestion = normalize(userQuestion);
        List<ScoredEntry> scoredEntries = new ArrayList<>();
        for (BusinessObjectEntry entry : entries()) {
            int score = 0;
            int keywordHits = 0;
            for (String keyword : entry.keywords()) {
                String normalizedKeyword = normalize(keyword);
                if (StringUtils.hasText(normalizedKeyword) && normalizedQuestion.contains(normalizedKeyword)) {
                    keywordHits++;
                    score += keyword.length() >= 4 ? 2 : 1;
                }
            }
            if (keywordHits == 0) {
                continue;
            }
            if (entry.matchesDomain(routedDomain)) {
                score += 2;
            }
            scoredEntries.add(new ScoredEntry(entry, score));
        }
        return scoredEntries.stream()
                .max(Comparator.comparingInt(ScoredEntry::score))
                .map(ScoredEntry::entry);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static BusinessObjectEntry entry(
            String objectCode,
            String reportCode,
            String domain,
            String pagePath,
            String primaryTarget,
            List<String> qualityNotes,
            List<String> keyFields,
            List<String> supportedQuestions,
            List<String> keywords,
            List<String> sourceRefs) {
        return new BusinessObjectEntry(
                objectCode,
                reportCode,
                domain,
                "BUSINESS_INSIGHT",
                "L0_BUSINESS_OBJECT_PROFILE",
                pagePath,
                primaryTarget,
                "MEDIUM",
                qualityNotes,
                keyFields,
                supportedQuestions,
                keywords,
                sourceRefs,
                true);
    }

    private record ScoredEntry(BusinessObjectEntry entry, int score) {}

    public record BusinessObjectEntry(
            String objectCode,
            String reportCode,
            String domain,
            String responseKind,
            String dataSurface,
            String pagePath,
            String primaryTarget,
            String qualityLevel,
            List<String> qualityNotes,
            List<String> keyFields,
            List<String> supportedQuestions,
            List<String> keywords,
            List<String> sourceRefs,
            boolean readOnly
    ) {
        public BusinessObjectEntry {
            qualityNotes = qualityNotes == null ? List.of() : List.copyOf(qualityNotes);
            keyFields = keyFields == null ? List.of() : List.copyOf(keyFields);
            supportedQuestions = supportedQuestions == null ? List.of() : List.copyOf(supportedQuestions);
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        }

        private boolean matchesDomain(String routedDomain) {
            if (!StringUtils.hasText(routedDomain)) {
                return false;
            }
            String normalized = normalize(routedDomain);
            if (domain.equals(normalized)) {
                return true;
            }
            return switch (normalized) {
                case "flowerbiz", "flower", "curing", "pendulum", "task" -> "flowerbiz".equals(domain);
                case "purchase", "procurement", "purchase_inventory" -> "purchase_inventory".equals(domain);
                case "project", "customer", "contract", "green" -> "project".equals(domain);
                case "finance", "finace", "settlement", "expense", "operate", "operations" -> "finance".equals(domain);
                case "inventory", "stock", "warehouse", "store", "storehouse" -> "warehouse".equals(domain);
                default -> false;
            };
        }
    }
}
