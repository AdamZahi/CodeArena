import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { CurrentUser, AuthUserSyncService } from '../../../../core/auth/auth-user-sync.service';
import { CustomizeIdentityComponent } from './components/customize-identity/customize-identity.component';
import { SubmissionService, SubmissionDto } from '../../../../core/services/submission.service';
import { Observable, interval } from 'rxjs';
import { switchMap, map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, CustomizeIdentityComponent],
  templateUrl: './my-profile.component.html',
  styleUrls: ['./my-profile.component.css']
})
export class MyProfileComponent {
  private readonly auth = inject(AuthService);
  private readonly authUserSync = inject(AuthUserSyncService);
  private readonly submissionService = inject(SubmissionService);

  readonly user$ = this.auth.user$;
  readonly currentUser$ = this.authUserSync.currentUser$;


  readonly submissions$: Observable<SubmissionDto[]> = this.currentUser$.pipe(
    switchMap(user => {
      if (user?.keycloakId) {
        return this.submissionService.getUserSubmissions(user.keycloakId).pipe(
          map(submissions => submissions.slice(0, 10)) // Get last 10 entries
        );
      }
      return [];
    })
  );

  isCustomizeModalOpen = false;

  openCustomizeModal() {
    this.isCustomizeModalOpen = true;
  }

  closeCustomizeModal() {
    this.isCustomizeModalOpen = false;
  }
}
