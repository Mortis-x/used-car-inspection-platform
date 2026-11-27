package com.usedcar.platform.template;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.common.BizException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {

    private final InspectionTemplateMapper templateMapper;
    private final InspectionTemplateItemMapper itemMapper;

    public TemplateService(InspectionTemplateMapper templateMapper,
                           InspectionTemplateItemMapper itemMapper) {
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
    }

    public List<InspectionTemplate> listActive() {
        return templateMapper.selectList(Wrappers.<InspectionTemplate>lambdaQuery()
                .eq(InspectionTemplate::getStatus, 1)
                .orderByAsc(InspectionTemplate::getId));
    }

    public InspectionTemplate get(Long id) {
        InspectionTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(404, "检测模板不存在");
        }
        return template;
    }

    public List<InspectionTemplateItem> items(Long templateId) {
        get(templateId);
        return itemMapper.selectList(Wrappers.<InspectionTemplateItem>lambdaQuery()
                .eq(InspectionTemplateItem::getTemplateId, templateId)
                .orderByAsc(InspectionTemplateItem::getSort));
    }
}
