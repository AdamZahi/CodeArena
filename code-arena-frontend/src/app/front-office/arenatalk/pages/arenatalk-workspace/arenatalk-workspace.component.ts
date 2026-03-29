import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Hub, TextChannel, Message } from '../../models/arenatalk.model';

@Component({
  selector: 'app-arenatalk-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-workspace.component.html',
  styleUrl: './arenatalk-workspace.component.css'
})
export class ArenatalkWorkspaceComponent implements OnInit {
  hubs: Hub[] = [];
  selectedHub: Hub | null = null;

  channels: TextChannel[] = [];
  selectedChannel: TextChannel | null = null;

  messages: Message[] = [];
  newMessage = '';

  constructor(
    private router: Router
  ) {}

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state || history.state;

    const createdHub = state?.selectedHub as Hub | undefined;
    const createdChannels = (state?.createdChannels as TextChannel[] | undefined) || [];

    if (createdHub) {
      this.hubs = [createdHub];
      this.selectedHub = createdHub;
    }

    if (createdChannels.length > 0) {
      this.channels = createdChannels;

      const generalChannel =
        this.channels.find(c => c.name.toLowerCase() === 'general') || this.channels[0];

      this.selectedChannel = generalChannel;
    }
  }

  selectHub(hub: Hub): void {
    this.selectedHub = hub;
  }

  selectChannel(channel: TextChannel): void {
    this.selectedChannel = channel;
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChannel) return;

    const message: Message = {
      content: this.newMessage,
      senderName: 'You',
      sentAt: new Date().toISOString()
    };

    this.messages.push(message);
    this.newMessage = '';
  }

  get hasMessages(): boolean {
    return this.messages.length > 0;
  }

  get categoryLabel(): string {
    return this.selectedHub?.category || 'COMMUNITY';
  }
}