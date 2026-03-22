import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../services/challenge.service';
import { SubmissionService } from '../../services/submission.service';

@Component({
  selector: 'app-challenge-detail',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './challenge-detail.component.html',
  styleUrls: ['./challenge-detail.component.css']
})
export class ChallengeDetailComponent implements OnInit {
  challengeId!: string;
  challenge: any;
  isLoading = true;

  activeTab: 'description' | 'submissions' = 'description';

  // Editor State
  code = '';
  language = '62'; // Judge0 ID for Java by default, maybe configurable
  languages = [
    { id: '62', name: 'Java' },
    { id: '71', name: 'Python' },
    { id: '50', name: 'C' },
    { id: '54', name: 'C++' },
    { id: '63', name: 'JavaScript' }
  ];

  isSubmitting = false;
  submissionResult: any = null;

  mySubmissions: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private challengeService: ChallengeService,
    private submissionService: SubmissionService
  ) {}

  ngOnInit(): void {
    this.challengeId = this.route.snapshot.paramMap.get('id')!;
    this.loadChallenge();
  }

  loadChallenge(): void {
    this.challengeService.getById(this.challengeId).subscribe({
      next: (data) => {
        this.challenge = data;
        this.isLoading = false;
        // set default boilerplate
        this.code = this.getBoilerplate(this.language);
      },
      error: () => this.isLoading = false
    });
  }

  getBoilerplate(langId: string): string {
    switch (langId) {
      case '62': return 'public class Main {\n    public static void main(String[] args) {\n        // Write your code here\n    }\n}';
      case '71': return 'def main():\n    # Write your code here\n    pass\n\nif __name__ == "__main__":\n    main()';
      case '63': return 'function main() {\n    // Write your code here\n}\n\nmain();';
      case '50': return '#include <stdio.h>\n\nint main() {\n    // Write your code here\n    return 0;\n}';
      case '54': return '#include <iostream>\n\nint main() {\n    // Write your code here\n    return 0;\n}';
      default: return '// Write your code here';
    }
  }

  onLanguageChange(): void {
    this.code = this.getBoilerplate(this.language);
  }

  submitCode(): void {
    if (!this.code.trim()) return;
    this.isSubmitting = true;
    this.submissionResult = null;
    this.activeTab = 'submissions'; // auto switch to see result

    const payload = {
      code: this.code,
      language: this.language,
      challengeId: this.challengeId
    };

    this.submissionService.submitCode(payload).subscribe({
      next: (res) => {
        this.submissionResult = res;
        this.isSubmitting = false;
        this.loadMySubmissions();
      },
      error: (e) => {
        console.error(e);
        this.isSubmitting = false;
        this.submissionResult = { status: 'ERROR', errorOutput: 'Failed to connect to backend.' };
      }
    });
  }

  loadMySubmissions(): void {
    // In a real scenario, fetch based on user session
    // Since we mocked "user-123" in controller, let's fetch for it
    this.submissionService.getUserSubmissions("user-123").subscribe({
      next: (res) => {
        // filter by challenge
        this.mySubmissions = res.filter((s: any) => s.challengeId === this.challengeId);
      }
    });
  }

  viewSubmissions(): void {
    this.activeTab = 'submissions';
    this.loadMySubmissions();
  }
}
