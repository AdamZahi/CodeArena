import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArenaTalkService } from '../../services/arenatalk.service';
import { Hub, TextChannel, Message } from '../../models/arenatalk.model';

@Component({
  selector: 'app-arenatalk-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-workspace.component.html',
  styleUrl: './arenatalk-workspace.component.css'
})
export class ArenaTalkWorkspaceComponent implements OnInit {
  hubs: Hub[] = [];
  channels: TextChannel[] = [];
  messages: Message[] = [];

  selectedHub: Hub | null = null;
  selectedChannel: TextChannel | null = null;

  newMessage = '';

  constructor(private arenaTalkService: ArenaTalkService) {}

  ngOnInit(): void {
    this.loadHubs();
  }

  loadHubs(): void {
    this.arenaTalkService.getHubs().subscribe({
      next: (data) => {
        this.hubs = data;
      },
      error: (err) => console.error('Error loading hubs', err)
    });
  }

  selectHub(hub: Hub): void {
    this.selectedHub = hub;
    this.selectedChannel = null;
    this.messages = [];

    if (!hub.id) return;

    this.arenaTalkService.getChannelsByHub(hub.id).subscribe({
      next: (data) => {
        this.channels = data;
      },
      error: (err) => console.error('Error loading channels', err)
    });
  }

  selectChannel(channel: TextChannel): void {
    this.selectedChannel = channel;

    if (!channel.id) return;

    this.arenaTalkService.getMessagesByChannel(channel.id).subscribe({
      next: (data) => {
        this.messages = data;
      },
      error: (err) => console.error('Error loading messages', err)
    });
  }

  sendMessage(): void {
    if (!this.selectedChannel?.id || !this.newMessage.trim()) return;

    const payload: Message = {
      content: this.newMessage,
      senderName: 'Ghofrane'
    };

    this.arenaTalkService.sendMessage(this.selectedChannel.id, payload).subscribe({
      next: () => {
        this.newMessage = '';
        this.selectChannel(this.selectedChannel!);
      },
      error: (err) => console.error('Error sending message', err)
    });
  }
}