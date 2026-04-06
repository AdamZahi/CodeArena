import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Hub, TextChannel, Message, MessageReaction } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';
import { HubMemberService, HubMember } from '../../services/hub-member.service';
import { AuthUserSyncService, CurrentUser } from '../../../../core/auth/auth-user-sync.service';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';
import { VoiceChannelComponent } from '../voice-channel/voice-channel.component';
import { MessageReactionsComponent } from '../message-reactions/message-reactions.component';
import { ReactionService } from '../../services/reaction.service';

type DeleteTargetType = 'hub' | 'channel' | 'message' | null;

@Component({
  selector: 'app-arenatalk-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, VoiceChannelComponent, MessageReactionsComponent],
  templateUrl: './arenatalk-workspace.component.html',
  styleUrl: './arenatalk-workspace.component.css'
})
export class ArenatalkWorkspaceComponent implements OnInit {

  hubs: Hub[] = [];
  selectedHub: Hub | null = null;
  currentUserName = '';

  channels: TextChannel[] = [];
  selectedChannel: TextChannel | null = null;

  messages: Message[] = [];
  newMessage = '';

  members: HubMember[] = [];
  currentUserMember: HubMember | null = null;
  currentKeycloakId = '';

  reactions: { [messageId: number]: MessageReaction } = {};

  pendingRequests: HubMember[] = [];
  showPendingRequests = false;

  showCreateChannelForm = false;
  newChannel: TextChannel = { name: '', topic: '' };

  showDeleteModal = false;
  deleteTargetType: DeleteTargetType = null;
  deleteTargetId: number | null = null;
  deleteMessageText = '';

  editingMessageId: number | null = null;
  editedMessageContent = '';

  constructor(
    private router: Router,
    private arenaService: ArenatalkService,
    private hubMemberService: HubMemberService,
    private authUserSync: AuthUserSyncService,
    private auth: AuthService,
    private reactionService: ReactionService
  ) {}

  ngOnInit(): void {
    this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.currentKeycloakId = payload.sub;
    });

    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state || history.state;

    const createdHub = state?.selectedHub as Hub | undefined;
    const createdChannels = (state?.createdChannels as TextChannel[] | undefined) || [];

    if (createdHub) {
      this.hubs = [createdHub];
      this.selectedHub = createdHub;
      this.loadMembers(createdHub.id!);
    }

    if (createdChannels.length > 0) {
      this.channels = createdChannels;
      this.selectFirstChannel();
    }
  }

  selectHub(hub: Hub): void {
    this.selectedHub = hub;
    this.channels = [];
    this.messages = [];
    this.selectedChannel = null;
    this.members = [];
    this.currentUserMember = null;
    this.pendingRequests = [];
    this.showPendingRequests = false;

    if (hub.id) {
      this.arenaService.getChannelsByHub(hub.id).subscribe({
        next: (data) => { this.channels = data; this.selectFirstChannel(); },
        error: (err) => console.error('Error loading channels', err)
      });
      this.loadMembers(hub.id);
    }
  }

  loadMembers(hubId: number): void {
    this.hubMemberService.getMembers(hubId).subscribe({
      next: (members) => {
        this.members = members;
        this.detectCurrentUser(members);
      },
      error: (err) => console.error('Error loading members', err)
    });
  }

  private detectCurrentUser(members: HubMember[]): void {
    this.authUserSync.currentUser$.pipe(take(1)).subscribe((currentUser: CurrentUser | null) => {
      if (!currentUser?.id) return;
      this.currentUserMember = members.find(m => m.user.id === currentUser.id) ?? null;
      this.currentUserName = `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim()
        || currentUser.email
        || this.currentKeycloakId;

      if (this.isOwner && this.selectedHub?.id && this.currentKeycloakId) {
        this.hubMemberService.getPendingRequests(this.selectedHub.id, this.currentKeycloakId)
          .subscribe({ next: (reqs) => this.pendingRequests = reqs, error: () => {} });
      }
    });
  }

  get isOwner(): boolean {
    return this.currentUserMember?.role === 'OWNER';
  }

  loadPendingRequests(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;
    this.hubMemberService.getPendingRequests(this.selectedHub.id, this.currentKeycloakId).subscribe({
      next: (requests) => {
        this.pendingRequests = requests;
        this.showPendingRequests = !this.showPendingRequests;
      },
      error: (err) => console.error('Error loading requests', err)
    });
  }

  acceptMember(memberId: number): void {
    if (!this.selectedHub?.id) return;
    this.hubMemberService.acceptRequest(this.selectedHub.id, memberId, this.currentKeycloakId).subscribe({
      next: () => {
        this.pendingRequests = this.pendingRequests.filter(r => r.id !== memberId);
        this.loadMembers(this.selectedHub!.id!);
      },
      error: (err) => console.error('Error accepting request', err)
    });
  }

  rejectMember(memberId: number): void {
    if (!this.selectedHub?.id) return;
    this.hubMemberService.rejectRequest(this.selectedHub.id, memberId, this.currentKeycloakId).subscribe({
      next: () => {
        this.pendingRequests = this.pendingRequests.filter(r => r.id !== memberId);
      },
      error: (err) => console.error('Error rejecting request', err)
    });
  }

  private selectFirstChannel(): void {
    const general = this.channels.find(c => c.name.toLowerCase() === 'general') || this.channels[0];
    this.selectedChannel = general || null;
    if (this.selectedChannel?.id) {
      this.loadMessagesByChannel(this.selectedChannel.id);
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
        this.loadReactions();
      },
      error: (err) => {
        console.error('Error loading messages', err);
        this.messages = [];
      }
    });
  }

  loadReactions(): void {
    if (!this.messages.length || !this.currentKeycloakId) return;
    const ids = this.messages.map(m => m.id!).filter(Boolean);
    this.reactionService.getForChannel(ids, this.currentKeycloakId).subscribe({
      next: (data) => this.reactions = data,
      error: () => {}
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChannel?.id) return;
    this.arenaService.sendMessage(this.selectedChannel.id, { content: this.newMessage.trim() }).subscribe({
      next: (savedMessage) => {
        this.messages.push(savedMessage);
        this.newMessage = '';
        this.loadReactions();
      },
      error: (err) => console.error('Error sending message', err)
    });
  }

  openCreateChannelForm(): void { this.showCreateChannelForm = true; }

  closeCreateChannelForm(): void {
    this.showCreateChannelForm = false;
    this.newChannel = { name: '', topic: '' };
  }

  createChannel(): void {
    if (!this.selectedHub?.id) return;
    const rawName = this.newChannel.name?.trim();
    const topic = this.newChannel.topic?.trim() || '';
    if (!rawName) return;
    const normalizedName = rawName.toLowerCase().replace(/\s+/g, '-');
    if (normalizedName.length < 3 || normalizedName.length > 20) return;
    if (!/^[a-z0-9-_]+$/.test(normalizedName)) return;
    if (topic.length > 100) return;
    this.arenaService.createChannel(this.selectedHub.id, { name: normalizedName, topic }).subscribe({
      next: (createdChannel) => {
        this.channels.push(createdChannel);
        this.selectedChannel = createdChannel;
        this.messages = [];
        this.closeCreateChannelForm();
      },
      error: (err) => console.error('Error creating channel', err)
    });
  }

  openDeleteModal(type: DeleteTargetType, id?: number): void {
    if (!id) return;
    this.deleteTargetType = type;
    this.deleteTargetId = id;
    this.showDeleteModal = true;
    const messages: Record<string, string> = {
      hub: 'Are you sure you want to delete this community? This action is irreversible.',
      channel: 'Are you sure you want to delete this channel and all its messages?',
      message: 'Are you sure you want to delete this message?'
    };
    this.deleteMessageText = messages[type!] || '';
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.deleteTargetType = null;
    this.deleteTargetId = null;
    this.deleteMessageText = '';
  }

  confirmDelete(): void {
    if (!this.deleteTargetType || !this.deleteTargetId) return;
    if (this.deleteTargetType === 'hub') this.deleteHubConfirmed(this.deleteTargetId);
    else if (this.deleteTargetType === 'channel') this.deleteChannelConfirmed(this.deleteTargetId);
    else if (this.deleteTargetType === 'message') this.deleteMessageConfirmed(this.deleteTargetId);
  }

  deleteHubConfirmed(hubId: number): void {
    this.arenaService.deleteHub(hubId).subscribe({
      next: () => { this.closeDeleteModal(); this.router.navigate(['/arenatalk']); },
      error: (err) => console.error('Error deleting hub', err)
    });
  }

  deleteChannelConfirmed(channelId: number): void {
    this.arenaService.deleteChannel(channelId).subscribe({
      next: () => {
        this.channels = this.channels.filter(c => c.id !== channelId);
        if (this.selectedChannel?.id === channelId) {
          this.selectedChannel = this.channels[0] || null;
          if (this.selectedChannel?.id) this.loadMessagesByChannel(this.selectedChannel.id);
          else this.messages = [];
        }
        this.closeDeleteModal();
      },
      error: (err) => console.error('Error deleting channel', err)
    });
  }

  deleteMessageConfirmed(messageId: number): void {
    this.arenaService.deleteMessage(messageId).subscribe({
      next: () => {
        this.messages = this.messages.filter(msg => msg.id !== messageId);
        this.closeDeleteModal();
      },
      error: (err) => console.error('Error deleting message', err)
    });
  }

  startEditMessage(message: Message): void {
    if (!message.id) return;
    this.editingMessageId = message.id;
    this.editedMessageContent = message.content;
  }

  cancelEditMessage(): void {
    this.editingMessageId = null;
    this.editedMessageContent = '';
  }

  saveEditedMessage(message: Message): void {
    if (!message.id || !this.editedMessageContent.trim()) return;
    this.arenaService.updateMessage(message.id, { ...message, content: this.editedMessageContent.trim() }).subscribe({
      next: (saved) => {
        this.messages = this.messages.map(m => m.id === saved.id ? saved : m);
        this.cancelEditMessage();
      },
      error: (err) => console.error('Error updating message', err)
    });
  }

  leaveHub(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;
    this.hubMemberService.leaveHub(this.selectedHub.id, this.currentKeycloakId).subscribe({
      next: () => this.router.navigate(['/arenatalk']),
      error: (err) => console.error('Error leaving hub', err)
    });
  }

  onHubImageError(event: Event, type: 'icon' | 'banner'): void {
    const img = event.target as HTMLImageElement;
    if (type === 'icon') img.style.display = 'none';
    if (type === 'banner') { const c = img.parentElement; if (c) c.style.display = 'none'; }
  }

  get hasMessages(): boolean { return this.messages.length > 0; }
  get categoryLabel(): string { return this.selectedHub?.category || 'COMMUNITY'; }
  get activeMembers(): HubMember[] { return this.members.filter(m => m.status === 'ACTIVE'); }
}