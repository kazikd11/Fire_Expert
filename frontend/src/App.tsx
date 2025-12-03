import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { ChatService } from './services/ChatService';
import { Message } from './types/Message';
import './App.css';

function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [streamingMessage, setStreamingMessage] = useState<Message | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingMessage]);

  const simulateStreaming = (text: string, messageId: string) => {
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
        setMessages(prev => [...prev, tempMessage]);
        setStreamingMessage(null);
        setIsLoading(false);
      }
    }, 30);
  };

  const handleSendMessage = async (content: string) => {
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);

    try {
      const response = await ChatService.sendQuestion(content);
      const assistantMessageId = (Date.now() + 1).toString();
      simulateStreaming(response, assistantMessageId);
    } catch (error) {
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Przepraszam, wystąpił błąd. Spróbuj ponownie później.',
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
      setIsLoading(false);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <div className="header-title">
            <span className="fire-icon">🔥</span>
            <h1>Fire Expert AI</h1>
          </div>
          <div className="header-subtitle">
            Twój asystent ds. bezpieczeństwa pożarowego
          </div>
        </div>
      </header>

      <main className="chat-container">
        {messages.length === 0 && !streamingMessage && (
          <div className="welcome-screen">
            <div className="welcome-icon">🔥</div>
            <h2>Witaj w Fire Expert AI</h2>
            <p>Zadaj mi pytanie o bezpieczeństwo pożarowe, przepisy lub procedury.</p>
            <div className="example-questions">
              <div className="example-question" onClick={() => handleSendMessage('Jakie są wymagania dotyczące gaśnic w budynkach użyteczności publicznej?')}>
                Wymagania dotyczące gaśnic
              </div>
              <div className="example-question" onClick={() => handleSendMessage('Co to jest klasa odporności pożarowej?')}>
                Klasa odporności pożarowej
              </div>
              <div className="example-question" onClick={() => handleSendMessage('Jakie są procedury ewakuacyjne w przypadku pożaru?')}>
                Procedury ewakuacyjne
              </div>
            </div>
          </div>
        )}

        <div className="messages-list">
          {messages.map((message) => (
            <ChatMessage key={message.id} message={message} />
          ))}
          {streamingMessage && (
            <ChatMessage message={streamingMessage} isStreaming={true} />
          )}
          <div ref={messagesEndRef} />
        </div>
      </main>

      <footer className="chat-footer">
        <ChatInput onSendMessage={handleSendMessage} disabled={isLoading} />
      </footer>
    </div>
  );
}

export default App;
