import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Message } from '../../models/arenatalk.model';

@Component({
  selector: 'app-message-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-search.component.html',
  styleUrl: './message-search.component.css'
})
export class MessageSearchComponent implements OnChanges {

  @Input() messages: Message[] = [];
  @Output() resultClick = new EventEmitter<Message>();

  searchTerm = '';
  results: Message[] = [];
  showResults = false;

  ngOnChanges(): void {
    if (this.searchTerm) this.search();
  }

  search(): void {
    if (!this.searchTerm.trim()) {
      this.results = [];
      this.showResults = false;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.results = this.messages.filter(m =>
      m.content.toLowerCase().includes(term) ||
      (m.senderName ?? '').toLowerCase().includes(term)
    );
    this.showResults = true;
  }

  onResultClick(msg: Message): void {
    this.resultClick.emit(msg);
    this.searchTerm = '';
    this.results = [];
    this.showResults = false;
  }

  clear(): void {
    this.searchTerm = '';
    this.results = [];
    this.showResults = false;
  }

  highlight(text: string): string {
    if (!this.searchTerm.trim()) return text;
    const regex = new RegExp(`(${this.searchTerm.trim()})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }
}