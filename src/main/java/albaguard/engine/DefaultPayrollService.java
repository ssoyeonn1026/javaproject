package albaguard.engine;

import albaguard.domain.Contract;
import albaguard.domain.PayLine;
import albaguard.domain.PayrollResult;
import albaguard.domain.WorkRecord;
import albaguard.engine.rule.PayContext;
import albaguard.engine.rule.PayRule;
import albaguard.service.PayrollService;
import albaguard.service.RecordService;
import albaguard.service.ValidationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * PayrollService 구현. 저장된 근로 조건과 근무 기록을 모아 사업장 규모에 맞는 수당 규칙을 모두 적용하고
 * 항목별 결과(PayLine)와 합계를 돌려준다.
 */
public class DefaultPayrollService implements PayrollService {

    private final RecordService recordService;

    public DefaultPayrollService(RecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    public PayrollResult calculate(YearMonth month) {
        if (month == null) {
            throw new ValidationException("계산할 월을 선택해 주세요.");
        }
        Contract contract = recordService.getContract()
                .orElseThrow(() -> new ValidationException("근로 조건을 먼저 저장해 주세요."));

        List<WorkRecord> records = recordService.getRecords(month);
        PayContext ctx = new PayContext(contract, month, records, previousMonthTail(month));

        List<PayLine> lines = new ArrayList<>();
        for (PayRule rule : RuleSets.payRules(contract.businessSize())) {
            lines.addAll(rule.apply(ctx));
        }

        int totalWorkMinutes = records.stream().mapToInt(WorkTime::workMinutes).sum();
        int total = lines.stream().mapToInt(PayLine::amount).sum();
        return new PayrollResult(month, totalWorkMinutes, lines, total);
    }

    /**
     * 이번 달 1일이 속한 주(월~일)에서 지난달에 해당하는 날의 근무 기록.
     * 월초에 걸친 주는 일요일이 속한 이번 달에 계산하므로, 주휴수당 개근 판단과 주 연장 계산에 필요하다.
     */
    private List<WorkRecord> previousMonthTail(YearMonth month) {
        LocalDate weekStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!weekStart.isBefore(month.atDay(1))) {
            return List.of();
        }
        return recordService.getRecords(month.minusMonths(1)).stream()
                .filter(r -> !r.date().isBefore(weekStart))
                .toList();
    }
}
