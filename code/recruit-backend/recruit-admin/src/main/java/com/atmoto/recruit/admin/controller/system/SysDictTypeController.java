package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.system.domain.SysDictType;
import com.atmoto.recruit.system.service.ISysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典类型 Controller —— 字典类型管理
 *
 * @author atmoto-recruit
 */
@RestController
@RequestMapping("/api/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final ISysDictTypeService dictTypeService;

    @GetMapping("/list")
    public AjaxResult list(SysDictType dictType) {
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        return AjaxResult.success(list);
    }

    @GetMapping("/{dictId}")
    public AjaxResult getInfo(@PathVariable Long dictId) {
        return AjaxResult.success(dictTypeService.selectDictTypeById(dictId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysDictType dictType) {
        return AjaxResult.success(dictTypeService.insertDictType(dictType));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysDictType dictType) {
        return AjaxResult.success(dictTypeService.updateDictType(dictType));
    }

    @DeleteMapping("/{dictIds}")
    public AjaxResult remove(@PathVariable Long[] dictIds) {
        return AjaxResult.success(dictTypeService.deleteDictTypeByIds(dictIds));
    }
}
