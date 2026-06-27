package com.zhiyi.erp.module.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.report.dto.ReportRecordDTO;
import com.zhiyi.erp.module.report.dto.WageReportDTO;
import com.zhiyi.erp.module.report.entity.WorkReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkReportMapper extends BaseMapper<WorkReport> {

    @Select("SELECT wr.id, wr.order_id AS orderId, wr.process_id AS processId, " +
            "wr.qty, wr.price, wr.amount, wr.report_time AS reportTime, " +
            "o.order_no AS orderNo, o.style_id AS styleId, " +
            "s.style_code AS styleCode, s.show_mobile_price AS showMobilePrice, " +
            "p.name AS processName " +
            "FROM work_report wr " +
            "LEFT JOIN `order` o ON wr.order_id = o.id AND o.deleted = 0 " +
            "LEFT JOIN style s ON o.style_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN process p ON wr.process_id = p.id AND p.deleted = 0 " +
            "WHERE wr.employee_id = #{employeeId} AND wr.status = 1 " +
            "AND DATE(wr.report_time) >= #{startDate} " +
            "AND DATE(wr.report_time) <= #{endDate} " +
            "ORDER BY wr.report_time DESC")
    List<WageReportDTO> selectWageReports(@Param("employeeId") Long employeeId,
                                           @Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT wr.id, wr.report_time AS reportTime, wr.qty, wr.price, wr.amount, " +
            "o.order_no AS orderNo, " +
            "s.style_code AS styleCode, s.show_mobile_price AS showMobilePrice, " +
            "b.bundle_no AS bundleNo, " +
            "p.name AS processName " +
            "FROM work_report wr " +
            "LEFT JOIN `order` o ON wr.order_id = o.id AND o.deleted = 0 " +
            "LEFT JOIN style s ON o.style_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN bundle b ON wr.bundle_id = b.id " +
            "LEFT JOIN process p ON wr.process_id = p.id AND p.deleted = 0 " +
            "WHERE wr.employee_id = #{employeeId} AND wr.status = 1 " +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND DATE(wr.report_time) &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND DATE(wr.report_time) &lt;= #{endDate} " +
            "</if>" +
            "ORDER BY wr.report_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<ReportRecordDTO> selectReportRecords(@Param("employeeId") Long employeeId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM work_report wr " +
            "LEFT JOIN `order` o ON wr.order_id = o.id AND o.deleted = 0 " +
            "LEFT JOIN style s ON o.style_id = s.id AND s.deleted = 0 " +
            "WHERE wr.employee_id = #{employeeId} AND wr.status = 1 " +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND DATE(wr.report_time) &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND DATE(wr.report_time) &lt;= #{endDate} " +
            "</if>" +
            "</script>")
    long selectReportRecordsCount(@Param("employeeId") Long employeeId,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate);
}
