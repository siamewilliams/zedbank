package com.example.banking.web;

import com.example.banking.dto.request.DepositRequest;
import com.example.banking.dto.request.TransferRequest;
import com.example.banking.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/transactions")
@RequiredArgsConstructor
public class TransactionWebController {

    private final TransactionService transactionService;

    @GetMapping("/deposit")
    public String depositForm(Model model) {
        model.addAttribute("depositRequest", new DepositRequest());
        return "transactions/deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("depositRequest") DepositRequest req,
                          BindingResult result,
                          @AuthenticationPrincipal UserDetails user,
                          Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return "transactions/deposit";
        try {
            var txn = transactionService.deposit(req, user.getUsername(), "web");
            redirect.addFlashAttribute("message", "Deposit successful: " + txn.reference());
            return "redirect:/web/transactions";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "transactions/deposit";
        }
    }

    @GetMapping("/transfer")
    public String transferForm(Model model) {
        model.addAttribute("transferRequest", new TransferRequest());
        return "transactions/transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest req,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetails user,
                           Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return "transactions/transfer";
        try {
            var txn = transactionService.transfer(req, user.getUsername(), "web");
            redirect.addFlashAttribute("message", "Transfer successful: " + txn.reference());
            return "redirect:/web/transactions";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "transactions/transfer";
        }
    }

    @GetMapping
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("transactions", transactionService.allHistory(
            PageRequest.of(page, 10, Sort.by("createdAt").descending())));
        return "transactions/history";
    }
}