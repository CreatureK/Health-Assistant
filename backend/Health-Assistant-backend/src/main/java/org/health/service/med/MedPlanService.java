package org.health.service.med;

import org.health.common.ResultCode;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用药计划服务
 */
@Service
public class MedPlanService {

    @Autowired
    private MedPlanMapper medPlanMapper;

    @Autowired
    private PlanTimesMapper planTimesMapper;

    @Autowired
    private PlanRepeatDaysMapper planRepeatDaysMapper;

    @Autowired
    private MedRecordMapper medRecordMapper;

    /**
     * 创建用药计划
     *
     * @param request 创建请求
     * @return 计划ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(CreatePlanRequest request) {
        Long userId = UserContext.getUserId();

        // 参数校验
        validatePlanRequest(request);

        // 创建计划
        MedPlan plan = new MedPlan();
        plan.setUserId(userId);
        plan.setName(request.getName());
        plan.setDosage(request.getDosage());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setRepeatType(request.getRepeatType());
        plan.setRemindEnabled(request.getRemindEnabled() != null ? request.getRemindEnabled() : false);

        medPlanMapper.insert(plan);

        // 创建时间点
        List<PlanTimes> timesList = new ArrayList<>();
        for (int i = 0; i < request.getTimes().size(); i++) {
            PlanTimes planTime = new PlanTimes();
            planTime.setPlanId(plan.getId());
            planTime.setTime(LocalTime.parse(request.getTimes().get(i)));
            planTime.setSortOrder(i);
            timesList.add(planTime);
        }
        if (!timesList.isEmpty()) {
            planTimesMapper.batchInsert(timesList);
        }

        // 如果是weekly类型，创建重复天数
        if ("weekly".equals(request.getRepeatType()) && request.getRepeatDays() != null && !request.getRepeatDays().isEmpty()) {
            List<PlanRepeatDays> daysList = request.getRepeatDays().stream()
                    .map(day -> {
                        PlanRepeatDays planDay = new PlanRepeatDays();
                        planDay.setPlanId(plan.getId());
                        planDay.setDayOfWeek(day);
                        return planDay;
                    })
                    .collect(Collectors.toList());
            planRepeatDaysMapper.batchInsert(daysList);
        }

        return plan.getId();
    }

    /**
     * 获取计划列表
     *
     * @param status 状态过滤（active|expired）
     * @param keyword 关键词搜索
     * @return 计划列表
     */
    public List<MedPlanVO> getPlanList(String status, String keyword) {
        Long userId = UserContext.getUserId();
        List<MedPlan> plans = medPlanMapper.selectByUserId(userId, status, keyword);

        return plans.stream().map(plan -> {
            MedPlanVO vo = new MedPlanVO();
            vo.setId(plan.getId());
            vo.setName(plan.getName());
            vo.setDosage(plan.getDosage());
            vo.setStartDate(plan.getStartDate());
            vo.setEndDate(plan.getEndDate());
            vo.setRepeatType(plan.getRepeatType());
            vo.setRemindEnabled(plan.getRemindEnabled());
            vo.setCreatedAt(plan.getCreatedAt());
            vo.setUpdatedAt(plan.getUpdatedAt());

            // 查询时间点
            List<PlanTimes> times = planTimesMapper.selectByPlanId(plan.getId());
            vo.setTimes(times.stream()
                    .map(t -> t.getTime().toString())
                    .collect(Collectors.toList()));

            // 如果是weekly类型，查询重复天数
            if ("weekly".equals(plan.getRepeatType())) {
                List<PlanRepeatDays> days = planRepeatDaysMapper.selectByPlanId(plan.getId());
                vo.setRepeatDays(days.stream()
                        .map(PlanRepeatDays::getDayOfWeek)
                        .collect(Collectors.toList()));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取计划详情
     *
     * @param id 计划ID
     * @return 计划详情
     */
    public MedPlanVO getPlanDetail(Long id) {
        Long userId = UserContext.getUserId();
        MedPlan plan = medPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        MedPlanVO vo = new MedPlanVO();
        vo.setId(plan.getId());
        vo.setName(plan.getName());
        vo.setDosage(plan.getDosage());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setRepeatType(plan.getRepeatType());
        vo.setRemindEnabled(plan.getRemindEnabled());
        vo.setCreatedAt(plan.getCreatedAt());
        vo.setUpdatedAt(plan.getUpdatedAt());

        // 查询时间点
        List<PlanTimes> times = planTimesMapper.selectByPlanId(plan.getId());
        vo.setTimes(times.stream()
                .map(t -> t.getTime().toString())
                .collect(Collectors.toList()));

        // 如果是weekly类型，查询重复天数
        if ("weekly".equals(plan.getRepeatType())) {
            List<PlanRepeatDays> days = planRepeatDaysMapper.selectByPlanId(plan.getId());
            vo.setRepeatDays(days.stream()
                    .map(PlanRepeatDays::getDayOfWeek)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * 更新计划
     *
     * @param id 计划ID
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, UpdatePlanRequest request) {
        Long userId = UserContext.getUserId();
        MedPlan plan = medPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        // 参数校验
        validatePlanRequest(request);

        // 更新计划基本信息
        plan.setName(request.getName());
        plan.setDosage(request.getDosage());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setRepeatType(request.getRepeatType());
        if (request.getRemindEnabled() != null) {
            plan.setRemindEnabled(request.getRemindEnabled());
        }
        medPlanMapper.update(plan);

        // 删除旧的时间点和重复天数
        planTimesMapper.deleteByPlanId(id);
        planRepeatDaysMapper.deleteByPlanId(id);

        // 创建新的时间点
        if (request.getTimes() != null && !request.getTimes().isEmpty()) {
            List<PlanTimes> timesList = new ArrayList<>();
            for (int i = 0; i < request.getTimes().size(); i++) {
                PlanTimes planTime = new PlanTimes();
                planTime.setPlanId(id);
                planTime.setTime(LocalTime.parse(request.getTimes().get(i)));
                planTime.setSortOrder(i);
                timesList.add(planTime);
            }
            planTimesMapper.batchInsert(timesList);
        }

        // 如果是weekly类型，创建新的重复天数
        if ("weekly".equals(request.getRepeatType()) && request.getRepeatDays() != null && !request.getRepeatDays().isEmpty()) {
            List<PlanRepeatDays> daysList = request.getRepeatDays().stream()
                    .map(day -> {
                        PlanRepeatDays planDay = new PlanRepeatDays();
                        planDay.setPlanId(id);
                        planDay.setDayOfWeek(day);
                        return planDay;
                    })
                    .collect(Collectors.toList());
            planRepeatDaysMapper.batchInsert(daysList);
        }

        // 重建 today+1 之后的点位记录
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        medRecordMapper.deleteByPlanIdAndDate(id, tomorrow);
        // 重新生成点位记录（从明天开始到结束日期）
        regenerateRecordsFromDate(id, tomorrow, plan.getEndDate());
    }

    /**
     * 删除计划
     *
     * @param id 计划ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        Long userId = UserContext.getUserId();
        MedPlan plan = medPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        // 软删除
        medPlanMapper.deleteById(id);
    }

    /**
     * 更新提醒开关
     *
     * @param id 计划ID
     * @param remindEnabled 提醒开关
     */
    public void updateRemindEnabled(Long id, Boolean remindEnabled) {
        Long userId = UserContext.getUserId();
        MedPlan plan = medPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        medPlanMapper.updateRemindEnabled(id, remindEnabled);
    }

    /**
     * 从指定日期开始重新生成点位记录
     */
    private void regenerateRecordsFromDate(Long planId, LocalDate startDate, LocalDate endDate) {
        MedPlan plan = medPlanMapper.selectById(planId);
        if (plan == null) {
            return;
        }

        // 获取计划的时间点
        List<PlanTimes> times = planTimesMapper.selectByPlanId(planId);
        if (times.isEmpty()) {
            return;
        }

        // 获取重复天数（如果是weekly类型）
        List<PlanRepeatDays> repeatDays = null;
        if ("weekly".equals(plan.getRepeatType())) {
            repeatDays = planRepeatDaysMapper.selectByPlanId(planId);
            if (repeatDays == null || repeatDays.isEmpty()) {
                return;
            }
        }

        // 生成点位记录
        List<MedRecord> recordsToInsert = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // 判断该日期是否需要生成记录
            boolean shouldGenerate = false;
            if ("daily".equals(plan.getRepeatType())) {
                shouldGenerate = true;
            } else if ("weekly".equals(plan.getRepeatType()) && repeatDays != null) {
                DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
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
                    String key = planId + "_" + currentDate + "_" + planTime.getTime();
                    if (!existingKeys.contains(key)) {
                        // 检查是否已存在记录
                        MedRecord existing = medRecordMapper.selectByPlanDateTime(
                                planId, currentDate, planTime.getTime().toString());
                        if (existing == null) {
                            MedRecord record = new MedRecord();
                            record.setPlanId(planId);
                            record.setDate(currentDate);
                            record.setTime(planTime.getTime());
                            record.setStatus("todo");
                            recordsToInsert.add(record);
                            existingKeys.add(key);
                        }
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        // 批量插入记录
        if (!recordsToInsert.isEmpty()) {
            medRecordMapper.batchInsert(recordsToInsert);
        }
    }

    /**
     * 校验计划请求参数
     */
    private void validatePlanRequest(PlanRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("药品名称不能为空");
        }
        if (request.getName().length() > 50) {
            throw new RuntimeException("药品名称长度不能超过50字符");
        }
        if (request.getDosage() == null || request.getDosage().trim().isEmpty()) {
            throw new RuntimeException("剂量不能为空");
        }
        if (request.getTimes() == null || request.getTimes().isEmpty()) {
            throw new RuntimeException("至少需要设置一个服用时间");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("开始日期和结束日期不能为空");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }
        if (!"daily".equals(request.getRepeatType()) && !"weekly".equals(request.getRepeatType())) {
            throw new RuntimeException("重复类型必须是daily或weekly");
        }
        if ("weekly".equals(request.getRepeatType())) {
            if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
                throw new RuntimeException("weekly类型必须至少设置一个重复天数");
            }
            for (Integer day : request.getRepeatDays()) {
                if (day < 0 || day > 6) {
                    throw new RuntimeException("重复天数必须在0-6之间（0=周日，6=周六）");
                }
            }
        }
    }

    /**
     * 计划请求基类
     */
    public static abstract class PlanRequest {
        protected String name;
        protected String dosage;
        protected List<String> times;
        protected LocalDate startDate;
        protected LocalDate endDate;
        protected String repeatType;
        protected List<Integer> repeatDays;
        protected Boolean remindEnabled;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public List<String> getTimes() { return times; }
        public void setTimes(List<String> times) { this.times = times; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getRepeatType() { return repeatType; }
        public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
        public List<Integer> getRepeatDays() { return repeatDays; }
        public void setRepeatDays(List<Integer> repeatDays) { this.repeatDays = repeatDays; }
        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    }

    /**
     * 创建计划请求
     */
    public static class CreatePlanRequest extends PlanRequest {
    }

    /**
     * 更新计划请求
     */
    public static class UpdatePlanRequest extends PlanRequest {
    }

    /**
     * 计划视图对象
     */
    public static class MedPlanVO {
        private Long id;
        private String name;
        private String dosage;
        private List<String> times;
        private LocalDate startDate;
        private LocalDate endDate;
        private String repeatType;
        private List<Integer> repeatDays;
        private Boolean remindEnabled;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public List<String> getTimes() { return times; }
        public void setTimes(List<String> times) { this.times = times; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getRepeatType() { return repeatType; }
        public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
        public List<Integer> getRepeatDays() { return repeatDays; }
        public void setRepeatDays(List<Integer> repeatDays) { this.repeatDays = repeatDays; }
        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}

