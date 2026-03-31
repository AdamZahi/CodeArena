import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Hub, TextChannel, Message } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';

type DeleteTargetType = 'hub' | 'channel' | 'message' | null;

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

  showCreateChannelForm = false;
  newChannel: TextChannel = {
    name: '',
    topic: ''
  };

  // delete modal
  showDeleteModal = false;
  deleteTargetType: DeleteTargetType = null;
  deleteTargetId: number | null = null;
  deleteMessageText = '';

  // edit message
  editingMessageId: number | null = null;
  editedMessageContent = '';

  constructor(
    private router: Router,
    private arenaService: ArenatalkService
  ) {}
ngOnInit(): void {
  const nav = this.router.getCurrentNavigation();
  const state = nav?.extras?.state || history.state;

  const createdHub = state?.selectedHub as Hub | undefined;
  const createdChannels = (state?.createdChannels as TextChannel[] | undefined) || [];

  const storedHub = localStorage.getItem('communityArena_selectedHub');
  const storedChannels = localStorage.getItem('communityArena_channels');

  if (createdHub) {
    this.hubs = [createdHub];
    this.selectedHub = createdHub;
  } else if (storedHub) {
    const parsedHub = JSON.parse(storedHub) as Hub;
    this.hubs = [parsedHub];
    this.selectedHub = parsedHub;
  }

  if (createdChannels.length > 0) {
    this.channels = createdChannels;
  } else if (storedChannels) {
    this.channels = JSON.parse(storedChannels) as TextChannel[];
  }

  if (this.channels.length > 0) {
    const generalChannel =
      this.channels.find(c => c.name.toLowerCase() === 'general') || this.channels[0];

    this.selectedChannel = generalChannel;

    if (this.selectedChannel?.id) {
      this.loadMessagesByChannel(this.selectedChannel.id);
    } else {
      this.messages = [];
    }
  }
}
  selectHub(hub: Hub): void {
    this.selectedHub = hub;

    if (hub.id) {
      this.arenaService.getChannelsByHub(hub.id).subscribe({
        next: (data) => {
          this.channels = data;
localStorage.setItem('communityArena_selectedHub', JSON.stringify(hub));
localStorage.setItem('communityArena_channels', JSON.stringify(data));
          const generalChannel =
            this.channels.find(c => c.name.toLowerCase() === 'general') || this.channels[0];

          this.selectedChannel = generalChannel || null;

          if (this.selectedChannel?.id) {
            this.loadMessagesByChannel(this.selectedChannel.id);
          } else {
            this.messages = [];
          }
        },
        error: (err) => {
          console.error('Error loading channels', err);
          this.channels = [];
          this.selectedChannel = null;
          this.messages = [];
        }
      });
    }
  }

  selectChannel(channel: TextChannel): void {
    this.selectedChannel = channel;
    this.cancelEditMessage();

    if (channel.id) {
      this.loadMessagesByChannel(channel.id);
    } else {
      this.messages = [];
    }
  }

  loadMessagesByChannel(channelId: number): void {
    this.arenaService.getMessagesByChannel(channelId).subscribe({
      next: (data) => {
        this.messages = data;
      },
      error: (err) => {
        console.error('Error loading messages', err);
        this.messages = [];
      }
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChannel?.id) return;

    const message: Message = {
      content: this.newMessage.trim(),
      senderName: 'You'
    };

    this.arenaService.sendMessage(this.selectedChannel.id, message).subscribe({
      next: (savedMessage) => {
        this.messages.push(savedMessage);
        this.newMessage = '';
      },
      error: (err) => {
        console.error('Error sending message', err);
      }
    });
  }

  // ---------- create channel ----------
  openCreateChannelForm(): void {
    this.showCreateChannelForm = true;
  }

  closeCreateChannelForm(): void {
    this.showCreateChannelForm = false;
    this.newChannel = {
      name: '',
      topic: ''
    };
  }

  createChannel(): void {
  if (!this.selectedHub?.id) {
    return;
  }

  const rawName = this.newChannel.name?.trim();
  const topic = this.newChannel.topic?.trim() || '';

  if (!rawName) {
    return;
  }

  const normalizedName = rawName.toLowerCase().replace(/\s+/g, '-');

  if (normalizedName.length < 3 || normalizedName.length > 20) {
    return;
  }

  const pattern = /^[a-z0-9-_]+$/;
  if (!pattern.test(normalizedName)) {
    return;
  }

  if (topic.length > 100) {
    return;
  }

  const payload: TextChannel = {
    name: normalizedName,
    topic
  };

  this.arenaService.createChannel(this.selectedHub.id, payload).subscribe({
    next: (createdChannel) => {
      this.channels.push(createdChannel);
      this.selectedChannel = createdChannel;
      this.messages = [];
      this.closeCreateChannelForm();
    },
    error: (err) => {
      console.error('Error creating channel', err);
      alert('Failed to create channel.');
    }
  });
}

  // ---------- delete modal ----------
  openDeleteModal(type: DeleteTargetType, id?: number): void {
    if (!id) return;

    this.deleteTargetType = type;
    this.deleteTargetId = id;
    this.showDeleteModal = true;

    if (type === 'hub') {
      this.deleteMessageText = 'Are you sure you want to delete this community?';
    } else if (type === 'channel') {
      this.deleteMessageText = 'Are you sure you want to delete this channel?';
    } else {
      this.deleteMessageText = 'Are you sure you want to delete this message?';
    }
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.deleteTargetType = null;
    this.deleteTargetId = null;
    this.deleteMessageText = '';
  }

  confirmDelete(): void {
    localStorage.removeItem('communityArena_selectedHub');
localStorage.removeItem('communityArena_channels');
    if (!this.deleteTargetType || !this.deleteTargetId) return;

    if (this.deleteTargetType === 'hub') {
      this.deleteHubConfirmed(this.deleteTargetId);
    } else if (this.deleteTargetType === 'channel') {
      this.deleteChannelConfirmed(this.deleteTargetId);
    } else if (this.deleteTargetType === 'message') {
      this.deleteMessageConfirmed(this.deleteTargetId);
    }
  }

  deleteHubConfirmed(hubId: number): void {
    this.arenaService.deleteHub(hubId).subscribe({
      next: () => {
        this.hubs = this.hubs.filter(hub => hub.id !== hubId);

        if (this.selectedHub?.id === hubId) {
          this.selectedHub = this.hubs[0] || null;
          this.channels = [];
          this.selectedChannel = null;
          this.messages = [];

          if (this.selectedHub) {
            this.selectHub(this.selectedHub);
          } else {
            this.router.navigate(['/arenatalk']);
          }
        }

        this.closeDeleteModal();
      },
      error: (err) => {
        console.error('Error deleting hub', err);
      }
    });
  }

  deleteChannelConfirmed(channelId: number): void {
    this.arenaService.deleteChannel(channelId).subscribe({
      next: () => {
        this.channels = this.channels.filter(channel => channel.id !== channelId);

        if (this.selectedChannel?.id === channelId) {
          this.selectedChannel = this.channels[0] || null;

          if (this.selectedChannel?.id) {
            this.loadMessagesByChannel(this.selectedChannel.id);
          } else {
            this.messages = [];
          }
        }

        this.closeDeleteModal();
      },
      error: (err) => {
        console.error('Error deleting channel', err);
      }
    });
  }

  deleteMessageConfirmed(messageId: number): void {
    this.arenaService.deleteMessage(messageId).subscribe({
      next: () => {
        this.messages = this.messages.filter(msg => msg.id !== messageId);
        this.closeDeleteModal();
      },
      error: (err) => {
        console.error('Error deleting message', err);
      }
    });
  }

  // ---------- edit message ----------
  startEditMessage(message: Message): void {
    if (!message.id) return;
    this.editingMessageId = message.id;
    this.editedMessageContent = message.content;
  }

  cancelEditMessage(): void {
    this.editingMessageId = null;
    this.editedMessageContent = '';
  }
onHubImageError(event: Event, type: 'icon' | 'banner'): void {
  const img = event.target as HTMLImageElement;

  if (type === 'icon') {
    img.style.display = 'none';
  }

  if (type === 'banner') {
    const bannerContainer = img.parentElement;
    if (bannerContainer) {
      bannerContainer.style.display = 'none';
    }
  }
}
  saveEditedMessage(message: Message): void {
    if (!message.id || !this.editedMessageContent.trim()) return;

    const updatedMessage: Message = {
      ...message,
      content: this.editedMessageContent.trim()
    };

    this.arenaService.updateMessage(message.id, updatedMessage).subscribe({
      next: (savedMessage) => {
        this.messages = this.messages.map(msg =>
          msg.id === savedMessage.id ? savedMessage : msg
        );
        this.cancelEditMessage();
      },
      error: (err) => {
        console.error('Error updating message', err);
      }
    });
  }

  get hasMessages(): boolean {
    return this.messages.length > 0;
  }

  get categoryLabel(): string {
    return this.selectedHub?.category || 'COMMUNITY';
  }
}