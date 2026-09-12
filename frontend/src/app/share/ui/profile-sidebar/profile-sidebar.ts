import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';

@Component({
  selector: 'app-profile-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './profile-sidebar.html',
  styleUrl: './profile-sidebar.css',
})
export class ProfileSidebar {
  private readonly auth = inject(AuthService);
  readonly user = this.auth.currentUser;
  readonly userInitials = computed(() => {
    const first = (this.user()?.firstName || 'F').trim().charAt(0);
    const last = (this.user()?.lastName || 'G').trim().charAt(0);
    return `${first}${last}`.toUpperCase();
  });
  onLogout(): void { this.auth.logout(); }
}
