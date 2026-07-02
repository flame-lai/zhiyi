# 智衣 ERP 文档目录

本目录统一存放项目文档。根目录只保留工程、部署和源码入口，需求、设计、接口、部署运维类文档都放在 `docs/` 下。

## 文档分类

| 分类 | 目录 | 说明 |
| --- | --- | --- |
| 需求文档 | [requirements/](requirements/) | 产品分析、完整需求、各业务模块需求、专项分析报告 |
| 设计文档 | [design/](design/) | 技术方案、实现设计、移动端重构设计 |
| 实施计划 | [plans/](plans/) | 后端基础设施、业务模块、移动端重构等开发计划 |
| API 文档 | [api/](api/) | 后端接口说明 |
| 部署运维 | [operations/](operations/) | 生产部署、Docker Compose、一键部署、备份回滚 |

## 常用入口

- [完整需求文档](requirements/full-requirements.md)
- [技术方案文档](design/technical-design.md)
- [API 文档](api/)
- [线上部署文档](operations/deployment.md)
- [移动端重构设计](design/mobile-redesign.md)

## 维护约定

- 新需求优先放到 `requirements/`，按业务模块拆分到 `requirements/modules/`。
- 技术选型、架构、接口契约之外的实现细节放到 `design/`。
- 可执行开发任务、阶段计划、迁移步骤放到 `plans/`。
- 线上部署、备份、回滚、排障放到 `operations/`。
- 不再在仓库根目录新增 Markdown 文档。
