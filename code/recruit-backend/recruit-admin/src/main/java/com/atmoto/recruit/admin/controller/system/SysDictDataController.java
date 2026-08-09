package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典数据 Controller —— 字典管理
 * <p>管理字典数据的增删改查，支持按字典类型查询</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/dict/data")
public class SysDictDataController {

    private final ISysDictDataService dictDataService;

    /**
     * 分页查询字典数据
     */
    @GetMapping("/list")
    public AjaxResult list(SysDictData dictData, PageQuery pageQuery) {
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        int total = list.size();
        int from = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<SysDictData> pageList = from < total ? list.subList(from, to) : List.of();
        return AjaxResult.page(TableDataInfo.of(total, pageList));
    }

    /**
     * 根据字典类型查询所有字典数据（常用下拉选查询）
     */
    @GetMapping("/type/{dictType}")
    public AjaxResult dictType(@PathVariable String dictType) {
        List<SysDictData> list = dictDataService.selectDictDataByType(dictType);
        return AjaxResult.success(list);
    }

    /**
     * 查询字典数据详情
     */
    @GetMapping("/{dictCode}")
    public AjaxResult getInfo(@PathVariable Long dictCode) {
        SysDictData dictData = dictDataService.selectDictDataById(dictCode);
        return AjaxResult.success(dictData);
    }

    /**
     * 新增字典数据
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysDictData dictData) {
        int rows = dictDataService.insertDictData(dictData);
        return rows > 0 ? AjaxResult.success("新增字典数据成功") : AjaxResult.error("新增字典数据失败");
    }

    /**
     * 修改字典数据
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysDictData dictData) {
        int rows = dictDataService.updateDictData(dictData);
        return rows > 0 ? AjaxResult.success("修改字典数据成功") : AjaxResult.error("修改字典数据失败");
    }

    /**
     * 删除字典数据（批量）
     */
    @DeleteMapping("/{dictCodes}")
    public AjaxResult remove(@PathVariable Long[] dictCodes) {
        int rows = dictDataService.deleteDictDataByIds(dictCodes);
        return rows > 0 ? AjaxResult.success("删除字典数据成功") : AjaxResult.error("删除字典数据失败");
    }
}
