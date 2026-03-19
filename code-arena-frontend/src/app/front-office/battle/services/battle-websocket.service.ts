import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class BattleWebsocketService {
  connect(roomId: string): void {
    // TODO: Connect STOMP client to /ws and subscribe to /topic/battle/{roomId}.
    void roomId;
  }
}
