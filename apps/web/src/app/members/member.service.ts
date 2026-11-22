import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';

export const SERVICE_FUNCTIONS = ['CREW_CHIEF', 'DRIVER', 'CREW_MEMBER'] as const;
export const RANKS = [
  'CADET',
  'TRAINEE',
  'FIREFIGHTER_3RD_CLASS',
  'FIREFIGHTER_2ND_CLASS',
  'FIREFIGHTER_1ST_CLASS',
  'SUB_CHIEF',
  'CHIEF',
  'OFFICER',
  'COMMANDER',
] as const;
export const ROSTER_STATUSES = ['COMMAND_ROSTER', 'ACTIVE_ROSTER', 'RESERVE_ROSTER', 'HONORARY_ROSTER'] as const;
export const LICENCE_CLASSES = ['B', 'C1', 'C', 'C1E', 'CE'] as const;

export interface Licence {
  licenceClass: string;
  expiresOn: string;
}

export interface Certification {
  type: string;
  name?: string;
  issuedOn: string;
  expiresOn: string;
}

export interface Member {
  id: string;
  fullName: string;
  accessRole: string;
  active: boolean;
  rank: string | null;
  rosterStatus: string;
  withinAgeLimit: boolean;
  /** Only for the admin and the member themself (09). */
  contact?: string;
  functions: string[];
  licences: Licence[];
  certifications: Certification[];
}

export interface CertificationType {
  code: string;
  name: string;
}

export interface MemberDetails {
  fullName: string;
  rank: string;
  rosterStatus: string;
  withinAgeLimit: boolean;
  contact: string | null;
  functions: string[];
}

export interface Qualifications {
  licences: Licence[];
  certifications: Certification[];
}

/** An invalid field as the API answers it: the field and a code, never the value (SCRUM-77). */
export interface InvalidField {
  field: string;
  code: string;
}

/** The members API (SCRUM-77). */
@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);

  list(): Observable<Member[]> {
    return this.http.get<Member[]>('/api/v1/members');
  }

  get(id: string): Observable<Member> {
    return this.http.get<Member>(`/api/v1/members/${id}`);
  }

  certificationTypes(): Observable<CertificationType[]> {
    return this.http.get<CertificationType[]>('/api/v1/certification-types');
  }

  register(details: MemberDetails, qualifications: Qualifications): Observable<Member> {
    return this.http.post<Member>('/api/v1/members', { ...details, ...qualifications });
  }

  /** Saves the details, then the licences and certifications. */
  update(id: string, details: MemberDetails, qualifications: Qualifications): Observable<Member> {
    return this.http
      .put<Member>(`/api/v1/members/${id}`, details)
      .pipe(switchMap(() => this.http.put<Member>(`/api/v1/members/${id}/certifications`, qualifications)));
  }
}
