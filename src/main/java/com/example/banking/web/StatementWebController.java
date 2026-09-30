package com.example.banking.web;

import com.example.banking.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/web/statements")
@RequiredArgsConstructor
public class StatementWebController {

    private final StatementService statementService;

    @GetMapping
    public String view(@RequestParam(required = false) String accountNumber,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                       Model model) {
        if (accountNumber != null) {
            LocalDateTime start = from != null ? from : LocalDateTime.now().minusMonths(1);
            LocalDateTime end = to != null ? to : LocalDateTime.now();
            try {
                model.addAttribute("statement", statementService.generate(accountNumber, start, end));
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return "statements/view";
    }
}