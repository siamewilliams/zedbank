package com.example.banking.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionListener {

    @Async("notificationExecutor")
    @EventListener
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[NOTIFY] Transaction {} completed: {} {} {}",
            event.reference(), event.amount(), event.currency(), event.type());
    }
}