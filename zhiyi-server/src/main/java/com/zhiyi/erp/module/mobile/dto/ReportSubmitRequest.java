package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ReportSubmitRequest {
    @NotBlank(message = "扫码类型不能为空")
    private String scanType;  // BUNDLE / CUTTING

    @NotBlank(message = "扫码内容不能为空")
    private String code;

    @NotEmpty(message = "请选择至少一个扎号")
    private List<Long> selectedBundleIds;

    @NotEmpty(message = "请选择至少一道工序")
    private List<Long> selectedProcessIds;
}
