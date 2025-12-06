import { ChatSession } from '../types/Message';

const STORAGE_KEY = 'bhp-expert-chats';

export class ChatStorage {
  static getAllChats(): ChatSession[] {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return [];
    
    const chats = JSON.parse(stored);
    return chats.map((chat: any) => ({
      ...chat,
      createdAt: new Date(chat.createdAt),
      updatedAt: new Date(chat.updatedAt),
      messages: chat.messages.map((msg: any) => ({
        ...msg,
        timestamp: new Date(msg.timestamp)
      }))
    }));
  }

  static saveChat(chat: ChatSession): void {
    const chats = this.getAllChats();
    const index = chats.findIndex(c => c.id === chat.id);
    
    if (index >= 0) {
      chats[index] = chat;
    } else {
      chats.unshift(chat);
    }
    
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chats));
  }

  static deleteChat(chatId: string): void {
    const chats = this.getAllChats().filter(c => c.id !== chatId);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chats));
  }

  static createNewChat(): ChatSession {
    return {
      id: Date.now().toString(),
      title: 'Nowa rozmowa',
      messages: [],
      createdAt: new Date(),
      updatedAt: new Date()
    };
  }

  static updateChatTitle(chat: ChatSession): ChatSession {
    if (chat.messages.length > 0 && chat.title === 'Nowa rozmowa') {
      const firstUserMessage = chat.messages.find(m => m.role === 'user');
      if (firstUserMessage) {
        chat.title = firstUserMessage.content.substring(0, 50) + (firstUserMessage.content.length > 50 ? '...' : '');
      }
    }
    return chat;
  }
}
