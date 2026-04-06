import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MessageReaction } from '../../models/arenatalk.model';
import { ReactionService } from '../../services/reaction.service';

const AVAILABLE_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🔥'];

@Component({
  selector: 'app-message-reactions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './message-reactions.component.html',
  styleUrl: './message-reactions.component.css'
})
export class MessageReactionsComponent implements OnInit, OnChanges {

  @Input() messageId!: number;
  @Input() keycloakId!: string;
  @Input() reaction: MessageReaction | null = null;

  availableEmojis = AVAILABLE_EMOJIS;
  showPicker = false;
  reactionData: MessageReaction | null = null;

  constructor(private reactionService: ReactionService) {}

  ngOnInit(): void {
    this.reactionData = this.reaction;
  }

  ngOnChanges(): void {
    this.reactionData = this.reaction;
  }

  get reactionEntries(): { emoji: string; count: number; reacted: boolean }[] {
    if (!this.reactionData?.counts) return [];
    return Object.entries(this.reactionData.counts)
      .filter(([, count]) => count > 0)
      .map(([emoji, count]) => ({
        emoji,
        count,
        reacted: this.reactionData?.reacted?.[emoji] ?? false
      }));
  }

  toggleEmoji(emoji: string): void {
    this.reactionService.toggle(this.messageId, emoji, this.keycloakId).subscribe({
      next: (updated) => {
        this.reactionData = updated;
        this.showPicker = false;
      },
      error: (err) => console.error('Error toggling reaction', err)
    });
  }

  togglePicker(event: MouseEvent): void {
    event.stopPropagation();
    this.showPicker = !this.showPicker;
  }

  closePicker(): void {
    this.showPicker = false;
  }
}