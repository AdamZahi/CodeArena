import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { take } from 'rxjs/operators';
import { AuthService } from '@auth0/auth0-angular';
import { Hub, TextChannel } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';
import { HubMemberService } from '../../services/hub-member.service';

@Component({
  selector: 'app-arenatalk-join',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-join.component.html',
  styleUrl: './arenatalk-join.component.css'
})
export class ArenaTalkJoinComponent implements OnInit {

  hubs: Hub[] = [];
  myHubIds: Set<number> = new Set();
  searchTerm = '';
  selectedCategory = 'ALL';
  loading = false;

  hubStates: Map<number, 'NONE' | 'ACTIVE' | 'PENDING'> = new Map();
  joiningHubId: number | null = null;
  currentKeycloakId = '';

  categories = ['ALL', 'GAMING', 'PROGRAMMING', 'ESPORT', 'STUDY', 'CUSTOM'];

  constructor(
    private arenaService: ArenatalkService,
    private hubMemberService: HubMemberService,
    private router: Router,
    private auth: AuthService
  ) {}

 ngOnInit(): void {
  this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
    const payload = JSON.parse(atob(token.split('.')[1]));
    this.currentKeycloakId = payload.sub;
    this.loadHubsWithState();
  });
}

loadHubsWithState(): void {
  this.loading = true;
  this.arenaService.getHubs().subscribe({
    next: (data) => {
      this.hubMemberService.getMyHubIds(this.currentKeycloakId).subscribe({
        next: (myIds) => {
          this.myHubIds = new Set(myIds);
          this.hubs = data;
          this.loading = false;
          data.forEach(hub => {
            if (hub.id) this.hubStates.set(hub.id, 'NONE');
          });
        },
        error: () => {
          this.hubs = data;
          this.loading = false;
        }
      });
    },
    error: (err) => {
      console.error('Error loading hubs', err);
      this.loading = false;
    }
  });
}

  get filteredHubs(): Hub[] {
    return this.hubs.filter((hub) => {
      if (hub.id && this.myHubIds.has(hub.id)) return false; // hide user's hubs
      const matchesSearch =
        hub.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        hub.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesCategory =
        this.selectedCategory === 'ALL' || hub.category === this.selectedCategory;
      return matchesSearch && matchesCategory;
    });
  }

  getHubState(hubId: number): 'NONE' | 'ACTIVE' | 'PENDING' {
    return this.hubStates.get(hubId) ?? 'NONE';
  }

  onJoinClick(hub: Hub): void {
    if (!hub.id) return;

    this.auth.isAuthenticated$.pipe(take(1)).subscribe(isAuth => {
      if (!isAuth) { this.auth.loginWithRedirect(); return; }

      this.auth.getAccessTokenSilently().pipe(take(1)).subscribe(token => {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const keycloakId = payload.sub;
        this.joiningHubId = hub.id!;

        this.hubMemberService.joinHub(hub.id!, keycloakId).subscribe({
          next: (member) => {
            this.joiningHubId = null;
            if (member.status === 'ACTIVE') {
              this.hubStates.set(hub.id!, 'ACTIVE');
              this.goToWorkspace(hub);
            } else if (member.status === 'PENDING') {
              this.hubStates.set(hub.id!, 'PENDING');
            }
          },
          error: (err) => {
            this.joiningHubId = null;
            if (err.status === 409) {
              this.hubStates.set(hub.id!, 'ACTIVE');
            } else {
              console.error('Error joining hub', err);
            }
          }
        });
      });
    });
  }

  goToWorkspace(hub: Hub): void {
    if (!hub.id) return;
    this.arenaService.getChannelsByHub(hub.id).subscribe({
      next: (channels: TextChannel[]) => {
        this.router.navigate(['/arenatalk/workspace'], {
          state: { selectedHub: hub, createdChannels: channels }
        });
      },
      error: (err) => console.error('Error loading channels', err)
    });
  }
}