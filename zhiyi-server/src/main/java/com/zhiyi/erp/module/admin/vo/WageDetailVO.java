package com.zhiyi.erp.module.admin.vo;

import com.zhiyi.erp.module.admin.entity.WageOther;
import lombok.Data;

import java.util.List;

@Data
public class WageDetailVO {
    private List<WagePieceDetailVO> pieceDetails;
    private List<TimedWageVO> timedDetails;
    private List<WageOther> otherItems;
}
