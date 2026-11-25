package albaguard.service;

import java.nio.file.Path;
import java.time.YearMonth;

/**
 * 기간별 리포트 파일을 생성한다.
 * 구현: HtmlReportService (프론트엔드). 내부에서 다른 네 서비스를 호출해 결과를 모은다.
 */
public interface ReportService {

    /**
     * 기간 내 근무 기록, 급여 계산, 비교, 위반 항목을 HTML 파일 하나로 저장한다.
     *
     * @param from 시작 월 (포함)
     * @param to   종료 월 (포함)
     * @param dir  파일을 저장할 폴더
     * @return 생성된 파일의 경로
     * @throws ValidationException 시작 월이 종료 월보다 늦은 경우 등 기간이 올바르지 않을 때
     */
    Path export(YearMonth from, YearMonth to, Path dir);
}
