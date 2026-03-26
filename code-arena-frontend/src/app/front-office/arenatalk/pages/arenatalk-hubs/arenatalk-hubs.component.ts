import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ArenaTalkService } from '../../services/arenatalk.service';
import { Hub } from '../../models/arenatalk.model';

@Component({
  selector: 'app-arenatalk-hubs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-hubs.component.html',
  styleUrl: './arenatalk-hubs.component.css'
})
export class ArenaTalkHubsComponent implements OnInit {
  search = '';
  hubs: Hub[] = [];

  newHub: Hub = {
    name: '',
    description: '',
    bannerUrl: ''
  };

  constructor(
    private router: Router,
    private arenaTalkService: ArenaTalkService
  ) {}

  ngOnInit(): void {
    this.loadHubs();
  }

  loadHubs(): void {
    this.arenaTalkService.getHubs().subscribe({
      next: (data) => {
        this.hubs = data;
      },
      error: (err) => {
        console.error('Error loading hubs:', err);
      }
    });
  }

  get filteredHubs(): Hub[] {
    const q = this.search.toLowerCase().trim();
    if (!q) return this.hubs;

    return this.hubs.filter(hub =>
      hub.name.toLowerCase().includes(q) ||
      hub.description.toLowerCase().includes(q)
    );
  }

  createHub(): void {
    if (!this.newHub.name.trim()) return;

    this.arenaTalkService.createHub(this.newHub).subscribe({
      next: () => {
        this.newHub = {
          name: '',
          description: '',
          bannerUrl: ''
        };
        this.loadHubs();
      },
      error: (err) => {
        console.error('Error creating hub:', err);
      }
    });
  }

  openWorkspace(hub: Hub): void {
    this.router.navigate(['/arenatalk/workspace'], {
      state: { selectedHub: hub }
    });
  }
}