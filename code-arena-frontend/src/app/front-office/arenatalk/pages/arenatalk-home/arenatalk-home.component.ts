import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Hub, TextChannel } from '../../models/arenatalk.model';

@Component({
  selector: 'app-arenatalk-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './arenatalk-home.component.html',
  styleUrl: './arenatalk-home.component.css'
})
export class ArenatalkHomeComponent {
  constructor(private router: Router) {}

  goToJoin(): void {
    this.router.navigate(['/arenatalk/join']);
  }

  goToCreate(): void {
    this.router.navigate(['/arenatalk/create']);
  }

  goToMySpace(): void {
    const savedHub = localStorage.getItem('communityArena_selectedHub');
    const savedChannels = localStorage.getItem('communityArena_channels');

    if (savedHub) {
      const selectedHub: Hub = JSON.parse(savedHub);
      const createdChannels: TextChannel[] = savedChannels ? JSON.parse(savedChannels) : [];

      this.router.navigate(['/arenatalk/workspace'], {
        state: {
          selectedHub,
          createdChannels
        }
      });
    } else {
      this.router.navigate(['/arenatalk/join']);
    }
  }
}