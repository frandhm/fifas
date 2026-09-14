import { Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfileSidebar } from '../../../share/ui/profile-sidebar/profile-sidebar';
import { AuthService } from '../../auth/services/auth.service';
import { SupportCategory, SupportService, SupportStatus } from '../services/support.service';

@Component({
  selector: 'app-support-page',
  imports: [ProfileSidebar, ReactiveFormsModule, DatePipe],
  templateUrl: './support-page.html',
  styleUrl: './support-page.css',
})
export class SupportPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly support = inject(SupportService);
  readonly sentId = signal<number | null>(null);
  readonly email = computed(() => this.auth.currentUser()?.email ?? '');
  readonly categories: { value: SupportCategory; label: string }[] = [
    { value: 'pedido', label: 'Pedido o despacho' },
    { value: 'producto', label: 'Producto o personalización' },
    { value: 'pago', label: 'Pago' },
    { value: 'cuenta', label: 'Mi cuenta' },
    { value: 'otro', label: 'Otro' },
  ];
  readonly statusLabels: Record<SupportStatus, string> = {
    abierto: 'Abierto', en_proceso: 'En proceso', cerrado: 'Cerrado',
  };
  readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    asunto: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(120)]],
    categoria: ['pedido' as SupportCategory, Validators.required],
    mensaje: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
  });

  constructor() {
    effect(() => {
      const email = this.email();
      if (email && !this.form.controls.correo.dirty) this.form.controls.correo.setValue(email);
    });
    this.support.refresh();
  }

  submit(): void {
    this.sentId.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.support.create(this.form.getRawValue(), created => {
      this.sentId.set(created.id);
      this.form.reset({ correo: this.email(), asunto: '', categoria: 'pedido', mensaje: '' });
    });
  }
}
