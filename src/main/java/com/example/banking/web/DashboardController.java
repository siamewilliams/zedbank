package com.example.banking.web;

import com.example.banking.dto.response.AccountResponse;
import com.example.banking.entity.User;
import com.example.banking.repository.CustomerRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/web/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountService accountService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRoles().iterator().next().getName());

        customerRepository.findByUserUsername(user.getUsername()).ifPresent(c -> {
            var accounts = accountService.getByCustomer(c.getId());
            model.addAttribute("customerId", c.getId());
            model.addAttribute("accounts", accounts);
            BigDecimal total = accounts.stream()
                .map(AccountResponse::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("totalBalance", String.format("%,.2f", total));
        });
        return "dashboard";
    }
}