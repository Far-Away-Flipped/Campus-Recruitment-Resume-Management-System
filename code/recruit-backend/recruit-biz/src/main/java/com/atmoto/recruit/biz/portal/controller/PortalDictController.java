package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门户端字典只读接口（公开访问）
 * <p>供学生门户前端按需加载字典（如工作地点 work_location），返回 [{label, value}]，
 * 避免前端硬编码地点/学历中文映射与后台字典脱钩导致显示字典码（历史 bug 根治）。
 * 查询已由 Mapper 层过滤启用（status='0'）且未软删（del_flag='0'）的字典项，匿名暴露安全。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/dict")
public class PortalDictController {

    private final ISysDictDataService dictDataService;

    /**
     * 按字典类型读取启用状态字典项，返回 [{label, value}] 结构
     *
     * @param dictType 字典类型编码，如 work_location / education_degree
     */
    @GetMapping("/data/{dictType}")
    public AjaxResult data(@PathVariable String dictType) {
        List<SysDictData> dictList = dictDataService.selectDictDataByType(dictType);
        List<Map<String, Object>> options = dictList.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.getDictLabel());
            m.put("value", d.getDictValue());
            // 字典项备注：如 apply_source 用作该渠道选填详情字段的显示名称
            m.put("remark", d.getRemark());
            return m;
        }).collect(Collectors.toList());
        return AjaxResult.success(options);
    }
}
