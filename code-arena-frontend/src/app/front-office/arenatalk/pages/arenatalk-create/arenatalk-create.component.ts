import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { ArenatalkService } from '../../services/arenatalk.service';
import { Hub, TextChannel } from '../../models/arenatalk.model';

type CommunityCategory = 'GAMING' | 'PROGRAMMING' | 'ESPORT' | 'STUDY' | 'CUSTOM';
type CommunityVisibility = 'PUBLIC' | 'PRIVATE';

interface CategoryCard {
  key: CommunityCategory;
  label: string;
  description: string;
}

interface VisibilityCard {
  key: CommunityVisibility;
  label: string;
  description: string;
}

interface ChannelOption {
  name: string;
  topic: string;
  selected: boolean;
}

@Component({
  selector: 'app-arenatalk-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './arenatalk-create.component.html',
  styleUrl: './arenatalk-create.component.css'
})
export class ArenatalkCreateComponent {
  step = 1;
  loading = false;
  errorMessage = '';

  hubForm: FormGroup;

  categories: CategoryCard[] = [
    {
      key: 'GAMING',
      label: 'Gaming Squad',
      description: 'For players, strategy talks, team-up sessions, and game-focused discussions.'
    },
    {
      key: 'PROGRAMMING',
      label: 'Programming Team',
      description: 'For coding communities, dev talks, debugging help, and collaborative learning.'
    },
    {
      key: 'ESPORT',
      label: 'Esport Community',
      description: 'For competition-focused groups, match coordination, and training discussions.'
    },
    {
      key: 'STUDY',
      label: 'Study Group',
      description: 'For revision, shared notes, support, and organized academic collaboration.'
    },
    {
      key: 'CUSTOM',
      label: 'Custom Community',
      description: 'Create a fully personalized community with your own purpose and structure.'
    }
  ];

  visibilities: VisibilityCard[] = [
    {
      key: 'PUBLIC',
      label: 'Public',
      description: 'Anyone can discover and join this community.'
    },
    {
      key: 'PRIVATE',
      label: 'Private',
      description: 'Only invited or approved users can access this community.'
    }
  ];

  selectedCategory: CommunityCategory | null = null;
  selectedVisibility: CommunityVisibility | null = null;

  channelOptions: ChannelOption[] = [];

  constructor(
    private fb: FormBuilder,
    private arenatalkService: ArenatalkService,
    private router: Router
  ) {
    this.hubForm = this.fb.group({
      name: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(30),
          this.noWhitespaceValidator,
          Validators.pattern(/^[a-zA-Z0-9 _-]+$/)
        ]
      ],
      description: [
        '',
        [
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(200),
          this.noWhitespaceValidator
        ]
      ],
      bannerUrl: ['', [this.optionalUrlValidator]],
      iconUrl: ['', [this.optionalUrlValidator]]
    });
  }

  noWhitespaceValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value?.trim();
    return value ? null : { whitespace: true };
  }

  optionalUrlValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value?.trim();
    if (!value) {
      return null;
    }

    const urlPattern = /^(https?:\/\/)[^\s$.?#].[^\s]*$/i;
    return urlPattern.test(value) ? null : { invalidUrl: true };
  }

  selectCategory(category: CommunityCategory): void {
    this.selectedCategory = category;
    this.channelOptions = this.buildDefaultChannels(category);
    this.errorMessage = '';
  }

  selectVisibility(visibility: CommunityVisibility): void {
    this.selectedVisibility = visibility;
    this.errorMessage = '';
  }

  nextStep(): void {
    this.errorMessage = '';

    if (this.step === 1) {
      if (!this.selectedCategory) {
        this.errorMessage = 'Please select a category.';
        return;
      }
      this.step = 2;
      return;
    }

    if (this.step === 2) {
      if (this.hubForm.invalid) {
        this.hubForm.markAllAsTouched();
        this.errorMessage = 'Please correct the form errors before continuing.';
        return;
      }
      this.step = 3;
      return;
    }

    if (this.step === 3) {
      if (!this.selectedVisibility) {
        this.errorMessage = 'Please select a visibility.';
        return;
      }
      this.step = 4;
      return;
    }

    if (this.step === 4) {
      if (this.selectedChannelsCount === 0) {
        this.errorMessage = 'Please select at least one channel.';
        return;
      }
      this.step = 5;
    }
  }

  prevStep(): void {
    if (this.step > 1) {
      this.step--;
      this.errorMessage = '';
    }
  }

  toggleChannel(channel: ChannelOption): void {
    channel.selected = !channel.selected;
    this.errorMessage = '';
  }

  createCommunity(): void {
    if (!this.selectedCategory) {
      this.errorMessage = 'Please select a category.';
      return;
    }

    if (this.hubForm.invalid) {
      this.hubForm.markAllAsTouched();
      this.errorMessage = 'Please correct the form errors.';
      return;
    }

    if (!this.selectedVisibility) {
      this.errorMessage = 'Please select a visibility.';
      return;
    }

    if (this.selectedChannelsCount === 0) {
      this.errorMessage = 'Please select at least one channel.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const hubPayload: Hub = {
      name: this.hubForm.value.name.trim(),
      description: this.hubForm.value.description.trim(),
      bannerUrl: this.hubForm.value.bannerUrl?.trim() || '',
      iconUrl: this.hubForm.value.iconUrl?.trim() || '',
      category: this.selectedCategory as any,
      visibility: this.selectedVisibility as any
    };

    this.arenatalkService.createHub(hubPayload).pipe(
      switchMap((createdHub) => {
        const selectedChannels = this.channelOptions.filter((c) => c.selected);

        if (!createdHub.id || selectedChannels.length === 0) {
          return of({ createdHub, createdChannels: [] });
        }

        const requests = selectedChannels.map((channel) =>
          this.arenatalkService.createChannel(createdHub.id!, {
            name: channel.name,
            topic: channel.topic
          } as TextChannel)
        );

        return forkJoin(requests).pipe(
          switchMap((createdChannels) => of({ createdHub, createdChannels }))
        );
      })
    ).subscribe({
      next: ({ createdHub, createdChannels }) => {
        this.loading = false;

        localStorage.setItem('communityArena_selectedHub', JSON.stringify(createdHub));
        localStorage.setItem('communityArena_channels', JSON.stringify(createdChannels));

        this.router.navigate(['/arenatalk/workspace'], {
          state: {
            selectedHub: createdHub,
            createdChannels: createdChannels
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err?.error?.message || 'Failed to create the community. Please try again.';
      }
    });
  }

  get name(): AbstractControl | null {
    return this.hubForm.get('name');
  }

  get description(): AbstractControl | null {
    return this.hubForm.get('description');
  }

  get bannerUrl(): AbstractControl | null {
    return this.hubForm.get('bannerUrl');
  }

  get iconUrl(): AbstractControl | null {
    return this.hubForm.get('iconUrl');
  }

  get selectedChannelsCount(): number {
    return this.channelOptions.filter((c) => c.selected).length;
  }

  private buildDefaultChannels(category: CommunityCategory): ChannelOption[] {
    const common: ChannelOption[] = [
      { name: 'general', topic: 'Main community discussion', selected: true },
      { name: 'announcements', topic: 'Official updates and notices', selected: true }
    ];

    const categoryChannels: Record<CommunityCategory, ChannelOption[]> = {
      GAMING: [
        { name: 'strategy', topic: 'Gameplay strategies and tips', selected: true },
        { name: 'team-up', topic: 'Find teammates and squad up', selected: true }
      ],
      PROGRAMMING: [
        { name: 'dev-talk', topic: 'Coding discussions and technical exchange', selected: true },
        { name: 'resources', topic: 'Useful resources and shared learning material', selected: true }
      ],
      ESPORT: [
        { name: 'training', topic: 'Practice sessions and improvement plans', selected: true },
        { name: 'matches', topic: 'Competitive schedules and match talk', selected: true }
      ],
      STUDY: [
        { name: 'help', topic: 'Ask questions and get support', selected: true },
        { name: 'notes', topic: 'Share notes and learning summaries', selected: true }
      ],
      CUSTOM: [
        { name: 'ideas', topic: 'Brainstorming and concept discussion', selected: true },
        { name: 'resources', topic: 'Useful links and references', selected: false }
      ]
    };

    return [...common, ...categoryChannels[category]];
  }
}