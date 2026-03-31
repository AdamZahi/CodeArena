import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';
import { StoryMission, StoryChapter } from '../../../../front-office/terminal-quest/models/terminal-quest.model';

@Component({
  selector: 'app-mission-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mission-list.component.html',
  styleUrls: ['./mission-list.component.css']
})
export class MissionListComponent implements OnInit {
  chapterId = '';
  chapter: StoryChapter | null = null;
  missions: StoryMission[] = [];
  isLoading = true;
  errorMsg = '';
  deletingId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private adminTqService: AdminTqService
  ) {}

  ngOnInit(): void {
    this.chapterId = this.route.snapshot.paramMap.get('chapterId') || '';

    this.adminTqService.getChapterById(this.chapterId).subscribe({
      next: (ch) => { this.chapter = ch; },
      error: (err) => console.error('Failed to load chapter:', err)
    });

    this.loadMissions();
  }

  private loadMissions(): void {
    this.isLoading = true;
    this.adminTqService.getMissionsByChapter(this.chapterId).subscribe({
      next: (missions) => {
        this.missions = missions;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load missions:', err);
        this.errorMsg = 'Failed to load missions.';
        this.isLoading = false;
      }
    });
  }

  confirmDelete(mission: StoryMission): void {
    if (!confirm(`Delete mission "${mission.title}"?`)) return;
    this.deletingId = mission.id;
    this.adminTqService.deleteMission(mission.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.loadMissions();
      },
      error: (err) => {
        console.error('Delete failed:', err);
        this.errorMsg = 'Failed to delete mission.';
        this.deletingId = null;
      }
    });
  }

  diffClass(difficulty: string): string {
    return 'diff-' + difficulty.toLowerCase();
  }

  truncate(text: string, max = 60): string {
    return text.length > max ? text.slice(0, max) + '…' : text;
  }
}
