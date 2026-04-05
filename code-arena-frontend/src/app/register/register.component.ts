import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, NgIf],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  showEmailForm = false;
  isSubmitting = false;
  formError = '';

  readonly emailSignupForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_.-]+$/)]],
    bio: ['', [Validators.maxLength(280)]],
    avatarUrl: ['', [Validators.pattern(/^$|^https?:\/\/.+/i)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  private get returnUrl(): string {
    return this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
  }

  registerWithGoogle(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'google-oauth2' },
      appState: { target: this.returnUrl }
    });
  }

  registerWithGitHub(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'github' },
      appState: { target: this.returnUrl }
    });
  }

  registerWithEmail(): void {
    this.showEmailForm = true;
    this.formError = '';
  }

  submitEmailRegistration(): void {
    this.formError = '';
    this.emailSignupForm.markAllAsTouched();

    if (this.emailSignupForm.invalid) {
      return;
    }

    const formValue = this.emailSignupForm.getRawValue();
    if (formValue.password !== formValue.confirmPassword) {
      this.formError = 'Password confirmation does not match.';
      return;
    }

    const [firstName, ...rest] = formValue.fullName.trim().split(/\s+/);
    const lastName = rest.join(' ').trim() || null;

    const pendingProfile = {
      firstName,
      lastName,
      nickname: formValue.username.trim(),
      bio: formValue.bio.trim(),
      avatarUrl: formValue.avatarUrl.trim()
    };

    sessionStorage.setItem('codearena.pendingSignupProfile', JSON.stringify(pendingProfile));

    this.isSubmitting = true;
    this.http.post(
      `https://${environment.auth0Domain}/dbconnections/signup`,
      {
        client_id: environment.auth0ClientId,
        email: formValue.email.trim().toLowerCase(),
        password: formValue.password,
        connection: 'Username-Password-Authentication',
        user_metadata: {
          full_name: formValue.fullName.trim(),
          username: formValue.username.trim(),
          bio: formValue.bio.trim(),
          avatar_url: formValue.avatarUrl.trim()
        }
      }
    ).subscribe({
      next: () => {
        void this.auth.loginWithRedirect({
          authorizationParams: {
            login_hint: formValue.email.trim().toLowerCase()
          },
          appState: { target: this.returnUrl }
        });
      },
      error: (error: { error?: string; description?: string; message?: string }) => {
        this.isSubmitting = false;
        this.formError = error?.description || error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  get fullName() {
    return this.emailSignupForm.controls.fullName;
  }

  get username() {
    return this.emailSignupForm.controls.username;
  }

  get email() {
    return this.emailSignupForm.controls.email;
  }

  get password() {
    return this.emailSignupForm.controls.password;
  }

  get confirmPassword() {
    return this.emailSignupForm.controls.confirmPassword;
  }
}
