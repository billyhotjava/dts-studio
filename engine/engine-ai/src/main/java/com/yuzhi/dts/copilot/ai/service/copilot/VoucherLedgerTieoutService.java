package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class VoucherLedgerTieoutService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public TieoutReport tieOut(List<VoucherLedgerRow> rows) {
        List<VoucherLedgerRow> safeRows = rows == null ? List.of() : rows;
        Map<SubjectKey, AmountPair> subjectTotals = new TreeMap<>();
        Map<VoucherKey, AmountPair> voucherTotals = new TreeMap<>();

        for (VoucherLedgerRow row : safeRows) {
            subjectTotals
                    .computeIfAbsent(SubjectKey.from(row), key -> new AmountPair())
                    .add(row.debitAmount(), row.creditAmount());
            voucherTotals
                    .computeIfAbsent(VoucherKey.from(row), key -> new AmountPair())
                    .add(row.debitAmount(), row.creditAmount());
        }

        List<SubjectLine> subjectLines = subjectTotals.entrySet().stream()
                .map(entry -> entry.getKey().toSubjectLine(entry.getValue()))
                .toList();
        String failureMessage = voucherTotals.entrySet().stream()
                .filter(entry -> entry.getValue().difference().compareTo(ZERO_CENTS) != 0)
                .findFirst()
                .map(entry -> entry.getKey().failureMessage(entry.getValue()))
                .orElse("");

        return new TieoutReport(failureMessage.isEmpty(), subjectLines, failureMessage);
    }

    private static BigDecimal cents(BigDecimal amount) {
        if (amount == null) {
            return ZERO_CENTS;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Long longOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static Integer intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String amountText(BigDecimal amount) {
        return cents(amount).toPlainString();
    }

    public record VoucherLedgerRow(
            String businessCode,
            String voucherCode,
            String accountPeriod,
            Integer businessType,
            Long subjectId,
            BigDecimal debitAmount,
            BigDecimal creditAmount) {

        public VoucherLedgerRow {
            businessCode = textOrEmpty(businessCode);
            voucherCode = textOrEmpty(voucherCode);
            accountPeriod = textOrEmpty(accountPeriod);
            businessType = intOrZero(businessType);
            subjectId = longOrZero(subjectId);
            debitAmount = cents(debitAmount);
            creditAmount = cents(creditAmount);
        }

        public VoucherLedgerRow(
                String businessCode,
                String voucherCode,
                String accountPeriod,
                Long subjectId,
                BigDecimal debitAmount,
                BigDecimal creditAmount) {
            this(businessCode, voucherCode, accountPeriod, 0, subjectId, debitAmount, creditAmount);
        }
    }

    public record TieoutReport(boolean balanced, List<SubjectLine> subjectLines, String failureMessage) {
        public TieoutReport {
            subjectLines = subjectLines == null ? List.of() : List.copyOf(subjectLines);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record SubjectLine(
            String businessCode,
            String accountPeriod,
            Long subjectId,
            BigDecimal debitAmount,
            BigDecimal creditAmount) {

        public SubjectLine {
            businessCode = textOrEmpty(businessCode);
            accountPeriod = textOrEmpty(accountPeriod);
            subjectId = longOrZero(subjectId);
            debitAmount = cents(debitAmount);
            creditAmount = cents(creditAmount);
        }
    }

    private record SubjectKey(String businessCode, String accountPeriod, Long subjectId)
            implements Comparable<SubjectKey> {

        private SubjectKey {
            businessCode = textOrEmpty(businessCode);
            accountPeriod = textOrEmpty(accountPeriod);
            subjectId = longOrZero(subjectId);
        }

        static SubjectKey from(VoucherLedgerRow row) {
            return new SubjectKey(row.businessCode(), row.accountPeriod(), row.subjectId());
        }

        SubjectLine toSubjectLine(AmountPair amountPair) {
            return new SubjectLine(
                    businessCode,
                    accountPeriod,
                    subjectId,
                    amountPair.debitAmount(),
                    amountPair.creditAmount());
        }

        @Override
        public int compareTo(SubjectKey other) {
            int businessCompare = businessCode.compareTo(other.businessCode);
            if (businessCompare != 0) {
                return businessCompare;
            }
            int periodCompare = accountPeriod.compareTo(other.accountPeriod);
            if (periodCompare != 0) {
                return periodCompare;
            }
            return subjectId.compareTo(other.subjectId);
        }
    }

    private record VoucherKey(String businessCode, String accountPeriod, String voucherCode)
            implements Comparable<VoucherKey> {

        private VoucherKey {
            businessCode = textOrEmpty(businessCode);
            accountPeriod = textOrEmpty(accountPeriod);
            voucherCode = textOrEmpty(voucherCode);
        }

        static VoucherKey from(VoucherLedgerRow row) {
            return new VoucherKey(row.businessCode(), row.accountPeriod(), row.voucherCode());
        }

        String failureMessage(AmountPair amountPair) {
            return "Voucher tie-out imbalance: businessCode=" + businessCode
                    + ", voucherCode=" + voucherCode
                    + ", accountPeriod=" + accountPeriod
                    + ", debit=" + amountText(amountPair.debitAmount())
                    + ", credit=" + amountText(amountPair.creditAmount())
                    + ", difference=" + amountText(amountPair.difference().abs());
        }

        @Override
        public int compareTo(VoucherKey other) {
            int businessCompare = businessCode.compareTo(other.businessCode);
            if (businessCompare != 0) {
                return businessCompare;
            }
            int periodCompare = accountPeriod.compareTo(other.accountPeriod);
            if (periodCompare != 0) {
                return periodCompare;
            }
            return voucherCode.compareTo(other.voucherCode);
        }
    }

    private static final class AmountPair {

        private BigDecimal debitAmount = ZERO_CENTS;
        private BigDecimal creditAmount = ZERO_CENTS;

        void add(BigDecimal debitAmount, BigDecimal creditAmount) {
            this.debitAmount = cents(this.debitAmount.add(cents(debitAmount)));
            this.creditAmount = cents(this.creditAmount.add(cents(creditAmount)));
        }

        BigDecimal debitAmount() {
            return debitAmount;
        }

        BigDecimal creditAmount() {
            return creditAmount;
        }

        BigDecimal difference() {
            return cents(debitAmount.subtract(creditAmount));
        }
    }
}
