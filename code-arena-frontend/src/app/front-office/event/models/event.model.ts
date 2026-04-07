export interface ProgrammingEvent {
  id: string;
  title: string;
  description: string;
  location?: string;
  organizerId: string;
  status: string;
  startDate: string;
  endDate: string;
  maxParticipants: number;
  currentParticipants: number;
  type: 'OPEN' | 'EXCLUSIVE';
  category:
    | 'HACKATHON'
    | 'NETWORKING'
    | 'BOOTCAMP'
    | 'CONFERENCE'
    | 'REMISE_PRIX';
  createdAt: string;
  availablePlaces: number;
  isFull: boolean;
  fillRate: number;
}

export interface EventRegistration {
  id: string;
  participantId: string;
  eventId: string;
  status: 'CONFIRMED' | 'WAITLIST' | 'CANCELLED';
  qrCode: string;
  registeredAt: string;
}

export interface EventInvitation {
  id: string;
  eventId: string;
  participantId: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  sentAt: string;
  respondedAt: string;
}

export interface EventCandidature {
  id: string;
  eventId: string;
  participantId: string;
  motivation: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  appliedAt: string;
}
