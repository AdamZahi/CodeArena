export interface Hub {
  id?: number;
  name: string;
  description: string;
  bannerUrl: string;
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
  senderName: string;
  sentAt?: string;
}