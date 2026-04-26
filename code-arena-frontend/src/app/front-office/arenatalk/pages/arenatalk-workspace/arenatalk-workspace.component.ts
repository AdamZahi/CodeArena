import { Component, HostListener, OnDestroy, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Hub, TextChannel, Message, MessageReaction, ReadReceipt } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';
import { HubMemberService, HubMember } from '../../services/hub-member.service';
import { AuthUserSyncService, CurrentUser } from '../../../../core/auth/auth-user-sync.service';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { VoiceChannelComponent } from '../voice-channel/voice-channel.component';
import { MessageReactionsComponent } from '../message-reactions/message-reactions.component';
import { ReactionService } from '../../services/reaction.service';
import { MessageSearchComponent } from '../message-search/message-search.component';
import { ChannelSummaryComponent } from '../channel-summary/channel-summary.component';
import { MessageModerationComponent } from '../message-moderation/message-moderation.component';
import { SmartReplyComponent } from '../smart-reply/smart-reply.component';
import { AiModerationService } from '../../services/ai/ai-moderation.service';
import { SemanticSearchComponent } from '../semantic-search/semantic-search.component';
import { VoiceSignalingService } from '../../services/voice-signaling.service';
import { VideoStreamDirective } from '../../../../shared/directives/video-stream.directive';
import { VoiceGiftComponent } from '../voice-gift/voice-gift.component';
import { PaymentService } from '../../services/payment.service';

type DeleteTargetType = 'hub' | 'channel' | 'message' | null;

@Component({
  selector: 'app-arenatalk-workspace',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    VoiceChannelComponent,
    MessageReactionsComponent,
    MessageSearchComponent,
    ChannelSummaryComponent,
    MessageModerationComponent,
    SmartReplyComponent,
    SemanticSearchComponent,
    VideoStreamDirective,
    VoiceGiftComponent
  ],
  templateUrl: './arenatalk-workspace.component.html',
  styleUrl: './arenatalk-workspace.component.css'
})
export class ArenatalkWorkspaceComponent implements OnInit, OnDestroy {

  hubs: Hub[] = [];
  selectedHub: Hub | null = null;
  hubReady = false;
  currentUserName = '';

  channels: TextChannel[] = [];
  selectedChannel: TextChannel | null = null;

  messages: Message[] = [];
  pinnedMessages: Message[] = [];
  newMessage = '';

  members: HubMember[] = [];
  currentUserMember: HubMember | null = null;
  currentKeycloakId = '';

  reactions: { [messageId: number]: MessageReaction } = {};
  readReceipts: { [messageId: number]: ReadReceipt } = {};

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

  showModerationWarning = false;
  moderationWarningText = '';
  pendingMessage = '';
  isCheckingMessage = false;

  activeVoiceChannelId: number | null = null;
  activeVoiceChannelName = '';
  voiceParticipants: any[] = [];
  voiceInRoom = false;
  voiceIsMuted = false;
  voiceLocalSpeaking = false;
  voiceCameraOn = false;
  voiceLocalVideoStream: MediaStream | null = null;

  giftNotification = '';
  showGiftNotification = false;
  private hideGiftTimeout: any;

  private voiceSubs: Subscription[] = [];

  constructor(
    private router: Router,
    private arenaService: ArenatalkService,
    private hubMemberService: HubMemberService,
    private authUserSync: AuthUserSyncService,
    private auth: AuthService,
    private reactionService: ReactionService,
    private aiModerationService: AiModerationService,
    private cdr: ChangeDetectorRef,
    private zone: NgZone,
    private paymentService: PaymentService,
    private voiceSignalingService: VoiceSignalingService
  ) {}

  ngOnInit(): void {
    this.voiceSubs.push(
      this.voiceSignalingService.participants$.subscribe(p => this.voiceParticipants = p),
      this.voiceSignalingService.isMuted$.subscribe(v => this.voiceIsMuted = v),
      this.voiceSignalingService.localSpeaking$.subscribe(v => this.voiceLocalSpeaking = v),
      this.voiceSignalingService.inRoom$.subscribe(v => this.voiceInRoom = v),
      this.voiceSignalingService.cameraOn$.subscribe(v => this.voiceCameraOn = v),
      this.voiceSignalingService.localVideoStream$.subscribe(v => this.voiceLocalVideoStream = v)
    );

    this.voiceSubs.push(
      this.voiceSignalingService.giftEvents$.subscribe(gift => {
        if (!gift) return;

        this.zone.run(() => {
          const icon =
            gift.giftType === 'FIRE' ? '🔥' :
            gift.giftType === 'CROWN' ? '👑' :
            gift.giftType === 'ROCKET' ? '🚀' : '🪙';

          this.giftNotification =
            `${icon} ${gift.fromUserName} gifted ${gift.coins} coins to ${gift.toUserName}`;

          clearTimeout(this.hideGiftTimeout);

          this.showGiftNotification = true;
          this.cdr.detectChanges();

          this.hideGiftTimeout = setTimeout(() => {
            this.showGiftNotification = false;
            this.cdr.detectChanges();
          }, 10000);
        });
      })
    );

    this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.currentKeycloakId = payload.sub;

      const nav = this.router.getCurrentNavigation();
      const state = nav?.extras?.state || history.state;

      const createdHub = state?.selectedHub as Hub | undefined;
      const createdChannels = (state?.createdChannels as TextChannel[] | undefined) || [];

      if (createdHub) {
        localStorage.setItem('selectedHubId', String(createdHub.id));
        this.hubs = [createdHub];
        this.hubReady = false;
        this.selectedHub = createdHub;
        this.hubReady = true;
        this.loadMembers(createdHub.id!);
        this.setCurrentUserOnline();

        if (createdChannels.length > 0) {
          this.channels = createdChannels;
          this.selectFirstChannel();
        } else if (createdHub.id) {
          this.arenaService.getChannelsByHub(createdHub.id).subscribe({
            next: data => {
              this.channels = data;
              this.selectFirstChannel();
            },
            error: err => console.error(err)
          });
        }
        return;
      }

      const savedHubId = localStorage.getItem('selectedHubId');
      if (savedHubId) {
        this.arenaService.getHubById(Number(savedHubId)).subscribe({
          next: hub => {
            this.hubs = [hub];
            this.hubReady = false;
            this.selectedHub = hub;
            this.hubReady = true;
            this.loadMembers(hub.id!);
            this.setCurrentUserOnline();

            if (hub.id) {
              this.arenaService.getChannelsByHub(hub.id).subscribe({
                next: data => {
                  this.channels = data;
                  this.selectFirstChannel();
                },
                error: err => console.error(err)
              });
            }
          },
          error: () => localStorage.removeItem('selectedHubId')
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.voiceSubs.forEach(s => s.unsubscribe());
    clearTimeout(this.hideGiftTimeout);
    this.setCurrentUserOffline();
  }

  @HostListener('window:beforeunload')
  handleBeforeUnload(): void {
    this.setCurrentUserOffline();
  }

  onVoiceRoomChanged(event: { channelId: number | null; channelName: string }): void {
    this.activeVoiceChannelId = event.channelId;
    this.activeVoiceChannelName = event.channelName;
  }

  toggleVoiceMute(): void {
    this.voiceSignalingService.toggleMute();
  }

  toggleVoiceCamera(): void {
    this.voiceSignalingService.toggleCamera();
  }

  leaveVoiceRoom(): void {
    this.voiceSignalingService.leaveRoom().then(() => {
      this.activeVoiceChannelId = null;
      this.activeVoiceChannelName = '';
    });
  }

  kickVoiceParticipant(event: PointerEvent, userId: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.voiceSignalingService.kickParticipant(userId);
  }

  buyCoins(coins: number): void {
    if (!this.currentKeycloakId || !this.currentUserName) {
      console.error('User not ready');
      return;
    }

    this.paymentService.createCheckout(
      coins,
      this.currentKeycloakId,
      this.currentUserName
    ).subscribe({
      next: res => {
        window.location.href = res.checkoutUrl || res.url || '';
      },
      error: err => {
        console.error('Payment error:', err);
      }
    });
  }

  private setCurrentUserOnline(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;
    this.hubMemberService.setOnline(this.selectedHub.id, this.currentKeycloakId).subscribe({
      next: () => this.loadMembers(this.selectedHub!.id!),
      error: () => {}
    });
  }

  private setCurrentUserOffline(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;
    this.hubMemberService.setOffline(this.selectedHub.id, this.currentKeycloakId).subscribe({
      error: () => {}
    });
  }

  selectHub(hub: Hub): void {
    if (this.selectedHub?.id && this.selectedHub.id !== hub.id) {
      this.setCurrentUserOffline();
    }

    localStorage.setItem('selectedHubId', String(hub.id));
    this.hubReady = false;

    this.selectedHub = null;
    this.channels = [];
    this.messages = [];
    this.pinnedMessages = [];
    this.readReceipts = {};
    this.selectedChannel = null;
    this.members = [];
    this.currentUserMember = null;
    this.pendingRequests = [];
    this.showPendingRequests = false;

    setTimeout(() => {
      this.selectedHub = hub;
      this.hubReady = true;

      if (hub.id) {
        this.arenaService.getChannelsByHub(hub.id).subscribe({
          next: data => {
            this.channels = data;
            this.selectFirstChannel();
          },
          error: err => console.error('Error loading channels', err)
        });

        this.loadMembers(hub.id);
        this.setCurrentUserOnline();
      }
    }, 0);
  }

  loadMembers(hubId: number): void {
    this.hubMemberService.getMembers(hubId).subscribe({
      next: members => {
        this.members = members;
        this.detectCurrentUser(members);
      },
      error: err => console.error('Error loading members', err)
    });
  }

  private detectCurrentUser(members: HubMember[]): void {
    this.authUserSync.currentUser$.pipe(take(1)).subscribe((currentUser: CurrentUser | null) => {
      if (!currentUser?.id) return;

      this.currentUserMember = members.find(m => m.user.id === currentUser.id) ?? null;

      this.currentUserName =
        `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim()
        || currentUser.email
        || this.currentKeycloakId;

      if (this.isOwner && this.selectedHub?.id && this.currentKeycloakId) {
        this.hubMemberService.getPendingRequests(this.selectedHub.id, this.currentKeycloakId)
          .subscribe({
            next: reqs => this.pendingRequests = reqs,
            error: () => {}
          });
      }
    });
  }

  get isOwner(): boolean {
    return this.currentUserMember?.role === 'OWNER';
  }

  loadPendingRequests(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;

    this.hubMemberService.getPendingRequests(this.selectedHub.id, this.currentKeycloakId).subscribe({
      next: requests => {
        this.pendingRequests = requests;
        this.showPendingRequests = !this.showPendingRequests;
      },
      error: err => console.error('Error loading requests', err)
    });
  }

  acceptMember(memberId: number): void {
    if (!this.selectedHub?.id) return;

    this.hubMemberService.acceptRequest(this.selectedHub.id, memberId, this.currentKeycloakId).subscribe({
      next: () => {
        this.pendingRequests = this.pendingRequests.filter(r => r.id !== memberId);
        this.loadMembers(this.selectedHub!.id!);
      },
      error: err => console.error('Error accepting request', err)
    });
  }

  rejectMember(memberId: number): void {
    if (!this.selectedHub?.id) return;

    this.hubMemberService.rejectRequest(this.selectedHub.id, memberId, this.currentKeycloakId).subscribe({
      next: () => {
        this.pendingRequests = this.pendingRequests.filter(r => r.id !== memberId);
      },
      error: err => console.error('Error rejecting request', err)
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
      this.pinnedMessages = [];
      this.readReceipts = {};
    }
  }

  loadMessagesByChannel(channelId: number): void {
    this.arenaService.getMessagesByChannel(channelId).subscribe({
      next: data => {
        this.messages = data;
        this.loadPinnedMessages(channelId);
        this.loadReactions();
      },
      error: err => {
        console.error('Error loading messages', err);
        this.messages = [];
        this.pinnedMessages = [];
        this.readReceipts = {};
      }
    });
  }

  loadPinnedMessages(channelId: number): void {
    this.arenaService.getPinnedMessages(channelId).subscribe({
      next: data => this.pinnedMessages = data,
      error: () => this.pinnedMessages = []
    });
  }

  loadReactions(): void {
    if (!this.messages.length || !this.currentKeycloakId) return;

    const ids = this.messages.map(m => m.id!).filter(Boolean);

    this.reactionService.getForChannel(ids, this.currentKeycloakId).subscribe({
      next: data => this.reactions = data,
      error: () => {}
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedChannel?.id) return;
    if (this.isCheckingMessage) return;

    this.isCheckingMessage = true;
    const content = this.newMessage.trim();

    this.aiModerationService.moderate(content).subscribe({
      next: result => {
        this.isCheckingMessage = false;

        if (!result.safe) {
          this.pendingMessage = content;
          this.moderationWarningText = result.reason;
          this.showModerationWarning = true;
        } else {
          this.doSendMessage(content);
        }
      },
      error: () => {
        this.isCheckingMessage = false;
        this.doSendMessage(content);
      }
    });
  }

  doSendMessage(content: string): void {
    if (!this.selectedChannel?.id) return;

    this.arenaService.sendMessage(this.selectedChannel.id, { content }).subscribe({
      next: savedMessage => {
        this.messages.push(savedMessage);
        this.newMessage = '';
        this.loadReactions();
      },
      error: err => console.error('Error sending message', err)
    });
  }

  onSendAnyway(): void {
    this.showModerationWarning = false;
    this.doSendMessage(this.pendingMessage);
    this.newMessage = '';
    this.pendingMessage = '';
  }

  onModerationCancel(): void {
    this.showModerationWarning = false;
    this.pendingMessage = '';
  }

  onSmartReplySelected(reply: string): void {
    this.newMessage = reply;
  }

  pinMessage(messageId: number): void {
    this.arenaService.pinMessage(messageId).subscribe({
      next: () => {
        if (this.selectedChannel?.id) this.loadMessagesByChannel(this.selectedChannel.id);
      },
      error: err => console.error('Error pinning message', err)
    });
  }

  unpinMessage(messageId: number): void {
    this.arenaService.unpinMessage(messageId).subscribe({
      next: () => {
        if (this.selectedChannel?.id) this.loadMessagesByChannel(this.selectedChannel.id);
      },
      error: err => console.error('Error unpinning message', err)
    });
  }

  openCreateChannelForm(): void {
    this.showCreateChannelForm = true;
  }

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
      next: createdChannel => {
        this.channels.push(createdChannel);
        this.selectedChannel = createdChannel;
        this.messages = [];
        this.pinnedMessages = [];
        this.readReceipts = {};
        this.closeCreateChannelForm();
      },
      error: err => console.error('Error creating channel', err)
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
      next: () => {
        localStorage.removeItem('selectedHubId');
        this.closeDeleteModal();
        this.router.navigate(['/arenatalk']);
      },
      error: err => console.error('Error deleting hub', err)
    });
  }

  deleteChannelConfirmed(channelId: number): void {
    this.arenaService.deleteChannel(channelId).subscribe({
      next: () => {
        this.channels = this.channels.filter(c => c.id !== channelId);

        if (this.selectedChannel?.id === channelId) {
          this.selectedChannel = this.channels[0] || null;

          if (this.selectedChannel?.id) {
            this.loadMessagesByChannel(this.selectedChannel.id);
          } else {
            this.messages = [];
            this.pinnedMessages = [];
            this.readReceipts = {};
          }
        }

        this.closeDeleteModal();
      },
      error: err => console.error('Error deleting channel', err)
    });
  }

  deleteMessageConfirmed(messageId: number): void {
    this.arenaService.deleteMessage(messageId).subscribe({
      next: () => {
        this.messages = this.messages.filter(msg => msg.id !== messageId);
        this.pinnedMessages = this.pinnedMessages.filter(msg => msg.id !== messageId);
        delete this.readReceipts[messageId];
        this.closeDeleteModal();
      },
      error: err => console.error('Error deleting message', err)
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

    this.arenaService.updateMessage(message.id, {
      ...message,
      content: this.editedMessageContent.trim()
    }).subscribe({
      next: saved => {
        this.messages = this.messages.map(m => m.id === saved.id ? saved : m);
        this.pinnedMessages = this.pinnedMessages.map(m => m.id === saved.id ? saved : m);
        this.cancelEditMessage();
      },
      error: err => console.error('Error updating message', err)
    });
  }

  leaveHub(): void {
    if (!this.selectedHub?.id || !this.currentKeycloakId) return;

    this.hubMemberService.leaveHub(this.selectedHub.id, this.currentKeycloakId).subscribe({
      next: () => {
        localStorage.removeItem('selectedHubId');
        this.router.navigate(['/arenatalk']);
      },
      error: err => console.error('Error leaving hub', err)
    });
  }

  onHubImageError(event: Event, type: 'icon' | 'banner'): void {
    const img = event.target as HTMLImageElement;

    if (type === 'icon') img.style.display = 'none';

    if (type === 'banner') {
      const c = img.parentElement;
      if (c) c.style.display = 'none';
    }
  }

  scrollToMessage(msg: Message): void {
    if (!msg.id) return;

    setTimeout(() => {
      const el = document.getElementById(`msg-${msg.id}`);

      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.classList.add('highlighted');

        setTimeout(() => el.classList.remove('highlighted'), 2000);
      }
    }, 100);
  }

  get ownerMember(): HubMember | null {
    return this.activeMembers.find(m => m.role === 'OWNER') ?? null;
  }

  get ownerUserId(): string {
    const owner = this.ownerMember?.user as any;
    return owner?.keycloakId || owner?.auth0Id || String(owner?.id || '');
  }

  get ownerUserName(): string {
    const owner = this.ownerMember?.user;

    if (!owner) return 'Owner';

    return `${owner.firstName ?? ''} ${owner.lastName ?? ''}`.trim()
      || owner.email
      || 'Owner';
  }

  onGiftSent(message: string): void {
    this.giftNotification = message;

    clearTimeout(this.hideGiftTimeout);

    this.showGiftNotification = true;

    this.hideGiftTimeout = setTimeout(() => {
      this.showGiftNotification = false;
      this.cdr.detectChanges();
    }, 10000);
  }

  get hasMessages(): boolean {
    return this.messages.length > 0;
  }

  get categoryLabel(): string {
    return this.selectedHub?.category || 'COMMUNITY';
  }

  get activeMembers(): HubMember[] {
    return this.members.filter(m => m.status === 'ACTIVE');
  }

  get onlineMembers(): HubMember[] {
    return this.activeMembers.filter(m => m.online);
  }
}