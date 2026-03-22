import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../services/challenge.service';
import { SubmissionService } from '../../services/submission.service';

@Component({
  selector: 'app-challenge-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './challenge-detail.component.html',
  styleUrls: ['./challenge-detail.component.css']
})
export class ChallengeDetailComponent implements OnInit {
  challengeId!: string;
  challenge: any;
  isLoading = true;

  activeTab: 'description' | 'submissions' = 'description';

  code = '';
  language = '62';
  languages = [
    { id: '62', name: 'Java (OpenJDK 13)' },
    { id: '71', name: 'Python (3.8)' },
    { id: '50', name: 'C (GCC 9.2)' },
    { id: '54', name: 'C++ (GCC 9.2)' },
    { id: '63', name: 'JavaScript (Node 12)' }
  ];

  isSubmitting = false;
  submissionResult: any = null;
  mySubmissions: any[] = [];
  lineCount = 1;

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
        this.code = this.getBoilerplate(this.language);
        this.updateLineNumbers();
      },
      error: () => this.isLoading = false
    });
  }

  getBoilerplate(langId: string): string {
    switch (langId) {
      case '62': return 'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // Read input\n        \n        // Your solution here\n        \n    }\n}';
      case '71': return 'import sys\n\ndef solve():\n    # Read input\n    line = input()\n    \n    # Your solution here\n    \n    pass\n\nsolve()';
      case '63': return 'const readline = require(\'readline\');\nconst rl = readline.createInterface({ input: process.stdin });\nconst lines = [];\n\nrl.on(\'line\', (line) => lines.push(line));\nrl.on(\'close\', () => {\n    // Your solution here\n    \n});';
      case '50': return '#include <stdio.h>\n\nint main() {\n    // Read input\n    \n    // Your solution here\n    \n    return 0;\n}';
      case '54': return '#include <iostream>\nusing namespace std;\n\nint main() {\n    // Read input\n    \n    // Your solution here\n    \n    return 0;\n}';
      default: return '// Write your code here';
    }
  }

  onLanguageChange(): void {
    this.code = this.getBoilerplate(this.language);
    this.updateLineNumbers();
  }

  updateLineNumbers(): void {
    this.lineCount = (this.code.match(/\n/g) || []).length + 1;
  }

  getLineNumbers(): number[] {
    return Array.from({ length: Math.max(this.lineCount, 15) }, (_, i) => i + 1);
  }

  handleTab(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const textarea = event.target as HTMLTextAreaElement;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      this.code = this.code.substring(0, start) + '    ' + this.code.substring(end);
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 4;
      });
    }
  }

  submitCode(): void {
    if (!this.code.trim()) return;
    this.isSubmitting = true;
    this.submissionResult = null;

    const payload = {
      code: this.code,
      language: this.language,
      challengeId: this.challengeId
    };

    this.submissionService.submitCode(payload).subscribe({
      next: (res) => {
        this.submissionResult = res;
        this.isSubmitting = false;
        this.activeTab = 'submissions';
        this.pollSubmission(res.id);
      },
      error: (e) => {
        this.isSubmitting = false;
        this.submissionResult = { status: 'ERROR', errorOutput: 'Connection to backend failed.' };
        this.activeTab = 'submissions';
      }
    });
  }

  pollSubmission(submissionId: string): void {
    const interval = setInterval(() => {
      this.submissionService.getSubmissionStatus(submissionId).subscribe({
        next: (res) => {
          this.submissionResult = res;
          if (res.status !== 'PENDING') {
            clearInterval(interval);
            this.loadMySubmissions();
          }
        }
      });
    }, 2000);

    // Stop after 30 seconds regardless
    setTimeout(() => clearInterval(interval), 30000);
  }

  loadMySubmissions(): void {
    this.submissionService.getUserSubmissions('user-123').subscribe({
      next: (res) => {
        this.mySubmissions = res.filter((s: any) => s.challengeId === this.challengeId);
      }
    });
  }

  viewSubmissions(): void {
    this.activeTab = 'submissions';
    this.loadMySubmissions();
  }

  formatDescription(desc: string): string {
    if (!desc) return '';
    return desc.replace(/\n/g, '<br/>');
  }

  getVisibleTestCases(): any[] {
    if (!this.challenge?.testCases) return [];
    return this.challenge.testCases.filter((tc: any) => !tc.isHidden);
  }

  getTags(tags: string): string[] {
    if (!tags) return [];
    return tags.split(',').map((t: string) => t.trim());
  }

  getLanguageName(langId: string): string {
    const lang = this.languages.find(l => l.id === langId);
    return lang ? lang.name : langId;
  }
}
