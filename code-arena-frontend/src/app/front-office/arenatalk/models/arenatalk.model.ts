export interface Hub {
  id?: number;
  name: string;
  description: string;
  bannerUrl: string;
  iconUrl: string;
  category?: string;
  visibility?: string;
  createdAt?: string;
}

export interface TextChannel {
  id?: number;
  name: string;
  topic: string;
  createdAt?: string;
}

export interface Message {
  id?: number;
  content: string;
  senderName?: string;
  sentAt?: string;
}
export interface MessageReaction {
  messageId: number;
  counts: { [emoji: string]: number };
  reacted: { [emoji: string]: boolean };
}