-- 创建数据库
CREATE DATABASE IF NOT EXISTS zhiyi_erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhiyi_erp;

-- 工厂/租户表
CREATE TABLE `factory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '工厂名称',
  `code` VARCHAR(50) NOT NULL COMMENT '工厂编码',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `contact` VARCHAR(50) COMMENT '联系人',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂/租户表';

-- 员工档案
CREATE TABLE `employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_no` VARCHAR(20) NOT NULL COMMENT '工号',
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20),
  `role_id` BIGINT COMMENT '关联角色',
  `login_account` VARCHAR(50) COMMENT 'Web端登录账号',
  `login_password` VARCHAR(100) COMMENT 'BCrypt加密密码',
  `login_code` VARCHAR(100) COMMENT '移动端登录码',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_no` (`factory_id`, `employee_no`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

-- 角色表
CREATE TABLE `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE `menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `code` VARCHAR(100) NOT NULL COMMENT '菜单编码',
  `path` VARCHAR(200) COMMENT '路由路径',
  `component` VARCHAR(200) COMMENT '前端组件路径',
  `icon` VARCHAR(50) COMMENT '图标',
  `type` TINYINT COMMENT '1目录 2菜单 3按钮',
  `permission` VARCHAR(100) COMMENT '权限标识',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联
CREATE TABLE `role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 工序库
CREATE TABLE `process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `category` VARCHAR(50) COMMENT '分类',
  `name` VARCHAR(50) NOT NULL COMMENT '工序名称',
  `ref_price` DECIMAL(12,2) COMMENT '参考工价',
  `sort` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序库';

-- 款号表
CREATE TABLE `style` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `style_code` VARCHAR(50) NOT NULL COMMENT '款号编码',
  `style_name` VARCHAR(100) NOT NULL COMMENT '款式名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `style_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号表';

-- 款号标准工序路由
CREATE TABLE `style_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `style_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `ref_price` DECIMAL(12,2) COMMENT '参考工价',
  `sort` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号标准工序路由';

-- 款号标准物料清单
CREATE TABLE `style_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `style_id` BIGINT NOT NULL,
  `material_name` VARCHAR(100) NOT NULL COMMENT '物料名称',
  `type` TINYINT COMMENT '1原料 2辅料',
  `unit_qty` DECIMAL(12,4) COMMENT '单件用量',
  `unit` VARCHAR(20) COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号标准物料清单';

-- 客户表
CREATE TABLE `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `customer_code` VARCHAR(20) NOT NULL COMMENT '客户编号',
  `name` VARCHAR(100) NOT NULL,
  `contact` VARCHAR(50),
  `phone` VARCHAR(20),
  `address` VARCHAR(200),
  `settle_type` VARCHAR(50) COMMENT '结算方式',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `customer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 供应商表
CREATE TABLE `supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `supplier_code` VARCHAR(20) NOT NULL COMMENT '供应商编号',
  `name` VARCHAR(100) NOT NULL,
  `contact` VARCHAR(50),
  `phone` VARCHAR(20),
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 订单主表
CREATE TABLE `order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `customer_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(12,2) COMMENT '订单金额',
  `delivery_date` DATE COMMENT '计划交期',
  `status` TINYINT DEFAULT 0 COMMENT '0待确认 1已确认 2生产中 3已完成 4已出货 5已关闭',
  `remark` VARCHAR(500),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_no` (`factory_id`, `order_no`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单明细行
CREATE TABLE `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `style_id` BIGINT NOT NULL,
  `color` VARCHAR(50) COMMENT '颜色',
  `size` VARCHAR(20) COMMENT '尺码',
  `qty` INT COMMENT '数量',
  `unit_price` DECIMAL(12,2) COMMENT '单价',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细行';

-- 订单工序
CREATE TABLE `order_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `process_price` DECIMAL(12,2) COMMENT '工价',
  `sort` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单工序';

-- 订单物料
CREATE TABLE `order_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `material_name` VARCHAR(100) NOT NULL,
  `type` TINYINT COMMENT '1原料 2辅料',
  `unit_qty` DECIMAL(12,4),
  `unit` VARCHAR(20),
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物料';

-- 出货记录
CREATE TABLE `shipment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `qty` INT NOT NULL COMMENT '出货数量',
  `shipment_date` DATE NOT NULL,
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出货记录';

-- 裁床计划
CREATE TABLE `cutting_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `fabric` VARCHAR(100) COMMENT '裁剪面料',
  `bed_no` INT NOT NULL COMMENT '床次',
  `layers` INT COMMENT '铺料层数',
  `plan_qty` INT COMMENT '总计划件数',
  `actual_qty` INT COMMENT '总实际件数',
  `bundle_count` INT COMMENT '总扎数',
  `cutting_date` DATE COMMENT '裁床日期',
  `barcode` VARCHAR(100) COMMENT '订单条形码内容',
  `status` TINYINT DEFAULT 0 COMMENT '0待裁剪 1已裁剪 2已开菲',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁床计划';

-- 裁床计划配比明细
CREATE TABLE `cutting_plan_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cutting_plan_id` BIGINT NOT NULL,
  `part` VARCHAR(200) COMMENT '部位',
  `color` VARCHAR(50) COMMENT '颜色',
  `size` VARCHAR(20) COMMENT '尺码',
  `qty_per_bundle` INT COMMENT '每扎件数',
  `ratio` INT COMMENT '配比',
  `plan_qty` INT COMMENT '计划成衣件数',
  `plan_pieces` INT COMMENT '计划裁片片数',
  `actual_qty` INT COMMENT '实际成衣件数',
  `actual_pieces` INT COMMENT '实际裁片片数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cutting_plan_id` (`cutting_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁床计划配比明细';

-- 款号裁剪部位配置
CREATE TABLE `style_part` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `style_id` BIGINT NOT NULL,
  `part_name` VARCHAR(100) NOT NULL COMMENT '部位名称',
  `fabric` VARCHAR(100) COMMENT '面料',
  `qty_per_piece` INT COMMENT '单件裁片数',
  `remark` VARCHAR(200) COMMENT '备注',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号裁剪部位配置';

-- 订单颜色尺码明细
CREATE TABLE `order_color_size` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `color` VARCHAR(50) COMMENT '颜色',
  `size` VARCHAR(20) COMMENT '尺码',
  `qty` INT COMMENT '数量',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单颜色尺码明细';

-- 扎号表
CREATE TABLE `bundle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `cutting_plan_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `color` VARCHAR(50) COMMENT '颜色',
  `size` VARCHAR(20) COMMENT '尺码',
  `fabric` VARCHAR(100) COMMENT '面料',
  `part` VARCHAR(200) COMMENT '部位',
  `bundle_no` VARCHAR(20) NOT NULL COMMENT '扎号',
  `global_bundle_no` VARCHAR(100) NOT NULL COMMENT '全局唯一标识',
  `qty` INT NOT NULL COMMENT '成衣件数',
  `cutting_pieces` INT COMMENT '裁片片数',
  `cutting_date` DATE COMMENT '裁床日期',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_no` (`global_bundle_no`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_cutting_plan_id` (`cutting_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扎号表';

-- 报工记录
CREATE TABLE `work_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `bundle_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `qty` INT NOT NULL COMMENT '件数',
  `price` DECIMAL(12,2) NOT NULL COMMENT '工价',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `report_type` TINYINT DEFAULT 1 COMMENT '1扫码报工 2裁床报工 3管理员补录',
  `status` TINYINT DEFAULT 1 COMMENT '1有效 0已清除',
  `report_time` DATETIME COMMENT '报工时间',
  `clear_reason` VARCHAR(200) COMMENT '清除原因',
  `supplement_flag` TINYINT DEFAULT 0 COMMENT '是否补录',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_bundle_id` (`bundle_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工记录';

-- 计时工资
CREATE TABLE `timed_wage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `work_date` DATE NOT NULL COMMENT '工作日期',
  `hours` DECIMAL(6,2) NOT NULL COMMENT '工时',
  `hourly_rate` DECIMAL(12,2) NOT NULL COMMENT '时薪',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `work_content` VARCHAR(200) COMMENT '工作内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计时工资';

-- 其他工资项目
CREATE TABLE `wage_other` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `year_month` VARCHAR(7) NOT NULL COMMENT '月份',
  `item_name` VARCHAR(50) NOT NULL COMMENT '项目名称',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_month` (`factory_id`, `year_month`),
  KEY `idx_employee_month` (`employee_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他工资项目';

-- 工资汇总
CREATE TABLE `wage_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `year_month` VARCHAR(7) NOT NULL COMMENT '月份',
  `piece_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '计件工资合计',
  `timed_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '计时工资合计',
  `other_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '其他项目合计',
  `total_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '应发合计',
  `status` TINYINT DEFAULT 0 COMMENT '0待审核 1已审核 2已发放',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_emp_month` (`factory_id`, `employee_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资汇总';

-- 收支记录
CREATE TABLE `payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_id` BIGINT COMMENT '关联订单',
  `type` TINYINT NOT NULL COMMENT '1收入 2支出',
  `amount` DECIMAL(12,2) NOT NULL,
  `payment_date` DATE NOT NULL,
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支记录';

-- 断层预警
CREATE TABLE `anomaly_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `bundle_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `front_process_id` BIGINT COMMENT '前置工序',
  `back_process_id` BIGINT COMMENT '后置工序',
  `report_employee_id` BIGINT COMMENT '后置工序报工人',
  `status` TINYINT DEFAULT 0 COMMENT '0待处理 1已忽略 2已清除 3已补录',
  `handle_type` TINYINT COMMENT '1忽略 2清除后置 3补录前置',
  `handle_by` BIGINT COMMENT '处理人',
  `handle_time` DATETIME COMMENT '处理时间',
  `handle_remark` VARCHAR(200) COMMENT '处理备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_status` (`factory_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='断层预警';

-- 操作日志
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `module` VARCHAR(50) COMMENT '模块',
  `action` VARCHAR(50) COMMENT '操作',
  `target_id` BIGINT COMMENT '目标ID',
  `target_type` VARCHAR(50) COMMENT '目标类型',
  `content` VARCHAR(500) COMMENT '操作内容',
  `operator_id` BIGINT COMMENT '操作人',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 初始数据：默认工厂
INSERT INTO `factory` (`name`, `code`, `status`) VALUES ('默认工厂', 'DEFAULT', 1);

-- 初始数据：超级管理员角色
INSERT INTO `role` (`factory_id`, `name`, `code`, `sort`, `status`) VALUES (1, '超级管理员', 'super_admin', 1, 1);

-- 初始数据：默认超级管理员（密码: admin123 BCrypt加密）
INSERT INTO `employee` (`factory_id`, `employee_no`, `name`, `role_id`, `login_account`, `login_password`, `login_code`, `status`)
VALUES (1, 'A001', '管理员', 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN001', 1);
