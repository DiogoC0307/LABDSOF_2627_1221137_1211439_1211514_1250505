import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CertificationType,
  InvalidField,
  LICENCE_CLASSES,
  MemberDetails,
  MemberService,
  Qualifications,
  RANKS,
  ROSTER_STATUSES,
  SERVICE_FUNCTIONS,
} from './member.service';

interface LicenceRow {
  licenceClass: string;
  expiresOn: string;
}

interface CertificationRow {
  type: string;
  issuedOn: string;
  expiresOn: string;
}

const MESSAGES: Record<string, string> = {
  required: 'is required.',
  too_long: 'is too long.',
  duplicate: 'appears twice.',
  unknown_certification_type: 'is an unknown certification type.',
  expires_before_issued: 'The certification expires before it was issued.',
  driver_without_licence: 'A driver needs a driving licence.',
  not_saved: 'The member was not saved. Try again.',
};

/** Codes whose message names the field it is about. */
const PER_FIELD = new Set(['required', 'too_long', 'duplicate', 'unknown_certification_type']);

/**
 * Register a member, or edit one, as the admin (SCRUM-77): details, functions, licences and certifications. The
 * form checks what it can before sending; the API checks everything again and its errors are shown the same way.
 */
@Component({
  selector: 'app-member-form',
  imports: [FormsModule],
  templateUrl: './member-form.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class MemberForm implements OnInit {
  private readonly members = inject(MemberService);
  private readonly router = inject(Router);
  private readonly memberId = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly ranks = RANKS;
  protected readonly rosterStatuses = ROSTER_STATUSES;
  protected readonly licenceClasses = LICENCE_CLASSES;
  protected readonly serviceFunctions = SERVICE_FUNCTIONS;
  protected readonly editing = this.memberId !== null;

  protected readonly types = signal<CertificationType[]>([]);
  protected readonly errors = signal<InvalidField[]>([]);
  protected readonly loadError = signal<string | null>(null);

  protected fullName = '';
  protected rank: string = 'FIREFIGHTER_3RD_CLASS';
  protected rosterStatus: string = 'ACTIVE_ROSTER';
  protected withinAgeLimit = true;
  protected contact = '';
  protected functions: Record<string, boolean> = { CREW_CHIEF: false, DRIVER: false, CREW_MEMBER: false };
  protected licences: LicenceRow[] = [];
  protected certifications: CertificationRow[] = [];

  ngOnInit(): void {
    this.members.certificationTypes().subscribe({
      next: (types) => this.types.set(types),
      error: () => this.loadError.set('Could not load the certification types.'),
    });
    if (this.memberId) {
      this.members.get(this.memberId).subscribe({
        next: (member) => {
          this.fullName = member.fullName;
          this.rank = member.rank ?? 'FIREFIGHTER_3RD_CLASS';
          this.rosterStatus = member.rosterStatus;
          this.withinAgeLimit = member.withinAgeLimit;
          this.contact = member.contact ?? '';
          SERVICE_FUNCTIONS.forEach((fn) => (this.functions[fn] = member.functions.includes(fn)));
          this.licences = member.licences.map((licence) => ({ ...licence }));
          this.certifications = member.certifications.map(({ type, issuedOn, expiresOn }) => ({ type, issuedOn, expiresOn }));
        },
        error: () => this.loadError.set('Could not load the member.'),
      });
    }
  }

  protected addLicence(): void {
    this.licences.push({ licenceClass: 'B', expiresOn: '' });
  }

  protected removeLicence(index: number): void {
    this.licences.splice(index, 1);
  }

  protected addCertification(): void {
    this.certifications.push({ type: this.types()[0]?.code ?? 'TAS', issuedOn: '', expiresOn: '' });
  }

  protected removeCertification(index: number): void {
    this.certifications.splice(index, 1);
  }

  protected message(error: InvalidField): string {
    const text = MESSAGES[error.code] ?? 'is not valid.';
    return PER_FIELD.has(error.code) ? `${error.field} ${text}` : text;
  }

  protected save(): void {
    const errors = this.check();
    this.errors.set(errors);
    if (errors.length > 0) {
      return;
    }
    const details: MemberDetails = {
      fullName: this.fullName.trim(),
      rank: this.rank,
      rosterStatus: this.rosterStatus,
      withinAgeLimit: this.withinAgeLimit,
      contact: this.contact.trim() === '' ? null : this.contact.trim(),
      functions: SERVICE_FUNCTIONS.filter((fn) => this.functions[fn]),
    };
    const qualifications: Qualifications = {
      licences: this.licences.map(({ licenceClass, expiresOn }) => ({ licenceClass, expiresOn })),
      certifications: this.certifications.map(({ type, issuedOn, expiresOn }) => ({ type, issuedOn, expiresOn })),
    };
    const request = this.memberId
      ? this.members.update(this.memberId, details, qualifications)
      : this.members.register(details, qualifications);
    request.subscribe({
      next: () => void this.router.navigateByUrl('/members'),
      error: (error: unknown) => this.errors.set(this.apiErrors(error)),
    });
  }

  /** What the form can check before sending: the API checks the same and more. */
  private check(): InvalidField[] {
    const errors: InvalidField[] = [];
    if (this.fullName.trim() === '') {
      errors.push({ field: 'fullName', code: 'required' });
    }
    this.licences.forEach((licence, i) => {
      if (licence.expiresOn === '') {
        errors.push({ field: `licences[${i}].expiresOn`, code: 'required' });
      }
    });
    this.certifications.forEach((certification, i) => {
      if (certification.issuedOn === '') {
        errors.push({ field: `certifications[${i}].issuedOn`, code: 'required' });
      }
      if (certification.expiresOn === '') {
        errors.push({ field: `certifications[${i}].expiresOn`, code: 'required' });
      } else if (certification.issuedOn !== '' && certification.expiresOn < certification.issuedOn) {
        errors.push({ field: `certifications[${i}].expiresOn`, code: 'expires_before_issued' });
      }
    });
    if (this.functions['DRIVER'] && this.licences.length === 0) {
      errors.push({ field: 'licences', code: 'driver_without_licence' });
    }
    return errors;
  }

  private apiErrors(error: unknown): InvalidField[] {
    const body = error instanceof HttpErrorResponse ? (error.error as { errors?: InvalidField[] } | null) : null;
    if (body?.errors && body.errors.length > 0) {
      return body.errors;
    }
    return [{ field: 'member', code: 'not_saved' }];
  }
}
