import { ProfileSidebar } from '../../../share/ui/profile-sidebar/profile-sidebar';
import { NotificationsService } from '../../notifications/services/notifications.service';
import { Component, effect, inject, signal } from '@angular/core';
import { ProfileService } from '../services/profile.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [ProfileSidebar],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage {
  private readonly notifications = inject(NotificationsService);
  readonly profiles = inject(ProfileService);
  readonly firstName = signal('');
  readonly lastName = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly address = signal('');
  readonly savedNotice = this.profiles.saved;

  constructor() {
    effect(() => {
      const profile = this.profiles.profile();
      this.firstName.set(profile?.nombre ?? '');
      this.lastName.set(profile?.apellido ?? '');
      this.email.set(profile?.correo ?? '');
      this.phone.set(profile?.telefono ?? '');
      this.address.set(profile?.direccion ?? '');
    });
  }

  onSave(event: Event): void {
    event.preventDefault();
    this.profiles.save({ nombre: this.firstName().trim(), apellido: this.lastName().trim(),
      correo: this.email().trim(), telefono: this.phone().trim(), direccion: this.address().trim(),
    }, () => this.notifications.notify('perfil', 'Guardaste los cambios de tu perfil.'));
  }
}
