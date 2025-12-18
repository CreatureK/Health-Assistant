package org.health.service.med;

import org.health.common.UserContext;
import org.health.entity.med.MedPlan;
import org.health.entity.med.MedRecord;
import org.health.entity.med.PlanRepeatDays;
import org.health.entity.med.PlanTimes;
import org.health.mapper.med.MedPlanMapper;
import org.health.mapper.med.MedRecordMapper;
import org.health.mapper.med.PlanRepeatDaysMapper;
import org.health.mapper.med.PlanTimesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 今日用药服务
 */
@Service
public class MedTodayService {

    @Autowired
    private MedRecordMapper medRecordMapper;

    @Autowired
    private MedPlanMapper medPlanMapper;

    @Autowired
    private PlanTimesMapper planTimesMapper;

    @Autowired
    private PlanRepeatDaysMapper planRepeatDaysMapper;

    /**
     * 获取当天点位
     *
     * @param date 日期（可选，默认为今天）
     * @return 当天点位列表
     */
    public TodayRecordsVO getTodayRecords(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        Long userId = UserContext.getUserId();

        // 确保点位已生成
        ensureRecords(date);

        // 查询当天的记录
        List<MedRecord> records = medRecordMapper.selectByDate(userId, date);

        TodayRecordsVO vo = new TodayRecordsVO();
        vo.setDate(date);
        vo.setList(records.stream().map(record -> {
            TodayRecordVO recordVO = new TodayRecordVO();
            recordVO.setId(record.getId());
            recordVO.setPlanId(record.getPlanId());
            recordVO.setPlanName(record.getPlanName());
            recordVO.setDosage(record.getDosage());
            recordVO.setDate(record.getDate());
            recordVO.setTime(record.getTime().toString());
            recordVO.setStatus(record.getStatus());
            recordVO.setActionAt(record.getActionAt());
            recordVO.setNote(record.getNote());
            return recordVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 确保生成点位
     *
     * @param date 日期
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureRecords(LocalDate date) {
        Long userId = UserContext.getUserId();
        // 查询用户的所有有效计划
        List<MedPlan> plans = medPlanMapper.selectByUserId(userId, null, null);

        // 过滤出在指定日期有效的计划
        List<MedPlan> validPlans = plans.stream()
                .filter(plan -> !date.isBefore(plan.getStartDate()) && !date.isAfter(plan.getEndDate()))
                .collect(Collectors.toList());

        // 生成点位记录
        List<MedRecord> recordsToInsert = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();

        for (MedPlan plan : validPlans) {
            // 获取计划的时间点
            List<PlanTimes> times = planTimesMapper.selectByPlanId(plan.getId());

            // 判断该日期是否需要生成记录
            boolean shouldGenerate = false;
            if ("daily".equals(plan.getRepeatType())) {
                // daily类型：每天都生成
                shouldGenerate = true;
            } else if ("weekly".equals(plan.getRepeatType())) {
                // weekly类型：检查是否在重复天数中
                List<PlanRepeatDays> repeatDays = planRepeatDaysMapper.selectByPlanId(plan.getId());
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                int dayValue = dayOfWeek.getValue() % 7; // 转换为0-6格式（0=周日）
                for (PlanRepeatDays repeatDay : repeatDays) {
                    if (repeatDay.getDayOfWeek() == dayValue) {
                        shouldGenerate = true;
                        break;
                    }
                }
            }

            if (shouldGenerate) {
                // 为每个时间点生成记录
                for (PlanTimes planTime : times) {
                    String key = plan.getId() + "_" + date + "_" + planTime.getTime();
                    if (!existingKeys.contains(key)) {
                        // 检查是否已存在记录
                        MedRecord existing = medRecordMapper.selectByPlanDateTime(
                                plan.getId(), date, planTime.getTime().toString());
                        if (existing == null) {
                            MedRecord record = new MedRecord();
                            record.setPlanId(plan.getId());
                            record.setDate(date);
                            record.setTime(planTime.getTime());
                            record.setStatus("todo");
                            recordsToInsert.add(record);
                            existingKeys.add(key);
                        }
                    }
                }
            }
        }

        // 批量插入记录
        if (!recordsToInsert.isEmpty()) {
            medRecordMapper.batchInsert(recordsToInsert);
        }
    }

    /**
     * 今日记录视图对象
     */
    public static class TodayRecordsVO {
        private LocalDate date;
        private List<TodayRecordVO> list;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public List<TodayRecordVO> getList() { return list; }
        public void setList(List<TodayRecordVO> list) { this.list = list; }
    }

    /**
     * 今日记录项视图对象
     */
    public static class TodayRecordVO {
        private Long id;
        private Long planId;
        private String planName;
        private String dosage;
        private LocalDate date;
        private String time;
        private String status;
        private java.time.LocalDateTime actionAt;
        private String note;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getActionAt() { return actionAt; }
        public void setActionAt(java.time.LocalDateTime actionAt) { this.actionAt = actionAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}

