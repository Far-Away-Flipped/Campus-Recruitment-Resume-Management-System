package com.atmoto.recruit.biz.admin.controller;

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
 * 管理端字典只读接口
 * <p>面向 HR 角色的下拉选项数据源（如岗位工作地点）。不依赖 /api/system/dict/**，
 * 因为该路径受 requireDirector() 拦截，普通 HR 角色不可访问。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dict")
public class AdminDictController {

    private final ISysDictDataService dictDataService;

    /**
     * 按字典类型读取启用状态的字典项，返回 [{label, value}] 结构
     *
     * @param dictType 字典类型编码，如 work_location
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
