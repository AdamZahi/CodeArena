// ─── Product Category Enum ───────────────────────────────────────
// Fixed list of categories — admin picks one when creating a product
export enum ProductCategory {
  TSHIRT    = 'TSHIRT',
  HOODIE    = 'HOODIE',
  MUG       = 'MUG',
  STICKER   = 'STICKER',
  CAP       = 'CAP',
  MOUSEPAD  = 'MOUSEPAD',
  KEYBOARD  = 'KEYBOARD',
  ACCESSORY = 'ACCESSORY',
  OTHER     = 'OTHER'
}


// ─── Product Interface ────────────────────────────────────────────
// Describes exactly what a product looks like — mirrors the backend entity
export interface Product {
  id:          string;
  name:        string;
  description: string;
  price:       number;
  stock:       number;
  imageUrl:    string;
  category:    ProductCategory;
}

