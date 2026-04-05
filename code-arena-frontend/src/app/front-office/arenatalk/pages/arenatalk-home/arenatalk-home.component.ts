import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { take } from 'rxjs/operators';
import { AuthService } from '@auth0/auth0-angular';
import { Hub, TextChannel } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';
import { HubMemberService } from '../../services/hub-member.service';
import { NotificationService, ArenNotification } from '../../services/notification.service';

@Component({
  selector: 'app-arenatalk-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './arenatalk-home.component.html',
  styleUrl: './arenatalk-home.component.css'
})
export class ArenatalkHomeComponent implements OnInit {

  showHubSelector = false;
  myHubs: Hub[] = [];
  loadingHubs = false;
  currentKeycloakId = '';

  showNotifications = false;
  notifications: ArenNotification[] = [];

  constructor(
    private router: Router,
    private auth: AuthService,
    private arenaService: ArenatalkService,
    private hubMemberService: HubMemberService,
    public notifService: NotificationService
  ) {}

  ngOnInit(): void {
    this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.currentKeycloakId = payload.sub;
      this.notifService.loadNotifications(this.currentKeycloakId);
    });

    this.notifService.notifications$.subscribe(n => this.notifications = n);
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  closeNotifications(): void {
    this.showNotifications = false;
  }

  onNotificationClick(notif: ArenNotification): void {
    if (!notif.read) {
      this.notifService.markAsRead(notif.id).subscribe(() => {
        notif.read = true;
      });
    }
    if (notif.type === 'ACCEPTED' && notif.hubId) {
      this.arenaService.getHubById(notif.hubId).pipe(take(1)).subscribe(hub => {
        this.arenaService.getChannelsByHub(hub.id!).subscribe(channels => {
          this.showNotifications = false;
          this.router.navigate(['/arenatalk/workspace'], {
            state: { selectedHub: hub, createdChannels: channels }
          });
        });
      });
    }
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.currentKeycloakId).subscribe(() => {
      this.notifications.forEach(n => n.read = true);
    });
  }

  goToJoin(): void { this.router.navigate(['/arenatalk/join']); }
  goToCreate(): void { this.router.navigate(['/arenatalk/create']); }

  goToMySpace(): void {
    if (!this.currentKeycloakId) return;
    this.loadingHubs = true;
    this.hubMemberService.getMyHubIds(this.currentKeycloakId).subscribe({
      next: (ids) => {
        if (ids.length === 0) { this.loadingHubs = false; this.router.navigate(['/arenatalk/join']); return; }
        Promise.all(ids.map(id => new Promise<Hub>(resolve =>
          this.arenaService.getHubById(id).pipe(take(1)).subscribe(resolve)
        ))).then(hubs => {
          this.myHubs = hubs;
          this.loadingHubs = false;
          this.showHubSelector = true;
        });
      },
      error: () => { this.loadingHubs = false; this.router.navigate(['/arenatalk/join']); }
    });
  }

  selectHub(hub: Hub): void {
    if (!hub.id) return;
    this.arenaService.getChannelsByHub(hub.id).subscribe({
      next: (channels: TextChannel[]) => {
        this.showHubSelector = false;
        this.router.navigate(['/arenatalk/workspace'], {
          state: { selectedHub: hub, createdChannels: channels }
        });
      },
      error: (err) => console.error('Error loading channels', err)
    });
  }

  closeSelector(): void { this.showHubSelector = false; }
}