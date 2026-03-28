export interface CustomizationItem {
  id: number;
  itemType: 'ICON' | 'BORDER' | 'TITLE' | 'BADGE' | 'BANNER';
  itemKey: string;
  label: string;
  imageUrl?: string;
  rarity: 'COMMON' | 'UNCOMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  description?: string;
  unlockType: 'DEFAULT' | 'LEVEL' | 'XP' | 'ACHIEVEMENT' | 'PURCHASE';
  unlockThreshold: number;
  isDefault: boolean;
}

export interface UserUnlock {
  id: number;
  userId: string;
  itemType: string;
  itemKey: string;
  unlockedAt: string;
  acquisitionSource?: string;
  
  // Joined fields
  label?: string;
  imageUrl?: string;
  rarity?: string;
}

export interface EquipItemRequest {
  itemType: string;
  itemKey: string;
}
