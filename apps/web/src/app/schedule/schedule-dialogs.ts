import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogConfig,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';

// Schedule page dialogs
export function scheduleDialogConfig<D>(data: D, role: 'dialog' | 'alertdialog'): MatDialogConfig<D> {
  return {
    data,
    role,
    maxWidth: '480px',
    width: 'calc(100vw - 2rem)',
    autoFocus: 'first-tabbable',
    restoreFocus: true,
    panelClass: ['schedule-theme', 'schedule-dialog'],
  };
}

export interface DeleteShiftData {
  day: string;
  time: string;
  people: number;
}

/** Delete shift confirmation dialog */
@Component({
  selector: 'app-delete-shift-dialog',
  imports: [MatButton, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Delete this shift?</h2>
    <mat-dialog-content>
      <p>
        {{ data.day }}, {{ data.time }}.
        @if (data.people > 0) {
          The {{ data.people }} {{ data.people === 1 ? 'person' : 'people' }} on it will be taken off.
        }
        Members only see this after you publish.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-stroked-button type="button" class="dialog-secondary" [mat-dialog-close]="false">Keep shift</button>
      <button mat-flat-button type="button" class="dialog-destructive" [mat-dialog-close]="true">Delete shift</button>
    </mat-dialog-actions>
  `,
})
export class DeleteShiftDialog {
  protected readonly data = inject<DeleteShiftData>(MAT_DIALOG_DATA);
}

export interface PublishData {
  range: string;
  changes: number;
}

/** Publish schedule confirmation dialog */
@Component({
  selector: 'app-publish-dialog',
  imports: [MatButton, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Publish this week's schedule?</h2>
    <mat-dialog-content>
      <p>
        {{ data.range }} · {{ data.changes }} unpublished {{ data.changes === 1 ? 'change' : 'changes' }}. Members will
        see the new shifts and who is on them.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-stroked-button type="button" class="dialog-secondary" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">Publish</button>
    </mat-dialog-actions>
  `,
})
export class PublishDialog {
  protected readonly data = inject<PublishData>(MAT_DIALOG_DATA);
}

// One conflict as the dialog lists it: which shift, what is wrong, and the draft shift to show
export interface ConflictLine {
  shiftKey: string;
  shift: string; // "Wednesday 2 December, 08:00 – 20:00"
  problem: string; // "Ana Silva (Crew chief) is on another shift at the same time."
}

export interface ConflictsData {
  lines: ConflictLine[];
}

/**
 * Weekly schedule publication conflicts dialog.
 * 
 * List all conflicts that prevent the schedule from being published, with a button to show each conflicting shift.
 */
@Component({
  selector: 'app-conflicts-dialog',
  imports: [MatButton, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>The schedule has conflicts</h2>
    <mat-dialog-content>
      <ul class="conflict-list">
        @for (line of data.lines; track $index) {
          <li class="conflict">
            <span class="conflict-text">
              <strong>{{ line.shift }}</strong>
              <span>{{ line.problem }}</span>
            </span>
            <button mat-stroked-button type="button" class="dialog-secondary" [mat-dialog-close]="line.shiftKey">
              Show shift
            </button>
          </li>
        }
      </ul>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-flat-button type="button" [mat-dialog-close]="null">Go back and fix</button>
    </mat-dialog-actions>
  `,
})
export class ConflictsDialog {
  protected readonly data = inject<ConflictsData>(MAT_DIALOG_DATA);
}
