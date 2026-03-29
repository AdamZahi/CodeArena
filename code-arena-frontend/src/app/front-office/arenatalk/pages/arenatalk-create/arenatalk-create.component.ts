import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
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
  imports: [CommonModule, FormsModule],
  templateUrl: './arenatalk-create.component.html',
  styleUrl: './arenatalk-create.component.css'
})
export class ArenatalkCreateComponent {
  step = 1;
  loading = false;
  errorMessage = '';

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

  hubData: Hub = {
    name: '',
    description: '',
    bannerUrl: '',
    iconUrl: '',
    category: undefined as any,
    visibility: undefined as any
  };

  channelOptions: ChannelOption[] = [];

  constructor(
    private arenatalkService: ArenatalkService,
    private router: Router
  ) {}

  selectCategory(category: CommunityCategory): void {
    this.selectedCategory = category;
    this.hubData.category = category as any;
    this.channelOptions = this.buildDefaultChannels(category);
  }

  selectVisibility(visibility: CommunityVisibility): void {
    this.selectedVisibility = visibility;
    this.hubData.visibility = visibility as any;
  }

  nextStep(): void {
    this.errorMessage = '';

    if (this.step === 1 && this.selectedCategory) {
      this.step = 2;
      return;
    }

    if (
      this.step === 2 &&
      this.hubData.name.trim() &&
      this.hubData.description.trim()
    ) {
      this.step = 3;
      return;
    }

    if (this.step === 3 && this.selectedVisibility) {
      this.step = 4;
      return;
    }

    if (this.step === 4) {
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
  }

  createCommunity(): void {
    if (!this.selectedCategory || !this.selectedVisibility) {
      this.errorMessage = 'Please complete all required steps before creating your community.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const hubPayload: Hub = {
      name: this.hubData.name,
      description: this.hubData.description,
      bannerUrl: this.hubData.bannerUrl,
      iconUrl: this.hubData.iconUrl,
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

        this.router.navigate(['/arenatalk/workspace'], {
          state: {
            selectedHub: createdHub,
            createdChannels: createdChannels
          }
        });
      },
      error: (err: unknown) => {
        console.error('Error creating community:', err);
        this.loading = false;
        this.errorMessage = 'Something went wrong while creating the community.';
      }
    });
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