package com.enroll.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级轮次 DTO（API 出参专用）
 *
 * 替代 periods JSON 数组里的每个对象
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ClassRoundDTO implements Serializable {

    private Integer roundNum;      // 轮次编号（1/2/3...）
    private LocalDateTime periodStart;  // 报名开始时间
    private LocalDateTime periodEnd;    // 报名结束时间

    public ClassRoundDTO() {}

    public ClassRoundDTO(Integer roundNum, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.roundNum = roundNum;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public Integer getRoundNum() { return roundNum; }
    public void setRoundNum(Integer roundNum) { this.roundNum = roundNum; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer roundNum;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;

        public Builder roundNum(Integer v) { this.roundNum = v; return this; }
        public Builder periodStart(LocalDateTime v) { this.periodStart = v; return this; }
        public Builder periodEnd(LocalDateTime v) { this.periodEnd = v; return this; }

        public ClassRoundDTO build() {
            ClassRoundDTO dto = new ClassRoundDTO();
            dto.setRoundNum(roundNum);
            dto.setPeriodStart(periodStart);
            dto.setPeriodEnd(periodEnd);
            return dto;
        }
    }
}
