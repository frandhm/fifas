export type ProductCategory = 'local' | 'visitante' | 'tercera' | 'portero' | 'retro';

export interface Product {
  id: string;
  name: string;
  price: number;
  accent: string;
  stripe: string;
  category: ProductCategory;
  description: string;
}
