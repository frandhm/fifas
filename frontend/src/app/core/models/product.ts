export type ProductCategory = 'local' | 'visitante' | 'tercera' | 'portero' | 'retro';

export const ADULT_SIZES = ['S', 'M', 'L', 'XL', 'XXL'] as const;
export const KIDS_SIZES = ['6', '8', '10', '12', '14'] as const;

export interface Product {
  id: string;
  name: string;
  price: number;
  accent: string;
  stripe: string;
  category: ProductCategory;
  description: string;
  sizes: string[];
}
