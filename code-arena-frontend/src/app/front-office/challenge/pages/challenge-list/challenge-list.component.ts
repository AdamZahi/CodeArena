import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChallengeService } from '../../services/challenge.service';
import { Router } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-challenge-list',
  standalone: true,
  imports: [CommonModule, HttpClientModule, RouterModule],
  templateUrl: './challenge-list.component.html',
  styleUrls: ['./challenge-list.component.css']
})
export class ChallengeListComponent implements OnInit {
  challenges: any[] = [];
  isLoading = true;

  constructor(
    private challengeService: ChallengeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.challengeService.getAll().subscribe({
      next: (data) => {
        this.challenges = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  goToChallenge(id: string): void {
    this.router.navigate(['/challenge', id]);
  }
}
