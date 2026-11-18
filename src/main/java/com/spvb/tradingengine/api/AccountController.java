package com.spvb.tradingengine.api;

import com.spvb.tradingengine.api.dto.DepositRequest;
import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.spvb.tradingengine.util.ApiConstants.USER_ID_HEADER;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AccountController {
    AccountService accountService;

    @PutMapping("/accounts/{userId}")
    public ResponseEntity<Void> openTradingAccount(@PathVariable @Positive final long userId) {
        accountService.openTradingAccount(userId);
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/accounts/{userId}/deposits")
    public ResponseEntity<Balance> depositAsset(
            @PathVariable @Positive final long userId,
            @Valid @RequestBody @NonNull final DepositRequest request
    ) {
        return ResponseEntity
                .ok(accountService.depositAsset(userId, request.asset(), request.amount()));
    }

    @GetMapping("/balances")
    public ResponseEntity<List<Balance>> getAccountBalances(
            @RequestHeader(USER_ID_HEADER) @Positive final long userId
    ) {
        return ResponseEntity
                .ok(accountService.getAccountBalances(userId));
    }
}
