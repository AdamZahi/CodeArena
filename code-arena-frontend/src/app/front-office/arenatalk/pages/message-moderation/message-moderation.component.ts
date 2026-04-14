import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-message-moderation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './message-moderation.component.html',
  styleUrl: './message-moderation.component.css'
})
export class MessageModerationComponent {

  @Input() warning = '';
  @Input() show = false;
  @Output() sendAnyway = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onSendAnyway(): void { this.sendAnyway.emit(); }
  onCancel(): void { this.cancel.emit(); }
}