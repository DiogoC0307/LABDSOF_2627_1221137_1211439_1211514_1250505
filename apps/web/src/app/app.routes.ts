import { Routes } from '@angular/router';
import { roleGuard } from './auth/role.guard';
import { MemberForm } from './members/member-form';
import { MembersList } from './members/members-list';
import { Schedule } from './schedule/schedule';

export const routes: Routes = [
  { path: '', component: Schedule },
  { path: 'members', component: MembersList, canActivate: [roleGuard('SUPERVISOR', 'ADMIN')] },
  { path: 'members/new', component: MemberForm, canActivate: [roleGuard('ADMIN')] },
  { path: 'members/:id/edit', component: MemberForm, canActivate: [roleGuard('ADMIN')] },
  { path: '**', redirectTo: '' },
];
