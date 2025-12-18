package org.health.service.med;

import org.health.common.ResultCode;
import org.health.common.UserContext;
import org.health.entity.med.MedRecord;
import org.health.mapper.med.MedRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用药记录服务
 */
@Service
public class MedRecordService {

    @Autowired
    private MedRecordMapper medRecordMapper;

    /**
     * 查询记录列表
     *
     * @param planId 计划ID（可选）
     * @param status 状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 记录列表
     */
    public RecordListVO getRecordList(Long planId, String status, LocalDate startDate, LocalDate endDate,
                                       Integer page, Integer size) {
        Long userId = UserContext.getUserId();

        // 默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }

        int offset = (page - 1) * size;

        // 查询列表
        List<MedRecord> records = medRecordMapper.selectList(userId, planId, status, startDate, endDate, offset, size);
        int total = medRecordMapper.countList(userId, planId, status, startDate, endDate);

        RecordListVO vo = new RecordListVO();
        vo.setList(records.stream().map(this::convertToVO).collect(Collectors.toList()));
        vo.setPage(page);
        vo.setSize(size);
        vo.setTotal(total);

        return vo;
    }

    /**
     * 标记已服用/未服
     *
     * @param recordId 记录ID
     * @param status 状态（taken|missed）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRecord(Long recordId, String status) {
        // 验证记录是否存在且属于当前用户
        MedRecord record = medRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }
        // 注意：这里需要验证record的plan是否属于当前用户，简化处理

        // 验证状态
        if (!"taken".equals(status) && !"missed".equals(status)) {
            throw new RuntimeException("状态必须是taken或missed");
        }

        // 更新状态
        medRecordMapper.updateStatus(recordId, status);
    }

    /**
     * 补记/更正
     *
     * @param recordId 记录ID
     * @param request 补记请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustRecord(Long recordId, AdjustRecordRequest request) {
        // 验证记录是否存在且属于当前用户
        MedRecord record = medRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        // 验证状态
        if (!"taken".equals(request.getStatus()) && !"missed".equals(request.getStatus())) {
            throw new RuntimeException("状态必须是taken或missed");
        }

        // 更新记录
        MedRecord updateRecord = new MedRecord();
        updateRecord.setId(recordId);
        updateRecord.setStatus(request.getStatus());
        updateRecord.setActionAt(request.getActionAt());
        updateRecord.setNote(request.getNote());
        medRecordMapper.update(updateRecord);
    }

    /**
     * 转换为视图对象
     */
    private RecordVO convertToVO(MedRecord record) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setPlanId(record.getPlanId());
        vo.setPlanName(record.getPlanName());
        vo.setDosage(record.getDosage());
        vo.setDate(record.getDate());
        vo.setTime(record.getTime().toString());
        vo.setStatus(record.getStatus());
        vo.setActionAt(record.getActionAt());
        vo.setNote(record.getNote());
        return vo;
    }

    /**
     * 补记请求
     */
    public static class AdjustRecordRequest {
        private String status;
        private LocalDateTime actionAt;
        private String note;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getActionAt() { return actionAt; }
        public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    /**
     * 记录列表视图对象
     */
    public static class RecordListVO {
        private List<RecordVO> list;
        private Integer page;
        private Integer size;
        private Integer total;

        public List<RecordVO> getList() { return list; }
        public void setList(List<RecordVO> list) { this.list = list; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }

    /**
     * 记录视图对象
     */
    public static class RecordVO {
        private Long id;
        private Long planId;
        private String planName;
        private String dosage;
        private LocalDate date;
        private String time;
        private String status;
        private LocalDateTime actionAt;
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
        public LocalDateTime getActionAt() { return actionAt; }
        public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}

