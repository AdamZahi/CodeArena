import { Component, inject } from '@angular/core';
import { AsyncPipe, NgIf } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { User } from '@auth0/auth0-spa-js';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [AsyncPipe, NgIf],
  template: `
    <section>
      <h1>My Profile</h1>
      <h2 *ngIf="user$ | async as user">{{ user?.name ?? 'Anonymous' }}</h2>
      <p *ngIf="user$ | async as user">{{ user?.email }}</p>
      <img *ngIf="user$ | async as user" [src]="user?.picture" alt="Avatar" width="100" />
      <form>
        <label>
          Avatar URL
          <input type="text" name="avatarUrl" />
        </label>
        <label>
          Bio
          <textarea name="bio"></textarea>
        </label>
        <button type="submit">Save</button>
      </form>
    </section>
  `
})
export class ProfileComponent {
  private readonly auth = inject(AuthService);
  readonly user$: Observable<User | null | undefined> = this.auth.user$;
}
