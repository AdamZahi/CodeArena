package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.ArenaTalkWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArenaTalkWalletRepository extends JpaRepository<ArenaTalkWallet, Long> {

    Optional<ArenaTalkWallet> findByUserId(String userId);
}