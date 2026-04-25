import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GiftService, GiftType } from '../../services/gift.service';

@Component({
  selector: 'app-voice-gift',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voice-gift.component.html',
  styleUrl: './voice-gift.component.css'
})
export class VoiceGiftComponent {
  @Input() currentUserId = '';
  @Input() currentUserName = '';
  @Input() ownerUserId = '';
  @Input() ownerUserName = '';
  @Input() hubId!: number;
  @Input() voiceChannelId!: number;

  @Output() giftSent = new EventEmitter<string>();

  sending = false;

  gifts: { type: GiftType; label: string; coins: number; amount: number; icon: string }[] = [
    { type: 'COINS', label: 'Coins', coins: 5, amount: 0.5, icon: '🪙' },
    { type: 'FIRE', label: 'Fire', coins: 10, amount: 1, icon: '🔥' },
    { type: 'CROWN', label: 'Crown', coins: 25, amount: 2.5, icon: '👑' },
    { type: 'ROCKET', label: 'Rocket', coins: 50, amount: 5, icon: '🚀' }
  ];

  constructor(private giftService: GiftService) {}

  sendGift(gift: { type: GiftType; coins: number; amount: number; icon: string }): void {
    if (!this.currentUserId || !this.ownerUserId || !this.hubId || !this.voiceChannelId) return;

    this.sending = true;

    this.giftService.sendGift({
      fromUserId: this.currentUserId,
      fromUserName: this.currentUserName,
      toUserId: this.ownerUserId,
      toUserName: this.ownerUserName,
      hubId: this.hubId,
      voiceChannelId: this.voiceChannelId,
      giftType: gift.type,
      coins: gift.coins,
      amountMoney: gift.amount,
      currency: 'EUR'
    }).subscribe({
      next: () => {
        this.sending = false;
        this.giftSent.emit(`${gift.icon} ${this.currentUserName} gifted ${gift.coins} coins to ${this.ownerUserName}`);
      },
      error: (err) => {
        this.sending = false;
        console.error('Gift error', err);
      }
    });
  }
}