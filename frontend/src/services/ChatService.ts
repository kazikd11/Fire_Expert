import axios from 'axios';
import { ChatRequest, ChatResponse, ChatMessageDto } from '../types/Message';

const API_BASE_URL = '/api/v1';

export class ChatService {
  static async sendQuestion(
    question: string, 
    previousMessages: ChatMessageDto[]
  ): Promise<{ answer: string; searchingSourceDocuments: boolean }> {
    try {
      const request: ChatRequest = {
        question,
        previousMessages
      };
      
      const response = await axios.post<ChatResponse>(`${API_BASE_URL}/query`, request);
      return {
        answer: response.data.answer,
        searchingSourceDocuments: response.data.searchingSourceDocuments
      };
    } catch (error) {
      console.error('Error sending question:', error);
      throw new Error('Nie udało się uzyskać odpowiedzi. Spróbuj ponownie.');
    }
  }
}
