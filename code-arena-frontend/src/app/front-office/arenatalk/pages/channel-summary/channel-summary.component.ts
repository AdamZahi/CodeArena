import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../models/arenatalk.model';
import { AiSummaryService } from '../../services/ai/ai-summary.service';

@Component({
  selector: 'app-channel-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './channel-summary.component.html',
  styleUrl: './channel-summary.component.css'
})
export class ChannelSummaryComponent implements OnChanges {

  @Input() messages: Message[] = [];
  @Input() channelName = '';

  summary = '';
  loading = false;
  showSummary = false;
  error = false;

  constructor(private aiSummaryService: AiSummaryService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['messages']) {
      this.summary = '';
      this.showSummary = false;
    }
  }

  summarize(): void {
    if (!this.messages.length) return;
    this.loading = true;
    this.error = false;
    this.showSummary = true;

    const last50 = this.messages.slice(-50).map(m => ({
      sender: m.senderName ?? 'Unknown',
      content: m.content
    }));

    this.aiSummaryService.summarize(last50).subscribe({
      next: (text) => { this.summary = text; this.loading = false; },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  close(): void {
    this.showSummary = false;
    this.summary = '';
  }
}