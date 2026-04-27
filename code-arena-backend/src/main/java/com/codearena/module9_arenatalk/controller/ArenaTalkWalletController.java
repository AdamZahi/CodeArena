package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.entity.ArenaTalkWallet;
import com.codearena.module9_arenatalk.service.ArenaTalkWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arenatalk/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ArenaTalkWalletController {

    private final ArenaTalkWalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<ArenaTalkWallet> getWallet(
            @PathVariable String userId,
            @RequestParam(required = false) String userName
    ) {
        return ResponseEntity.ok(
                walletService.getOrCreateWallet(userId, userName != null ? userName : "Unknown")
        );
    }
}