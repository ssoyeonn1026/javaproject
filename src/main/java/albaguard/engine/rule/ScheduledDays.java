package albaguard.engine.rule;

import albaguard.domain.Contract;

import java.time.LocalDate;

/** 소정근로일(계약 근무 요일이면서 계약 기간 안인 날) 판단. 주휴수당과 휴업수당이 같은 기준을 쓴다. */
final class ScheduledDays {

    private ScheduledDays() {
    }

    /** 계약 근무 요일이고, 계약 시작일 이후이며, 종료일(없으면 무제한) 이전이면 true. */
    static boolean isScheduled(Contract contract, LocalDate date) {
        if (!contract.workDays().contains(date.getDayOfWeek())) {
            return false;
        }
        if (date.isBefore(contract.startDate())) {
            return false;
        }
        return contract.endDate() == null || !date.isAfter(contract.endDate());
    }
}
