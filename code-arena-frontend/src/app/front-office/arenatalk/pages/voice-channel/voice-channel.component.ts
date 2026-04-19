import { Component, Input, OnInit, OnChanges, OnDestroy, SimpleChanges, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { VoiceChannelService, VoiceChannel } from '../../services/voice-channel.service';
import { VoiceSignalingService, VoiceParticipant } from '../../services/voice-signaling.service';

@Component({
  selector: 'app-voice-channel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './voice-channel.component.html',
  styleUrl: './voice-channel.component.css'
})
export class VoiceChannelComponent implements OnInit, OnChanges, OnDestroy {

  @Input() hubId: number | null | undefined = null;
  @Input() isOwner = false;
  @Input() currentUserId = '';
  @Input() currentUserName = '';
  @Output() voiceRoomChanged = new EventEmitter<{channelId: number | null, channelName: string}>();

  voiceChannels: VoiceChannel[] = [];
  activeChannelId: number | null = null;
  activeChannelName = '';
  participants: VoiceParticipant[] = [];
  inRoom = false;
  isMuted = false;
  roomFull = false;
  kicked = false;
  showCreateForm = false;
  newChannelName = '';
  localSpeaking = false;
  maxParticipants = 8;

  private subs: Subscription[] = [];

  constructor(
    private vcService: VoiceChannelService,
    public signalingService: VoiceSignalingService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    if (this.hubId) this.loadChannels();

    this.subs.push(
      this.signalingService.participants$.subscribe(p => this.participants = p),
      this.signalingService.inRoom$.subscribe(v => this.inRoom = v),
      this.signalingService.isMuted$.subscribe(v => this.isMuted = v),
      this.signalingService.roomFull$.subscribe(v => this.roomFull = v),
      this.signalingService.kicked$.subscribe(v => {
        if (v) {
          this.kicked = true;
          this.activeChannelId = null;
          this.voiceRoomChanged.emit({ channelId: null, channelName: '' });
        }
      }),
      this.signalingService.localSpeaking$.subscribe(v => this.localSpeaking = v)
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['hubId'] && this.hubId) {
      this.voiceChannels = [];
      this.loadChannels();
    }
  }

  loadChannels(): void {
    if (!this.hubId) return;
    this.vcService.getByHub(this.hubId).subscribe({
      next: (channels) => this.voiceChannels = channels,
      error: (err) => console.error('Error loading voice channels', err)
    });
  }

  joinChannel(channel: VoiceChannel): void {
    this.kicked = false;
    if (this.inRoom) {
      this.signalingService.leaveRoom().then(() => {
        this.activeChannelId = null;
        this.startJoin(channel);
      });
    } else {
      this.startJoin(channel);
    }
  }

  private startJoin(channel: VoiceChannel): void {
    this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
      this.activeChannelId = channel.id;
      this.activeChannelName = channel.name;
      this.roomFull = false;
      this.signalingService.joinRoom(
        String(channel.id),
        this.currentUserId,
        this.currentUserName,
        token
      );
      this.voiceRoomChanged.emit({ channelId: channel.id, channelName: channel.name });
    });
  }

  leaveChannel(): void {
    this.signalingService.leaveRoom().then(() => {
      this.activeChannelId = null;
      this.activeChannelName = '';
      this.voiceRoomChanged.emit({ channelId: null, channelName: '' });
    });
  }

  toggleMute(): void {
    this.signalingService.toggleMute();
  }

  kickParticipant(userId: string): void {
    this.signalingService.kickParticipant(userId);
  }

  get participantCount(): number {
    return this.participants.length + (this.inRoom ? 1 : 0);
  }

  createChannel(): void {
    if (!this.hubId || !this.newChannelName.trim()) return;
    this.vcService.create(this.hubId, this.newChannelName.trim()).subscribe({
      next: (channel) => {
        this.voiceChannels.push(channel);
        this.newChannelName = '';
        this.showCreateForm = false;
      },
      error: (err) => console.error('Error creating voice channel', err)
    });
  }

  deleteChannel(channelId: number): void {
    this.vcService.delete(channelId).subscribe({
      next: () => {
        this.voiceChannels = this.voiceChannels.filter(c => c.id !== channelId);
        if (this.activeChannelId === channelId) {
          this.signalingService.leaveRoom();
          this.activeChannelId = null;
          this.voiceRoomChanged.emit({ channelId: null, channelName: '' });
        }
      },
      error: (err) => console.error('Error deleting voice channel', err)
    });
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    if (this.inRoom) this.signalingService.leaveRoom();
  }
}