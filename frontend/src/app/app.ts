import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface ChecklistItem {
  id: string;
  label: string;
  done: boolean;
  owner: string;
}

interface MedicationReview {
  status: string;
  openInteractions: number;
  lastReviewedBy: string;
  nextAction: string;
}

interface LabSignal {
  status: string;
  pendingResults: number;
  criticalFlag: boolean;
  nextReview: string;
}

interface ClinicalNote {
  id: string;
  author: string;
  body: string;
  createdAt: string;
}

interface CareEvent {
  id: string;
  caseId: string;
  type: string;
  summary: string;
  source: string;
  createdAt: string;
}

interface CareCase {
  id: string;
  patientCode: string;
  ward: string;
  pathway: string;
  priority: 'Critical' | 'High' | 'Medium' | 'Low';
  slaMinutes: number;
  nextAction: string;
  owner: string;
  status: string;
  readinessScore: number;
  riskFlags: string[];
  checklist: ChecklistItem[];
  medicationReview: MedicationReview;
  labSignal: LabSignal;
  notes: ClinicalNote[];
  timeline: CareEvent[];
  updatedAt: string;
}

interface PlatformStatus {
  status: string;
  openCases: number;
  highPriorityCases: number;
  pendingLabResults: number;
  eventStream: string;
  deployment: string;
  generatedAt: string;
}

const fallbackCases: CareCase[] = [
  {
    id: 'case-discharge-1024',
    patientCode: 'CF-1024',
    ward: 'Cardiology B',
    pathway: 'Discharge readiness',
    priority: 'High',
    slaMinutes: 42,
    nextAction: 'Medication reconciliation before discharge order',
    owner: 'Dr. Lena Hoffmann',
    status: 'Awaiting clinical review',
    readinessScore: 76,
    riskFlags: ['pending labs', 'medication interaction', 'home-care slot'],
    checklist: [
      { id: 'task-medication', label: 'Medication reconciliation completed', done: false, owner: 'Pharmacy' },
      { id: 'task-labs', label: 'Final troponin result reviewed', done: false, owner: 'Lab' },
      { id: 'task-care', label: 'Home-care appointment confirmed', done: true, owner: 'Care coordination' },
      { id: 'task-summary', label: 'Discharge summary drafted', done: true, owner: 'Resident physician' }
    ],
    medicationReview: {
      status: 'Needs review',
      openInteractions: 2,
      lastReviewedBy: 'Pharmacy Team',
      nextAction: 'Resolve ACE inhibitor interaction'
    },
    labSignal: { status: 'Pending', pendingResults: 1, criticalFlag: false, nextReview: 'Review final troponin before 14:00' },
    notes: [
      {
        id: 'note-fallback',
        author: 'Care coordination',
        body: 'Family pickup confirmed. Waiting for medication reconciliation and final lab result.',
        createdAt: new Date().toISOString()
      }
    ],
    timeline: [
      {
        id: 'event-fallback',
        caseId: 'case-discharge-1024',
        type: 'KAFKA_EVENT',
        summary: 'Discharge readiness event published',
        source: 'clinical-events',
        createdAt: new Date().toISOString()
      }
    ],
    updatedAt: new Date().toISOString()
  }
];

const fallbackStatus: PlatformStatus = {
  status: 'fallback',
  openCases: fallbackCases.length,
  highPriorityCases: 1,
  pendingLabResults: 1,
  eventStream: 'preview stream',
  deployment: 'stage-ready fallback',
  generatedAt: new Date().toISOString()
};

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  readonly apiState = signal<'online' | 'fallback'>('fallback');
  readonly cases = signal<CareCase[]>(fallbackCases);
  readonly status = signal<PlatformStatus>(fallbackStatus);
  readonly selectedCaseId = signal('case-discharge-1024');
  readonly wardFilter = signal('All wards');
  readonly priorityFilter = signal('All');
  readonly searchTerm = signal('');
  readonly noteBody = signal('Medication reconciliation requested from pharmacy. Discharge summary remains blocked until final lab review is confirmed.');
  readonly noteAuthor = signal('Sascha D.');
  readonly savingNote = signal(false);
  readonly saveError = signal('');
  readonly lastSavedAt = signal('');

  readonly wards = computed(() => ['All wards', ...new Set(this.cases().map((careCase) => careCase.ward))]);
  readonly priorities = computed(() => ['All', 'Critical', 'High', 'Medium', 'Low']);
  readonly filteredCases = computed(() => {
    const ward = this.wardFilter();
    const priority = this.priorityFilter();
    const search = this.searchTerm().trim().toLowerCase();

    return this.cases().filter((careCase) => {
      const matchesWard = ward === 'All wards' || careCase.ward === ward;
      const matchesPriority = priority === 'All' || careCase.priority === priority;
      const matchesSearch = !search || [
        careCase.patientCode,
        careCase.ward,
        careCase.pathway,
        careCase.nextAction,
        careCase.owner,
        careCase.status
      ].some((value) => value.toLowerCase().includes(search));

      return matchesWard && matchesPriority && matchesSearch;
    });
  });
  readonly selectedCase = computed(() => this.cases().find((careCase) => careCase.id === this.selectedCaseId()) ?? this.cases()[0]);
  readonly completion = computed(() => {
    const careCase = this.selectedCase();
    if (!careCase || careCase.checklist.length === 0) {
      return 0;
    }

    const completed = careCase.checklist.filter((item) => item.done).length;
    return Math.round((completed / careCase.checklist.length) * 100);
  });
  readonly completedChecklistItems = computed(() => this.selectedCase()?.checklist.filter((item) => item.done).length ?? 0);
  readonly totalChecklistItems = computed(() => this.selectedCase()?.checklist.length ?? 0);
  readonly unresolvedChecklistItems = computed(() => this.totalChecklistItems() - this.completedChecklistItems());
  readonly riskCount = computed(() => this.selectedCase()?.riskFlags.length ?? 0);
  readonly activeEvents = computed(() => this.selectedCase()?.timeline.slice(0, 5) ?? []);

  ngOnInit(): void {
    void this.load();
  }

  selectCase(caseId: string): void {
    this.selectedCaseId.set(caseId);
  }

  setPriority(priority: string): void {
    this.priorityFilter.set(priority);
  }

  async addNote(): Promise<void> {
    const careCase = this.selectedCase();
    const body = this.noteBody().trim();
    const author = this.noteAuthor().trim();
    if (!careCase || !body || !author || this.savingNote()) {
      return;
    }

    this.savingNote.set(true);
    this.saveError.set('');
    try {
      await this.request<ClinicalNote>(`/api/cases/${careCase.id}/notes`, {
        method: 'POST',
        body: JSON.stringify({ author, body })
      });
      const updatedCase = await this.request<CareCase>(`/api/cases/${careCase.id}`);
      this.cases.update((current) => current.map((item) => (item.id === updatedCase.id ? updatedCase : item)));
      this.noteBody.set('');
      this.lastSavedAt.set(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
      this.apiState.set('online');
    } catch {
      this.apiState.set('fallback');
      this.saveError.set('Note could not be synced. The workspace stayed available.');
    } finally {
      this.savingNote.set(false);
    }
  }

  priorityClass(priority: string): string {
    return priority.toLowerCase();
  }

  priorityCount(priority: string): number {
    if (priority === 'All') {
      return this.cases().length;
    }

    return this.cases().filter((careCase) => careCase.priority === priority).length;
  }

  formatUpdated(value: string): string {
    return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  trackById(_index: number, item: { id: string }): string {
    return item.id;
  }

  private async load(): Promise<void> {
    try {
      const [status, cases] = await Promise.all([
        this.request<PlatformStatus>('/api/status'),
        this.request<CareCase[]>('/api/cases')
      ]);
      this.status.set(status);
      this.cases.set(cases);
      this.selectedCaseId.set(cases.find((careCase) => careCase.id === 'case-discharge-1024')?.id ?? cases[0]?.id ?? 'case-discharge-1024');
      this.apiState.set('online');
    } catch {
      this.apiState.set('fallback');
    }
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      }
    });

    if (!response.ok) {
      throw new Error(`API request failed: ${response.status}`);
    }

    return (await response.json()) as T;
  }
}
