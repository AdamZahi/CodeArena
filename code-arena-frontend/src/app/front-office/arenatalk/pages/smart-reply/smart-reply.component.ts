import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AiSmartReplyService } from '../../services/ai/ai-smart-reply.service';
import { Message } from '../../models/arenatalk.model';

@Component({
  selector: 'app-smart-reply',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './smart-reply.component.html',
  styleUrl: './smart-reply.component.css'
})
export class SmartReplyComponent implements OnChanges {

  @Input() messages: Message[] = [];
  @Output() replySelected = new EventEmitter<string>();

  suggestions: string[] = [];
  loading = false;
  shown = false;

  constructor(private aiSmartReplyService: AiSmartReplyService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['messages']) {
      this.suggestions = [];
      this.shown = false;
    }
  }

  loadSuggestions(): void {
    if (!this.messages.length) return;
    this.loading = true;
    this.shown = true;

    const recent = this.messages.slice(-5).map(m => ({
      sender: m.senderName ?? 'Unknown',
      content: m.content
    }));

    this.aiSmartReplyService.getSuggestions(recent).subscribe({
      next: (replies) => { this.suggestions = replies; this.loading = false; },
      error: () => { this.suggestions = ['Sounds good!', 'Can you explain more?', 'I agree 👍']; this.loading = false; }
    });
  }

  selectReply(reply: string): void {
    this.replySelected.emit(reply);
    this.shown = false;
    this.suggestions = [];
  }

  close(): void {
    this.shown = false;
    this.suggestions = [];
  }
}