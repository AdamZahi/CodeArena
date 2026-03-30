import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Hub, TextChannel } from '../../models/arenatalk.model';
import { ArenatalkService } from '../../services/arenatalk.service';

@Component({
  selector: 'app-arenatalk-join',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-join.component.html',
  styleUrl: './arenatalk-join.component.css'
})
export class ArenaTalkJoinComponent implements OnInit {
  hubs: Hub[] = [];
  searchTerm = '';
  selectedCategory = 'ALL';
  loading = false;

  categories = ['ALL', 'GAMING', 'PROGRAMMING', 'ESPORT', 'STUDY', 'CUSTOM'];

  constructor(
    private arenaService: ArenatalkService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadHubs();
  }

  loadHubs(): void {
    this.loading = true;

    this.arenaService.getHubs().subscribe({
      next: (data) => {
        this.hubs = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading hubs', err);
        this.loading = false;
      }
    });
  }

  get filteredHubs(): Hub[] {
    return this.hubs.filter((hub) => {
      const matchesSearch =
        hub.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        hub.description.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesCategory =
        this.selectedCategory === 'ALL' || hub.category === this.selectedCategory;

      return matchesSearch && matchesCategory;
    });
  }

  joinCommunity(hub: Hub): void {
    if (hub.visibility !== 'PUBLIC' || !hub.id) return;

    this.arenaService.getChannelsByHub(hub.id).subscribe({
     next: (channels: TextChannel[]) => {
  localStorage.setItem('communityArena_selectedHub', JSON.stringify(hub));
  localStorage.setItem('communityArena_channels', JSON.stringify(channels));

  this.router.navigate(['/arenatalk/workspace'], {
    state: {
      selectedHub: hub,
      createdChannels: channels
    }
  });
},
      error: (err) => {
        console.error('Error loading channels', err);
      }
    });
  }
}