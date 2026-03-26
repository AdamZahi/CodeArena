import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ArenaTalkService } from '../../services/arenatalk.service';
import { Hub } from '../../models/arenatalk.model';

@Component({
  selector: 'app-arenatalk-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './arenatalk-create.component.html',
  styleUrl: './arenatalk-create.component.css'
})
export class ArenaTalkCreateComponent {
  newHub: Hub = {
    name: '',
    description: '',
    bannerUrl: ''
  };

  loading = false;
  errorMessage = '';

  constructor(
    private arenaTalkService: ArenaTalkService,
    private router: Router
  ) {}

  createHub(): void {
    if (!this.newHub.name.trim()) {
      this.errorMessage = 'Hub name is required.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.arenaTalkService.createHub(this.newHub).subscribe({
      next: (createdHub) => {
        this.loading = false;
        this.router.navigate(['/arenatalk/workspace'], {
          state: { selectedHub: createdHub }
        });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Something went wrong while creating the hub.';
      }
    });
  }
}