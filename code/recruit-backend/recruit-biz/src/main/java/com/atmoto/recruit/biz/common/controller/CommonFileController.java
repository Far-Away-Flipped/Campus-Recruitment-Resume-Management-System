package com.atmoto.recruit.biz.common.controller;

import com.atmoto.recruit.biz.common.service.PreviewTicketService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共文件预览 Controller（S-11 安全修复）
 * <p>通过一次性ticket返回文件流，URL中不带token。
 * ticket由HR端生成（60秒有效），用后即焚。</p>
 * <p>ticket 的签发/消费逻辑统一在 {@link PreviewTicketService}，学生端复用同一服务。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequestMapping("/api/common/file")
public class CommonFileController {

    private final PreviewTicketService previewTicketService;

    public CommonFileController(PreviewTicketService previewTicketService) {
        this.previewTicketService = previewTicketService;
    }

    /**
     * 通过一次性ticket预览文件
     * <p>
     * 安全设计：
     * - URL中不带token，仅凭ticket鉴权
     * - ticket是一次性的：读取后立即消费（删除）
     * - ticket 60秒自动过期
     * - 路径穿越防护
     * </p>
     *
     * @param ticket   一次性预览ticket
     * @param response HTTP响应
     */
    @GetMapping("/preview")
    public void preview(@RequestParam("ticket") String ticket, HttpServletResponse response) {
        previewTicketService.consumeAndStream(ticket, response, false);
    }
}
