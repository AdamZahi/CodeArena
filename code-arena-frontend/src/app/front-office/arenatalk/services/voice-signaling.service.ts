import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
declare var SockJS: any;
import { BehaviorSubject } from 'rxjs';

export interface VoiceParticipant {
  userId: string;
  userName: string;
  muted: boolean;
  stream?: MediaStream;
}

@Injectable({ providedIn: 'root' })
export class VoiceSignalingService {

  private stompClient: Client | null = null;
  private peerConnections: Map<string, RTCPeerConnection> = new Map();
  private localStream: MediaStream | null = null;
  private subscription: StompSubscription | null = null;
  private currentChannelId: string | null = null;
  private currentUserId: string | null = null;
  private currentUserName: string | null = null;

  participants$ = new BehaviorSubject<VoiceParticipant[]>([]);
  inRoom$ = new BehaviorSubject<boolean>(false);
  isMuted$ = new BehaviorSubject<boolean>(false);
  roomFull$ = new BehaviorSubject<boolean>(false);

  private iceServers = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  };

  async joinRoom(channelId: string, userId: string, userName: string, token: string): Promise<void> {
    this.currentChannelId = channelId;
    this.currentUserId = userId;
    this.currentUserName = userName;

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    } catch (err) {
      console.error('Microphone access denied', err);
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        this.subscribeToSignaling();
        this.sendJoin();
      }
    });

    this.stompClient.activate();
    this.inRoom$.next(true);
  }

  private subscribeToSignaling(): void {
    if (!this.stompClient || !this.currentUserId) return;

    this.subscription = this.stompClient.subscribe(
      `/user/${this.currentUserId}/queue/voice`,
      (message: IMessage) => {
        const msg = JSON.parse(message.body);
        this.handleSignalingMessage(msg);
      }
    );
  }

  private async handleSignalingMessage(msg: any): Promise<void> {
    switch (msg.type) {
      case 'room-full':
        this.roomFull$.next(true);
        await this.leaveRoom();
        break;

      case 'room-participants':
        if (msg.payload) {
          const existingUsers = msg.payload.split(',')
            .filter((id: string) => id && id !== this.currentUserId);
          for (const userId of existingUsers) {
            await this.createOffer(userId);
          }
        }
        break;

      case 'user-joined':
        this.addParticipant(msg.fromUserId, msg.userName);
        break;

      case 'user-left':
        this.removeParticipant(msg.fromUserId);
        break;

      case 'offer':
        await this.handleOffer(msg);
        break;

      case 'answer':
        await this.handleAnswer(msg);
        break;

      case 'ice-candidate':
        await this.handleIceCandidate(msg);
        break;
    }
  }

  private async createOffer(targetUserId: string): Promise<void> {
    const pc = this.createPeerConnection(targetUserId);
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);

    this.sendSignal({
      type: 'offer',
      fromUserId: this.currentUserId!,
      toUserId: targetUserId,
      channelId: this.currentChannelId!,
      payload: JSON.stringify(offer),
      userName: this.currentUserName!
    });
  }

  private async handleOffer(msg: any): Promise<void> {
    const pc = this.createPeerConnection(msg.fromUserId);
    await pc.setRemoteDescription(new RTCSessionDescription(JSON.parse(msg.payload)));
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);

    this.addParticipant(msg.fromUserId, msg.userName);

    this.sendSignal({
      type: 'answer',
      fromUserId: this.currentUserId!,
      toUserId: msg.fromUserId,
      channelId: this.currentChannelId!,
      payload: JSON.stringify(answer),
      userName: this.currentUserName!
    });
  }

  private async handleAnswer(msg: any): Promise<void> {
    const pc = this.peerConnections.get(msg.fromUserId);
    if (pc) {
      await pc.setRemoteDescription(new RTCSessionDescription(JSON.parse(msg.payload)));
    }
  }

  private async handleIceCandidate(msg: any): Promise<void> {
    const pc = this.peerConnections.get(msg.fromUserId);
    if (pc && msg.payload) {
      await pc.addIceCandidate(new RTCIceCandidate(JSON.parse(msg.payload)));
    }
  }

  private createPeerConnection(targetUserId: string): RTCPeerConnection {
    const pc = new RTCPeerConnection(this.iceServers);
    this.peerConnections.set(targetUserId, pc);

    if (this.localStream) {
      this.localStream.getTracks().forEach(track => pc.addTrack(track, this.localStream!));
    }

    pc.ontrack = (event) => {
      const remoteStream = event.streams[0];
      const audio = new Audio();
      audio.srcObject = remoteStream;
      audio.play();

      const current = this.participants$.value;
      const updated = current.map(p =>
        p.userId === targetUserId ? { ...p, stream: remoteStream } : p
      );
      this.participants$.next(updated);
    };

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignal({
          type: 'ice-candidate',
          fromUserId: this.currentUserId!,
          toUserId: targetUserId,
          channelId: this.currentChannelId!,
          payload: JSON.stringify(event.candidate),
          userName: this.currentUserName!
        });
      }
    };

    return pc;
  }

  private sendJoin(): void {
    this.stompClient?.publish({
      destination: '/app/voice/join',
      body: JSON.stringify({
        type: 'join',
        fromUserId: this.currentUserId,
        channelId: this.currentChannelId,
        userName: this.currentUserName
      })
    });
  }

  private sendSignal(msg: any): void {
    this.stompClient?.publish({
      destination: '/app/voice/signal',
      body: JSON.stringify(msg)
    });
  }

  async leaveRoom(): Promise<void> {
    this.stompClient?.publish({
      destination: '/app/voice/leave',
      body: JSON.stringify({
        type: 'leave',
        fromUserId: this.currentUserId,
        channelId: this.currentChannelId
      })
    });

    this.peerConnections.forEach(pc => pc.close());
    this.peerConnections.clear();

    this.localStream?.getTracks().forEach(track => track.stop());
    this.localStream = null;

    this.subscription?.unsubscribe();
    this.stompClient?.deactivate();
    this.stompClient = null;

    this.participants$.next([]);
    this.inRoom$.next(false);
    this.isMuted$.next(false);
    this.currentChannelId = null;
  }

  toggleMute(): void {
    if (!this.localStream) return;
    const audioTrack = this.localStream.getAudioTracks()[0];
    if (audioTrack) {
      audioTrack.enabled = !audioTrack.enabled;
      this.isMuted$.next(!audioTrack.enabled);
    }
  }

  private addParticipant(userId: string, userName: string): void {
    const current = this.participants$.value;
    if (!current.find(p => p.userId === userId)) {
      this.participants$.next([...current, { userId, userName, muted: false }]);
    }
  }

  private removeParticipant(userId: string): void {
    const pc = this.peerConnections.get(userId);
    if (pc) { pc.close(); this.peerConnections.delete(userId); }
    this.participants$.next(this.participants$.value.filter(p => p.userId !== userId));
  }
}