import {
  Component, EventEmitter, Input, OnChanges,
  Output, SimpleChanges, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { GiftService, GiftType } from '../../services/gift.service';
import { ArenaTalkWalletService, ArenaTalkWallet } from '../../services/arenatalk-wallet.service';

@Component({
  selector: 'app-voice-gift',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voice-gift.component.html',
  styleUrl: './voice-gift.component.css'
})
export class VoiceGiftComponent implements OnChanges, OnDestroy {

  @Input() currentUserId  = '';
  @Input() currentUserName = '';
  @Input() ownerUserId    = '';
  @Input() ownerUserName  = '';
  @Input() hubId!: number;
  @Input() voiceChannelId!: number;

  @Output() giftSent = new EventEmitter<string>();

  sending      = false;
  wallet: ArenaTalkWallet | null = null;

  // ── Rolling counter state ──────────────────────────────────────────────────
  displayBalance = 0;
  isRolling      = false;
  private rollTimeout: any;

  // ── Gift definitions ───────────────────────────────────────────────────────
  gifts: { type: GiftType; label: string; coins: number; icon: string; cssClass: string }[] = [
    { type: 'COINS',  label: 'Coins',  coins: 5,  icon: '🪙', cssClass: 'coins-card'  },
    { type: 'FIRE',   label: 'Fire',   coins: 10, icon: '🔥', cssClass: 'fire-card'   },
    { type: 'CROWN',  label: 'Crown',  coins: 25, icon: '👑', cssClass: 'crown-card'  },
    { type: 'ROCKET', label: 'Rocket', coins: 50, icon: '🚀', cssClass: 'rocket-card' },
  ];

  constructor(
    private giftService:   GiftService,
    private walletService: ArenaTalkWalletService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.currentUserId && this.currentUserName) {
      this.loadWallet();
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.rollTimeout);
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();
  }

  // ── Wallet ─────────────────────────────────────────────────────────────────
  loadWallet(): void {
    this.walletService.getWallet(this.currentUserId, this.currentUserName).subscribe({
      next: wallet => {
        const prev = this.wallet?.balance ?? 0;
        this.wallet = wallet;
        this.animateBalance(prev, wallet.balance);
      },
      error: err => console.error('Wallet error', err)
    });
  }

  // ── Rolling number animation ───────────────────────────────────────────────
  private animateBalance(from: number, to: number): void {
    const duration = 700;
    const start    = performance.now();
    this.isRolling = true;

    const step = (now: number) => {
      const t    = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 3);          // ease-out cubic
      this.displayBalance = Math.round(from + (to - from) * ease);

      if (t < 1) {
        requestAnimationFrame(step);
      } else {
        this.displayBalance = to;
        this.rollTimeout = setTimeout(() => this.isRolling = false, 200);
      }
    };

    requestAnimationFrame(step);
  }

  // ── Coin tier ──────────────────────────────────────────────────────────────
  get coinTierLabel(): string {
    const b = this.wallet?.balance ?? 0;
    if (b >= 2000) return 'Diamond';
    if (b >= 500)  return 'Gold';
    if (b >= 100)  return 'Silver';
    return 'Bronze';
  }

  get coinTierIcon(): string {
    const b = this.wallet?.balance ?? 0;
    if (b >= 2000) return '💎';
    if (b >= 500)  return '🥇';
    if (b >= 100)  return '🥈';
    return '🥉';
  }

  get coinTierClass(): string {
    const b = this.wallet?.balance ?? 0;
    if (b >= 2000) return 'diamond';
    if (b >= 500)  return 'gold';
    if (b >= 100)  return 'silver';
    return 'bronze';
  }

  // ── Can send ───────────────────────────────────────────────────────────────
  canSend(giftCoins: number): boolean {
    return !!this.wallet && this.wallet.balance >= giftCoins && !this.sending;
  }

  // ── Send gift ──────────────────────────────────────────────────────────────
  sendGift(gift: { type: GiftType; coins: number; icon: string }): void {
    if (!this.currentUserId || !this.ownerUserId || !this.hubId || !this.voiceChannelId) return;
    if (!this.canSend(gift.coins)) return;

    this.sending = true;

    this.giftService.sendGift({
      fromUserId:    this.currentUserId,
      fromUserName:  this.currentUserName,
      toUserId:      this.ownerUserId,
      toUserName:    this.ownerUserName,
      hubId:         this.hubId,
      voiceChannelId: this.voiceChannelId,
      giftType:      gift.type,
      coins:         gift.coins,
      amountMoney:   this.coinsToAmount(gift.coins),
      currency:      'EUR'
    }).subscribe({
      next: () => {
        this.sending = false;
        this.loadWallet();

        const message =
          `${gift.icon} ${this.currentUserName} gifted ${gift.coins} coins to ${this.ownerUserName}`;

        this.giftSent.emit(message);

        // ── AI Text-to-Speech announcement ──────────────────────────────────
        this.speakGiftAnnouncement(gift.type, gift.coins, gift.icon);
      },
      error: err => {
        this.sending = false;
        console.error('Gift error', err);
        alert('Not enough ArenaTalk coins.');
        this.loadWallet();
      }
    });
  }

  // ── TTS ────────────────────────────────────────────────────────────────────
  private speakGiftAnnouncement(type: GiftType, coins: number, icon: string): void {
    if (!('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();

    const lines: Record<string, string> = {
      COINS:  `Attention everyone! ${this.currentUserName} just donated ${coins} coins to ${this.ownerUserName}. Thank you so much for your support!`,
      FIRE:   `Oh wow! ${this.currentUserName} is on fire! They just sent a Fire gift of ${coins} coins to ${this.ownerUserName}. That is absolutely amazing!`,
      CROWN:  `A Royal moment! ${this.currentUserName} has gifted a Crown worth ${coins} coins to ${this.ownerUserName}. You are truly a king!`,
      ROCKET: `We are blasting off! ${this.currentUserName} just launched a Rocket gift of ${coins} coins to ${this.ownerUserName}. To the moon!`,
    };

    const text = lines[type] ?? `${this.currentUserName} gifted ${coins} coins to ${this.ownerUserName}.`;
    const utt  = new SpeechSynthesisUtterance(text);

    utt.rate   = 0.93;
    utt.pitch  = 1.05;
    utt.volume = 1;
    utt.lang   = 'en-US';

    // Pick the best available English voice
    const voices   = window.speechSynthesis.getVoices();
    const preferred =
      voices.find(v => v.name.includes('Google') && v.lang.startsWith('en')) ||
      voices.find(v => v.lang.startsWith('en')) ||
      voices[0];

    if (preferred) utt.voice = preferred;

    window.speechSynthesis.speak(utt);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────
  private coinsToAmount(coins: number): number {
    const map: Record<number, number> = { 5: 0.5, 10: 1, 25: 2.5, 50: 5 };
    return map[coins] ?? coins * 0.1;
  }
}