export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

export interface ChatMessageDto {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequest {
  question: string;
  previousMessages: ChatMessageDto[];
}

export interface ChatResponse {
  answer: string;
  status: string;
  searchingSourceDocuments: boolean;
}
