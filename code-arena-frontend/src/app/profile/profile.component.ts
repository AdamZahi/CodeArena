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
    <section *ngIf="user$ | async as user">
      <h1>My Profile</h1>
      <h2>{{ user['name'] ?? 'User' }}</h2>
      <p>{{ user['email'] }}</p>
      <img *ngIf="user['picture']" [src]="user['picture']" alt="Profile picture" width="120" />

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
  private auth = inject(AuthService);
  user$: Observable<User | null | undefined> = this.auth.user$;
}