-- 检测项快照补充权重：创建检测单时从模板带入，保证后续模板调整不影响历史评分
ALTER TABLE inspection_item
    ADD COLUMN weight INT NOT NULL DEFAULT 1 COMMENT '模板项权重快照' AFTER item_name;

UPDATE inspection_item i
    JOIN inspection_template_item t ON t.id = i.template_item_id
SET i.weight = t.weight;
