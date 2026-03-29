import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomizationItem, UserUnlock } from '../../../../../../core/models/customization.model';
import { ProfileCustomizationService } from '../../../../../../core/services/profile-customization.service';
import { AuthUserSyncService } from '../../../../../../core/auth/auth-user-sync.service';

@Component({
  selector: 'app-customize-identity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customize-identity.component.html',
  styleUrls: ['./customize-identity.component.css']
})
export class CustomizeIdentityComponent implements OnInit {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();

  private readonly customizationService = inject(ProfileCustomizationService);
  private readonly authUserSync = inject(AuthUserSyncService);

  readonly currentUser$ = this.authUserSync.currentUser$;

  tabs = ['ICONS', 'BORDERS', 'TITLES', 'BADGES'];
  activeTab = 'ICONS';

  catalogItems: CustomizationItem[] = [];
  userUnlocks: UserUnlock[] = [];

  // Used to preview currently viewing
  previewIcon = 'icon_default';
  previewBorder = 'border_default';
  previewTitle = 'Newbie';
  previewBadges: string[] = [];

  isLoading = true;
  saving = false;

  ngOnInit() {
    this.refreshData();
    this.customizationService.syncUnlocks().subscribe(); // Sync on open silently
    
    this.currentUser$.subscribe((user: any) => {
      if (user) {
        this.previewIcon = user.activeIconId || 'icon_default';
        this.previewBorder = user.activeBorderId || 'border_default';
        this.previewTitle = user.activeTitle || 'Newbie';
        this.previewBadges = [user.activeBadge1, user.activeBadge2, user.activeBadge3].filter(b => !!b);
      }
    });
  }

  refreshData() {
    this.isLoading = true;
    
    // Switch to singular to match type mapping
    const typeMap: any = {
      'ICONS': 'ICON',
      'BORDERS': 'BORDER',
      'TITLES': 'TITLE',
      'BADGES': 'BADGE'
    };
    
    const dbType = typeMap[this.activeTab];

    this.customizationService.getCatalogItemsByType(dbType).subscribe((catalog: CustomizationItem[]) => {
      this.catalogItems = catalog;
      
      this.customizationService.getMyUnlocksByType(dbType).subscribe((unlocks: UserUnlock[]) => {
        this.userUnlocks = unlocks;
        this.isLoading = false;
      });
    });
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.refreshData();
  }

  closeModal() {
    this.close.emit();
  }

  isUnlocked(itemKey: string): boolean {
    return this.userUnlocks.some(u => u.itemKey === itemKey);
  }

  selectItem(item: CustomizationItem) {
    if (!this.isUnlocked(item.itemKey)) return;

    if (this.activeTab === 'ICONS') this.previewIcon = item.itemKey;
    if (this.activeTab === 'BORDERS') this.previewBorder = item.itemKey;
    if (this.activeTab === 'TITLES') this.previewTitle = item.itemKey;
    
    // For badges, add to list if not already there (up to 3)
    if (this.activeTab === 'BADGES') {
      if (!this.previewBadges.includes(item.itemKey)) {
        if (this.previewBadges.length >= 3) this.previewBadges.shift();
        this.previewBadges.push(item.itemKey);
      }
    }

    this.equipSelected(item.itemType, item.itemKey);
  }

  equipSelected(itemType: string, itemKey: string) {
    this.saving = true;
    this.customizationService.equipItem(itemType, itemKey).subscribe({
      next: () => {
        this.saving = false;
        // Trigger a user refresh so the backend changes sync to frontend cache
        setTimeout(() => this.authUserSync.forceSync(), 500); 
      },
      error: () => this.saving = false
    });
  }
}
