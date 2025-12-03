import axios from 'axios';

const API_BASE_URL = '/api/v1';

export class ChatService {
  static async sendQuestion(question: string): Promise<string> {
    try {
      const response = await axios.get(`${API_BASE_URL}/query`, {
        params: { question }
      });
      return response.data;
    } catch (error) {
      console.error('Error sending question:', error);
      throw new Error('Nie udało się uzyskać odpowiedzi. Spróbuj ponownie.');
    }
  }
}
