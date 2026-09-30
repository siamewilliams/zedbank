package com.example.banking.web;

import com.example.banking.dto.request.CreateAccountRequest;
import com.example.banking.entity.AccountType;
import com.example.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/accounts")
@RequiredArgsConstructor
public class AccountWebController {

    private final AccountService accountService;

    @GetMapping
    public String list(@RequestParam(required = false) Long customerId, Model model) {
        if (customerId != null) {
            model.addAttribute("accounts", accountService.getByCustomer(customerId));
            model.addAttribute("customerId", customerId);
        }
        return "accounts/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("createAccountRequest", new CreateAccountRequest());
        model.addAttribute("accountTypes", AccountType.values());
        return "accounts/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("createAccountRequest") CreateAccountRequest req,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", AccountType.values());
            return "accounts/create";
        }
        try {
            var created = accountService.createAccount(req);
            redirect.addFlashAttribute("message", "Account created: " + created.accountNumber());
            return "redirect:/web/accounts?customerId=" + req.getCustomerId();
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("accountTypes", AccountType.values());
            return "accounts/create";
        }
    }

    @GetMapping("/{accountNumber}")
    public String detail(@PathVariable String accountNumber, Model model) {
        model.addAttribute("account", accountService.getByAccountNumber(accountNumber));
        return "accounts/detail";
    }
}