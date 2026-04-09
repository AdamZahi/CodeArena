package com.codearena.user.controller;

import com.codearena.user.dto.CustomizationItemDTO;
import com.codearena.user.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/customization")
public class CustomizationItemController {

    private final CustomizationService customizationService;

    @GetMapping
    public ResponseEntity<List<CustomizationItemDTO>> getAll() {
        return ResponseEntity.ok(customizationService.getAllItems());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CustomizationItemDTO>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(customizationService.getItemsByType(type));
    }

    @PostMapping
    public ResponseEntity<CustomizationItemDTO> create(@RequestBody CustomizationItemDTO dto) {
        return ResponseEntity.ok(customizationService.createItem(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomizationItemDTO> update(@PathVariable Long id, @RequestBody CustomizationItemDTO dto) {
        return ResponseEntity.ok(customizationService.updateItem(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customizationService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
