import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { ChatSidebar } from './components/ChatSidebar';
import { ChatService } from './services/ChatService';
import { ChatStorage } from './services/ChatStorage';
import { Message, ChatSession, ChatMessageDto } from './types/Message';
import './App.css';

function App() {
  const [chats, setChats] = useState<ChatSession[]>([]);
  const [currentChatId, setCurrentChatId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [streamingMessage, setStreamingMessage] = useState<Message | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const loadedChats = ChatStorage.getAllChats();
    if (loadedChats.length > 0) {
      setChats(loadedChats);
      setCurrentChatId(loadedChats[0].id);
    } else {
      const newChat = ChatStorage.createNewChat();
      setChats([newChat]);
      setCurrentChatId(newChat.id);
      ChatStorage.saveChat(newChat);
    }
  }, []);

  const currentChat = chats.find(c => c.id === currentChatId);
  const messages = currentChat?.messages || [];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingMessage]);

  const simulateStreaming = (text: string, messageId: string, baseMessages: Message[]) => {
    let currentIndex = 0;
    const tempMessage: Message = {
      id: messageId,
      role: 'assistant',
      content: '',
      timestamp: new Date()
    };

    const interval = setInterval(() => {
      if (currentIndex < text.length) {
        const chunkSize = Math.floor(Math.random() * 3) + 1;
        tempMessage.content = text.substring(0, currentIndex + chunkSize);
        setStreamingMessage({ ...tempMessage });
        currentIndex += chunkSize;
      } else {
        clearInterval(interval);
        updateCurrentChat([...baseMessages, tempMessage]);
        setStreamingMessage(null);
        setIsLoading(false);
      }
    }, 30);
  };

  const handleNewChat = () => {
    const newChat = ChatStorage.createNewChat();
    setChats(prev => [newChat, ...prev]);
    setCurrentChatId(newChat.id);
    ChatStorage.saveChat(newChat);
  };

  const handleSelectChat = (chatId: string) => {
    setCurrentChatId(chatId);
  };

  const handleDeleteChat = (chatId: string) => {
    ChatStorage.deleteChat(chatId);
    const updatedChats = chats.filter(c => c.id !== chatId);
    setChats(updatedChats);
    
    if (currentChatId === chatId) {
      if (updatedChats.length > 0) {
        setCurrentChatId(updatedChats[0].id);
      } else {
        const newChat = ChatStorage.createNewChat();
        setChats([newChat]);
        setCurrentChatId(newChat.id);
        ChatStorage.saveChat(newChat);
      }
    }
  };

  const updateCurrentChat = (updatedMessages: Message[]) => {
    if (!currentChatId) return;
    
    const updatedChat: ChatSession = {
      ...currentChat!,
      messages: updatedMessages,
      updatedAt: new Date()
    };
    
    const updatedChatWithTitle = ChatStorage.updateChatTitle(updatedChat);
    
    setChats(prev => prev.map(c => c.id === currentChatId ? updatedChatWithTitle : c));
    ChatStorage.saveChat(updatedChatWithTitle);
  };

  const handleSendMessage = async (content: string) => {
    if (!currentChat) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date()
    };

    const newMessages = [...messages, userMessage];
    updateCurrentChat(newMessages);
    setIsLoading(true);

    try {
      const previousMessagesDto: ChatMessageDto[] = messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }));

      const response = await ChatService.sendQuestion(content, previousMessagesDto);
      
      const assistantMessageId = (Date.now() + 1).toString();
      simulateStreaming(response.answer, assistantMessageId, newMessages);
    } catch (error) {
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Przepraszam, wystąpił błąd. Spróbuj ponownie później.',
        timestamp: new Date()
      };
      updateCurrentChat([...newMessages, errorMessage]);
      setIsLoading(false);
    }
  };

  return (
    <div className="app">
      <ChatSidebar
        chats={chats}
        currentChatId={currentChatId}
        onNewChat={handleNewChat}
        onSelectChat={handleSelectChat}
        onDeleteChat={handleDeleteChat}
      />
      
      <div className="app-main">
        <header className="app-header">
          <div className="header-content">
            <div className="header-title">
              <span className="fire-icon">🔥</span>
              <h1>BHP Expert</h1>
            </div>
            <div className="header-subtitle">
              Asystent przepisów BHP
            </div>
          </div>
        </header>

        <main className="chat-container">
          {messages.length === 0 && !streamingMessage && (
            <div className="welcome-screen">
              <div className="welcome-icon">🔥</div>
              <h2>Witaj w BHP Expert</h2>
              <p>Zadaj pytanie o przepisy BHP i bezpieczeństwo pożarowe</p>
              <div className="example-questions">
                <div className="example-question" onClick={() => handleSendMessage('Jak wygląda znak informujący o atmosferze wybuchowej?')}>
                  Znak atmosfery wybuchowej
                </div>
                <div className="example-question" onClick={() => handleSendMessage('Jakie są graniczne wymiary schodów stałych w budynkach dla różnych typów budynków? Rozrysuj mi tabelkę.')}>
                  Wymiary schodów w budynkach
                </div>
                <div className="example-question" onClick={() => handleSendMessage('Podaj mi wszystkie informacje o stopniach zagrożenia pożarowego lasów.')}>
                  Zagrożenie pożarowe lasów
                </div>
              </div>
            </div>
          )}

          <div className="messages-list">
            {messages.map((message) => (
              <ChatMessage key={message.id} message={message} />
            ))}
            {streamingMessage && (
              <ChatMessage message={streamingMessage} />
            )}
            <div ref={messagesEndRef} />
          </div>
        </main>

        <footer className="chat-footer">
          <ChatInput onSendMessage={handleSendMessage} disabled={isLoading} />
        </footer>
      </div>
    </div>
  );
}

export default App;
