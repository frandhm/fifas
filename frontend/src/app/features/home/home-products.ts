export interface HomeProduct {
  id: string;
  name: string;
  price: string;
  accent: string;
  stripe: string;
}

export const HOME_PRODUCTS: HomeProduct[] = [
  {
    id: 'local-2026',
    name: 'Camiseta local Los FIFAS 2026',
    price: '$200.00',
    accent: '#1e4fa3',
    stripe: '#f4f7fb',
  },
  {
    id: 'visitante-2026',
    name: 'Camiseta visitante 2026',
    price: '$200.00',
    accent: '#0b1730',
    stripe: '#d4af37',
  },
  {
    id: 'tercera-roja',
    name: 'Tercera equipación roja',
    price: '$190.00',
    accent: '#8b1e2d',
    stripe: '#f1d5c0',
  },
  {
    id: 'portero',
    name: 'Camiseta de portero',
    price: '$180.00',
    accent: '#15803d',
    stripe: '#0b1730',
  },
  {
    id: 'retro',
    name: 'Edición retro 98',
    price: '$220.00',
    accent: '#163a7a',
    stripe: '#e8c36a',
  },
];
