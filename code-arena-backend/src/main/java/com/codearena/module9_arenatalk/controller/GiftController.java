package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.SendGiftRequestDTO;
import com.codearena.module9_arenatalk.entity.GiftTransaction;
import com.codearena.module9_arenatalk.service.GiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arenatalk/gifts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class GiftController {

    private final GiftService giftService;

    @PostMapping("/send")
    public ResponseEntity<GiftTransaction> sendGift(@RequestBody SendGiftRequestDTO request) {
        return ResponseEntity.ok(giftService.sendGift(request));
    }
}