import { Injectable, signal } from '@angular/core';

export interface TransactionItem {
  name: string;
  quantity: number;
  size: string;
  customName?: string;
  customDorsal?: number;
  price: number;
}

export interface Transaction {
  id: string;
  date: string;
  items: TransactionItem[];
  total: number;
  status: 'Entregado' | 'En camino' | 'Procesando' | 'Cancelado';
  paymentMethod: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionsService {
  private readonly transactions = signal<Transaction[]>([
    {
      id: 'FIF-94821',
      date: '02 Sep 2026',
      items: [
        {
          name: 'Camiseta Selección Chile Local 2026',
          quantity: 1,
          size: 'L',
          customName: 'SANCHEZ',
          customDorsal: 7,
          price: 59990,
        },
        {
          name: 'Camiseta Real Madrid Local 2026',
          quantity: 1,
          size: 'M',
          price: 64990,
        },
      ],
      total: 124980,
      status: 'Entregado',
      paymentMethod: 'Tarjeta de Crédito (**** 4821)',
    },
    {
      id: 'FIF-88310',
      date: '18 Ago 2026',
      items: [
        {
          name: 'Camiseta Argentina 3 Estrellas',
          quantity: 1,
          size: 'M',
          customName: 'MESSI',
          customDorsal: 10,
          price: 62990,
        },
      ],
      total: 62990,
      status: 'Entregado',
      paymentMethod: 'Webpay / Débito',
    },
    {
      id: 'FIF-99104',
      date: '04 Sep 2026',
      items: [
        {
          name: 'Camiseta Japón Edición Especial Anime',
          quantity: 1,
          size: 'L',
          customName: 'ISAGI',
          customDorsal: 11,
          price: 69990,
        },
      ],
      total: 69990,
      status: 'En camino',
      paymentMethod: 'PayPal',
    },
  ]);

  readonly userTransactions = this.transactions.asReadonly();
}
